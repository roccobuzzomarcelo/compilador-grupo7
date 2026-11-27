@echo off
rem Genera el analizador lexico y el sintactico, compila el proyecto y arma el JAR ejecutable.
rem Requiere un JDK (8 o superior) en el PATH.
cd /d "%~dp0"

set CUP=lib\java-cup-11b-20160615.jar
set CUP_RUNTIME=lib\java-cup-runtime-11b-20160615.jar

echo 1/4 Generando Lexico.java con JFlex...
java -jar lib\jflex-full-1.9.1.jar --encoding utf-8 -d src\compilador src\compilador\Lexico.flex || exit /b 1

echo 2/4 Generando Sintactico.java y sym.java con Java CUP...
java -cp "%CUP%;%CUP_RUNTIME%" java_cup.Main -destdir src\compilador -parser Sintactico -symbols sym src\compilador\Sintactico.cup || exit /b 1

echo 3/4 Compilando...
if exist build rmdir /s /q build
mkdir build
javac -encoding UTF-8 --release 8 -Xlint:-options -nowarn -cp "%CUP_RUNTIME%" -d build src\compilador\*.java || exit /b 1

xcopy /y /i /q src\compilador\recursos build\compilador\recursos >nul
rem El runtime de CUP va dentro del JAR para que funcione sin dependencias externas
pushd build
jar xf ..\%CUP_RUNTIME% java_cup || (popd & exit /b 1)
popd

echo 4/4 Armando Compilador.jar...
jar cfe Compilador.jar compilador.Main -C build . || exit /b 1

echo Listo. Ejecutar con: java -jar Compilador.jar
