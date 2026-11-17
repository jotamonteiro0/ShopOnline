@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ==================================================
echo     COMPILANDO PROJETO SHOPONLINE (JAVA 17+)      
echo ==================================================
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
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
java -Dfile.encoding=UTF-8 -cp out Main %*
pause
