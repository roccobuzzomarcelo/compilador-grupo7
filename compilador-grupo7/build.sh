#!/bin/sh
# Genera el analizador léxico y el sintáctico, compila el proyecto y arma el JAR ejecutable.
# Requiere un JDK (8 o superior) en el PATH.
set -e
cd "$(dirname "$0")"

CUP=lib/java-cup-11b-20160615.jar
CUP_RUNTIME=lib/java-cup-runtime-11b-20160615.jar

echo "1/4 Generando Lexico.java con JFlex..."
java -jar lib/jflex-full-1.9.1.jar --encoding utf-8 -d src/compilador src/compilador/Lexico.flex

echo "2/4 Generando Sintactico.java y sym.java con Java CUP..."
java -cp "$CUP:$CUP_RUNTIME" java_cup.Main -destdir src/compilador -parser Sintactico -symbols sym \
    src/compilador/Sintactico.cup

echo "3/4 Compilando..."
rm -rf build
mkdir -p build
javac -encoding UTF-8 --release 8 -Xlint:-options -nowarn -cp "$CUP_RUNTIME" -d build src/compilador/*.java

mkdir -p build/compilador/recursos
cp src/compilador/recursos/*.png build/compilador/recursos/
# El runtime de CUP va dentro del JAR para que funcione sin dependencias externas
(cd build && jar xf "../$CUP_RUNTIME" java_cup)

echo "4/4 Armando Compilador.jar..."
jar cfe Compilador.jar compilador.Main -C build .

echo "Listo. Ejecutar con: java -jar Compilador.jar"
