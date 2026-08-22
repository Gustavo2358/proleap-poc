#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mvn -q compile exec:java
echo
echo "Abra dist/index.html para a Parse Tree ou dist/ast.html para a AST."
