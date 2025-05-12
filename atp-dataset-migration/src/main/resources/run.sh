#!/bin/sh
#Choose which type of DB you would like to use:
#pg - postgresql database
#Example: SET jdbc_type=pg
export jdbc_type=pg

/usr/bin/java  -Dlb.libs.path="scripts" -Djdbc_type=${jdbc_type} -cp "config/:lib/*" org.qubership.atp.dataset.db.migration.Main
