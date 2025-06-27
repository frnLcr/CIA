/*package com.ubp.clasificador.task;

import com.ubp.clasificador.MainApp;
import com.ubp.clasificador.ui.ResultTableEntry;
import com.ubp.clasificador.util.FileContentReader;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class FileClassificationTask extends Task<Void> {

    private final Path folderPath;

    public FileClassificationTask(Path folderPath) {
        this.folderPath = folderPath;
    }

    @Override
    protected Void call() throws Exception {
        if (!Files.isDirectory(folderPath)) {
            updateMessage("La ruta seleccionada no es un directorio.");
            updateProgress(0, 1);
            throw new IllegalArgumentException("La ruta no es un directorio.");
        }

        updateMessage("Contando archivos...");
        long totalFiles = 0;
        try (Stream<Path> files = Files.walk(folderPath)) {
            totalFiles = files.filter(Files::isRegularFile)
                              .count();
        } catch (IOException e) {
            updateMessage("Error al contar archivos: " + e.getMessage());
            throw e;
        }

        if (totalFiles == 0) {
            updateMessage("No se encontraron archivos en la carpeta.");
            updateProgress(1, 1);
            return null;
        }

        long processedFiles = 0;
        try (Stream<Path> files = Files.walk(folderPath)) {
            List<Path> filePaths = files.filter(Files::isRegularFile).toList();

            for (Path filePath : filePaths) {
                if (isCancelled()) {
                    updateMessage("Escaneo cancelado.");
                    break;
                }

                String fileName = filePath.getFileName().toString();
                String fileExtension = FileContentReader.getFileExtension(filePath);
                String predictedCategory = "Error de Clasificación"; // Default a error

                updateMessage("Procesando: " + fileName);

                if (FileContentReader.isImageFile(filePath)) {
                    predictedCategory = "IMAGES"; // Si es imagen, asigna esta categoría
                }
                            // 2. Si no es imagen, ¿es un archivo de texto? (Aquí entra el fragmento que preguntaste)
                else if (FileContentReader.isTextFile(filePath)) {
                    try {
                        List<String> preprocessedTokens = FileContentReader.readAndPreprocessFile(filePath);
                        // ACA ES DONDE LLAMAMOS AL SERVICIO DE CLASIFICACIÓN DE IA!
                        if (MainApp.classifierService != null) {
                            predictedCategory = MainApp.classifierService.classify(preprocessedTokens);
                        } else {
                            predictedCategory = "Modelo IA no cargado";
                        }
                    } catch (Exception e) { // Capturar Exception porque el clasificador puede lanzar varias
                        predictedCategory = "Error al clasificar (" + e.getMessage() + ")";
                        System.err.println("Error clasificando archivo " + filePath + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    predictedCategory = "No texto legible";
                }

                ResultTableEntry entry = new ResultTableEntry(fileName, fileExtension, predictedCategory);

                Platform.runLater(() -> {
                    if (MainApp.controllerInstance != null) {
                        MainApp.controllerInstance.getFileResults().add(entry);
                    }
                });

                processedFiles++;
                updateProgress(processedFiles, totalFiles);
            }
        }
        return null;
    }
}
*/