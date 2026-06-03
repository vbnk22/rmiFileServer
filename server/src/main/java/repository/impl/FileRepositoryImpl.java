package repository.impl;

import model.FileMetadata;
import repository.FileRepository;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    public byte[] downlaodFile(FileMetadata fileMetadata) {

        try {
            Path filePath =
                    storageDir.resolve(fileMetadata.getStoredName());

            if (!Files.exists(filePath)) {
                throw new RuntimeException(
                        "File not found: " + fileMetadata.getStoredName()
                );
            }

            return Files.readAllBytes(filePath);

        } catch (IOException e) {
            throw new RuntimeException("Error while downloading file", e);
        }
    }

    @Override
    public FileMetadata uploadFile(byte[] fileContent) {

        try {
            String id = UUID.randomUUID().toString();
            String storedName = id;

            Path filePath = storageDir.resolve(storedName);

            Files.write(filePath, fileContent);

            FileMetadata metadata = new FileMetadata();
            metadata.setId(id);
            metadata.setStoredName(storedName);
            metadata.setOriginalName("unknown");
            metadata.setContentType("application/octet-stream");
            metadata.setSize(fileContent.length);

            files.add(metadata);

            return metadata;

        } catch (IOException e) {
            throw new RuntimeException("Error while uploading file", e);
        }
    }

    @Override
    public List<FileMetadata> listFiles() {
        return files;
    }
}