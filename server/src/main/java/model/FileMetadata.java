package model;

public class FileMetadata {

    private String id;
    private String originalName;
    private String storedName;
    private String contentType;
    private long size;

    public FileMetadata() {}

    public FileMetadata(String id, String originalName, String storedName, String contentType, long size) {
        this.id = id;
        this.originalName = originalName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getStoredName() {
        return storedName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public void setStoredName(String storedName) {
        this.storedName = storedName;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setSize(long size) {
        this.size = size;
    }
}