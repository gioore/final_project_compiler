@echo off
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -d out src\*.java tests\*.java
if %errorlevel% neq 0 (
    echo Error de compilacion
    pause
    exit /b %errorlevel%
)
java -cp out TestRunner
pause
