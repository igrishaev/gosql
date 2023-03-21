
repl:
	lein with-profile +test repl

.PHONY: test
test:
	lein test

docker-prepare:
	rm -rf ./.docker/postgres/data

docker-up: docker-prepare
	docker-compose up

docker-down:
	docker-compose down

docker-rm:
	docker-compose rm --force

docker-psql:
	psql --port 25432 --host localhost -U test test
