#!/bin/bash
#
# Start script for company-metrics-consumer




PORT=8080
exec java -jar -Dserver.port="${PORT}" -XX:MaxRAMPercentage=80 "company-metrics-consumer.jar"
