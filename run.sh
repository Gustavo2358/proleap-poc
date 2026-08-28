#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mvn -q compile exec:java
echo
echo "Abra dist/index.html para a Parse Tree, dist/ast.html para a AST ou dist/symbols.html para a Symbol Table."
