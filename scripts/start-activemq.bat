@echo off
REM Script para iniciar o ActiveMQ - Versão 2.0

echo Iniciando ActiveMQ...
echo.

REM Configura caminhos absolutos
for %%I in ("%~dp0..") do set "PROJECT_ROOT=%%~fI"
set "ACTIVEMQ_HOME=%PROJECT_ROOT%\apache-activemq-5.19.0"

REM Verifica se o diretório do ActiveMQ existe
if not exist "%ACTIVEMQ_HOME%" (
    echo [ERRO] Pasta do ActiveMQ não encontrada em: %ACTIVEMQ_HOME%
    pause
    exit /b 1
)

REM Navega para a pasta do ActiveMQ e executa
echo [CONFIGURAÇÃO]
echo ActiveMQ Home: %ACTIVEMQ_HOME%
echo.
cd /d "%ACTIVEMQ_HOME%\bin"
call activemq start

echo.
echo ActiveMQ iniciado. Verifique o console para mensagens de status.