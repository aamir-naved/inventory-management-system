use std::fs;
use std::io::{Read, Write};
use std::net::{SocketAddr, TcpStream, ToSocketAddrs};
use std::path::{Path, PathBuf};
use std::process::{Child, Command, Stdio};
use std::sync::Mutex;
use std::thread;
use std::time::{Duration, Instant};

use tauri::{AppHandle, Manager};

const UI_PORT: u16 = 18080;
const PG_PORT: u16 = 54329;
const DB_NAME: &str = "inventory_management";
const DB_USER: &str = "inventory_user";

struct Runtime {
    postgres: Option<Child>,
    backend: Option<Child>,
    pg_ctl: Option<PathBuf>,
    pg_data: Option<PathBuf>,
}

static RUNTIME: Mutex<Runtime> = Mutex::new(Runtime {
    postgres: None,
    backend: None,
    pg_ctl: None,
    pg_data: None,
});

pub fn boot(app: AppHandle) {
    thread::spawn(move || {
        if let Err(error) = boot_inner(&app) {
            let _ = set_status(&app, &error);
        }
    });
}

pub fn shutdown() {
    let mut runtime = RUNTIME.lock().expect("desktop runtime lock");
    if let Some(mut child) = runtime.backend.take() {
        let _ = child.kill();
        let _ = child.wait();
    }
    if let (Some(pg_ctl), Some(pg_data)) = (runtime.pg_ctl.clone(), runtime.pg_data.clone()) {
        let mut stop = Command::new(&pg_ctl);
        hide_window(&mut stop);
        let _ = stop
            .args(["-D", &pg_data.to_string_lossy(), "stop", "-m", "fast"])
            .status();
    }
    if let Some(mut child) = runtime.postgres.take() {
        let _ = child.kill();
        let _ = child.wait();
    }
}

fn boot_inner(app: &AppHandle) -> Result<(), String> {
    if health_ok() {
        return navigate(app);
    }

    let data_dir = data_dir();
    fs::create_dir_all(&data_dir).map_err(|e| format!("Unable to create data folder: {e}"))?;
    let logs_dir = data_dir.join("logs");
    fs::create_dir_all(&logs_dir).map_err(|e| format!("Unable to create log folder: {e}"))?;

    let skip_postgres = env_flag("IMS_SKIP_POSTGRES");
    if !skip_postgres {
        start_postgres(app, &data_dir, &logs_dir)?;
    } else {
        let _ = set_status(app, "Using an already-running database…");
    }

    start_backend(app, &data_dir, &logs_dir)?;
    wait_for_health(Duration::from_secs(90))?;
    navigate(app)
}

fn start_postgres(app: &AppHandle, data_dir: &Path, logs_dir: &Path) -> Result<(), String> {
    let pg_bin = find_pg_bin(app).ok_or_else(|| {
        "PostgreSQL was not bundled. For local development set IMS_SKIP_POSTGRES=1 and run Docker Postgres, or set IMS_PG_BIN.".to_string()
    })?;
    let pg_ctl = pg_bin.join(exe("pg_ctl"));
    let initdb = pg_bin.join(exe("initdb"));
    let createdb = pg_bin.join(exe("createdb"));
    let pg_data = data_dir.join("pgdata");
    let password = read_or_create_password(data_dir)?;

    if !pg_data.join("PG_VERSION").exists() {
        let _ = set_status(app, "Preparing the local database…");
        let pwfile = data_dir.join("pg.password");
        fs::write(&pwfile, &password).map_err(|e| format!("Unable to write database password: {e}"))?;
        let mut cmd = Command::new(&initdb);
        hide_window(&mut cmd);
        let status = cmd
            .args([
                "-D",
                &pg_data.to_string_lossy(),
                "-U",
                DB_USER,
                "--pwfile",
                &pwfile.to_string_lossy(),
                "--auth=scram-sha-256",
                "--encoding=UTF8",
                "--no-locale",
            ])
            .status()
            .map_err(|e| format!("initdb failed to start: {e}"))?;
        if !status.success() {
            return Err("initdb failed. Delete the app data folder and try again.".into());
        }
    }

    let log_file = logs_dir.join("postgres.log");
    let mut start = Command::new(&pg_ctl);
    hide_window(&mut start);
    let options = format!("-p {PG_PORT} -h 127.0.0.1");
    let status = start
        .args([
            "-D",
            &pg_data.to_string_lossy(),
            "-l",
            &log_file.to_string_lossy(),
            "-o",
            &options,
            "start",
        ])
        .status()
        .map_err(|e| format!("pg_ctl failed to start: {e}"))?;
    if !status.success() && !port_open(PG_PORT) {
        return Err("PostgreSQL did not start. See logs in the app data folder.".into());
    }
    wait_for_port(PG_PORT, Duration::from_secs(30))?;

    let mut create = Command::new(&createdb);
    hide_window(&mut create);
    create
        .env("PGPASSWORD", &password)
        .args([
            "-h",
            "127.0.0.1",
            "-p",
            &PG_PORT.to_string(),
            "-U",
            DB_USER,
            DB_NAME,
        ]);
    let _ = create.status();

    let mut runtime = RUNTIME.lock().expect("desktop runtime lock");
    runtime.pg_ctl = Some(pg_ctl);
    runtime.pg_data = Some(pg_data);
    Ok(())
}

fn start_backend(app: &AppHandle, data_dir: &Path, logs_dir: &Path) -> Result<(), String> {
    let jar = find_jar(app).ok_or_else(|| {
        "The shop engine JAR was not found. Build with the desktop Maven profile, or set IMS_JAR.".to_string()
    })?;
    let java = find_java(app).ok_or_else(|| {
        "A Java runtime was not bundled. Install JDK 17 for development, or set IMS_JAVA_HOME.".to_string()
    })?;
    let password = read_or_create_password(data_dir)?;
    let pg_bin = find_pg_bin(app);
    let log_file = logs_dir.join("backend.log");
    let log = fs::File::create(&log_file).map_err(|e| format!("Unable to create backend log: {e}"))?;
    let err = log.try_clone().map_err(|e| format!("Unable to clone backend log: {e}"))?;

    let mut cmd = Command::new(&java);
    hide_window(&mut cmd);
    cmd.arg("-Xms128m")
        .arg("-Xmx512m")
        .arg("-jar")
        .arg(&jar)
        .current_dir(data_dir)
        .env("SPRING_PROFILES_ACTIVE", "desktop")
        .env("APP_DATA_DIR", data_dir)
        .env("SERVER_PORT", UI_PORT.to_string())
        .env(
            "DB_URL",
            format!("jdbc:postgresql://127.0.0.1:{PG_PORT}/{DB_NAME}"),
        )
        .env("DB_HOST", "127.0.0.1")
        .env("DB_PORT", PG_PORT.to_string())
        .env("DB_USERNAME", DB_USER)
        .env("DB_PASSWORD", password)
        .env("APP_PUBLIC_APP_URL", format!("http://127.0.0.1:{UI_PORT}"))
        .stdin(Stdio::null())
        .stdout(Stdio::from(log))
        .stderr(Stdio::from(err));
    if let Some(pg_bin) = pg_bin {
        cmd.env("APP_DESKTOP_PG_BIN", pg_bin);
    }
    if env_flag("IMS_SKIP_POSTGRES") {
        if let Ok(url) = std::env::var("DB_URL") {
            cmd.env("DB_URL", url);
        }
        if let Ok(port) = std::env::var("DB_PORT") {
            cmd.env("DB_PORT", port);
        }
        if let Ok(password) = std::env::var("DB_PASSWORD") {
            cmd.env("DB_PASSWORD", password);
        }
    }

    let child = cmd
        .spawn()
        .map_err(|e| format!("Unable to start the shop engine: {e}"))?;
    let mut runtime = RUNTIME.lock().expect("desktop runtime lock");
    runtime.backend = Some(child);
    let _ = set_status(app, "Starting the shop engine…");
    Ok(())
}

fn find_jar(app: &AppHandle) -> Option<PathBuf> {
    if let Ok(path) = std::env::var("IMS_JAR") {
        let candidate = PathBuf::from(path);
        if candidate.exists() {
            return Some(candidate);
        }
    }
    first_existing(&[
        resource_join(app, "app.jar"),
        resource_join(app, "resources/app.jar"),
        Some(PathBuf::from("src-tauri/resources/app.jar")),
        exe_dir().map(|p| p.join("resources").join("app.jar")),
    ])
}

fn find_java(app: &AppHandle) -> Option<PathBuf> {
    if let Ok(home) = std::env::var("IMS_JAVA_HOME") {
        let candidate = PathBuf::from(home).join("bin").join(exe("java"));
        if candidate.exists() {
            return Some(candidate);
        }
    }
    if let Ok(home) = std::env::var("JAVA_HOME") {
        let candidate = PathBuf::from(home).join("bin").join(exe("java"));
        if candidate.exists() {
            return Some(candidate);
        }
    }
    first_existing(&[
        resource_join(app, &format!("jre/bin/{}", exe("java"))),
        resource_join(app, &format!("resources/jre/bin/{}", exe("java"))),
        Some(PathBuf::from("src-tauri/resources/jre/bin").join(exe("java"))),
        which("java"),
    ])
}

fn find_pg_bin(app: &AppHandle) -> Option<PathBuf> {
    if let Ok(path) = std::env::var("IMS_PG_BIN") {
        let candidate = PathBuf::from(path);
        if candidate.exists() {
            return Some(candidate);
        }
    }
    first_existing(&[
        resource_join(app, "pgsql/bin"),
        resource_join(app, "resources/pgsql/bin"),
        Some(PathBuf::from("src-tauri/resources/pgsql/bin")),
        exe_dir().map(|p| p.join("resources").join("pgsql").join("bin")),
    ])
}

fn resource_join(app: &AppHandle, rel: &str) -> Option<PathBuf> {
    app.path()
        .resource_dir()
        .ok()
        .map(|root| root.join(rel))
}

fn exe_dir() -> Option<PathBuf> {
    std::env::current_exe()
        .ok()
        .and_then(|p| p.parent().map(Path::to_path_buf))
}

fn first_existing(candidates: &[Option<PathBuf>]) -> Option<PathBuf> {
    candidates
        .iter()
        .flatten()
        .find(|path| path.exists())
        .cloned()
}

fn which(tool: &str) -> Option<PathBuf> {
    let mut cmd = Command::new(if cfg!(windows) { "where" } else { "which" });
    hide_window(&mut cmd);
    let output = cmd.arg(tool).output().ok()?;
    if !output.status.success() {
        return None;
    }
    let line = String::from_utf8_lossy(&output.stdout)
        .lines()
        .next()?
        .trim()
        .to_string();
    if line.is_empty() {
        None
    } else {
        Some(PathBuf::from(line))
    }
}

fn data_dir() -> PathBuf {
    if let Ok(configured) = std::env::var("APP_DATA_DIR") {
        return PathBuf::from(configured);
    }
    let base = std::env::var_os("LOCALAPPDATA")
        .map(PathBuf::from)
        .or_else(dirs::data_local_dir)
        .unwrap_or_else(|| PathBuf::from("."));
    if cfg!(target_os = "linux") && std::env::var_os("LOCALAPPDATA").is_none() {
        return base.join("inventory-management");
    }
    base.join("InventoryManagement")
}

fn read_or_create_password(data_dir: &Path) -> Result<String, String> {
    let path = data_dir.join("pg.password");
    if let Ok(existing) = fs::read_to_string(&path) {
        let trimmed = existing.trim();
        if !trimmed.is_empty() {
            return Ok(trimmed.to_string());
        }
    }
    let password = random_password();
    fs::write(&path, &password).map_err(|e| format!("Unable to write database password: {e}"))?;
    Ok(password)
}

fn random_password() -> String {
    use std::time::{SystemTime, UNIX_EPOCH};
    let nanos = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_nanos())
        .unwrap_or(1);
    format!("ims-{nanos:x}")
}

fn wait_for_health(timeout: Duration) -> Result<(), String> {
    let deadline = Instant::now() + timeout;
    while Instant::now() < deadline {
        if health_ok() {
            return Ok(());
        }
        thread::sleep(Duration::from_millis(400));
    }
    Err("The shop engine did not become ready. See logs in the app data folder.".into())
}

fn wait_for_port(port: u16, timeout: Duration) -> Result<(), String> {
    let deadline = Instant::now() + timeout;
    while Instant::now() < deadline {
        if port_open(port) {
            return Ok(());
        }
        thread::sleep(Duration::from_millis(200));
    }
    Err(format!("Nothing is listening on port {port}."))
}

fn port_open(port: u16) -> bool {
    let addr = SocketAddr::from(([127, 0, 0, 1], port));
    TcpStream::connect_timeout(&addr, Duration::from_millis(250)).is_ok()
}

fn health_ok() -> bool {
    let addr = ("127.0.0.1", UI_PORT)
        .to_socket_addrs()
        .ok()
        .and_then(|mut addrs| addrs.next());
    let Some(addr) = addr else {
        return false;
    };
    let Ok(mut stream) = TcpStream::connect_timeout(&addr, Duration::from_millis(400)) else {
        return false;
    };
    let _ = stream.set_read_timeout(Some(Duration::from_secs(2)));
    let request = b"GET /api/actuator/health HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n";
    if stream.write_all(request).is_err() {
        return false;
    }
    let mut body = String::new();
    let _ = stream.read_to_string(&mut body);
    body.contains("200") && (body.contains("UP") || body.contains("\"status\""))
}

fn navigate(app: &AppHandle) -> Result<(), String> {
    let window = app
        .get_webview_window("main")
        .ok_or_else(|| "Main window is missing".to_string())?;
    let url = tauri::Url::parse("http://127.0.0.1:18080/")
        .map_err(|error| format!("Invalid shop URL: {error}"))?;
    window
        .navigate(url)
        .map_err(|error| format!("Unable to open the shop window: {error}"))
}

fn set_status(app: &AppHandle, message: &str) -> Result<(), String> {
    let window = app
        .get_webview_window("main")
        .ok_or_else(|| "Main window is missing".to_string())?;
    let escaped = message.replace('\\', "\\\\").replace('\'', "\\'");
    window
        .eval(&format!(
            "var el = document.getElementById('status'); if (el) el.textContent = '{escaped}';"
        ))
        .map_err(|e| e.to_string())
}

fn env_flag(name: &str) -> bool {
    matches!(
        std::env::var(name).as_deref(),
        Ok("1") | Ok("true") | Ok("TRUE") | Ok("yes")
    )
}

fn exe(name: &str) -> String {
    if cfg!(windows) {
        format!("{name}.exe")
    } else {
        name.to_string()
    }
}

fn hide_window(command: &mut Command) {
    #[cfg(windows)]
    {
        use std::os::windows::process::CommandExt;
        const CREATE_NO_WINDOW: u32 = 0x0800_0000;
        command.creation_flags(CREATE_NO_WINDOW);
    }
    let _ = command;
}
