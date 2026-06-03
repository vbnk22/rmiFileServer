package repository.impl;

import model.FileMetadata;
import repository.FileRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


//TODO file implementations
public class FileRepositoryImpl implements FileRepository {
    List<FileMetadata> files = new ArrayList<FileMetadata>();

    @Override
    public byte[] downlaodFile(FileMetadata fileMetadata) {
        return new byte[0];
    }

    @Override
    public FileMetadata uploadFile(byte[] fileContent) {
        return null;
    }
}
