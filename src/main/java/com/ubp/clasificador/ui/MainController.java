package com.ubp.clasificador.ui;

// Imports de JavaFX
import com.ubp.clasificador.task.FolderProcessingTask;
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
        directoryChooser.setTitle("Seleccionar Carpeta Raíz");
        File selectedDirectory = directoryChooser.showDialog(selectFolderButton.getScene().getWindow());

    if (selectedDirectory != null) {
        Path rootFolderPath = selectedDirectory.toPath();
        folderPathLabel.setText("Ruta Raíz: " + rootFolderPath.toString());
        fileResults.clear();
        progressBar.setProgress(0);
        selectFolderButton.setDisable(true);
        statusLabel.setText("Buscando subcarpetas...");

        // Usamos un try-catch porque Files.list() puede lanzar IOException
        try {
            // 1. Obtener la lista de todas las subcarpetas
            List<Path> subfolders;
            try (java.util.stream.Stream<Path> paths = Files.list(rootFolderPath)) {
                subfolders = paths.filter(Files::isDirectory).collect(Collectors.toList());
            }

            if (subfolders.isEmpty()) {
                statusLabel.setText("No se encontraron subcarpetas. Analizando carpeta raíz...");
                // Si no hay subcarpetas, al menos analiza la carpeta raíz
                subfolders.add(rootFolderPath);
            }

            final int totalFolders = subfolders.size();
            final AtomicInteger completedFolders = new AtomicInteger(0); // Contador atómico para progreso thread-safe
            statusLabel.setText("Encontradas " + totalFolders + " carpetas. Lanzando hilos de análisis...");

            // 2. Por cada subcarpeta, lanzar una tarea de procesamiento
            for (Path folder : subfolders) {
                FolderProcessingTask task = new FolderProcessingTask(folder);

                task.setOnSucceeded(event -> {
                    // 3. Actualizar el progreso cuando una tarea termina
                    int done = completedFolders.incrementAndGet();
                    progressBar.setProgress((double) done / totalFolders);
                    if (done == totalFolders) {
                        statusLabel.setText("¡Análisis completado en " + totalFolders + " carpetas!");
                        selectFolderButton.setDisable(false);
                    }
                });
                
                task.setOnFailed(event -> {
                    // Manejo de errores si una tarea específica falla
                    System.err.println("Falló la tarea para la carpeta: " + folder);
                    task.getException().printStackTrace();
                    int done = completedFolders.incrementAndGet(); // Contar también las fallidas para que la barra avance
                    if (done == totalFolders) {
                        statusLabel.setText("Análisis completado con errores.");
                        selectFolderButton.setDisable(false);
                    }
                });

                // 4. Enviar la tarea al pool de hilos para su ejecución
                executorService.submit(task);
            }

        } catch (IOException e) {
            statusLabel.setText("Error al leer el directorio: " + e.getMessage());
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