clean:
	rm -rf target

run:
	clj -M:dev

repl:
	clj -M:dev:nrepl

test:
	clj -M:test -m kaocha.runner

uberjar:
	clj -T:build all
