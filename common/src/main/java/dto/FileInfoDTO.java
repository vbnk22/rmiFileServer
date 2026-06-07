package dto;

import java.io.Serializable;

public class FileInfoDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String originalName;
    private String contentType;
    private long size;

    public String getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
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

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
