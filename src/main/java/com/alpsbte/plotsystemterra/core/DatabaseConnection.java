package com.alpsbte.plotsystemterra.core;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class DatabaseConnection {

    private final static HikariConfig config = new HikariConfig();
    private static HikariDataSource dataSource;

    public static void InitializeDatabase() throws ClassNotFoundException {
        Class.forName("org.mariadb.jdbc.Driver");

        FileConfiguration configFile = PlotSystemTerra.getPlugin().getConfig();
        String URL = configFile.getString(ConfigPaths.DATABASE_URL);
        String name = configFile.getString(ConfigPaths.DATABASE_NAME);
        String username = configFile.getString(ConfigPaths.DATABASE_USERNAME);
        String password = configFile.getString(ConfigPaths.DATABASE_PASSWORD);

        config.setJdbcUrl(URL + name);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
    }

    @Deprecated
    public static Connection getConnection() {
        int retries = 3;
        while (retries > 0) {
            try {
                return dataSource.getConnection();
            } catch (SQLException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Database connection failed!\n\n" + ex.getMessage());
            }
            retries--;
        }
        return null;
    }

    public static void closeResultSet(ResultSet resultSet) throws SQLException {
        resultSet.close();
        resultSet.getStatement().close();
        resultSet.getStatement().getConnection().close();
    }

    public static StatementBuilder createStatement(String sql) {
        return new StatementBuilder(sql);
    }

    /**
     * Returns a missing auto increment id
     * @param table in the database
     * @return smallest missing auto increment id in the table
     */
    public static int getTableID(String table) {
        try {
            String query ="SELECT id + 1 available_id FROM $table t WHERE NOT EXISTS (SELECT * FROM $table WHERE $table.id = t.id + 1) ORDER BY id LIMIT 1"
                    .replace("$table", table);
            try (ResultSet rs = DatabaseConnection.createStatement(query).executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                closeResultSet(rs);
                return 1;
            }
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "A SQL error occurred!", ex);
            return 1;
        }
    }

    public static class StatementBuilder {
        private final String sql;
        private final List<Object> values = new ArrayList<>();

        public StatementBuilder(String sql) {
            this.sql = sql;
        }

        public StatementBuilder setValue(Object value) {
            values.add(value);
            return this;
        }

        public ResultSet executeQuery() throws SQLException {
            Connection con = dataSource.getConnection();
            PreparedStatement ps = Objects.requireNonNull(con).prepareStatement(sql);
            return iterateValues(ps).executeQuery();
        }

        public void executeUpdate() throws SQLException {
            try (Connection con = dataSource.getConnection()) {
                try (PreparedStatement ps = Objects.requireNonNull(con).prepareStatement(sql)) {
                    iterateValues(ps).executeUpdate();
                }
            }
        }

        private PreparedStatement iterateValues(PreparedStatement ps) throws SQLException {
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            return ps;
        }
    }
}
