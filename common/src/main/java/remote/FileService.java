package remote;

import dto.FileInfoDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface FileService extends Remote {
    FileInfoDTO uploadFile(FileInfoDTO fileInfoDTO) throws RemoteException;
    byte[] downloadFile(String fileName) throws RemoteException;
    List<FileInfoDTO> listFiles() throws RemoteException;
}
