
repl:
	lein with-profile +test repl

release:
	lein release

.PHONY: test
test:
	lein test
