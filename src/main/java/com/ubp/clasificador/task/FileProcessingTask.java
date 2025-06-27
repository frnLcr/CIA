/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ubp.clasificador.task;

/**
 *
 * @author frlcr
 */
import com.ubp.clasificador.MainApp;
import com.ubp.clasificador.ui.ResultTableEntry;
import com.ubp.clasificador.util.FileContentReader;
import javafx.application.Platform;
import javafx.concurrent.Task;
import java.nio.file.Path;
import java.util.List;

/**
 * Tarea que procesa un ÚNICO archivo.
 * Se espera que se cree una instancia de esta tarea por cada archivo a procesar.
 */

public class FileProcessingTask extends Task<Void> {

    private final Path filePath; // Ahora procesa la ruta de un archivo, no una carpeta

    public FileProcessingTask(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    protected Void call() throws Exception {
        if (isCancelled()) {
            return null;
        }

        // La lógica de clasificación es la misma, pero aplicada directamente al filePath
        String fileName = filePath.getFileName().toString();
        String fileExtension = FileContentReader.getFileExtension(filePath);
        String predictedCategory;

        if (FileContentReader.isImageFile(filePath)) {
            predictedCategory = "IMAGES (Recomendado)";
        } else if (FileContentReader.isTextFile(filePath)) {
            try {
                List<String> tokens = FileContentReader.readAndPreprocessFile(filePath);
                predictedCategory = MainApp.classifierService.classify(tokens) + " (Recomendado)";
            } catch (Exception e) {
                predictedCategory = "Error al Clasificar";
                System.err.println("Error clasificando " + fileName + ": " + e.getMessage());
            }
        } else {
            predictedCategory = "Otro (No recomendado)";
        }

        ResultTableEntry entry = new ResultTableEntry(fileName, fileExtension, predictedCategory);

        // Actualiza la UI de forma segura
        Platform.runLater(() -> MainApp.controllerInstance.getFileResults().add(entry));

        return null;
    }
}