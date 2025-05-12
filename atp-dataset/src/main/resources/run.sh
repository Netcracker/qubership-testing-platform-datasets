#!/usr/bin/env sh

#Choose which type of DB you would like to use:
#pg - postgresql database
#Example: jdbc_type=pg
jdbc_type=pg

if [ "${ATP_INTERNAL_GATEWAY_ENABLED:-false}" = "true" ]; then
  echo "Internal gateway integration is enabled."
  FEIGN_ATP_CATALOGUE_NAME=${FEIGN_ATP_INTERNAL_GATEWAY_NAME}
  FEIGN_ATP_MACROS_NAME=${FEIGN_ATP_INTERNAL_GATEWAY_NAME}
  FEIGN_ATP_USERS_NAME=${FEIGN_ATP_INTERNAL_GATEWAY_NAME}
  FEIGN_ATP_EI_NAME=${FEIGN_ATP_INTERNAL_GATEWAY_NAME}
else
  echo "Internal gateway integration is disabled."
  FEIGN_ATP_CATALOGUE_ROUTE=
  FEIGN_ATP_MACROS_ROUTE=
  FEIGN_ATP_USERS_ROUTE=
  FEIGN_ATP_EI_ROUTE=
fi

export HAZELCAST_SERVER_ADDRESS="${HAZELCAST_ADDRESS%%:*}"
export HAZELCAST_SERVER_PORT="${HAZELCAST_ADDRESS##*:}"

JAVA_OPTIONS="${JAVA_OPTIONS} -Dspring.devtools.add-properties=false"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlb.libs.path=config/scripts"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dspring.config.location=${SPRING_CONFIG_LOCATION:-./config/application.properties}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dspring.cloud.bootstrap.location=./config/bootstrap.properties"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.on=${GRAYLOG_ON:-false}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.host=${GRAYLOG_HOST}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.port=${GRAYLOG_PORT}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Djdbc.Url=jdbc:postgresql://${PG_DB_ADDR}:${PG_DB_PORT}/${DATASET_PG_DB}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Djdbc.User=${DATASET_PG_USER:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Djdbc.Password=${DATASET_PG_PASSWORD:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Djdbc.MinIdle=20"
JAVA_OPTIONS="${JAVA_OPTIONS} -Djdbc.MaxPoolSize=50"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dgridfs.database=${DATASET_GRIDFS_DB:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dgridfs.host=${MONGO_DB_ADDR:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dgridfs.port=${MONGO_DB_PORT:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dgridfs.user=${DATASET_GRIDFS_USER:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dgridfs.password=${DATASET_GRIDFS_PASSWORD:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dei.gridfs.database=${EI_GRIDFS_DB:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dei.gridfs.host=${EI_GRIDFS_DB_ADDR:-$GRIDFS_DB_ADDR}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dei.gridfs.port=${EI_GRIDFS_DB_PORT:-$GRIDFS_DB_PORT}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dei.gridfs.user=${EI_GRIDFS_USER:?}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dei.gridfs.password=${EI_GRIDFS_PASSWORD:?}"

# Profiler uses this path
/usr/bin/java --add-opens java.base/java.lang=ALL-UNNAMED -XX:MaxRAM=${MAX_RAM:-1024m} ${ADDITIONAL_JAVA_OPTIONS} ${JAVA_OPTIONS} -cp "./config/:./lib/*:./q-classes/*" org.qubership.atp.dataset.Main
