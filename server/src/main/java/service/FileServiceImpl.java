package service;

import dto.FileInfoDTO;
import model.FileMetadata;
import remote.FileService;
import repository.FileRepository;
import repository.impl.FileRepositoryImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.stream.Collectors;

public class FileServiceImpl extends UnicastRemoteObject implements FileService {

    private final FileRepository fileRepository;

    public FileServiceImpl() throws RemoteException {
        super();
        this.fileRepository = new FileRepositoryImpl();
    }

    @Override
    public FileInfoDTO uploadFile(FileInfoDTO fileInfoDTO) throws RemoteException {
        FileMetadata metadata = new FileMetadata();
        metadata.setOriginalName(fileInfoDTO.getOriginalName());
        metadata.setContentType(fileInfoDTO.getContentType());
        metadata.setSize(fileInfoDTO.getSize());

        FileMetadata saved = fileRepository.uploadFile(fileInfoDTO.getData());

        saved.setOriginalName(metadata.getOriginalName());
        saved.setContentType(metadata.getContentType());

        return mapToDTO(saved);
    }

    @Override
    public byte[] downloadFile(String fileName) throws RemoteException {
        FileMetadata meta = fileRepository
                .listFiles()
                .stream()
                .filter(f -> f.getOriginalName().equals(fileName))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("File not found: " + fileName)
                );

        return fileRepository.downlaodFile(meta);
    }

    @Override
    public List<FileInfoDTO> listFiles() throws RemoteException {
        return fileRepository
                .listFiles()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private FileInfoDTO mapToDTO(FileMetadata meta) {
        FileInfoDTO dto = new FileInfoDTO();

        dto.setId(meta.getId());
        dto.setOriginalName(meta.getOriginalName());
        dto.setContentType(meta.getContentType());
        dto.setSize(meta.getSize());

        return dto;
    }
}