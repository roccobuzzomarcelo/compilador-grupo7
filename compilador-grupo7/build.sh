#!/bin/sh
# Genera el analizador, compila el proyecto y arma el JAR ejecutable.
# Requiere un JDK (8 o superior) en el PATH.
set -e
cd "$(dirname "$0")"

echo "1/3 Generando Lexico.java con JFlex..."
java -jar lib/jflex-full-1.9.1.jar --encoding utf-8 -d src/compilador src/compilador/Lexico.flex

echo "2/3 Compilando..."
rm -rf build
mkdir -p build
javac -encoding UTF-8 --release 8 -Xlint:-options -nowarn -d build src/compilador/*.java

mkdir -p build/compilador/recursos
cp src/compilador/recursos/*.png build/compilador/recursos/
echo "3/3 Armando Compilador.jar..."
jar cfe Compilador.jar compilador.Main -C build .

echo "Listo. Ejecutar con: java -jar Compilador.jar"
