package com.alpsbte.plotsystemterra.core;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.core.plotsystem.CityProject;
import com.alpsbte.plotsystemterra.core.plotsystem.Country;
import com.alpsbte.plotsystemterra.core.plotsystem.FTPConfiguration;
import com.alpsbte.plotsystemterra.core.plotsystem.Plot;
import com.alpsbte.plotsystemterra.core.plotsystem.Server;
import com.alpsbte.plotsystemterra.utils.Utils;
import com.sk89q.worldedit.Vector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.time.LocalDate;

public class DatabaseConnection implements Connection{

    private final static HikariConfig config = new HikariConfig();
    private static HikariDataSource dataSource;

    private static int connectionClosed, connectionOpened;
    private String teamAPIKey;

    public DatabaseConnection(String dbURL, String dbName, String username, String password, String teamAPIKey) throws ClassNotFoundException {
        this.teamAPIKey = teamAPIKey;

        Class.forName("org.mariadb.jdbc.Driver");



        config.setJdbcUrl(dbURL + dbName);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");


        dataSource = new HikariDataSource(config);
    }

    private static java.sql.Connection getSqlConnection() {
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

    private static StatementBuilder createStatement(String sql) {
        return new StatementBuilder(sql);
    }

    private static void closeResultSet(ResultSet resultSet) throws SQLException {
        if(resultSet.isClosed()
        && resultSet.getStatement().isClosed()
        && resultSet.getStatement().getConnection().isClosed())
            return;

        resultSet.close();
        resultSet.getStatement().close();
        resultSet.getStatement().getConnection().close();

        connectionClosed++;

        if(connectionOpened > connectionClosed + 5)
            Bukkit.getLogger().log(Level.SEVERE, "There are multiple database connections opened. Please report this issue.");
    }

    private static class StatementBuilder {
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
            java.sql.Connection con = dataSource.getConnection();
            PreparedStatement ps = Objects.requireNonNull(con).prepareStatement(sql);
            ResultSet rs = iterateValues(ps).executeQuery();

            connectionOpened++;

            return rs;
        }

        public void executeUpdate() throws SQLException {
            try (java.sql.Connection con = dataSource.getConnection()) {
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

    // @Override
    // public List<Integer> getBuilders() throws Exception{
    //     List<Integer> builderIDs = new ArrayList<>();
    //     ResultSet rs = createStatement("SELECT id FROM plotsystem_builders")
    //         .executeQuery();

    //     while (rs.next()) {
    //         builderIDs.add(rs.getInt("id"));
    //     }
    //     return builderIDs;
    // }

    private FTPConfiguration loadFtpConfiguration(int ID) {
        FTPConfiguration config = null;

        try (ResultSet rs = createStatement("SELECT schematics_path, address, port, isSFTP, username, password FROM plotsystem_ftp_configurations WHERE id = ?")
                .setValue(ID).executeQuery()) {

            if (rs.next()) {
                config = new FTPConfiguration(
                    ID, rs.getString(1),
                     rs.getString(2),
                     rs.getInt(3), rs.getBoolean(4), rs.getString(5), rs.getString(6));
            }

            closeResultSet(rs);

        }catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not check for FTP-Configuration for city project (" + ID + ")!", ex);
            return null;
        }
        return config;
    }

    @Override
    public FTPConfiguration getFTPConfiguration(CityProject cityProject) {
        try (ResultSet rsServer = createStatement("SELECT server_id FROM plotsystem_countries WHERE id = ?")
            .setValue(cityProject.country_id).executeQuery()) {

        if (rsServer.next()) {
            try (ResultSet rsFTP = createStatement("SELECT ftp_configuration_id FROM plotsystem_servers WHERE id = ?")
                    .setValue(rsServer.getInt(1)).executeQuery()) {

                if (rsFTP.next()) {
                    int ftpID = rsFTP.getInt(1);
                    if (!rsFTP.wasNull()){
                        closeResultSet(rsServer);
                        closeResultSet(rsFTP);
                        return loadFtpConfiguration(ftpID);
                    }
                }

                closeResultSet(rsFTP);
            }
        }

        closeResultSet(rsServer);

    } catch (SQLException ex) {
        Bukkit.getLogger().log(Level.SEVERE, "Could not check for FTP-Configuration for city project (" + cityProject.id + ")!", ex);
    }
    return null;
    }


    @Override
    public CityProject getCityProject(int cityID) {
  
        try (ResultSet rsCity = createStatement("SELECT country_id, name FROM plotsystem_city_projects WHERE id = ?")
                .setValue(cityID).executeQuery()) {
            int countryID = 0;
            String headID = "";
            String name = "";

            if (rsCity.next()) {
                countryID = rsCity.getInt(1);
                name = rsCity.getString(2);
            }

            closeResultSet(rsCity);

            // try (ResultSet rsCountry = createStatement("SELECT head_id FROM plotsystem_countries WHERE id = ?")
            //         .setValue(countryID).executeQuery();) {

            //     if (rsCountry.next()) {
            //         headID = rsCountry.getString(1);
            //     }

            //     closeResultSet(rsCountry);

            // }
            return new CityProject(cityID, countryID, name);
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while getting country head!", ex);
        }
        return null;
    }

    @Override
    public boolean getAllCityProjects(List<CityProject> resultList) {
        //FIXME limit query to cities from the current buildteam

        //try (ResultSet rs = createStatement("SELECT id FROM plotsystem_city_projects").executeQuery()) {
        try (ResultSet rs = createStatement(
            "SELECT cities.id as city_id " + //
                "FROM plotsystem_city_projects cities " + //
                "INNER JOIN plotsystem_buildteam_has_countries team_countries ON team_countries.country_id = cities.country_id " + //
                "INNER JOIN plotsystem_buildteams teams ON teams.id = team_countries.buildteam_id " + //
                "INNER JOIN plotsystem_api_keys apikeys ON apikeys.id = teams.api_key_id " + //
                "WHERE apikeys.api_key = ?").setValue(this.teamAPIKey).executeQuery()) {
        
            while (rs.next()) {
                int cityID = rs.getInt("city_id");
                //CityProject city = new CityProject(rs.getInt(1));
                resultList.add(getCityProject(cityID));
            }

            closeResultSet(rs);

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            return false;
        }

        return true;
    }
    
    @Override
    public int prepareCreatePlot(CityProject cityProject, int difficultyID, Vector plotCenter, String polyOutline, Player player, double plotVersion) throws Exception{
        java.sql.Connection sqlConnection = getSqlConnection();

        if (sqlConnection != null) {

            sqlConnection.setAutoCommit(false);
        
            try (PreparedStatement stmt = Objects.requireNonNull(sqlConnection)
                .prepareStatement("INSERT INTO plotsystem_plots (city_project_id, difficulty_id, mc_coordinates, outline, create_date, create_player, version) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) 
                {
                stmt.setInt(1, cityProject.id);
                stmt.setInt(2, difficultyID);
                stmt.setString(3, plotCenter.getX() + "," + plotCenter.getY() + "," + plotCenter.getZ());
                stmt.setString(4, polyOutline);
                stmt.setDate(5, java.sql.Date.valueOf(LocalDate.now()));
                stmt.setString(6, player.getUniqueId().toString());
                stmt.setDouble(7, plotVersion);
                stmt.executeUpdate();

                // Get the id of the new plot
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int plotID = rs.getInt(1);
                        return plotID;
                    } else throw new SQLException("Could not obtain generated key");
                }
            }
        } else throw new SQLException("Could not connect to database");
        
    }




    @Override
    public void commitPlot() throws Exception{
        getSqlConnection().commit();
        getSqlConnection().setAutoCommit(true);
    }

    @Override
    public void rollbackPlot() throws Exception{
        getSqlConnection().rollback();
        getSqlConnection().setAutoCommit(true);
        getSqlConnection().close();
    }


    @Override
    public Plot getPlot(int plotID) throws Exception {
        try (ResultSet rs = createStatement("SELECT status, city_project_id, mc_coordinates, version, pasted FROM plotsystem_plots WHERE id = ?")
                .setValue(plotID).executeQuery()) {

            Plot plot = null;
            if (rs.next()) {
                
                String[] splitCoordinates = rs.getString(3).split(",");
                Vector mcCoordinates = Vector.toBlockPoint(
                        Float.parseFloat(splitCoordinates[0]),
                        Float.parseFloat(splitCoordinates[1]),
                        Float.parseFloat(splitCoordinates[2])
                );
                //int id, String status, int city_project_id, Vector mc_coordinates, double version
                plot = new Plot(plotID, rs.getString("status"),
                    rs.getInt("city_project_id"), 
                    mcCoordinates, rs.getInt("pasted"), rs.getDouble("version"));
                    
            } 
            closeResultSet(rs);
            return plot;
        }
        catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while getting plot with the ID " + plotID + "!", ex);
            return null;
        }
    }

    @Override
    public List<Plot> getCompletedAndUnpastedPlots() throws Exception {
        List<Plot> plots = new ArrayList<>();
        
        try (ResultSet rs = createStatement("SELECT id, city_project_id, mc_coordinates, version FROM plotsystem_plots WHERE status = 'completed' AND pasted = '0' LIMIT 20")
            .executeQuery()) {

        if (rs.isBeforeFirst()) {
            while (rs.next()) {
                int plotID = -1;
                try {
                    plotID = rs.getInt(1);
                    //CityProject city = getCityProject(rs.getInt(2));
                    plots.add(getPlot(plotID));
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "An error occurred while pasting plot #" + plotID + "!", ex);
                }
            }


        }

        closeResultSet(rs);

        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "A SQL error occurred!", ex);
        }
        return plots;
    }

    @Override
    public Server getServer(int serverID) throws Exception{
        ResultSet rs = createStatement("SELECT * FROM plotsystem_servers WHERE id = ?")
            .setValue(serverID).executeQuery();
        if (rs.next()) {
            return new Server(rs.getInt("id"), rs.getInt("ftp_configuration_id"), rs.getString("name"));
        }
        return null;
    }

    @Override
    public void setPlotPasted(int plotID) throws Exception {
        createStatement("UPDATE plotsystem_plots SET pasted = '1' WHERE id = ?")
            .setValue(plotID).executeUpdate();
    }


    @Override
    public Country getCountry(int countryID) throws Exception {
        ResultSet rs = createStatement("SELECT * FROM plotsystem_countries WHERE id = ?")
            .setValue(countryID).executeQuery();
        if (rs.next()) {
            return new Country(rs.getInt("id"), rs.getString("head_id"),
                 rs.getString("continent"), rs.getString("name"), rs.getInt("server_id"));
        }
        return null;
    }

    @Override
    public FTPConfiguration getFTPConfiguration(int ftp_configuration_id) throws Exception {
        ResultSet rs = createStatement("SELECT * FROM plotsystem_ftp_configurations WHERE id = ?")
            .setValue(ftp_configuration_id).executeQuery();
        if (rs.next()) {
            return new FTPConfiguration(rs.getInt("id"), rs.getString("schematics_path"),
                 rs.getString("address"), rs.getInt("port"),rs.getBoolean("isSFTP"),
                 rs.getString("username"), rs.getString("password"));
        }
        return null;
    }

}
