/*
 * This file is part of VoltDB.
 * Copyright (C) 2020 VoltDB Inc.
 */

package org.voltdb.kafka.connect.source;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.voltdb.kafka.connect.common.Constants;

/**
 *
 */
public class VoltSourceTask extends SourceTask {
    private static final Logger log = LoggerFactory.getLogger(VoltSourceTask.class);

    AbstractConfig m_parsedConfig;
    private String m_volt;
    private String m_group;
    private String m_prefix;
    private List<String> m_topics;
    private Integer m_count = 0;

    private KafkaConsumer<Object, Object> m_consumer;

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        KafkaConsumer<Object, Object> consumer = getConsumer();
        ConsumerRecords<Object, Object> polledRecords = consumer.poll(Duration.ofSeconds(1));
        if (polledRecords.count() == 0) {
            return null;
        }

        log.info("XXX " + this.getClass().getSimpleName() + " polled " + polledRecords.count() + " records");
        ArrayList<SourceRecord> records = new ArrayList<>();
        for (ConsumerRecord<Object, Object> polledRecord : polledRecords) {

            if (polledRecord.value() != null) {
                log.info("XXX " + this.getClass().getSimpleName() + " polled " + polledRecord.value().getClass().getName()
                        + ", topic " + polledRecord.topic() + ", partition " + polledRecord.partition());
                m_count += 1;

                // Assign a key since call to fromConnectData
                Object key = polledRecord.key() == null ? m_count.toString().getBytes() : polledRecord.key();
                SourceRecord record = new SourceRecord(
                        Collections.singletonMap(polledRecord.topic(), polledRecord.partition()),
                        Collections.singletonMap(polledRecord.topic(), polledRecord.offset()),
                        m_prefix + polledRecord.topic(), /* to kafka topic name prepended with prefix */
                        null,   /* don't assign partition */
                        Schema.OPTIONAL_BYTES_SCHEMA,
                        polledRecord.value());
                records.add(record);
            }
            else {
                log.info("XXX " + this.getClass().getSimpleName() + " polled null value"
                        + ", topic " + polledRecord.topic() + ", partition " + polledRecord.partition());
            }
        }
        return records;
    }

    @Override
    public void commit() {
        log.info("XXX " + this.getClass().getSimpleName() + " commits");
    }

    @Override
    public void start(Map<String, String> props) {
        m_parsedConfig = new AbstractConfig(VoltSourceConnector.CONFIG_DEF, props);
        m_volt = m_parsedConfig.getString(Constants.CONFIG_VOLT_LISTENERS);
        m_group = m_parsedConfig.getString(Constants.CONFIG_VOLT_CONSUMER_GROUP);
        m_prefix = m_parsedConfig.getString(Constants.CONFIG_VOLT_KAFKA_PREFIX);
        m_topics = m_parsedConfig.getList(Constants.CONFIG_VOLT_TOPICS);
        log.info("XXX started " + this);
    }

    @Override
    public void stop() {
        if (m_consumer != null) {
            m_consumer.close();
        }
        log.info("XXX stopped " + this);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "-" + version() + ": volt = " + m_volt
                + ", group = " + m_group + ", topics = " + m_topics;
    }

    private KafkaConsumer<Object, Object> getConsumer() {
        if (m_consumer == null) {
            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, m_volt);
            props.put(ConsumerConfig.GROUP_ID_CONFIG, m_group);
            props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "6000");
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                    "org.apache.kafka.common.serialization.ByteArrayDeserializer");
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                    "org.apache.kafka.common.serialization.ByteArrayDeserializer");

            m_consumer = new KafkaConsumer<>(props);
            m_consumer.subscribe(m_topics);
        }
        return m_consumer;
    }
}
