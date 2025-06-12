#!/usr/bin/env bash
# Simple helper to run backend tests
set -e
clojure -M:test "$@"
