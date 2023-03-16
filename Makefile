
repl:
	lein with-profile +test repl

docker-prepare:
	rm -rf ./.docker/postgres/data

docker-up: docker-prepare
	docker-compose up

docker-down:
	docker-compose down

docker-rm:
	docker-compose rm --force
