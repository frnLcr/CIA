package com.ubp.clasificador.ml;

import com.ubp.clasificador.util.FileContentReader;
import com.ubp.clasificador.util.TextPreprocessor;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Importaciones de WEKA
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class ClassifierService {

    private Classifier classifier;
    private Instances dataFormat; // Formato de datos con el que se entrenó el clasificador (POST-FILTRO)
    private StringToWordVector filter; // Filtro para convertir texto a vectores

    private static final String MODEL_PATH = "model.ser"; // Nombre del archivo para guardar/cargar el modelo

    // Atributos originales (pre-filtro) para crear instancias de predicción
    private ArrayList<Attribute> rawAttributes; 


    public ClassifierService() {
        // Constructor vacío, el modelo se cargará o entrenará luego.
    }

    /**
     * Entrena un modelo de clasificación usando los datos de una carpeta de entrenamiento.
     * La carpeta debe contener subcarpetas, cada una representando una clase/categoría.
     * Por ejemplo: training_data/CODE, training_data/DOCUMENT, training_data/CONFIG
     * @param trainingDataPath La ruta a la carpeta que contiene las subcarpetas de categorías.
     * @throws Exception Si ocurre un error durante el entrenamiento.
     */
    public void trainModel(Path trainingDataPath) throws Exception {
        System.out.println("DEBUG (CS): Inicia trainModel para: " + trainingDataPath.toAbsolutePath().normalize());

        File trainingDirFile = trainingDataPath.toFile();

        System.out.println("DEBUG (CS): trainingDirFile.exists(): " + trainingDirFile.exists());
        System.out.println("DEBUG (CS): trainingDirFile.isDirectory(): " + trainingDirFile.isDirectory());
        System.out.println("DEBUG (CS): trainingDirFile.canRead(): " + trainingDirFile.canRead());

        List<File> categoryDirsList = new ArrayList<>();
        try (Stream<Path> pathStream = Files.list(trainingDataPath)) {
            System.out.println("DEBUG (CS): Files.list() stream abierto.");
            pathStream.forEach(p -> {
                System.out.println("DEBUG (CS):   - Visto Path: " + p.getFileName() + ", EsDirectorio: " + Files.isDirectory(p) + ", PuedeLeerse: " + Files.isReadable(p));
                if (Files.isDirectory(p) && Files.isReadable(p)) {
                    categoryDirsList.add(p.toFile());
                }
            });
            System.out.println("DEBUG (CS): Files.list() stream procesado.");
        } catch (IOException e) {
            System.err.println("ERROR (CS): Excepción al listar contenido de training_data: " + e.getMessage());
            throw new IOException("No se pudo listar el contenido de la carpeta de entrenamiento: " + trainingDataPath, e);
        }

        File[] categoryDirs = categoryDirsList.toArray(new File[0]);

        System.out.println("DEBUG (CS): Directorios de categoría encontrados: " + categoryDirs.length);

        if (categoryDirs.length == 0) {
            throw new IllegalArgumentException("No se encontraron subcarpetas de categorías válidas (CODE, DOCUMENT, CONFIG) en: " + trainingDataPath.toString() + ". Verifique que existan y sean directorios accesibles.");
        }

        List<String> classLabels = Arrays.stream(categoryDirs)
                                         .map(File::getName)
                                         .sorted()
                                         .collect(Collectors.toList());

        System.out.println("DEBUG (CS): Etiquetas de clase detectadas: " + classLabels);


        // 2. Definir los atributos (palabras) para WEKA para el DATASET ORIGINAL (PRE-FILTRO)
        rawAttributes = new ArrayList<>(); // Inicializar rawAttributes
        rawAttributes.add(new Attribute("text", (List<String>)null)); // Atributo de texto
        rawAttributes.add(new Attribute("file_category", classLabels)); // Atributo de clase

        // Crear un conjunto de datos vacío con la estructura definida para los datos originales
        Instances rawData = new Instances("RawTextData", rawAttributes, 0);
        rawData.setClassIndex(rawData.attribute("file_category").index()); 


        // 3. Leer los datos de entrenamiento y poblar 'Instances'
        for (String label : classLabels) {
            Path categoryPath = trainingDataPath.resolve(label);
            System.out.println("DEBUG (CS): Procesando categoría: " + label + " en " + categoryPath.toAbsolutePath().normalize());
            try (Stream<Path> files = Files.walk(categoryPath)) {
                List<Path> categoryFiles = files.filter(Files::isRegularFile).toList();
                if (categoryFiles.isEmpty()) {
                    System.err.println("ADVERTENCIA (CS): La carpeta de categoría '" + label + "' está vacía o no contiene archivos regulares: " + categoryPath);
                }
                for (Path filePath : categoryFiles) {
                    if (FileContentReader.isTextFile(filePath)) {
                        try {
                            List<String> preprocessedTokens = FileContentReader.readAndPreprocessFile(filePath);
                            String content = String.join(" ", preprocessedTokens);

                            // Crear instancia con el formato de rawAttributes
                            Instance instance = new DenseInstance(rawAttributes.size());
                            instance.setValue(rawAttributes.get(0), content); 
                            instance.setValue(rawAttributes.get(1), label); 
                            // Antes de añadir la instancia, asignarle el dataset para que sepa su formato
                            instance.setDataset(rawData); 
                            rawData.add(instance); // Añadir a rawData
                            System.out.println("DEBUG (CS):   - Añadido archivo a dataset: " + filePath.getFileName() + " para categoría: " + label);
                        } catch (IOException e) {
                            System.err.println("Advertencia (CS): No se pudo leer o preprocesar el archivo de entrenamiento " + filePath + ": " + e.getMessage());
                        }
                    } else {
                        System.out.println("DEBUG (CS):   - Archivo no de texto legible saltado: " + filePath.getFileName() + " en categoría: " + label);
                    }
                }
            }
        }

        if (rawData.isEmpty()) { 
            throw new IllegalStateException("No se pudieron cargar datos de entrenamiento válidos desde las subcarpetas. Asegúrate de que los archivos existan y sean legibles dentro de CODE, DOCUMENT, CONFIG.");
        }

        // --- INICIO DE MANEJO DE EXCEPCIONES ESPECÍFICAS DE WEKA ---
        try {
            System.out.println("DEBUG (CS): Aplicando filtro StringToWordVector a los datos...");
            filter = new StringToWordVector();
            // El filtro automáticamente infiere el classIndex de rawData. No necesita setClassIndex aquí.

            filter.setInputFormat(rawData); // Establecer el formato de entrada CON los datos originales (rawData)
            Instances filteredData = Filter.useFilter(rawData, filter); // Usar rawData aquí
            System.out.println("DEBUG (CS): Filtro aplicado. Datos filtrados: " + filteredData.numInstances() + " instancias, " + filteredData.numAttributes() + " atributos.");

            // El clasificador necesita que el atributo de clase esté configurado en los datos filtrados también
            filteredData.setClassIndex(filteredData.attribute("file_category").index()); 

            System.out.println("DEBUG (CS): Construyendo clasificador NaiveBayes...");
            classifier = new NaiveBayes();
            classifier.buildClassifier(filteredData);
            System.out.println("DEBUG (CS): Clasificador construido exitosamente.");

            // dataFormat se usa para la predicción, debe reflejar el formato de los datos FILTRADOS
            dataFormat = new Instances(filteredData, 0); // Crea una copia del formato, sin instancias
            dataFormat.setClassIndex(dataFormat.attribute("file_category").index()); 


        } catch (Throwable t) {
            System.err.println("ERROR (CS): Falla crítica en la fase de filtrado o entrenamiento de WEKA.");
            System.err.println("Causa: " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
            throw new Exception("Error al procesar datos o entrenar clasificador WEKA: " + t.getMessage(), t);
        }
        // --- FIN DE MANEJO DE EXCEPCIONES ESPECÍFICAS DE WEKA ---


        System.out.println("Modelo de clasificación entrenado exitosamente.");

        saveModel(Paths.get(MODEL_PATH), classifier, filter, dataFormat);
        System.out.println("Modelo guardado en: " + MODEL_PATH);
    }

    /**
     * Carga un modelo clasificador y su filtro StringToWordVector desde un archivo.
     * @param modelPath La ruta del archivo del modelo.
     * @return true si el modelo se cargó exitosamente, false en caso contrario.
     */
    public boolean loadModel(Path modelPath) {
        try {
            Object[] modelAndFilterAndRawAttributes = (Object[]) SerializationHelper.read(modelPath.toString()); // Cambiado a 4 elementos
            this.classifier = (Classifier) modelAndFilterAndRawAttributes[0];
            this.filter = (StringToWordVector) modelAndFilterAndRawAttributes[1];
            this.dataFormat = (Instances) modelAndFilterAndRawAttributes[2]; // Formato de datos POST-FILTRO
            this.rawAttributes = (ArrayList<Attribute>) modelAndFilterAndRawAttributes[3]; // Cargar rawAttributes
            
            // Asegúrate de que el índice de clase en dataFormat sea correcto después de cargar
            this.dataFormat.setClassIndex(this.dataFormat.attribute("file_category").index()); 

            System.out.println("Modelo de clasificación cargado desde: " + modelPath);
            return true;
        } catch (Exception e) {
            System.err.println("Error al cargar el modelo: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clasifica un texto preprocesado usando el modelo entrenado/cargado.
     * @param preprocessedTokens Lista de tokens (palabras limpias) del texto.
     * @return La categoría predicha como String.
     * @throws Exception Si el modelo no está entrenado/cargado o si ocurre un error en la predicción.
     */
    // Eliminada la anotación @Override ya que no sobrescribe ningún método
    public String classify(List<String> preprocessedTokens) throws Exception {
        if (classifier == null || dataFormat == null || filter == null || rawAttributes == null) { 
            throw new IllegalStateException("El modelo no ha sido entrenado o cargado correctamente (faltan componentes).");
        }

        String content = String.join(" ", preprocessedTokens);

        // 1. Crear un dataset temporal con la estructura de entrada ORIGINAL esperada por el filtro
        // Es CRÍTICO que uses 'rawAttributes' aquí para que el filtro sepa el formato de entrada original
        Instances predictionRawData = new Instances("PredictionInstance", rawAttributes, 0);
        predictionRawData.setClassIndex(predictionRawData.attribute("file_category").index());

        // 2. Crear la instancia con el texto a clasificar
        // La instancia debe tener el tamaño correcto de atributos (el de rawAttributes)
        Instance rawInstance = new DenseInstance(rawAttributes.size()); 
        rawInstance.setValue(rawAttributes.get(0), content); 
        
        // Asignar el dataset a la instancia y marcar la clase como missing para la predicción
        rawInstance.setDataset(predictionRawData); 
        rawInstance.setClassMissing(); 

        // 3. Añadir la instancia al dataset temporal
        predictionRawData.add(rawInstance);

        // 4. Aplicar el MISMO filtro StringToWordVector a este dataset de UNA instancia
        // El filtro automáticamente infiere el classIndex de predictionRawData. No necesita setClassIndex aquí.
        Instances filteredPredictionData = Filter.useFilter(predictionRawData, filter);

        // 5. Obtener la única instancia filtrada (que ahora tiene los atributos de palabras)
        Instance filteredInstance = filteredPredictionData.firstInstance(); 

        // 6. Asegurarse de que el índice de clase esté configurado correctamente y la clase esté marcada como missing
        // Se asegura que el índice de clase en la instancia filtrada sea el mismo que en el modelo final
        // ¡CORRECCIÓN! Esto se hace en el conjunto de datos de la instancia filtrada, no en la instancia directamente
        filteredInstance.setDataset(dataFormat); // Asocia la instancia filtrada al formato del clasificador
        filteredInstance.setClassMissing(); // Marcar la clase como desconocida para la predicción


        // Realizar la predicción
        double classValue = classifier.classifyInstance(filteredInstance);
        return dataFormat.classAttribute().value((int) classValue); 
    }

    /**
     * Guarda el clasificador, el filtro y el formato de datos en un archivo.
     */
    private void saveModel(Path path, Classifier classifier, StringToWordVector filter, Instances dataFormat) throws Exception {
        // Guardamos un array de objetos para mantener la integridad del modelo
        // También guardamos rawAttributes para poder reconstruir las instancias de predicción
        Object[] modelAndFilterAndRawAttributes = new Object[4]; // Ahora un array de 4
        modelAndFilterAndRawAttributes[0] = classifier;
        modelAndFilterAndRawAttributes[1] = filter;
        modelAndFilterAndRawAttributes[2] = dataFormat; // Formato de datos POST-FILTRO
        modelAndFilterAndRawAttributes[3] = rawAttributes; // Guardamos rawAttributes

        SerializationHelper.write(path.toString(), modelAndFilterAndRawAttributes);
    }
}