package com.ubp.clasificador.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileContentReader {

    public static List<String> readAndPreprocessFile(Path filePath) throws IOException {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new IOException("La ruta no apunta a un archivo válido: " + filePath);
        }

        String fileContent = Files.readString(filePath);
        return TextPreprocessor.preprocess(fileContent);
    }

    public static boolean isTextFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return false;
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase();

        return switch (extension) {
            case "txt", "csv", "log", "md", "json", "xml", "html", "css", "js",
                 "java", "py", "c", "cpp", "h", "hpp", "sh", "bat", "sql", "yml", "yaml",
                 "ini", "cfg", "properties", "conf" -> true; // <-- AÑADIDO "properties", "conf"
            default -> false;
        };
    }
    
    public static boolean isImageFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return false;
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase();

        return switch (extension) {
            case "jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp", "ico", "HEIC", "svg" -> true;
            default -> false;
        };
    }


    public static String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }
}