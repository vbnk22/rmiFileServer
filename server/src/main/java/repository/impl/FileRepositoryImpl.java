package repository.impl;

import model.FileMetadata;
import repository.FileRepository;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileRepositoryImpl implements FileRepository {

    private final List<FileMetadata> files = new ArrayList<>();
    private final Path storageDir = Paths.get("storage/files");

    public FileRepositoryImpl() {
        try {
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot create storage directory", e);
        }
    }

    @Override
    public byte[] downloadFile(FileMetadata fileMetadata) {
        try {
            Path filePath = storageDir.resolve(fileMetadata.getFileStoredName());

            if (!Files.exists(filePath)) {
                throw new RuntimeException( "File not found: " + fileMetadata.getFileStoredName());
            }

            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Error while downloading file", e);
        }
    }

    @Override
    public void uploadFile(FileMetadata fileMetadata) {
        try {
            files.add(fileMetadata);
        } catch (Exception e) {
            throw new RuntimeException("Error while uploading file", e);
        }
    }

    @Override
    public List<FileMetadata> listFiles() {
        return files;
    }

    @Override
    public Optional<FileMetadata> findFileByName(String fileName) {
        return files
                .stream()
                .filter(file -> file.getFileName().equals(fileName))
                .findFirst();
    }

    @Override
    public void deleteFile(String fileName) {
        files.removeIf(file -> file.getFileName().equals(fileName));
    }
}