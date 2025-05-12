::Choose which type of DB you would like to use:
::pg - postgresql database
::Example: SET jdbc_type=pg
SET jdbc_type=pg
java --add-opens java.base/java.lang=ALL-UNNAMED -XX:MaxRAM=!MAX_RAM! !JAVA_OPTIONS! -cp "config/;lib/*;q-classes/*" org.qubership.atp.dataset.Main
@echo off
if NOT ["%errorlevel%"]==["0"] (
    pause
    exit /b %errorlevel%
)
