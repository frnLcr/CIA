package com.ubp.clasificador.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ResultTableEntry {

    private final StringProperty fileName;
    private final StringProperty fileExtension;
    private final StringProperty predictedCategory;

    public ResultTableEntry(String fileName, String fileExtension, String predictedCategory) {
        this.fileName = new SimpleStringProperty(fileName);
        this.fileExtension = new SimpleStringProperty(fileExtension);
        this.predictedCategory = new SimpleStringProperty(predictedCategory);
    }

    public StringProperty fileNameProperty() {
        return fileName;
    }

    public String getFileName() {
        return fileName.get();
    }

    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }

    public StringProperty fileExtensionProperty() {
        return fileExtension;
    }

    public String getFileExtension() {
        return fileExtension.get();
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension.set(fileExtension);
    }

    public StringProperty predictedCategoryProperty() {
        return predictedCategory;
    }

    public String getPredictedCategory() {
        return predictedCategory.get();
    }

    public void setPredictedCategory(String predictedCategory) {
        this.predictedCategory.set(predictedCategory);
    }
}