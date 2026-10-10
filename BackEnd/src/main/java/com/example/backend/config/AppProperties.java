package com.example.backend.config;

import java.io.File;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String documentsDirectory = "../documents";
    private String vectorstoreDirectory = "../vectorstore";
    private int ragTopK = 5;
    private double ragSimilarityThreshold = 0.60;
    private int chunkSize = 1100;
    private int chunkOverlap = 180;
    public String getDocumentsDirectory() { return documentsDirectory; }
    public void setDocumentsDirectory(String value) { documentsDirectory = value; }
    public String getVectorstoreDirectory() { return vectorstoreDirectory; }
    public void setVectorstoreDirectory(String value) { vectorstoreDirectory = value; }
    public int getRagTopK() { return ragTopK; }
    public void setRagTopK(int value) { ragTopK = value; }
    public double getRagSimilarityThreshold() { return ragSimilarityThreshold; }
    public void setRagSimilarityThreshold(double value) { ragSimilarityThreshold = value; }
    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int value) { chunkSize = value; }
    public int getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(int value) { chunkOverlap = value; }
    public File vectorFile() { return new File(vectorstoreDirectory, "college-vectors.json"); }
    public File documentIndexFile() { return new File(vectorstoreDirectory, "documents-index.json"); }
}
