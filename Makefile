.PHONY: up down verify logs rebuild

up:
	docker compose up --build

down:
	docker compose down -v

verify:
	bash scripts/smoke-test.sh

logs:
	docker compose logs -f

rebuild:
	docker compose build --no-cache
