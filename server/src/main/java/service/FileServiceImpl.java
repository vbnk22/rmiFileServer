package service;

import dto.FileInfoDTO;
import enums.UserRole;
import model.FileMetadata;
import model.User;
import remote.AuthService;
import remote.FileService;
import repository.FileRepository;
import repository.impl.FileRepositoryImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FileServiceImpl extends UnicastRemoteObject implements FileService {

    private final FileRepository fileRepository;
    private final AuthServiceImpl authService = new AuthServiceImpl();

    public FileServiceImpl() throws RemoteException {
        super();
        this.fileRepository = new FileRepositoryImpl();
    }

    @Override
    public void uploadFile(String path) throws RemoteException {
        try {
            Path filePath = Paths.get(path);
            byte[] data = Files.readAllBytes(filePath);
            String fileName = filePath.getFileName().toString();

            FileMetadata meta = new FileMetadata(fileName, UUID.randomUUID().toString(), filePath.toString(), data);

            fileRepository.uploadFile(meta);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] downloadFile(String fileName) throws RemoteException {
//        FileMetadata meta = fileRepository
//                .listFiles()
//                .stream()
//                .filter(f -> f.getOriginalName().equals(fileName))
//                .findFirst()
//                .orElseThrow(() ->
//                        new RuntimeException("File not found: " + fileName)
//                );

        FileMetadata meta = fileRepository.findFileByName(fileName).get();

        return fileRepository.downloadFile(meta);
    }

    @Override
    public List<FileInfoDTO> listFiles() throws RemoteException {
        return fileRepository
                .listFiles()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteFile(String sessionId, String filename) throws RemoteException {
        User user = authService.getUser(sessionId);

        if (user == null) {
            throw new SecurityException("Not logged in");
        }

        if (user.getRole() != UserRole.ADMIN) {
            throw new SecurityException("No permission");
        }
        if (filename != null) {
            fileRepository.deleteFile(filename);
            return true;
        }
        return false;
    }

    private FileInfoDTO mapToDTO(FileMetadata meta) {
        FileInfoDTO dto = new FileInfoDTO();

        dto.setId(meta.getFileStoredName());
        dto.setOriginalName(meta.getFileName());
        dto.setSize(meta.getFileData().length);

        return dto;
    }
}