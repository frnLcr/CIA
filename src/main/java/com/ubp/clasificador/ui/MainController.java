package com.ubp.clasificador.ui;

// Imports de JavaFX
import com.ubp.clasificador.task.FileProcessingTask;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;

// Imports de Java Standard
import java.io.File;
import java.io.IOException; // <--- ASEGÚRATE DE TENER ESTE
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class MainController {

    @FXML private Button selectFolderButton;
    @FXML private Label folderPathLabel;
    @FXML private TableView<ResultTableEntry> resultsTable;
    @FXML private TableColumn<ResultTableEntry, String> fileNameColumn;
    @FXML private TableColumn<ResultTableEntry, String> fileExtensionColumn;
    @FXML private TableColumn<ResultTableEntry, String> predictedCategoryColumn;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;

    private ObservableList<ResultTableEntry> fileResults;
    private ExecutorService executorService;

    // Método que se llama automáticamente después de que el FXML es cargado
    @FXML
    public void initialize() {
        fileResults = FXCollections.observableArrayList();
        resultsTable.setItems(fileResults);

        // Configurar las celdas de las columnas para mapear a las propiedades de ResultTableEntry
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        fileExtensionColumn.setCellValueFactory(new PropertyValueFactory<>("fileExtension"));
        predictedCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("predictedCategory"));

        // Usar un pool de hilos para procesar carpetas en paralelo.
        // El tamaño puede ser el número de núcleos de la CPU para un rendimiento óptimo.
        int coreCount = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newFixedThreadPool(coreCount);

        // Estado inicial de la barra de progreso
        progressBar.setProgress(0);
        statusLabel.setText("Listo para seleccionar una carpeta...");
    }

    // Getter para la lista de resultados, usada por la tarea concurrente
    public ObservableList<ResultTableEntry> getFileResults() {
        return fileResults;
    }

    @FXML
private void handleSelectFolder() {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Seleccionar Carpeta para Analizar");
    File selectedDirectory = directoryChooser.showDialog(selectFolderButton.getScene().getWindow());

    if (selectedDirectory != null) {
        Path rootFolderPath = selectedDirectory.toPath();
        folderPathLabel.setText("Analizando: " + rootFolderPath.toString());
        fileResults.clear();
        progressBar.setProgress(0);
        selectFolderButton.setDisable(true);
        statusLabel.setText("Buscando todos los archivos...");

        try {
            // 1. Obtener la lista de TODOS los archivos recursivamente
            List<Path> allFiles;
            try (var pathStream = Files.walk(rootFolderPath)) {
                allFiles = pathStream.filter(Files::isRegularFile).collect(Collectors.toList());
            }

            if (allFiles.isEmpty()) {
                statusLabel.setText("No se encontraron archivos en la carpeta seleccionada.");
                selectFolderButton.setDisable(false);
                return; // Salir si no hay nada que hacer
            }

            final int totalFiles = allFiles.size();
            final AtomicInteger completedFiles = new AtomicInteger(0);
            statusLabel.setText("Encontrados " + totalFiles + " archivos. Iniciando análisis paralelo...");

            // 2. Por CADA ARCHIVO, lanzar una tarea de procesamiento
            for (Path file : allFiles) {
                FileProcessingTask task = new FileProcessingTask(file); // Usamos la nueva Tarea

                task.setOnSucceeded(event -> {
                    int done = completedFiles.incrementAndGet();
                    progressBar.setProgress((double) done / totalFiles);
                    if (done == totalFiles) {
                        statusLabel.setText("¡Análisis completado para " + totalFiles + " archivos!");
                        selectFolderButton.setDisable(false);
                    }
                });

                task.setOnFailed(event -> {
                    System.err.println("Falló la tarea para el archivo: " + file);
                    if (task.getException() != null) {
                        task.getException().printStackTrace();
                    }
                    int done = completedFiles.incrementAndGet();
                    if (done == totalFiles) {
                        statusLabel.setText("Análisis completado con errores.");
                        selectFolderButton.setDisable(false);
                    }
                });

                // 3. Enviar la tarea al pool de hilos
                executorService.submit(task);
            }

        } catch (IOException e) {
            statusLabel.setText("Error al escanear el directorio: " + e.getMessage());
            selectFolderButton.setDisable(false);
            e.printStackTrace();
        }
    }
}

    // Método para detener el ExecutorService cuando la aplicación se cierra (IMPORTANTE)
    public void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // Intenta detener todas las tareas en ejecución
        }
    }
}