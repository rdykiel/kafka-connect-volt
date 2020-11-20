/*
 * This file is part of VoltDB.
 * Copyright (C) 2020 VoltDB Inc.
 */

package org.voltdb.kafka.connect.sink;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.voltdb.kafka.connect.common.Constants;

/**
 *
 */
public class VoltSinkConnector extends SinkConnector {
    private static final Logger log = LoggerFactory.getLogger(VoltSinkTask.class);

    static final ConfigDef CONFIG_DEF = new ConfigDef()
        .define(Constants.CONFIG_VOLT_LISTENERS, Type.STRING, null, Importance.HIGH, "The volt topics listener")
        .define(SinkConnector.TOPICS_CONFIG, Type.LIST, null, Importance.HIGH, "The kafka topics to listen to");
;

    AbstractConfig m_parsedConfig;
    private String m_volt;
    private List<String> m_topics;

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        m_parsedConfig = new AbstractConfig(CONFIG_DEF, props);
        m_volt = m_parsedConfig.getString(Constants.CONFIG_VOLT_LISTENERS);
        if (m_volt == null || m_volt.isEmpty()) {
            throw new ConfigException(String.format(
                    "'%s' configuration requires non-empty string", Constants.CONFIG_VOLT_LISTENERS));
        }
        m_topics = m_parsedConfig.getList(SinkConnector.TOPICS_CONFIG);
        if (m_topics == null || m_topics.size() == 0) {
            throw new ConfigException(String.format(
                    "'%s' configuration requires non-empty topic list", SinkConnector.TOPICS_CONFIG));
        }
        log.info("XXX started " + this);
    }

    @Override
    public void stop() {
        log.info("XXX stopped " + this);
    }

    @Override
    public Class<? extends Task> taskClass() {
        return VoltSinkTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        // All tasks get the same config
        ArrayList<Map<String, String>> configs = new ArrayList<>();
        for (int i = 0; i < maxTasks; i++) {
            configs.add(m_parsedConfig.originalsStrings());
        }
        return configs;
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "-" + version() + ": volt = " + m_volt
                + ", topics = " + m_topics;
    }
}
