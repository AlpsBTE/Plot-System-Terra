package com.alpsbte.plotsystemterra.utils;

import com.alpsbte.plotsystemterra.core.plotsystem.FTPConfiguration;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class FTPManager {
    private static FileSystemOptions fileOptions;

    private final static String DEFAULT_SCHEMATIC_PATH_LINUX = "/var/lib/Plot-System/schematics";

    static {
        try {
            fileOptions = new FileSystemOptions();
            SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fileOptions, "no");
            SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(fileOptions, false);
        } catch (FileSystemException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Exception found with FileSystemManager!", ex);
        }
    }

    public static String getFTPUrl(FTPConfiguration ftpConfiguration, int cityID) {
        String schematicsPath = ftpConfiguration.getSchematicPath();

        return String.format("%sftp://%s:%s@%s:%d/%s/%s/%s/",
                ftpConfiguration.getPort() == 22 ? "s" : "",
                ftpConfiguration.getUsername(),
                ftpConfiguration.getPassword(),
                ftpConfiguration.getAddress(),
                ftpConfiguration.getPort(),
                schematicsPath == null ? DEFAULT_SCHEMATIC_PATH_LINUX : schematicsPath,
                "finishedSchematics",
                cityID
        );
    }

    public static CompletableFuture<Void> uploadSchematic(String ftpURL, File schematic) throws FileSystemException {
        try (StandardFileSystemManager fileManager = new StandardFileSystemManager()) {
            fileManager.init();

            // Get local schematic
            FileObject localSchematic = fileManager.toFileObject(schematic);

            // Get remote path and create missing directories
            FileObject remote = fileManager.resolveFile(ftpURL.replace("finishedSchematics/", ""), fileOptions);
            remote.createFolder();

            // Create remote schematic and write to it
            FileObject remoteSchematic = remote.resolveFile(schematic.getName());
            remoteSchematic.copyFrom(localSchematic, Selectors.SELECT_SELF);

            localSchematic.close();
            remoteSchematic.close();
        }
        return CompletableFuture.completedFuture(null);
    }

    public static CompletableFuture<Void> downloadSchematic(String ftpURL, File schematic) throws FileSystemException {
        try (StandardFileSystemManager fileManager = new StandardFileSystemManager()) {
            fileManager.init();

            // Get local schematic
            FileObject localSchematic = fileManager.toFileObject(schematic);

            // Get remote path
            FileObject remote = fileManager.resolveFile(ftpURL, fileOptions);

            // Get remote schematic and write it to local file
            FileObject remoteSchematic = remote.resolveFile(schematic.getName());
            localSchematic.copyFrom(remoteSchematic, Selectors.SELECT_SELF);

            localSchematic.close();
            remoteSchematic.close();
        }
        return CompletableFuture.completedFuture(null);
    }
}
