mod sidecar;

use tauri::Manager;
use tauri::RunEvent;

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .setup(|app| {
            sidecar::boot(app.handle().clone());
            Ok(())
        })
        .on_window_event(|_window, event| {
            if let tauri::WindowEvent::Destroyed = event {
                sidecar::shutdown();
            }
        })
        .build(tauri::generate_context!())
        .expect("error while building the desktop app")
        .run(|_app, event| {
            if let RunEvent::Exit = event {
                sidecar::shutdown();
            }
        });
}
