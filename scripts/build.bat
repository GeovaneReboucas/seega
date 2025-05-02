@echo off
setlocal

rem Define os diretórios
set SRC_DIR=..\src
set OUT_DIR=..\out
set JAR_FILE=..\app.jar
set MANIFEST_FILE=..\MANIFEST.MF

rem Cria a pasta out se não existir
if not exist %OUT_DIR% (
    mkdir %OUT_DIR%
)

rem Compila os arquivos .java
echo Compilando arquivos Java...
javac -d %OUT_DIR% %SRC_DIR%\*.java

if errorlevel 1 (
    echo Erro na compilação.
    exit /b 1
)

rem Cria o arquivo JAR
echo Gerando JAR...
jar cfm %JAR_FILE% %MANIFEST_FILE% -C %OUT_DIR% .

echo Build concluído com sucesso.
endlocal
pause
