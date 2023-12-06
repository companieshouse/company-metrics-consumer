company-metrics-consumer
=========================

company-metrics-consumer is responsible for transforming company metrics data from the company-metrics kafka as part of chips and chs data sync

## Development

Common commands used for development and running locally can be found in the Makefile, each make target has a 
description which can be listed by running `make help`

```text
Target               Description
------               -----------
all                  Calls methods required to build a locally runnable version, typically the build target
build                Pull down any dependencies and compile code into an executable if required
clean                Reset repo to pre-build state (i.e. a clean checkout state)
deps                 Install dependencies
docker/kafka         Run kafka and create topics within docker
docker/kafka-create-topics Create kafka topics within docker
docker/kafka-start   Run kafka within docker
docker/kafka-stop    Stop kafka within docker
package              Create a single versioned deployable package (i.e. jar, zip, tar, etc.). May be dependent on the build target being run before package
sonar                Run sonar scan
test                 Run all test-* targets (convenience method for developers)
test-integration     Run integration tests
test-unit            Run unit tests

```
## Running kafka locally
From root folder of this project run ```docker-compose up -d```

Once containers up, run ```docker-compose exec kafka bash``` to enter kafka bash to create topics

### Create kafka topics locally
kafka-topics.sh --create   --zookeeper zookeeper:2181   --replication-factor 1 --partitions 1   --topic company-metrics

kafka-topics.sh --create   --zookeeper zookeeper:2181   --replication-factor 1 --partitions 1   --topic company-metrics-retry

kafka-topics.sh --create   --zookeeper zookeeper:2181   --replication-factor 1 --partitions 1   --topic company-metrics-error

### Create kafka topics locally
kafka-topics.sh --list --zookeeper zookeeper:2181

### Produce kafka test messages locally
kafka-console-producer.sh --topic delta-topic --broker-list localhost:9092

#