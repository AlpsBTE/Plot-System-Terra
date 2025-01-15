package com.alpsbte.plotsystemterra.utils;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;

import static net.kyori.adventure.text.Component.text;


public class Updater {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.54 Safari/537.36";
    // Direct download link
    private String downloadLink;
    // Provided plugin
    private final Plugin plugin;
    // The folder where update will be downloaded
    private final File updateFolder;
    // The plugin file
    private final File file;
    // ID of a project
    private final int id;
    // return a page
    private int page = 1;
    // Set the update type
    private final UpdateType updateType;
    // Get the outcome result
    private Result result = Result.SUCCESS;
    // If next page is empty set it to true, and get info from previous page.
    private boolean emptyPage;
    // Version returned from spigot
    private String version;
    // If true updater is going to log progress to the console.
    private final boolean logger;
    // Updater thread
    private final Thread thread;

    private static final String DOWNLOAD = "/download";
    private static final String VERSIONS = "/versions";
    private static final String PAGE = "?page=";
    private static final String API_RESOURCE = "https://api.spiget.org/v2/resources/";

    public Updater(Plugin plugin, int id, File file, UpdateType updateType, boolean logger) {
        this.plugin = plugin;
        this.updateFolder = plugin.getServer().getUpdateFolderFile();
        this.id = id;
        this.file = file;
        this.updateType = updateType;
        this.logger = logger;

        downloadLink = API_RESOURCE + id;

        thread = new Thread(new UpdaterRunnable());
        thread.start();
    }

    public enum UpdateType {
        // Checks only the version
        VERSION_CHECK,
        // Downloads without checking the version
        DOWNLOAD,
        // If updater finds new version automatically it downloads it.
        CHECK_DOWNLOAD

    }

    public enum Result {

        UPDATE_FOUND,

        NO_UPDATE,

        SUCCESS,

        FAILED,

        BAD_ID
    }

    /**
     * Get the result of the update.
     *
     * @return result of the update.
     * @see Result
     */
    public Result getResult() {
        waitThread();
        return result;
    }

    /**
     * Get the latest version from spigot.
     *
     * @return latest version.
     */
    public String getVersion() {
        waitThread();
        return version;
    }

    public static void notifyUpdate(String newVersion) {
        for (Player p : Bukkit.getOnlinePlayers())
            notifyUpdate(p, newVersion);

        // TODO: use components
        Bukkit.getConsoleSender().sendMessage(" ");
        Bukkit.getConsoleSender().sendMessage("§c[BuildTeam Plugin] §eThe server automatically installed a new update (v" + newVersion + ").");
        Bukkit.getConsoleSender().sendMessage("§c>> §ePlease restart or reload the server to activate it.");
        Bukkit.getConsoleSender().sendMessage(" ");

    }

    public static void notifyUpdate(Player p, String newVersion) {
        if (p.hasPermission("plotsystem.notifyUpdate")) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
            // TODO: use components
            p.sendMessage("");
            p.sendMessage("§6§l[BuildTeam Plugin] §eThe server automatically installed a new update (v" + newVersion + ").");
            p.sendMessage("§6>> §ePlease restart or reload the server to activate it.");
            p.sendMessage("");
        }
    }

    /**
     * Check if id of resource is valid
     *
     * @param link link of the resource
     * @return true if id of resource is valid
     */
    private boolean checkResource(String link) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(link))
                    .header("User-Agent", USER_AGENT)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                result = Result.BAD_ID;
                return false;
            }
        } catch (IOException | InterruptedException e) {
            PlotSystemTerra.getPlugin().getComponentLogger().error(text("An exception occurred while checking resource id!"), e);
        }
        return true;
    }

    /**
     * Checks if there is any update available.
     */
    private void checkUpdate() {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(API_RESOURCE + id + VERSIONS + PAGE + page))
                    .header("User-Agent", USER_AGENT)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return;

            JSONParser parser = new JSONParser();
            JsonElement element = (JsonElement) parser.parse(response.body());
            JsonArray jsonArray = element.getAsJsonArray();

            if (jsonArray.size() == 10 && !emptyPage) {
                this.page++;
                checkUpdate();
            } else if (jsonArray.isEmpty()) {
                emptyPage = true;
                this.page--;
                checkUpdate();
            } else if (jsonArray.size() < 10) {
                if (logger)
                    plugin.getLogger().info("Found " + jsonArray.size() + " versions.");
                element = jsonArray.get(jsonArray.size() - 1);
                JsonObject object = element.getAsJsonObject();
                element = object.get("name");

                version = element.toString().replaceAll("\"", "").replace("v", "");
                if (logger) {
                    plugin.getLogger().info("Current version on this server: " + plugin.getDescription().getVersion());
                    plugin.getLogger().info("Latest version available: " + version);
                    plugin.getLogger().info("Checking for update...");
                }
                if (shouldUpdate(version, plugin.getDescription().getVersion()) && updateType == UpdateType.VERSION_CHECK) {
                    result = Result.UPDATE_FOUND;
                    if (logger) plugin.getLogger().info("Update found!");
                } else if (updateType == UpdateType.DOWNLOAD) {
                    if (logger) plugin.getLogger().info("Downloading update... version not checked");
                    download();
                } else if (updateType == UpdateType.CHECK_DOWNLOAD) {
                    if (shouldUpdate(version, plugin.getDescription().getVersion())) {
                        if (logger) plugin.getLogger().info("Update found, downloading now...");
                        download();
                    } else {
                        if (logger) plugin.getLogger().info("Update not necessary. Plugin is at the latest version.");
                        result = Result.NO_UPDATE;
                    }
                } else {
                    if (logger) plugin.getLogger().info("Update not found");
                    result = Result.NO_UPDATE;
                }
            }
        } catch (IOException | InterruptedException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if plugin should be updated
     *
     * @param newVersion remote version
     * @param oldVersion current version
     */
    private boolean shouldUpdate(String newVersion, String oldVersion) {
        // If version has format 1.0.0
        if (newVersion.contains(".")) {
            String[] newVersionSplit = newVersion.split("\\.");
            String[] oldVersionSplit = oldVersion.split("\\.");

            for (int i = 0; i < newVersionSplit.length; i++) {
                try {
                    if (Integer.parseInt(newVersionSplit[i]) > Integer.parseInt(oldVersionSplit[i]))
                        return true;
                    else if (Integer.parseInt(newVersionSplit[i]) < Integer.parseInt(oldVersionSplit[i]))
                        return false;
                } catch (NumberFormatException e) {
                    return !newVersion.equalsIgnoreCase(oldVersion);
                }
            }

            return false;

            // If version is an integer
        } else if (newVersion.matches("[0-9]+")) {
            return Integer.parseInt(newVersion) > Integer.parseInt(oldVersion);

            // If version has a different format
        } else
            return !newVersion.equalsIgnoreCase(oldVersion);
    }

    /**
     * Downloads the file
     */
    private void download() {
        BufferedInputStream in = null;
        FileOutputStream fout = null;

        try {
            URL url = new URL(downloadLink);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("User-Agent", USER_AGENT);
            InputStream inputStream = connection.getInputStream();

            in = new BufferedInputStream(inputStream);
            if (!updateFolder.exists())
                updateFolder.mkdirs();
            fout = new FileOutputStream(new File(updateFolder, file.getName()));

            final byte[] data = new byte[4096];
            int count;
            while ((count = in.read(data, 0, 4096)) != -1) {
                fout.write(data, 0, count);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (logger)
                plugin.getLogger().log(Level.SEVERE, "Updater tried to download the update, but was unsuccessful.");
            result = Result.FAILED;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                this.plugin.getLogger().log(Level.SEVERE, null, e);
                e.printStackTrace();
            }
            try {
                if (fout != null) {
                    fout.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
                this.plugin.getLogger().log(Level.SEVERE, null, e);
            }

            PlotSystemTerra.getPlugin().setUpdateInstalled(version);
        }
    }

    /**
     * Updater depends on thread's completion, so it is necessary to wait for thread to finish.
     */
    private void waitThread() {
        if (thread != null && thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                this.plugin.getLogger().log(Level.SEVERE, null, e);
            }
        }
    }

    public class UpdaterRunnable implements Runnable {

        public void run() {
            if (checkResource(downloadLink)) {
                downloadLink = downloadLink + DOWNLOAD;
                checkUpdate();
            }
        }
    }
}
