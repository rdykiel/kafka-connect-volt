/*
 * This file is part of VoltDB.
 * Copyright (C) 2020 VoltDB Inc.
 */

package org.voltdb.kafka.connect.common;

import java.io.IOException;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.data.Values;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.storage.Converter;
import org.apache.kafka.connect.storage.ConverterConfig;
import org.apache.kafka.connect.storage.HeaderConverter;

/**
 * Simple byte array converter inspired by {@link org.apache.kafka.connect.storage.StringConverter}
 */
public class ByteArrayConverter extends Values implements Converter, HeaderConverter {

    private final ByteArraySerializer m_serializer = new ByteArraySerializer();
    private final ByteArrayDeserializer m_deserializer = new ByteArrayDeserializer();

    private final static ConfigDef CONFIG = ConverterConfig.newConfigDef();

    @Override
    public void configure(Map<String, ?> arg0) {
        // No-op

    }

    @Override
    public void configure(Map<String, ?> arg0, boolean arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public ConfigDef config() {
        return CONFIG;
    }

    @Override
    public byte[] fromConnectHeader(String topic, String headerKey, Schema schema, Object value) {
        return fromConnectData(topic, schema, value);
    }

    @Override
    public SchemaAndValue toConnectHeader(String topic, String headerKey, byte[] value) {
        return toConnectData(topic, value);
    }

    @Override
    public byte[] fromConnectData(String topic, Schema schema, Object value) {
        try {
            byte[] bytes = (byte[]) convertTo(Schema.OPTIONAL_BYTES_SCHEMA, schema, value);
            return m_serializer.serialize(topic, bytes);
        } catch (SerializationException e) {
            throw new DataException("Failed to serialize to a byte array: ", e);
        }
    }

    @Override
    public SchemaAndValue toConnectData(String topic, byte[] value) {
        try {
            return new SchemaAndValue(Schema.OPTIONAL_BYTES_SCHEMA, m_deserializer.deserialize(topic, value));
        } catch (SerializationException e) {
            throw new DataException("Failed to deserialize a byte array: ", e);
        }
    }

}
