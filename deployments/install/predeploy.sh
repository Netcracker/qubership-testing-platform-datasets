#!/usr/bin/env sh

if [ ! -f ./atp-common-scripts/openshift/common.sh ]; then
  echo "ERROR: Cannot locate ./atp-common-scripts/openshift/common.sh"
  exit 1
fi

. ./atp-common-scripts/openshift/common.sh

_ns="${NAMESPACE}"

DATASET_PG_DB="$(env_default "${DATASET_PG_DB}" "atp-dataset" "${_ns}")"
DATASET_PG_USER="$(env_default "${DATASET_PG_USER}" "atp-dataset" "${_ns}")"
DATASET_PG_PASSWORD="$(env_default "${DATASET_PG_PASSWORD}" "atp-dataset" "${_ns}")"
DATASET_GRIDFS_DB="$(env_default "${DATASET_GRIDFS_DB}" "atp-dataset" "${_ns}")"
DATASET_GRIDFS_USER="$(env_default "${DATASET_GRIDFS_USER}" "atp-dataset" "${_ns}")"
DATASET_GRIDFS_PASSWORD="$(env_default "${DATASET_GRIDFS_PASSWORD}" "atp-dataset" "${_ns}")"
EI_GRIDFS_DB="$(env_default "${EI_GRIDFS_DB}" "atp-ei-gridfs" "${_ns}")"
EI_GRIDFS_USER="$(env_default "${EI_GRIDFS_USER}" "atp-ei-gridfs" "${_ns}")"
EI_GRIDFS_PASSWORD="$(env_default "${EI_GRIDFS_PASSWORD}" "atp-ei-gridfs" "${_ns}")"

echo "***** Initializing databases ******"
init_pg "${PG_DB_ADDR}" "${DATASET_PG_DB}" "${DATASET_PG_USER}" "${DATASET_PG_PASSWORD}" "${PG_DB_PORT}" "${pg_user}" "${pg_pass}"
if [ "${DATASET_GRIDFS_ENABLED:-true}" = "true" ]; then
  echo "***** Preparing MongoDB connection *****"
  init_mongo "${MONGO_DB_ADDR}" "${DATASET_GRIDFS_DB}" "${DATASET_GRIDFS_USER}" "${DATASET_GRIDFS_PASSWORD}" "${MONGO_PORT}" "${mongo_user}" "${mongo_pass}"
fi
if [ "${EI_GRIDFS_ENABLED:-true}" = "true" ]; then
  echo "***** Preparing Gridfs connection *****"
  init_mongo "${EI_GRIDFS_DB_ADDR:-$GRIDFS_DB_ADDR}" "${EI_GRIDFS_DB}" "${EI_GRIDFS_USER}" "${EI_GRIDFS_PASSWORD}" "${EI_GRIDFS_DB_PORT:-$GRIDFS_DB_PORT}" "${ei_gridfs_user}" "${ei_gridfs_pass}"
fi

echo "***** Setting up encryption ******"
encrypt "${ENCRYPT}" "${SERVICE_NAME}"
