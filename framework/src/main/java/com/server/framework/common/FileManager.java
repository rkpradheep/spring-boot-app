package com.server.framework.common;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FileManager {

    public List<File> listFiles(String directory) {
        List<File> files = new ArrayList<>();
        try {
            Path path = Paths.get(directory);
            if (Files.exists(path) && Files.isDirectory(path)) {
                Files.list(path).forEach(filePath -> {
                    files.add(filePath.toFile());
                });
            }
        } catch (IOException e) {
        }
        return files;
    }

    public Map<String, Object> getFileInfo(String filePath) {
        Map<String, Object> info = new HashMap<>();
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                info.put("name", path.getFileName().toString());
                info.put("size", Files.size(path));
                info.put("isDirectory", Files.isDirectory(path));
                info.put("lastModified", Files.getLastModifiedTime(path).toMillis());
            }
        } catch (IOException e) {
            info.put("error", "Failed to get file info: " + e.getMessage());
        }
        return info;
    }

    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    public boolean uploadFile(String filePath, byte[] fileData) {
        try {
            Path path = Paths.get(filePath);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, fileData);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}








