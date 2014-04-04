package com.thomsonreuters.lsps.transmart.cfg

import groovy.sql.Sql

/**
 * Created by bondarev on 4/3/14.
 */
class Database {
    def config
    DatabaseType databaseType = DatabaseType.Unknown
    String database
    String host
    int port = -1

    Database(dbConfig) {
        config = dbConfig
        parseJdbcConnectionString()
    }

    void withSql(Closure block) {
        Sql.withInstance(config.jdbcConnectionString, config.username, config.password, config.jdbcDriver, block)
    }

    private final def parseJdbcConnectionString() {
        if (config.jdbcConnectionString) {
            if (config.jdbcConnectionString.startsWith('jdbc:postgresql:')) {
                parsePostgresJdbcConnectionString()
            } else {
                parseOracleJdbcConnectionString()
            }
        }
    }

    private void parseOracleJdbcConnectionString() {
        def match = config.jdbcConnectionString =~ /^jdbc:oracle:thin:@(\w+)?(?::(\d+))?:(\w+)$/ ?:
                config.jdbcConnectionString =~ /^jdbc:oracle:thin:@\/\/(\w+)?(?::(\d+))?\/(\w+)$/
        if (match.size()) {
            databaseType = DatabaseType.Oracle
            host = match[0][1] ?: 'localhost'
            port = match[0][2]?.asType(Integer.class) ?: 1521
            database = match[0][3]
        }
    }

    private void parsePostgresJdbcConnectionString() {
        def match = config.jdbcConnectionString =~ /^jdbc:postgresql:(?:\/\/(\w+)(?::(\d+))?\/)?(\w+)(?:\?.*)?$/
        if (match) {
            databaseType = DatabaseType.PostgreSQL
            host = match[0][1] ?: 'localhost'
            port = match[0][2]?.asType(Integer.class) ?: 5432
            database = match[0][3]
        }
    }

    boolean isLocal() {
        host == 'localhost' || host == '127.0.0.1'
    }

    boolean isPostgresConnection() {
        databaseType == DatabaseType.PostgreSQL
    }
}