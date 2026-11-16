@echo off
REM Executa a compilação e execução do ShopOnline no Windows.
REM Sem dependências externas.
cd /d "%~dp0"

echo ==================================================
echo     COMPILANDO PROJETO SHOPONLINE (JAVA 17+)      
echo ==================================================
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
del sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo Erro durante a compilacao!
    exit /b %ERRORLEVEL%
)

echo Compilacao concluida com sucesso!
echo.
echo ==================================================
echo              INICIANDO SISTEMA                    
echo ==================================================
java -cp out Main
pause
