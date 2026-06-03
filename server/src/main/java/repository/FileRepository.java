package repository;

import model.FileMetadata;

public interface FileRepository {
    byte[] downlaodFile(FileMetadata fileMetadata);
    FileMetadata uploadFile(byte[] fileContent);
}
