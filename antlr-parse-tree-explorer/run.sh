#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mvn -q compile exec:java
echo
echo "Abra dist/index.html diretamente no navegador."
