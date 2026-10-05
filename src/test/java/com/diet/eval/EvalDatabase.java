package com.diet.eval;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/** Creates a fresh, uniquely named test schema; never initializes or drops the user's schema. */
final class EvalDatabase {
    static final String SCHEMA = "diet_eval_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    static final String URL;
    static final String USER;
    static final String PASSWORD;
    static {
        try {
            YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
            yaml.setResources(new ClassPathResource("application.yml"));
            Properties properties = yaml.getObject();
            MutablePropertySources sources = new MutablePropertySources();
            sources.addLast(new MapPropertySource("system", (Map) System.getProperties()));
            sources.addLast(new SystemEnvironmentPropertySource("environment", (Map) System.getenv()));
            sources.addLast(new PropertiesPropertySource("yaml", properties));
            PropertyResolver resolver = new PropertySourcesPropertyResolver(sources);
            String original = resolver.getRequiredProperty("spring.datasource.url");
            USER = resolver.getRequiredProperty("spring.datasource.username");
            PASSWORD = resolver.getRequiredProperty("spring.datasource.password");
            int databaseStart = original.indexOf('/', "jdbc:mysql://".length());
            int queryStart = original.indexOf('?', databaseStart);
            if (!original.startsWith("jdbc:mysql://") || databaseStart < 0) throw new IllegalStateException("Expected MySQL JDBC URL");
            String host = original.substring(0, databaseStart + 1);
            String query = queryStart < 0 ? "" : original.substring(queryStart);
            URL = host + SCHEMA + query;
            try (var connection = DriverManager.getConnection(host + query, USER, PASSWORD)) {
                connection.createStatement().execute("CREATE DATABASE `" + SCHEMA + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                connection.setCatalog(SCHEMA);
                if (!SCHEMA.equals(connection.getCatalog())) throw new IllegalStateException("Evaluation schema isolation failed");
                ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource("db/diet_db.sql"), StandardCharsets.UTF_8));
            }
        } catch (Exception error) {
            throw new ExceptionInInitializerError(error);
        }
    }
}
