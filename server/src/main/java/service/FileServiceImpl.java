package service;

import dto.FileInfoDTO;
import dto.UserDTO;
import enums.UserRole;
import model.FileMetadata;
import remote.AuthService;
import remote.FileService;
import repository.FileRepository;
import repository.UserRepository;
import repository.impl.FileRepositoryImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class FileServiceImpl extends UnicastRemoteObject implements FileService {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50 MB

    private final FileRepositoryImpl fileRepository;
    private final AuthService authService;
    private final UserRepository userRepository;

    public FileServiceImpl(FileRepository fileRepository, AuthService authService,
                           UserRepository userRepository) throws RemoteException {
        super();
        // FileRepositoryImpl przechowywany jako impl, bo używamy findAndDownload (atomowe TOCTOU-safe)
        this.fileRepository = (FileRepositoryImpl) fileRepository;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Override
    public void uploadFile(String filename, byte[] data) throws RemoteException {
        if (filename == null || filename.isBlank()) {
            throw new RemoteException("Nazwa pliku nie może być pusta");
        }
        if (data == null || data.length == 0) {
            throw new RemoteException("Plik jest pusty");
        }
        if (data.length > MAX_FILE_SIZE) {
            throw new RemoteException("Plik przekracza dozwolony limit 50 MB (rozmiar: "
                    + (data.length / 1024 / 1024) + " MB)");
        }
        try {
            String storedName = UUID.randomUUID().toString();
            FileMetadata meta = new FileMetadata(filename, storedName, "", data);
            fileRepository.uploadFile(meta);
            System.out.println("[File] Wgrano plik: " + filename + " (" + meta.getFileSize() + " B)");
        } catch (Exception e) {
            throw new RemoteException("Błąd przy wgrywaniu pliku: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] downloadFile(String fileName) throws RemoteException {
        if (fileName == null || fileName.isBlank()) {
            throw new RemoteException("Nazwa pliku nie może być pusta");
        }
        try {
            // Atomowe find+read pod jedną blokadą — zapobiega TOCTOU race condition
            byte[] data = fileRepository.findAndDownload(fileName);
            System.out.println("[File] Pobrano plik: " + fileName);
            return data;
        } catch (RuntimeException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public List<FileInfoDTO> listFiles() throws RemoteException {
        return fileRepository.listFiles().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteFile(String sessionId, String filename) throws RemoteException {
        if (filename == null || filename.isBlank()) {
            throw new RemoteException("Nazwa pliku nie może być pusta");
        }
        String username = authService.getUsername(sessionId);
        if (username == null) throw new RemoteException("Sesja wygasła lub nieprawidłowa");

        UserDTO user = userRepository.findByUsername(username);
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new RemoteException("Brak uprawnień — wymagana rola ADMIN");
        }
        try {
            fileRepository.deleteFile(filename);
            System.out.println("[File] Usunięto plik: " + filename + " przez " + username);
            return true;
        } catch (RuntimeException e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public FileInfoDTO findFileByName(String filename) throws RemoteException {
        if (filename == null || filename.isBlank()) {
            throw new RemoteException("Nazwa pliku nie może być pusta");
        }
        Optional<FileMetadata> found = fileRepository.findFileByName(filename);
        return found.map(this::mapToDTO).orElse(null);
    }

    private FileInfoDTO mapToDTO(FileMetadata meta) {
        FileInfoDTO dto = new FileInfoDTO();
        dto.setId(meta.getFileStoredName());
        dto.setOriginalName(meta.getFileName());
        // Używamy fileSize — nie dotykamy fileData (bajty zwolnione po uploadzie)
        dto.setSize(meta.getFileSize());
        return dto;
    }
}
