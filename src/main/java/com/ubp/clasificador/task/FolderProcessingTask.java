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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tarea que procesa todos los archivos DENTRO de una única carpeta.
 * NO es recursiva. Se espera que se cree una instancia de esta tarea por cada carpeta a procesar.
 */

public class FolderProcessingTask extends Task<Void> {

    private final Path singleFolderPath;

    public FolderProcessingTask(Path singleFolderPath) {
        this.singleFolderPath = singleFolderPath;
    }

    @Override
    protected Void call() throws Exception {
        // Obtiene la lista de archivos solo en el nivel actual de la carpeta
        List<Path> filesInFolder = Files.list(singleFolderPath)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());

        for (Path filePath : filesInFolder) {
            if (isCancelled()) {
                break;
            }
            
            String fileName = filePath.getFileName().toString();
            String fileExtension = FileContentReader.getFileExtension(filePath);
            String predictedCategory;

            if (FileContentReader.isImageFile(filePath)) {
                predictedCategory = "IMAGES";
            } else if (FileContentReader.isTextFile(filePath)) {
                try {
                    List<String> tokens = FileContentReader.readAndPreprocessFile(filePath);
                    predictedCategory = MainApp.classifierService.classify(tokens);
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
        }
        return null;
    }
}