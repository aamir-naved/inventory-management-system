.PHONY: start stop restart status logs up down test backup restore uptime e2e prod rollback

start:
	./scripts/app.sh start

stop:
	./scripts/app.sh stop

restart:
	./scripts/app.sh restart

status:
	./scripts/app.sh status

logs:
	./scripts/app.sh logs

up: start

down: stop

test:
	./scripts/test.sh

backup:
	./scripts/backup.sh

restore:
	@test -n "$(FILE)" || (echo 'Usage: make restore FILE=backups/inventory-….sql.gz'; exit 1)
	./scripts/restore.sh "$(FILE)"

uptime:
	./scripts/uptime-check.sh

e2e:
	./scripts/e2e.sh

prod:
	./scripts/deploy.sh

rollback:
	./scripts/rollback.sh
