package github.alpsbte.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import github.alpsbte.PlotSystemTerra;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class DatabaseConnection {

    private final static HikariConfig config = new HikariConfig();
    private static HikariDataSource dataSource;

    public static void InitializeDatabase() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            Bukkit.getLogger().log(Level.INFO, "Successfully registered MariaDB JDBC Driver!");

            FileConfiguration configFile = PlotSystemTerra.getPlugin().getConfig();

            config.setJdbcUrl(configFile.getString("database.url") + configFile.getString("database.name"));
            config.setUsername(configFile.getString("database.username"));
            config.setPassword(configFile.getString("database.password"));
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while initializing database!", ex);
        }
    }

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

}
