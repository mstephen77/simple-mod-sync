package dev.oxydien.utils;

import dev.oxydien.logger.Log;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {

    @Nullable
    public static Path PathExistsFromStartInDir(String dirPath, String search) {
        Path dir = Path.of(dirPath);
        if (Files.isDirectory(dir)) {
            try (var stream = Files.list(dir)) {
                for (var p : stream.toList()) {
                    if (p.getFileName().toString().startsWith(search)) {
                        return p;
                    }
                }
            } catch (IOException e) {
                Log.Log.error("path-utils.pefsid.IOException", "Error while searching for file in directory", e);
            }
        }
        return null;
    }

    public static boolean PathExists(String path) {
        return Files.exists(Path.of(path));
    }

    public static void CreateFolder(String path) {
        try {
            Files.createDirectories(Path.of(path));
        } catch (IOException e) {
            Log.Log.error("path-utils.create-folder.IOException", "Error while creating folder", e);
        }
    }

    /**
     * Sanitizes a given path by making sure it is a valid path and does not attempt to traverse outside the given base directory.
     * Used so the mod cannot access files outside the base (minecraft) directory.
     *
     * @param basePath The base directory to work from.
     * @param userProvidedPath The path specified by the user.
     * @return The sanitized path.
     * @throws SecurityException If the path is invalid or attempts to traverse outside the base directory.
     */
    public static String sanitizePath(String basePath, String userProvidedPath) {
        try {
            // Convert paths to canonical form
            Path baseDir = Paths.get(basePath).toAbsolutePath().normalize();
            Path requestedPath = baseDir.resolve(userProvidedPath).toAbsolutePath().normalize();

            // Check if the requested path starts with the base directory
            if (!requestedPath.startsWith(baseDir)) {
                throw new SecurityException("Path traversal attempt detected");
            }

            return requestedPath.toString();
        } catch (Exception e) {
            throw new SecurityException("Invalid path", e);
        }
    }
}
