package model;

public class FileMetadata {

    private String fileName;
    private String fileStoredName;
    private String filePath;
    private byte[] fileData;

    public FileMetadata(String fileName, String fileStoredName, String filePath, byte[] fileData) {
        this.fileName = fileName;
        this.fileStoredName = fileStoredName;
        this.filePath = filePath;
        this.fileData = fileData;
    }

    public String getFileStoredName() {
        return fileStoredName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileStoredName(String fileStoredName) {
        this.fileStoredName = fileStoredName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }
}