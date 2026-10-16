package com.example.outbox.outboxjob.config;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Configures two PostgreSQL datasources for the outbox job:
 *
 * <ul>
 *   <li><b>primary</b> ({@code spring.datasource}) - the primary Postgres instance, used for all
 *       WRITES (marking outbox records as {@code PUBLISHED}). Marked {@code @Primary} so Spring Data
 *       JPA / Hibernate use it by default.</li>
 *   <li><b>replica</b> ({@code spring.replica.datasource}) - a read-only Postgres replica, used only
 *       to READ pending outbox records. Its HikariCP pool is configured {@code read-only=true} so any
 *       accidental write is rejected by the driver.</li>
 * </ul>
 *
 * Reads and writes deliberately happen against different instances, so they are NOT part of the same
 * database transaction. The outbox publisher reads pending records from the replica, publishes them to
 * Kafka, and then persists the status change on the primary.
 */
@Configuration
public class DataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.replica.datasource")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public JdbcTemplate replicaJdbcTemplate(DataSource replicaDataSource) {
        return new JdbcTemplate(replicaDataSource);
    }
}
