# kafka-connect-volt
A project implementing prototype source and a sink connectors allowing connecting a VoltDB database to Kafka via Kafka Connect. It has been tested against the Confluent 7.0.0 platform.

# Installing the connectors
This is an Eclipse project that has been exported as a jar file in the jar/ subdirectory. Install the jar file in the plugin path of Kafka Connect, e.g.:

    cp jar/kafka-connect-volt.jar ~/confluent/confluent-7.0.0/share/java

    ls -l ~/confluent/confluent-7.0.0/share/java/*.jar
    -rw-r--r--  1 rdykiel  staff  3388335 Dec  1 13:29 /Users/rdykiel/confluent/confluent-7.0.0/share/java/kafka-connect-volt.jar

# Running the connectors

The `etc/` directory contains demo VoltDB sql creating topics (volt1, volt2) and demo connector configuration files for the source and sink connectors:
- volt1 uses csv format
- volt2 uses avro format

The demo outlined below uses the confluent services with Kaka listening on its default port **9092** and VoltDB topics listening on non-default port **9095**.

## Start Confluent services
The confluent 7.0.0 services can be started as follows:

    confluent local start

This command starts all the services of the Confluent platform, including Kafka, Kafka Connect, and Avro Schema Registry. The Confluent platform must be stated first because the demo requires the presence of the schema registry.  

**Note:** On MacOS there is a bug in Confluent's version check (see https://github.com/confluentinc/confluent-cli/issues/136), which can be worked around as follows:

- create a **sw_vers** script as follows:

        cat ~/bin/sw_vers
        echo "10.13"

- then start the confluent services as follows:

        export PATH=~/bin:$PATH; confluent local start

## Create the voltdb topics
- Initialize VoltDB with a deployment file enabling topics (see `etc/depl.xml`).
        voltdb init --force  -C etc/depl.xml

- Start voltdb listening on port 9095
         voltdb start --topicsport=9095 &

- And create **both** topics, i.e.:

        sqlcmd < etc/volt1.sql
        sqlcmd < etc/volt2.sql

**Note:** both topics MUST be created, as the source connector configuration expects both to exist, even if you don't plan on testing both.

VoltDB listens on port **9095** for topics.

## Start the connectors
Start the volt connectors:

    curl -s -X POST -H 'Content-Type: application/json' --data @etc/sink-volt.json http://localhost:8083/connectors
    curl -s -X POST -H 'Content-Type: application/json' --data @etc/source-volt.json http://localhost:8083/connectors

Kafka listens on the standard port **9092** and Kafka Connect's REST interface on port **8083**.

Use Kafka connector's REST interface to check the connectors:

    curl -s localhost:8083/connectors | jq .
    curl -s localhost:8083/connectors/sink-volt | jq .
    curl -s localhost:8083/connectors/source-volt | jq .

The status of a connctor can be cheked as follows, and in case it is in error, can be restarted as follows after the root cause of the error is fixed:

    curl -s localhost:8083/connectors/source-volt/status | jq .
    curl -X POST localhost:8083/connectors/source-volt/tasks/0/restart | jq .

## Produce and consume a csv topic

Produce csv data to a topic `volt1` on Kafka: the volt sink connector consumes from Kafka topic `volt1` and pushes the data to Volt topic `volt1`, the volt source connector consumes data from Volt topic `volt1` and pushes the data to Kafka topic `loop_volt1`.

- Produce data to Kafka's volt1 topic, using CSV (end each record with `return`), and terminate with CTRL-C:

        kafka-console-producer --broker-list localhost:9092 --topic volt1
        >10,"first record"
        >20,"second record"
        >30,"third record"
        >^C%

- Check the `volt1` topic in Kafka:

        kafka-console-consumer --bootstrap-server localhost:9092 --topic volt1 --from-beginning
        10,"first record"
        20,"second record"
        30,"third record"
        ^CProcessed a total of 3 messages

- Check the `volt1` topic in VoltDB (note the reordering caused by Volt's partitioning):

        kafka-console-consumer --bootstrap-server localhost:9095 --topic volt1 --from-beginning
        20,second record
        10,first record
        30,third record
        ^CProcessed a total of 3 messages

- Check the `loop_volt1` topic in Kafka, proving that the source connector is listening on volt's topic `volt1`:

        kafka-console-consumer --bootstrap-server localhost:9092 --topic loop_volt1 --from-beginning
        10,first record
        20,second record
        30,third record
        ^CProcessed a total of 3 messages

## Produce and consume an Avro topic

Repeat the same scenario except on topic `volt2` which uses Avro with schemas registered in the Avro schema registry, and expect records to end up in Kafka's `loop_volt2` topic. The Confluent schema registry is listening on port **8081**.

- Because VoltDB and Kafka use slightly different schemas, it is necessary to configure the schema registry's compatibility as follows:

      curl -X PUT -H "Content-Type: application/vnd.schemaregistry.v1+json" \
      --data '{"compatibility": "NONE"}' \
      http://localhost:8081/config

- Produce data (note that this tool doesn't display any `>` input prompt):

      kafka-avro-console-producer \
             --bootstrap-server localhost:9092 --topic volt2 \
             --property  value.schema='{"type":"record","name":"volt2","fields":[{"name":"a","type":"int"},{"name":"b","type":"string"}]}'

      {"a":10,"b":"first record"}
      {"a":20,"b":"second record"}
      {"a":30,"b":"third record"}

- Verify `volt2` in Kafka:

      kafka-avro-console-consumer --bootstrap-server localhost:9092 --topic volt2 --from-beginning

      {"a":10,"b":"first record"}
      {"a":20,"b":"second record"}
      {"a":30,"b":"third record"}
      ^CProcessed a total of 3 messages

- Verify `volt2` in VoltDB (note that VoltDB uses uppercase for column names):

      kafka-avro-console-consumer --bootstrap-server localhost:9095 --topic volt2 --from-beginning

      {"A":{"int":10},"B":{"string":"first record"}}
      {"A":{"int":30},"B":{"string":"third record"}}
      {"A":{"int":20},"B":{"string":"second record"}}
      ^CProcessed a total of 3 messages

- Verify 'loop_volt2` in Kafka:

      kafka-avro-console-consumer --bootstrap-server localhost:9092 --topic loop_volt2 --from-beginning

      {"A":{"int":10},"B":{"string":"first record"}}
      {"A":{"int":30},"B":{"string":"third record"}}
      {"A":{"int":20},"B":{"string":"second record"}}
      ^CProcessed a total of 3 messages

## Cleanup demo

Once the demo is over, the Confluent platform can be stopped as follows:

    confluent local stop

It is recommended to destroy the Confluent state as well (note the `destroy` command can be called directly to both stop Confluent and destroy the state):

    confluent local destroy

VotlDB can be stopped and the data cleaned with the usual commands.
