package github.alpsbte.core.config;

import github.alpsbte.PlotSystemTerra;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.logging.Level;

public class ConfigManager {

    private final File configFile;
    private FileConfiguration config;

    public ConfigManager() throws ConfigNotImplementedException {
        configFile = Paths.get(PlotSystemTerra.getPlugin().getDataFolder().getAbsolutePath(), "config.yml").toFile();

        if(!configFile.exists()) {
            if (this.createConfig()) {
                Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "------------------------------------------------------");
                Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "The config file must be configured! (" + configFile.getAbsolutePath() + ")");
                Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "------------------------------------------------------");

                PlotSystemTerra.getPlugin().getServer().getPluginManager().disablePlugin(PlotSystemTerra.getPlugin());
                throw new ConfigNotImplementedException("The config file must be configured!");
            }
        }

        reloadConfig();
    }

    /**
     * Saves intern config to config file.
     *
     * @return - true if config saved successfully.
     */
    public boolean saveConfig() {
        try (BufferedWriter configWriter = new BufferedWriter(new FileWriter(configFile))){
            String configuration = this.prepareConfigString(config.saveToString());

            configWriter.write(configuration);
            configWriter.flush();
            return true;
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while saving config file!", ex);
        }
        return false;
    }

    /**
     * Reloads intern config from config file.
     *
     * @return - true if config reloaded successfully.
     */
    public boolean reloadConfig() {
        try (@NotNull Reader configReader = getConfigContent()){
            this.scanConfig();
            this.config = YamlConfiguration.loadConfiguration(configReader);
            return true;
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while reloading config file!", ex);
        }
        return false;
    }

    /**
     * Scans given file for tabs, very useful when loading YAML configuration.
     * Any configuration loaded using the API in this class is automatically scanned.
     *
     * @return - true if config scanned successfully.
     */
    public boolean scanConfig() {
        if (!configFile.exists()) return false;

        int lineNumber = 0;
        String line;
        try (Scanner scanner = new Scanner(configFile)) {
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                lineNumber++;

                if (line.contains("\t")) {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "------------------------------------------------------");
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Tab found in file \"" + configFile.getAbsolutePath() + "\" on line #" + lineNumber + "!");
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "------------------------------------------------------");
                    throw new IllegalArgumentException("Tab found in file \"" + configFile.getAbsolutePath() + "\" on line # " + line + "!");
                }
            }
            return true;
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while scanning config file!", ex);
        }
        return false;
    }

    /**
     * Create a new config with default values
     *
     * @return - true if the config is created.
     */
    public boolean createConfig() {
        try {
            if (configFile.createNewFile()) {
                try (InputStream defConfigStream = PlotSystemTerra.getPlugin().getResource("defaultConfig.yml")) {
                    try (OutputStream outputStream = new FileOutputStream(configFile)) {
                        int length;
                        byte[] buf = new byte[1024];
                        while ((length = defConfigStream.read(buf)) > 0) {
                            outputStream.write(buf, 0, length);
                        }
                    }
                }
                return true;
            }
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while creating config file!", ex);
        }
        return false;
    }

    /**
     * Prepares the config file for parsing with SnakeYAML.
     *
     * @param configString - The configuration as string.
     * @return - ready-to-parse config.
     */
    private String prepareConfigString (String configString) {
        String[] lines = configString.split("\n");
        StringBuilder config = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("COMMENT")) {
                String comment = "#" + line.substring(line.indexOf(":") + 1).replace("'", "");
                config.append(comment).append("\n");
            } else if (line.startsWith("EMPTY_SPACE")) {
                config.append("\n");
            } else {
                config.append(line).append("\n");
            }
        }

        return config.toString();
    }

    /**
     * Read file and make comments SnakeYAML friendly
     *
     * @return - file as InputStreamReader (Reader)
     */
    private Reader getConfigContent() {
        if (!configFile.exists()) return new InputStreamReader(IOUtils.toInputStream(""));

        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            int commentNum = 0;
            int emptySpaceNum = 0;
            String addLine;
            String currentLine;

            StringBuilder whole = new StringBuilder();

            // Convert config file
            while ((currentLine = reader.readLine()) != null) {
                // Add comment
                if (currentLine.startsWith("#")) {
                    addLine = (currentLine.replaceFirst("#", "COMMENT_" + commentNum + ":")
                            .replaceFirst(":", ": '") + "'")
                            .replaceFirst("' ", "'");
                    whole.append(addLine).append("\n");
                    commentNum++;

                    // Add empty space
                } else if (currentLine.equals("") || currentLine.equals(" ") || currentLine.isEmpty()) {
                    addLine = "EMPTY_SPACE_" + emptySpaceNum + ": ''";
                    whole.append(addLine).append("\n");
                    emptySpaceNum++;

                    // Add config value
                } else {
                    whole.append(currentLine).append("\n");
                }
            }
            String config = whole.toString();
            reader.close();

            return new InputStreamReader(new ByteArrayInputStream(config.getBytes()), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "An error occurred while parsing config file!", ex);
            return new InputStreamReader(IOUtils.toInputStream("", StandardCharsets.UTF_8));
        }
    }

    public FileConfiguration getConfig() { return config; }
}
