#!/bin/sh
set -e

JAVA_OPTS=""

if [ "${JDWP_ENABLED}" = "true" ] || [ "${JDWP_ENABLED}" = "1" ]; then
  JDWP_PORT="${JDWP_PORT:-5005}"
  JDWP_SUSPEND="${JDWP_SUSPEND:-n}"
  JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=${JDWP_SUSPEND},address=*:${JDWP_PORT}"
  echo "JDWP remote debug enabled on port ${JDWP_PORT} (suspend=${JDWP_SUSPEND})"
fi

exec java ${JAVA_OPTS} -jar app.jar
