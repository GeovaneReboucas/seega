@echo off
REM Script para executar cliente de chat no Windows - Versão 2.0

echo Iniciando cliente de chat...
echo.

REM Configura caminhos absolutos
for %%I in ("%~dp0..") do set "PROJECT_ROOT=%%~fI"
set "ACTIVEMQ_HOME=%PROJECT_ROOT%\apache-activemq-5.19.0"
set "OUT_DIR=%PROJECT_ROOT%\out"

REM Define classpath com as bibliotecas do ActiveMQ e classes compiladas
set "CLASSPATH=%ACTIVEMQ_HOME%\lib\*;%OUT_DIR%"

REM Verifica se a classe ChatClient foi compilada
if not exist "%OUT_DIR%\client\ChatClient.class" (
    echo [ERRO] Classe ChatClient não encontrada em %OUT_DIR%\client
    echo Certifique-se de que o projeto foi compilado corretamente.
    pause
    exit /b 1
)

REM Executa o cliente
echo [CONFIGURAÇÃO]
echo Classpath: %CLASSPATH%
echo.
echo [INICIANDO CLIENTE]
java -cp "%CLASSPATH%" client.ChatClient

pause