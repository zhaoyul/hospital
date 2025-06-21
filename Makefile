clean:
	rm -rf target

run:
	clj -M:dev

repl:
	clj -M:dev:nrepl

test:
        clj -M:test

coverage:
        clj -M:coverage

uberjar:
	clj -T:build all
