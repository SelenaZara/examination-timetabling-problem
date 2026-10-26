@echo off
setlocal enabledelayedexpansion

echo ======================================================================
echo  Building and Launching Academic Examination Timetabling Platform
echo ======================================================================

if not exist bin mkdir bin

echo Compiling Java source files...
javac -encoding UTF-8 -d bin -sourcepath src\main\java src\main\java\edu\ump\timetabling\Main.java src\main\java\edu\ump\timetabling\model\*.java src\main\java\edu\ump\timetabling\dataset\*.java src\main\java\edu\ump\timetabling\solver\*.java src\main\java\edu\ump\timetabling\solver\initial\*.java src\main\java\edu\ump\timetabling\solver\improvement\*.java src\main\java\edu\ump\timetabling\util\*.java src\main\java\edu\ump\timetabling\controller\*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed! Please check Java compiler output above.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Compilation successful. Launching Timetabling server...
java -cp bin edu.ump.timetabling.Main 8080

pause
