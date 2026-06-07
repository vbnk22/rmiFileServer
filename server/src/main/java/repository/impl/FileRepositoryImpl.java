package repository.impl;

import model.FileMetadata;
import repository.FileRepository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileRepositoryImpl implements FileRepository {

    private final List<FileMetadata> files = new CopyOnWriteArrayList<>();
    private final Path storageDir = Paths.get("storage/files");
    private final Path metadataCsv = Paths.get("storage/files_metadata.csv");

    // Blokada drobnoziarnista: oddzielny ReadWriteLock na każdy plik (klucz = oryginalna nazwa)
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> fileLocks = new ConcurrentHashMap<>();
    // Osobny lock tylko do zapisu CSV — dwa równoległe uploady różnych plików nie mogą pisać do CSV jednocześnie
    private final ReentrantLock csvLock = new ReentrantLock();

    public FileRepositoryImpl() {
        try {
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
            loadMetadataFromCsv();
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize storage", e);
        }
    }

    private ReentrantReadWriteLock lockFor(String fileName) {
        return fileLocks.computeIfAbsent(fileName, k -> new ReentrantReadWriteLock());
    }

    private void loadMetadataFromCsv() throws IOException {
        if (!Files.exists(metadataCsv)) return;
        try (BufferedReader br = Files.newBufferedReader(metadataCsv, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 3);
                if (parts.length != 3) continue;
                String storedName = parts[0];
                String originalName = parts[1];
                long size = Long.parseLong(parts[2]);
                if (Files.exists(storageDir.resolve(storedName))) {
                    files.add(new FileMetadata(originalName, storedName, size));
                    fileLocks.computeIfAbsent(originalName, k -> new ReentrantReadWriteLock());
                }
            }
        }
        System.out.println("[Storage] Załadowano " + files.size() + " plików z CSV.");
    }

    private void saveMetadataToCsv() throws IOException {
        csvLock.lock();
        try (BufferedWriter bw = Files.newBufferedWriter(metadataCsv, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (FileMetadata m : files) {
                bw.write(m.getFileStoredName() + "," + m.getFileName() + "," + m.getFileSize());
                bw.newLine();
            }
        } finally {
            csvLock.unlock();
        }
    }

    @Override
    public void uploadFile(FileMetadata meta) {
        ReentrantReadWriteLock lock = lockFor(meta.getFileName());
        lock.writeLock().lock();
        try {
            Path dest = storageDir.resolve(meta.getFileStoredName());
            Files.write(dest, meta.getFileData());
            // Zwolnienie bajtów z pamięci po zapisaniu na dysk — zapobieganie wyciekowi pamięci
            meta.setFileData(null);
            files.add(meta);
            saveMetadataToCsv();
        } catch (IOException e) {
            throw new RuntimeException("Error saving file to disk", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public byte[] downloadFile(FileMetadata meta) {
        ReentrantReadWriteLock lock = fileLocks.get(meta.getFileName());
        if (lock == null) throw new RuntimeException("File not found: " + meta.getFileName());
        lock.readLock().lock();
        try {
            Path filePath = storageDir.resolve(meta.getFileStoredName());
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File not found on disk: " + meta.getFileStoredName());
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Error reading file", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Atomowe wyszukanie i odczyt pliku pod jedną blokadą odczytu per-plik.
     * Zapobiega TOCTOU race condition: admin nie może usunąć pliku między
     * wyszukaniem a odczytem, bo musiałby poczekać na zwolnienie readLock.
     */
    public byte[] findAndDownload(String fileName) {
        ReentrantReadWriteLock lock = fileLocks.get(fileName);
        if (lock == null) throw new RuntimeException("File not found: " + fileName);
        lock.readLock().lock();
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
            lock.readLock().unlock();
        }
    }

    @Override
    public List<FileMetadata> listFiles() {
        // CopyOnWriteArrayList — bezpieczny odczyt bez blokady
        return List.copyOf(files);
    }

    @Override
    public Optional<FileMetadata> findFileByName(String fileName) {
        ReentrantReadWriteLock lock = fileLocks.get(fileName);
        if (lock == null) return Optional.empty();
        lock.readLock().lock();
        try {
            return files.stream()
                    .filter(f -> f.getFileName().equals(fileName))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void deleteFile(String fileName) {
        ReentrantReadWriteLock lock = fileLocks.get(fileName);
        if (lock == null) throw new RuntimeException("File not found: " + fileName);
        lock.writeLock().lock();
        try {
            Optional<FileMetadata> found = files.stream()
                    .filter(f -> f.getFileName().equals(fileName))
                    .findFirst();
            if (found.isEmpty()) throw new RuntimeException("File not found: " + fileName);
            Files.deleteIfExists(storageDir.resolve(found.get().getFileStoredName()));
            files.removeIf(f -> f.getFileName().equals(fileName));
            // Lock pozostaje w mapie — usunięcie go stworzyłoby race condition
            // z wątkami które już pobrały referencję do tego locka
            saveMetadataToCsv();
        } catch (IOException e) {
            throw new RuntimeException("Error deleting file", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
