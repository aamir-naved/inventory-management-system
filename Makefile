.PHONY: start stop restart status logs up down test backup restore prod

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

prod:
	./scripts/deploy.sh
