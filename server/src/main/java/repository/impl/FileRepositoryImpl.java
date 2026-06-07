package repository.impl;

import model.FileMetadata;
import repository.FileRepository;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileRepositoryImpl implements FileRepository {

    private final List<FileMetadata> files = new CopyOnWriteArrayList<>();
    private final Path storageDir = Paths.get("storage/files");
    // Wielu czytelników jednocześnie (download), wyłączny zapis (upload/delete)
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

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
    public void uploadFile(FileMetadata meta) {
        rwLock.writeLock().lock();
        try {
            Path dest = storageDir.resolve(meta.getFileStoredName());
            Files.write(dest, meta.getFileData());
            // Zwolnienie bajtów z pamięci po zapisaniu na dysk — zapobieganie wyciekowi pamięci
            meta.setFileData(null);
            files.add(meta);
        } catch (IOException e) {
            throw new RuntimeException("Error saving file to disk", e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public byte[] downloadFile(FileMetadata meta) {
        rwLock.readLock().lock();
        try {
            Path filePath = storageDir.resolve(meta.getFileStoredName());
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File not found on disk: " + meta.getFileStoredName());
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file", e);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Atomowe wyszukanie i odczyt pliku pod jedną blokadą odczytu.
     * Zapobiega TOCTOU race condition: bez tego admin mógłby usunąć plik
     * między findFileByName() a downloadFile() w FileServiceImpl.
     */
    public byte[] findAndDownload(String fileName) {
        rwLock.readLock().lock();
        try {
            FileMetadata meta = files.stream()
                    .filter(f -> f.getFileName().equals(fileName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("File not found: " + fileName));
            Path filePath = storageDir.resolve(meta.getFileStoredName());
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File missing on disk: " + meta.getFileStoredName());
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file", e);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<FileMetadata> listFiles() {
        rwLock.readLock().lock();
        try {
            return List.copyOf(files);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Optional<FileMetadata> findFileByName(String fileName) {
        rwLock.readLock().lock();
        try {
            return files.stream()
                    .filter(f -> f.getFileName().equals(fileName))
                    .findFirst();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void deleteFile(String fileName) {
        rwLock.writeLock().lock();
        try {
            Optional<FileMetadata> found = files.stream()
                    .filter(f -> f.getFileName().equals(fileName))
                    .findFirst();
            if (found.isEmpty()) throw new RuntimeException("File not found: " + fileName);
            Path filePath = storageDir.resolve(found.get().getFileStoredName());
            Files.deleteIfExists(filePath);
            files.removeIf(f -> f.getFileName().equals(fileName));
        } catch (IOException e) {
            throw new RuntimeException("Error deleting file", e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
