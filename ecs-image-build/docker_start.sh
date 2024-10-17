#!/bin/bash
#
# Start script for company-metrics-consumer

PORT=8080
exec java -jar -Dserver.port="${PORT}" "company-metrics-consumer.jar"
