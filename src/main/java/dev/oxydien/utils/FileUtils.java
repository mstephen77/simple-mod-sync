package dev.oxydien.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

public class FileUtils {
    public static String ReadFile(String path) throws IOException {
        Path filePath = Paths.get(path);
        List<String> lines = Files.readAllLines(filePath);
        return String.join("\n", lines);
    }

    public static void WriteFile(String path, String content) throws IOException {
        Path filePath = Paths.get(path);
        Files.write(filePath, content.getBytes());
    }

    public static List<Path> UnZipFile(String zipFilePath, String destinationDirectory) throws IOException {
        List<Path> extractedFiles = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            zipFile.entries().asIterator().forEachRemaining(zipEntry -> {
                try {
                    if (zipEntry.isDirectory()) {
                        Path dir = Paths.get(destinationDirectory, zipEntry.getName());
                        Files.createDirectories(dir);
                        extractedFiles.add(dir.toAbsolutePath());
                    } else {
                        Path file = Paths.get(destinationDirectory, zipEntry.getName());
                        Files.createDirectories(file.getParent());
                        Files.copy(zipFile.getInputStream(zipEntry), file);
                        extractedFiles.add(file.toAbsolutePath());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return extractedFiles;
    }

    /**
     * Returns a list of all file paths under the given directory, recursively.
     * @param startPath The root directory path to start searching from
     * @return List of absolute file paths as strings
     */
    public static List<String> GetFilePaths(String startPath) {
        List<String> filePaths = new ArrayList<>();
        File startDir = new File(startPath);

        if (!startDir.exists() || !startDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory path: " + startPath);
        }

        collectFilePaths(startDir, filePaths);
        return filePaths;
    }

    private static void collectFilePaths(File directory, List<String> filePaths) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                filePaths.add(file.getAbsolutePath());
                if (file.isDirectory()) {
                    collectFilePaths(file, filePaths);
                }
            }
        }
    }
}
