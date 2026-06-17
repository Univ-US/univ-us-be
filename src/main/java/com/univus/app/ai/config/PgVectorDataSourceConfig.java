package com.univus.app.ai.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.sql.DataSource;

@EnableAsync
@Configuration
public class PgVectorDataSourceConfig {

    @Primary
    @Bean(name = "dataSource")
    public DataSource oracleDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.hikari.username}") String username,
            @Value("${spring.datasource.hikari.password}") String password) {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("oracle.jdbc.OracleDriver")
                .build();
    }

    @Bean("pgVectorDataSource")
    public DataSource pgVectorDataSource(
            @Value("${pgvector.datasource.url}") String url,
            @Value("${pgvector.datasource.username}") String username,
            @Value("${pgvector.datasource.password}") String password) {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean("pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
