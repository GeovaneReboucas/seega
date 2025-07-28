@echo off
REM SCRIPT DE COMPILAÇÃO DEFINITIVO - VERSÃO 4.0 (RECURSIVA)

:: --------------------------------------------------
:: Configuração absoluta dos diretórios
:: --------------------------------------------------
setlocal enabledelayedexpansion

:: Obtém o diretório do projeto (raiz)
for %%I in ("%~dp0..") do set "PROJECT_ROOT=%%~fI"

:: Define todos os caminhos absolutos
set "ACTIVEMQ_HOME=%PROJECT_ROOT%\apache-activemq-5.19.0"
set "SRC_DIR=%PROJECT_ROOT%\src"
set "OUT_DIR=%PROJECT_ROOT%\out"
set "SCRIPTS_DIR=%PROJECT_ROOT%\scripts"

:: --------------------------------------------------
:: Preparação do ambiente
:: --------------------------------------------------
echo [SISTEMA] Configurando ambiente de compilação...
echo.
echo [DIRETÓRIOS]
echo Raiz do Projeto: %PROJECT_ROOT%
echo Source: %SRC_DIR%
echo Output: %OUT_DIR%
echo Scripts: %SCRIPTS_DIR%
echo.

:: Limpa e cria a pasta de output
if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

:: --------------------------------------------------
:: Compilação
:: --------------------------------------------------
echo [COMPILAÇÃO] Iniciando processo...
echo.

set CLASSPATH=%ACTIVEMQ_HOME%\lib\*;%OUT_DIR%
set FILE_COUNT=0
set SUCCESS_COUNT=0

:: Compila recursivamente todos os arquivos .java em src e subpastas
for /r "%SRC_DIR%" %%F in (*.java) do (
    set /a FILE_COUNT+=1
    echo [COMPILANDO] %%~nxF...
    
    javac -d "%OUT_DIR%" -cp "%CLASSPATH%" -sourcepath "%SRC_DIR%" "%%F"
    
    if !errorlevel! equ 0 (
        set /a SUCCESS_COUNT+=1
        echo [OK] %%~nxF compilado com sucesso
    ) else (
        echo [FALHA] Erro ao compilar %%~nxF
    )
    echo.
)

:: --------------------------------------------------
:: Relatório final
:: --------------------------------------------------
echo [RESULTADO FINAL]
echo Arquivos .java encontrados: %FILE_COUNT%
echo Arquivos compilados com sucesso: %SUCCESS_COUNT%
echo.

if %FILE_COUNT% equ %SUCCESS_COUNT% (
    echo [STATUS] COMPILAÇÃO CONCLUÍDA COM SUCESSO!
    echo.
    echo [INSTRUÇÕES] Para executar o sistema:
    echo 1. Inicie o ActiveMQ: "%SCRIPTS_DIR%\start-activemq.bat"
    echo 2. Execute o servidor: "%SCRIPTS_DIR%\run-server.bat"
    echo 3. Execute o administrador: "%SCRIPTS_DIR%\run-admin.bat"
    echo 4. Execute clientes: "%SCRIPTS_DIR%\run-client.bat"
) else (
    set /a FAIL_COUNT=FILE_COUNT - SUCCESS_COUNT
    echo [STATUS] COMPILAÇÃO COM ERROS (%FAIL_COUNT% falhas)
    echo Verifique as mensagens acima para identificar os problemas.
)

endlocal
pause