package com.ubp.clasificador;

import com.ubp.clasificador.ml.ClassifierService;
import com.ubp.clasificador.ui.MainController; // ¡Importa MainController!
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainApp extends Application {

    public static MainController controllerInstance; 
    public static ClassifierService classifierService;

    @Override
    public void start(Stage stage) throws IOException {
        // ¡AÑADIR ESTA LÍNEA AQUÍ! Le dice a WEKA que no use optimizaciones nativas.
        System.setProperty("weka.core.maxres.native", "false");
        System.out.println("DEBUG: Propiedad 'weka.core.maxres.native' configurada a false.");


        classifierService = new ClassifierService();

        Path modelPath = Paths.get("model.ser");
        Path trainingDataRootPath = Paths.get("training_data");

        System.out.println("DEBUG: Directorio de trabajo actual: " + Paths.get(".").toAbsolutePath().normalize());
        System.out.println("DEBUG: Buscando training_data en: " + trainingDataRootPath.toAbsolutePath().normalize());
        System.out.println("DEBUG: ¿Existe training_data?: " + Files.exists(trainingDataRootPath));
        System.out.println("DEBUG: ¿Es training_data un directorio?: " + Files.isDirectory(trainingDataRootPath));

        if (!classifierService.loadModel(modelPath)) {
            System.out.println("No se encontró un modelo existente o hubo un error al cargarlo. Entrenando uno nuevo...");
            try {
                if (Files.exists(trainingDataRootPath) && Files.isDirectory(trainingDataRootPath)) {
                    classifierService.trainModel(trainingDataRootPath);
                    System.out.println("Modelo entrenado y guardado con éxito.");
                } else {
                    System.err.println("ERROR: La carpeta 'training_data' no se encontró o no es un directorio válido en la ruta esperada.");
                    throw new IllegalStateException("La carpeta 'training_data' no está disponible para el entrenamiento.");
                }
            } catch (Exception e) {
                System.err.println("Error al entrenar el modelo de IA: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // ¡VERIFICA ESTA LÍNEA! DEBE SER CORRECTA.
        // Asumiendo que main_view.fxml está en src/main/resources/com/ubp/clasificador/ui/
        // Y MainApp.java está en src/main/java/com/ubp/clasificador/
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("main_view.fxml")); // Línea 55
        
        // Si no funciona, intenta esta alternativa:
        // FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ui/main_view.fxml"));
        // O si estás seguro que MainController.class está en el mismo paquete que el FXML en resources:
        // FXMLLoader fxmlLoader = new FXMLLoader(MainController.class.getResource("main_view.fxml"));


        Scene scene = new Scene(fxmlLoader.load(), 900, 600);

        controllerInstance = fxmlLoader.getController();

        stage.setTitle("Clasificador Inteligente de Archivos");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (controllerInstance != null) {
            controllerInstance.shutdownExecutor();
        }
        System.out.println("Aplicación terminada. ExecutorService apagado.");
    }

    public static void main(String[] args) {
        launch();
    }
}