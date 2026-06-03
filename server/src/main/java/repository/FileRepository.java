package repository;

import model.FileMetadata;

import java.util.List;

public interface FileRepository {
    byte[] downlaodFile(FileMetadata fileMetadata);
    FileMetadata uploadFile(byte[] fileContent);
    List<FileMetadata> listFiles();
}
