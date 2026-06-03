package repository;

import model.FileMetadata;

import java.util.List;
import java.util.Optional;

public interface FileRepository {
    byte[] downloadFile(FileMetadata fileMetadata);
    void uploadFile(FileMetadata fileMetadata);
    List<FileMetadata> listFiles();
    Optional<FileMetadata> findFileByName(String fileName);
    void deleteFile(String fileName);
}
