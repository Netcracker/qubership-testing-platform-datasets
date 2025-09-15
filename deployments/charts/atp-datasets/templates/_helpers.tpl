{{/* Helper functions, do NOT modify */}}
{{- define "env.default" -}}
{{- $ctx := get . "ctx" -}}
{{- $def := get . "def" | default $ctx.Values.SERVICE_NAME -}}
{{- $pre := get . "pre" | default (eq $ctx.Values.PAAS_PLATFORM "COMPOSE" | ternary "" $ctx.Release.Namespace) -}}
{{- get . "val" | default ((empty $pre | ternary $def (print $pre "_" (trimPrefix "atp-" $def))) | nospace | replace "-" "_") -}}
{{- end -}}

{{- define "env.factor" -}}
{{- $ctx := get . "ctx" -}}
{{- get . "def" | default (eq $ctx.Values.PAAS_PLATFORM "COMPOSE" | ternary "1" (default "3" $ctx.Values.KAFKA_REPLICATION_FACTOR)) -}}
{{- end -}}

{{- define "env.compose" }}
{{- range $key, $val := merge (include "env.lines" . | fromYaml) (include "env.secrets" . | fromYaml) }}
{{ printf "- %s=%s" $key $val }}
{{- end }}
{{- end }}

{{- define "env.cloud" }}
{{- range $key, $val := (include "env.lines" . | fromYaml) }}
{{ printf "- name: %s" $key }}
{{ printf "  value: \"%s\"" $val }}
{{- end }}
{{- $keys := (include "env.secrets" . | fromYaml | keys | uniq | sortAlpha) }}
{{- if eq (default "" .Values.ENCRYPT) "secrets" }}
{{- $keys = concat $keys (list "ATP_CRYPTO_KEY" "ATP_CRYPTO_PRIVATE_KEY") }}
{{- end }}
{{- range $keys }}
{{ printf "- name: %s" . }}
{{ printf "  valueFrom:" }}
{{ printf "    secretKeyRef:" }}
{{ printf "      name: %s-secrets" $.Values.SERVICE_NAME }}
{{ printf "      key: %s" . }}
{{- end }}
{{- end }}

{{- define "securityContext.pod" -}}
runAsNonRoot: true
seccompProfile:
  type: "RuntimeDefault"
{{- with .Values.podSecurityContext }}
{{ toYaml . }}
{{- end -}}
{{- end -}}

{{- define "securityContext.container" -}}
allowPrivilegeEscalation: false
capabilities:
  drop: ["ALL"]
{{- with .Values.containerSecurityContext }}
{{ toYaml . }}
{{- end -}}
{{- end -}}
{{/* Helper functions end */}}

{{/* Environment variables to be used AS IS */}}
{{- define "env.lines" }}
ADDITIONAL_JAVA_OPTIONS: "{{ .Values.ADDITIONAL_JAVA_OPTIONS }}"
ATP_ARCHIVE_CRON_EXPRESSION: "{{ .Values.ATP_ARCHIVE_CRON_EXPRESSION }}"
ATP_ARCHIVE_JOB_NAME: "{{ .Values.ATP_ARCHIVE_JOB_NAME }}"
ATP_HTTP_LOGGING: "{{ .Values.ATP_HTTP_LOGGING }}"
ATP_HTTP_LOGGING_HEADERS: "{{ .Values.ATP_HTTP_LOGGING_HEADERS }}"
ATP_HTTP_LOGGING_HEADERS_IGNORE: "{{ .Values.ATP_HTTP_LOGGING_HEADERS_IGNORE }}"
ATP_HTTP_LOGGING_URI_IGNORE: "{{ .Values.ATP_HTTP_LOGGING_URI_IGNORE }}"
ATP_INTERNAL_GATEWAY_ENABLED: "{{ .Values.ATP_INTERNAL_GATEWAY_ENABLED }}"
ATP_LAST_REVISION_COUNT: "{{ .Values.ATP_LAST_REVISION_COUNT }}"
AUDIT_LOGGING_ENABLE: "{{ .Values.AUDIT_LOGGING_ENABLE }}"
AUDIT_LOGGING_TOPIC_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.AUDIT_LOGGING_TOPIC_NAME "def" "audit_logging_topic") }}"
CONSUL_ENABLED: "{{ .Values.CONSUL_ENABLED }}"
CONSUL_HEALTH_CHECK_ENABLED: "{{ .Values.CONSUL_HEALTH_CHECK_ENABLED }}"
CONSUL_PORT: "{{ .Values.CONSUL_PORT }}"
CONSUL_PREFIX: "{{ .Values.CONSUL_PREFIX }}"
CONSUL_TOKEN: "{{ .Values.CONSUL_TOKEN }}"
CONSUL_URL: "{{ .Values.CONSUL_URL }}"
CONTENT_SECURITY_POLICY: "{{ .Values.CONTENT_SECURITY_POLICY }}"
DATASET_GRIDFS_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.DATASET_GRIDFS_DB "def" "atp-dataset") }}"
DATASET_PG_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.DATASET_PG_DB "def" "atp-dataset") }}"
EI_GRIDFS_ENABLED: "{{ .Values.EI_GRIDFS_ENABLED }}"
EI_GRIDFS_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_DB "def" "atp-ei-gridfs") }}"
EUREKA_CLIENT_ENABLED: "{{ .Values.EUREKA_CLIENT_ENABLED }}"
FEIGN_ATP_CATALOGUE_NAME: "{{ .Values.FEIGN_ATP_CATALOGUE_NAME }}"
FEIGN_ATP_CATALOGUE_ROUTE: "{{ .Values.FEIGN_ATP_CATALOGUE_ROUTE }}"
FEIGN_ATP_CATALOGUE_URL: "{{ .Values.FEIGN_ATP_CATALOGUE_URL }}"
FEIGN_ATP_EI_NAME: "{{ .Values.FEIGN_ATP_EI_NAME }}"
FEIGN_ATP_EI_ROUTE: "{{ .Values.FEIGN_ATP_EI_ROUTE }}"
FEIGN_ATP_EI_URL: "{{ .Values.FEIGN_ATP_EI_URL }}"
EI_CLEAN_JOB_ENABLED: "{{ .Values.EI_CLEAN_JOB_ENABLED }}"
EI_CLEAN_JOB_WORKDIR: "{{ .Values.EI_CLEAN_JOB_WORKDIR }}"
EI_CLEAN_SCHEDULED_JOB_PERIOD_MS: "{{ .Values.EI_CLEAN_SCHEDULED_JOB_PERIOD_MS }}"
EI_CLEAN_JOB_FILE_DELETE_AFTER_MS: "{{ .Values.EI_CLEAN_JOB_FILE_DELETE_AFTER_MS }}"
FEIGN_ATP_INTERNAL_GATEWAY_NAME: "{{ .Values.FEIGN_ATP_INTERNAL_GATEWAY_NAME }}"
FEIGN_ATP_MACROS_NAME: "{{ .Values.FEIGN_ATP_MACROS_NAME }}"
FEIGN_ATP_MACROS_ROUTE: "{{ .Values.FEIGN_ATP_MACROS_ROUTE }}"
FEIGN_ATP_MACROS_URL: "{{ .Values.FEIGN_ATP_MACROS_URL }}"
FEIGN_ATP_USERS_NAME: "{{ .Values.FEIGN_ATP_USERS_NAME }}"
FEIGN_ATP_USERS_ROUTE: "{{ .Values.FEIGN_ATP_USERS_ROUTE }}"
FEIGN_ATP_USERS_URL: "{{ .Values.FEIGN_ATP_USERS_URL }}"
GRAYLOG_HOST: "{{ .Values.GRAYLOG_HOST }}"
GRAYLOG_ON: "{{ .Values.GRAYLOG_ON }}"
GRAYLOG_PORT: "{{ .Values.GRAYLOG_PORT }}"
GRIDFS_DB_ADDR: "{{ .Values.GRIDFS_DB_ADDR }}"
GRIDFS_DB_PORT: "{{ .Values.GRIDFS_DB_PORT }}"
HAZELCAST_CLUSTER_NAME: "{{ .Values.HAZELCAST_CLUSTER_NAME }}"
HAZELCAST_ENABLE: "{{ .Values.HAZELCAST_ENABLE }}"
HAZELCAST_ADDRESS: "{{ .Values.HAZELCAST_ADDRESS }}"
HTTPS_ENABLE: "{{ .Values.HTTPS_ENABLE }}"
JAVA_OPTIONS: "{{ if .Values.HEAPDUMP_ENABLED }}-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/diagnostic{{ end }} -Dcom.sun.management.jmxremote={{ .Values.JMX_ENABLE }} -Dcom.sun.management.jmxremote.port={{ .Values.JMX_PORT }} -Dcom.sun.management.jmxremote.rmi.port={{ .Values.JMX_RMI_PORT }} -Djava.rmi.server.hostname=127.0.0.1 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
JAVERS_ENABLED: "{{ .Values.JAVERS_ENABLED }}"
KAFKA_ENABLE: "{{ .Values.KAFKA_ENABLE }}"
KAFKA_GROUP_ID: "{{ .Values.KAFKA_GROUP_ID }}"
KAFKA_REPORTING_SERVERS: '{{ .Values.KAFKA_REPORTING_SERVERS }}'
KAFKA_PROJECT_EVENT_CONSUMER_TOPIC_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_PROJECT_EVENT_CONSUMER_TOPIC_NAME "def" "catalog_notification_topic") }}"
KAFKA_SERVERS: "{{ .Values.KAFKA_SERVERS }}"
KAFKA_SERVICE_ENTITIES_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_SERVICE_ENTITIES_TOPIC "def" "service_entities") }}"
KAFKA_SERVICE_ENTITIES_TOPIC_PARTITION: "{{ .Values.KAFKA_SERVICE_ENTITIES_TOPIC_PARTITION }}"
KAFKA_SERVICE_ENTITIES_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_SERVICE_ENTITIES_TOPIC_REPLICATION_FACTOR) }}"
KEYCLOAK_AUTH_URL: "{{ .Values.KEYCLOAK_AUTH_URL }}"
KEYCLOAK_ENABLED: "{{ .Values.KEYCLOAK_ENABLED }}"
KEYCLOAK_REALM: "{{ .Values.KEYCLOAK_REALM }}"
LOCALE_RESOLVER: "{{ .Values.LOCALE_RESOLVER }}"
LOG_LEVEL: "{{ .Values.LOG_LEVEL }}"
MAX_RAM: "{{ .Values.MAX_RAM }}"
MICROSERVICE_NAME: "{{ .Values.SERVICE_NAME }}"
MIGRATION_MODULE_LAUNCH_ENABLED: "{{ .Values.MIGRATION_MODULE_LAUNCH_ENABLED }}"
MONGO_DB_ADDR: "{{ .Values.MONGO_DB_ADDR }}"
MONGO_DB_PORT: "{{ .Values.MONGO_DB_PORT }}"
DATASET_GRIDFS_ENABLED: "{{ .Values.DATASET_GRIDFS_ENABLED }}"
PG_DB_ADDR: "{{ .Values.PG_DB_ADDR }}"
PG_DB_PORT: "{{ .Values.PG_DB_PORT }}"
PROFILER_ENABLED: "{{ .Values.PROFILER_ENABLED }}"
PROJECT_INFO_ENDPOINT: "{{ .Values.PROJECT_INFO_ENDPOINT }}"
REMOTE_DUMP_HOST: "{{ .Values.REMOTE_DUMP_HOST }}"
REMOTE_DUMP_PORT: "{{ .Values.REMOTE_DUMP_PORT }}"
SERVICE_ENTITIES_MIGRATION_ENABLED: "{{ .Values.SERVICE_ENTITIES_MIGRATION_ENABLED }}"
SERVICE_NAME: "{{ .Values.SERVICE_NAME }}"
SERVICE_REGISTRY_URL: "{{ .Values.SERVICE_REGISTRY_URL }}"
SPRING_PROFILES: "{{ .Values.SPRING_PROFILES }}"
SWAGGER_ENABLED: "{{ .Values.SWAGGER_ENABLED }}"
VAULT_ENABLE: "{{ .Values.VAULT_ENABLE}}"
VAULT_NAMESPACE: "{{ .Values.VAULT_NAMESPACE}}"
VAULT_ROLE_ID: "{{ .Values.VAULT_ROLE_ID}}"
VAULT_URI: "{{ .Values.VAULT_URI}}"
ZIPKIN_ENABLE: "{{ .Values.ZIPKIN_ENABLE }}"
ZIPKIN_PROBABILITY: "{{ .Values.ZIPKIN_PROBABILITY }}"
ZIPKIN_URL: "{{ .Values.ZIPKIN_URL }}"
AUDIT_LOGGING_TOPIC_PARTITIONS: '{{ .Values.AUDIT_LOGGING_TOPIC_PARTITIONS }}'
AUDIT_LOGGING_TOPIC_REPLICAS: "{{ include "env.factor" (dict "ctx" . "def" .Values.AUDIT_LOGGING_TOPIC_REPLICAS) }}"
{{- end }}

{{/* Sensitive data to be converted into secrets whenever possible */}}
{{- define "env.secrets" }}
DATASET_PG_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.DATASET_PG_PASSWORD "def" "atp-dataset") }}"
DATASET_PG_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.DATASET_PG_USER "def" "atp-dataset") }}"
DATASET_GRIDFS_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.DATASET_GRIDFS_PASSWORD "def" "atp-dataset") }}"
DATASET_GRIDFS_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.DATASET_GRIDFS_USER "def" "atp-dataset") }}"
EI_GRIDFS_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_PASSWORD "def" "atp-ei-gridfs") }}"
EI_GRIDFS_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_USER "def" "atp-ei-gridfs") }}"
KEYCLOAK_CLIENT_NAME: "{{ default "atp-dataset" .Values.KEYCLOAK_CLIENT_NAME }}"
KEYCLOAK_SECRET: "{{ default "32608a37-4245-447d-b9b2-83261ee3a350" .Values.KEYCLOAK_SECRET }}"
VAULT_SECRET_ID: "{{ default "" .Values.VAULT_SECRET_ID }}"
{{- end }}

{{- define "env.deploy" }}
ei_gridfs_pass: "{{ .Values.ei_gridfs_pass }}"
ei_gridfs_user: "{{ .Values.ei_gridfs_user }}"
mongo_pass: "{{ .Values.mongo_pass }}"
mongo_user: "{{ .Values.mongo_user }}"
pg_pass: "{{ .Values.pg_pass }}"
pg_user: "{{ .Values.pg_user }}"
{{- end }}
