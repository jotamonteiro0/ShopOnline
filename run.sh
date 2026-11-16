#!/usr/bin/env bash
# Executa a compilação e execução do ShopOnline.
# Sem Maven, sem Gradle, sem dependências externas.
# Testado em Linux e macOS.
set -euo pipefail
cd "$(dirname "$0")"

echo "=================================================="
echo "    COMPILANDO PROJETO SHOPONLINE (JAVA 17+)      "
echo "=================================================="
mkdir -p out
find src -name "*.java" -print0 | xargs -0 javac -d out

echo "Compilação concluída com sucesso!"
echo
echo "=================================================="
echo "             INICIANDO SISTEMA                    "
echo "=================================================="
java -cp out Main
