package model;

public class FileMetadata {

    private String fileName;
    private String fileStoredName;
    private String filePath;
    private byte[] fileData;
    private long fileSize;

    public FileMetadata(String fileName, String fileStoredName, String filePath, byte[] fileData) {
        this.fileName = fileName;
        this.fileStoredName = fileStoredName;
        this.filePath = filePath;
        this.fileData = fileData;
        this.fileSize = fileData != null ? fileData.length : 0;
    }

    // Konstruktor do odtwarzania metadanych z CSV (bez bajtów — plik jest na dysku)
    public FileMetadata(String fileName, String fileStoredName, long fileSize) {
        this.fileName = fileName;
        this.fileStoredName = fileStoredName;
        this.filePath = "";
        this.fileData = null;
        this.fileSize = fileSize;
    }

    public String getFileName() { return fileName; }
    public String getFileStoredName() { return fileStoredName; }
    public String getFilePath() { return filePath; }
    public byte[] getFileData() { return fileData; }
    public long getFileSize() { return fileSize; }

    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setFileStoredName(String fileStoredName) { this.fileStoredName = fileStoredName; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
}
