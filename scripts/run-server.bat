@echo off
REM Script para executar servidor de chat no Windows - Versão adaptada

echo Iniciando servidor de chat...
echo.

REM Configura caminhos absolutos
for %%I in ("%~dp0..") do set "PROJECT_ROOT=%%~fI"
set "ACTIVEMQ_HOME=%PROJECT_ROOT%\apache-activemq-5.19.0"
set "OUT_DIR=%PROJECT_ROOT%\out"

REM Define classpath com bibliotecas do ActiveMQ e classes compiladas
set "CLASSPATH=%ACTIVEMQ_HOME%\lib\*;%OUT_DIR%"

REM Executa o servidor
echo [CONFIGURAÇÃO]
echo Classpath: %CLASSPATH%
echo.
echo [INICIANDO SERVIDOR]
java -cp "%CLASSPATH%" server.Server

pause