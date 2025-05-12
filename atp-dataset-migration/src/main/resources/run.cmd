::Choose which type of DB you would like to use:
::pg - postgresql database
::Example: SET jdbc_type=pg
SET jdbc_type=pg

java -Dlb.libs.path="scripts" -Djdbc_type=%jdbc_type% -cp "config/;lib/*" migration.db.org.qubership.atp.dataset.Main
@echo off
if NOT ["%errorlevel%"]==["0"] (
    pause
    exit /b %errorlevel%
)
