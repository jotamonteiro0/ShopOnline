#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo "=================================================="
echo "    COMPILANDO PROJETO SHOPONLINE (JAVA 17+)      "
echo "=================================================="
mkdir -p out
find src -name "*.java" -print0 | xargs -0 javac -encoding UTF-8 -d out

echo "Compilacao concluida com sucesso!"
echo
echo "=================================================="
echo "             INICIANDO SISTEMA                    "
echo "=================================================="
java -Dfile.encoding=UTF-8 -cp out Main "$@"
