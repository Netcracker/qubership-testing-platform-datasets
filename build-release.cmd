del /s build-release-log.log
CALL build-test.cmd
CALL :CHECK_FAIL
CALL mvn install -Dstage=release -DskipTests=true -Djdbc.Url=jdbc:postgresql://localhost:5432/datasets -Djdbc.User=datasets -Djdbc.Password=datasets > build-release-log.log
CALL :CHECK_FAIL
GOTO :EOF
:CHECK_FAIL
@echo off
if NOT ["%errorlevel%"]==["0"] (
    pause
    exit /b %errorlevel%
)