@echo off
REM Script para executar a interface de administração no Windows - Versão 2.0

echo Iniciando interface de administração...
echo.

REM Configura caminhos absolutos
for %%I in ("%~dp0..") do set "PROJECT_ROOT=%%~fI"
set "ACTIVEMQ_HOME=%PROJECT_ROOT%\apache-activemq-5.19.0"
set "OUT_DIR=%PROJECT_ROOT%\out"

REM Define classpath com as bibliotecas do ActiveMQ e classes compiladas
set "CLASSPATH=%ACTIVEMQ_HOME%\lib\*;%OUT_DIR%"

REM Verifica se a classe AdminUI foi compilada
if not exist "%OUT_DIR%\admin\AdminUI.class" (
    echo [ERRO] Classe AdminUI não encontrada em %OUT_DIR%\admin
    echo Certifique-se de que o projeto foi compilado corretamente.
    pause
    exit /b 1
)

REM Executa a interface de administração
echo [CONFIGURAÇÃO]
echo Classpath: %CLASSPATH%
echo.
echo [INICIANDO ADMINISTRADOR]
java -cp "%CLASSPATH%" admin.AdminUI

pause