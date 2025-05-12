del /s build-test-log.log
CALL mvn clean verify -Dstage=test > build-test-log.log
if NOT ["%errorlevel%"]==["0"] (
    pause
    exit /b %errorlevel%
)