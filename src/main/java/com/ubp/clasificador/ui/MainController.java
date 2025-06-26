package com.ubp.clasificador.ui;

import com.ubp.clasificador.MainApp; // Para acceder a la referencia estática del controlador
import com.ubp.clasificador.task.FileClassificationTask;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        // Inicializa el ExecutorService (un pool de un solo hilo para esta tarea)
        // Usamos un solo hilo para que las tareas se ejecuten secuencialmente y no saturen.
        executorService = Executors.newSingleThreadExecutor();

        // Estado inicial de la barra de progreso
        progressBar.setProgress(0);
        statusLabel.setText("Listo para seleccionar una carpeta...");
    }

    // Getter para la lista de resultados, usada por la tarea concurrente
    public ObservableList<ResultTableEntry> getFileResults() {
        return fileResults;
    }

    // Método llamado cuando el botón "Seleccionar Carpeta" es presionado
    @FXML
    private void handleSelectFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleccionar Carpeta para Clasificar");
        File selectedDirectory = directoryChooser.showDialog(selectFolderButton.getScene().getWindow());

        if (selectedDirectory != null) {
            Path folderPath = selectedDirectory.toPath();
            folderPathLabel.setText("Ruta de la Carpeta: " + folderPath.toString());

            // Limpiar resultados anteriores
            fileResults.clear();
            progressBar.setProgress(0); // Resetear progreso al iniciar nuevo escaneo

            // Crear y ejecutar la tarea de clasificación
            FileClassificationTask task = new FileClassificationTask(folderPath);

            // Enlazar propiedades de la tarea a los elementos de la UI
            progressBar.progressProperty().bind(task.progressProperty());
            statusLabel.textProperty().bind(task.messageProperty());

            // Deshabilitar botón mientras la tarea está en curso
            selectFolderButton.setDisable(true);

            // Manejar el final de la tarea (éxito o fallo)
            task.setOnSucceeded(event -> {
                statusLabel.textProperty().unbind();
                statusLabel.setText("Escaneo completado.");
                progressBar.progressProperty().unbind();
                progressBar.setProgress(1.0); // Asegurar que la barra muestre 100% al finalizar
                selectFolderButton.setDisable(false); // Habilitar botón
            });

            task.setOnFailed(event -> {
                statusLabel.textProperty().unbind();
                statusLabel.setText("Error durante el escaneo: " + task.getException().getMessage());
                task.getException().printStackTrace(); // Imprimir stack trace para depuración
                progressBar.progressProperty().unbind();
                progressBar.setProgress(0);
                selectFolderButton.setDisable(false); // Habilitar botón
            });

            task.setOnCancelled(event -> {
                statusLabel.textProperty().unbind();
                statusLabel.setText("Escaneo cancelado.");
                progressBar.progressProperty().unbind();
                progressBar.setProgress(0);
                selectFolderButton.setDisable(false); // Habilitar botón
            });

            // Iniciar la tarea en un hilo del ExecutorService
            executorService.submit(task);
        } else {
            statusLabel.setText("Selección de carpeta cancelada.");
        }
    }

    // Método para detener el ExecutorService cuando la aplicación se cierra (IMPORTANTE)
    public void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // Intenta detener todas las tareas en ejecución
        }
    }
}