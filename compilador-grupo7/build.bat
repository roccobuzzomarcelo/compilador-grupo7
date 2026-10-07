@echo off
rem Genera el analizador, compila el proyecto y arma el JAR ejecutable.
rem Requiere un JDK (8 o superior) en el PATH.
cd /d "%~dp0"

echo 1/3 Generando Lexico.java con JFlex...
java -jar lib\jflex-full-1.9.1.jar --encoding utf-8 -d src\compilador src\compilador\Lexico.flex || exit /b 1

echo 2/3 Compilando...
if exist build rmdir /s /q build
mkdir build
javac -encoding UTF-8 --release 8 -Xlint:-options -nowarn -d build src\compilador\*.java || exit /b 1

xcopy /y /i /q src\compilador\recursos build\compilador\recursos >nul
echo 3/3 Armando Compilador.jar...
jar cfe Compilador.jar compilador.Main -C build . || exit /b 1

echo Listo. Ejecutar con: java -jar Compilador.jar
