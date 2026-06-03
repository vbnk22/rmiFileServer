package remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AdminSerivce extends Remote {
    boolean registerUser(String username, String password) throws RemoteException;
    byte[] uploadFile(String filename, byte[] data) throws RemoteException;
    boolean deleteFile(String filename) throws RemoteException;
    void deleteUser(String username) throws RemoteException;
}
