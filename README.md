# Clasificador Inteligente de Archivos 📂🤖

![Java](https://img.shields.io/badge/Java-22-blue?logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-4.0.0-red?logo=apachemaven&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-22-orange?logo=openjfx&logoColor=white)
![Weka](https://img.shields.io/badge/Weka-3.8.6-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green)

Una aplicación de escritorio construida con JavaFX que utiliza un modelo de concurrencia avanzado y Machine Learning para analizar, clasificar y organizar archivos de forma inteligente.

![Captura de Pantalla de la Aplicación](https://i.imgur.com/s67MDFj.png)

---

## ✨ Características Principales

* **🧠 Clasificación por Contenido (IA):** Utiliza un modelo de Machine Learning (Naive Bayes con WEKA) para leer el contenido de los archivos de texto y clasificarlos en categorías como `CODE`, `DOCUMENT` o `CONFIG`.
* **🖼️ Reconocimiento de Imágenes:** Identifica archivos de imagen por su extensión (JPG, PNG, GIF, etc.) y los agrupa en la categoría `IMAGES`.
* **⚡ Interfaz de Usuario Fluida:** Gracias a un robusto modelo de concurrencia, la interfaz gráfica nunca se congela, incluso al procesar miles de archivos.
* **🚀 Procesamiento Paralelo:** Asigna un hilo de ejecución a cada subcarpeta, aprovechando al máximo los procesadores multi-núcleo para un análisis ultra rápido.
* **📊 Feedback en Tiempo Real:** Una barra de progreso y mensajes de estado mantienen al usuario informado durante todo el proceso de clasificación.

---

## 🛠️ Tecnologías Utilizadas

* **Lenguaje:** Java 17+
* **Framework UI:** JavaFX
* **Gestor de Proyecto y Dependencias:** Apache Maven
* **Machine Learning:** Librería [Weka](https://www.cs.waikato.ac.nz/ml/weka/) (Clasificador Naive Bayes).
* **Concurrencia:** Java Concurrency API (`ExecutorService`, `FixedThreadPool`, `Task`, `AtomicInteger`).

---

## 🧠 Conceptos Clave Implementados

Este proyecto sirve como una demostración práctica de varios conceptos avanzados de la ingeniería de software:

### 1. Programación Concurrente
La aplicación está diseñada para ser altamente eficiente y responsiva.
* **Modelo Multi-hilo:** Se utiliza un `ExecutorService` con un `FixedThreadPool` para crear un pool de hilos.
* **Paralelismo de Tareas:** Se lanza una tarea (`FolderProcessingTask`) independiente por cada subcarpeta encontrada. Esto permite procesar múltiples carpetas en paralelo, reduciendo drásticamente el tiempo total.
* **Sincronización Segura:** Se utiliza un `AtomicInteger` para llevar la cuenta del progreso de forma segura a través de múltiples hilos, y `Platform.runLater()` para garantizar que todas las actualizaciones de la UI se realicen en el hilo de la aplicación de JavaFX, evitando condiciones de carrera.

### 2. Machine Learning (Clasificación Supervisada)
El corazón "inteligente" del clasificador se basa en un modelo entrenado.
* **Preprocesamiento de Texto:** Antes de la clasificación, el texto de los archivos es normalizado: se convierte a minúsculas, se tokeniza (divide en palabras) y se eliminan las "stop words" (palabras comunes sin significado relevante).
* **Entrenamiento:** El modelo se entrena con un conjunto de datos de ejemplo ubicado en la carpeta `training_data`. Utiliza el algoritmo Naive Bayes para aprender la probabilidad de que ciertas palabras aparezcan en cada categoría.
* **Inferencia:** Para cada nuevo archivo, se aplica el mismo preprocesamiento y el modelo entrenado predice la categoría más probable basándose en su contenido.

### 3. Arquitectura de Software
El proyecto sigue una variación del patrón **MVC (Modelo-Vista-Controlador)** adaptada para JavaFX.
* **Vista:** Definida de forma declarativa en el archivo FXML (`main_view.fxml`).
* **Controlador:** La clase `MainController.java` maneja la lógica de la interfaz y los eventos del usuario.
* **Modelo:** Las clases de lógica de negocio (`ClassifierService`, `FolderProcessingTask`, etc.) encapsulan el funcionamiento interno de la aplicación.

---

## 🚀 Instalación y Ejecución

Para ejecutar este proyecto en tu máquina local, sigue estos pasos:

### Prerrequisitos
* Tener instalado el **JDK (Java Development Kit)**, versión 17 o superior.
* Tener instalado **Apache Maven**.

### Pasos
1.  **Clona el repositorio:**
    ```bash
    git clone [https://github.com/tu-usuario/tu-repositorio.git](https://github.com/tu-usuario/tu-repositorio.git)
    ```
2.  **Navega al directorio del proyecto:**
    ```bash
    cd tu-repositorio
    ```
3.  **Compila el proyecto y empaquétalo:**
    Este comando descargará las dependencias y creará un archivo `.jar` ejecutable.
    ```bash
    mvn clean package
    ```
4.  **Ejecuta la aplicación:**
    El archivo `.jar` se encontrará en el directorio `target/`.
    ```bash
    java -jar target/nombre-de-tu-archivo-1.0-SNAPSHOT.jar
    ```
    (Reemplaza `nombre-de-tu-archivo` por el nombre real generado por Maven).

---

## 🤖 Entrenamiento del Modelo

* La aplicación está pre-configurada para entrenarse automáticamente en el primer inicio si no encuentra un modelo serializado (`model.ser`).
* Para el entrenamiento, utiliza los archivos de ejemplo que se encuentran en la carpeta `training_data`. Puedes añadir tus propios archivos `.txt` a las subcarpetas `CODE`, `CONFIG` y `DOCUMENT` para mejorar o personalizar el modelo.
* Si deseas **forzar un re-entrenamiento** con nuevos datos, simplemente **elimina el archivo `model.ser`** de la raíz del proyecto y vuelve a ejecutar la aplicación.

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo `LICENSE` para más detalles.

---

## 👤 Autor

**Franco Lucero**

* **GitHub:** `[@frnLcr](https://github.com/frnLcr)`
* **LinkedIn:** `[Franco Lucero](https://linkedin.com/in/lucerofranco/)`
