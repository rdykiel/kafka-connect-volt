/*
 * This file is part of VoltDB.
 * Copyright (C) 2020 VoltDB Inc.
 */

package org.voltdb.kafka.connect.sink;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.connect.sink.SinkConnector;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.voltdb.kafka.connect.common.Constants;

/**
 *
 */
public class VoltSinkTask extends SinkTask {
    private static final Logger log = LoggerFactory.getLogger(VoltSinkTask.class);

    AbstractConfig m_parsedConfig;
    private String m_volt;
    private List<String> m_topics;  // used only for logging

    private KafkaProducer<Object, Object> m_producer;

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void put(Collection<SinkRecord> sinkRecords) {
        KafkaProducer<Object, Object> producer = getProducer();
        for (SinkRecord record : sinkRecords) {
            log.info("XXX record for topic " + record.topic() + ":" + record.kafkaPartition()
            + ", value = " + record.value() + ", class " + record.value().getClass().getName());

            producer.send(new ProducerRecord<Object, Object>(record.topic(), null, record.value()),
                    new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception e) {
                    if (e != null) {
                        log.error("Unexpected producer error: ", e);
                    }
                }
            });

        }
    }

    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> topics) {
        topics.forEach((k, v) -> log.info("XXX flushing " + k + " = " + v));
    }

    @Override
    public void start(Map<String, String> props) {
        m_parsedConfig = new AbstractConfig(VoltSinkConnector.CONFIG_DEF, props);
        m_volt = m_parsedConfig.getString(Constants.CONFIG_VOLT_LISTENERS);
        m_topics = m_parsedConfig.getList(SinkConnector.TOPICS_CONFIG);
        log.info("XXX started " + this);
    }

    @Override
    public void stop() {
        if (m_producer != null) {
            m_producer.close();
        }
        log.info("XXX stopped " + this);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "-" + version() + ": volt = " + m_volt
                + ", topics = " + m_topics;
    }

    private KafkaProducer<Object, Object> getProducer() {
        if (m_producer == null) {
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, m_volt);
            props.put(ProducerConfig.ACKS_CONFIG, "all");
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");

            // Timeout control when expecting no reply,
            setProducerTimeouts(props);
            m_producer = new KafkaProducer<>(props);
        }
        return m_producer;
    }

    private void setProducerTimeouts(Properties props) {
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 2000);
    }
}
