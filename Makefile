clean:
	rm -rf target

run:
	clj -M:dev

repl:
	clj -M:dev:nrepl

.PHONY: test

test:
        clojure -M:test

uberjar:
	clj -T:build all
