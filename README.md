# kafka-connect-volt
A project implementing prototype source and a sink connectors allowing connecting a VoltDB database to Kafka via Kafka Connect. It has been tested against the Confluent 5.5.1 platform.

# Installing the connectors
There is no build script; this is an Eclipse project. Export the project as a jar file and install the jar file in the plugin path of Kafka Connect, e.g.:

`▶ ls -l ~/confluent/confluent-5.5.1/share/java/*.jar
-rw-r--r--  1 rdykiel  staff  3384276 Nov  4 13:34 /Users/rdykiel/confluent/confluent-5.5.1/share/java/kafka-connect-volt.jar`

# Running the connectors

The `etc/` directory contains demo VoltDB sql creating topics (volt1, volt2) and demo connector configuration files for the source and sink connectors:
- volt1 uses csv format
- volt2 uses avro format

## Create the voltdb topics
Initialize VoltDB with a deployment file enabling topics on port 9095 (see `etc/depl.xml`). Start voltdb and create the topics, e.g.:

`sqlcmd < volt1.sql`

VoltDB listens on port 9095 for topics.

## Start the connectors
Start the Confluent platform and start the volt connectors:

`curl -s -X POST -H 'Content-Type: application/json' --data @sink-volt.json http://localhost:8083/connectors`  
`curl -s -X POST -H 'Content-Type: application/json' --data @source-volt.json http://localhost:8083/connectors`  

Kafka listens on the standard port 9092 and Kafka Connect's REST interface on port 8083.

Use Kafka connector's REST interface to check the connectors:

`curl -s localhost:8083/connectors | jq .`  
`curl -s localhost:8083/connectors/sink-volt | jq .`  
`curl -s localhost:8083/connectors/source-volt | jq .`  

## Produce and consume a csv topic

Produce csv data to a topic `volt1` on Kafka:
- the volt sink connector consumes from Kafka topic `volt1` and pushes the data to Volt topic `volt1`.
- the volt source connector consumes data from Volt topic `volt1` and pushes the data to Kafka topic `loop_volt1`

`kafka-console-producer --broker-list localhost:9092 --topic volt1`  
`kafka-console-consumer --bootstrap-server localhost:9092 --topic volt1 --from-beginning`  
`kafka-console-consumer --bootstrap-server localhost:9095 --topic volt1 --from-beginning`  
`kafka-console-consumer --bootstrap-server localhost:9092 --topic loop_volt1 --from-beginning`  

## Produce and consume an Avro topic

Repeat the same scenario except on topic `volt2` which uses Avro:

`kafka-avro-console-producer
         --bootstrap-server localhost:9092 --topic volt2
         --property value.schema='{"type":"record","name":"volt2","fields":[{"name":"a","type":"int"},{"name":"b","type":"string"}]}'`  

`kafka-avro-console-consumer --bootstrap-server localhost:9092 --topic volt2 --from-beginning`  
`kafka-avro-console-consumer --bootstrap-server localhost:9095 --topic volt2 --from-beginning`  
`kafka-avro-console-consumer --bootstrap-server localhost:9092 --topic loop_volt2 --from-beginning`  

Example input in the kafka-avro-console-producer:

`{"a":1,"b":"the quick brown fox"}`  
`{"a":2,"b":"jumps over the lazy dog"}`  
`{"a":3,"b":"portez ce vieux whisky"}`  
`{"a":4,"b":"au juge blond qui l'aime fort"}`  
