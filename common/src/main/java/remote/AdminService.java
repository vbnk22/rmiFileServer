package remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AdminService extends Remote {
    boolean registerUser(String username, String password) throws RemoteException;
    void deleteUser(String sessionId, String username) throws RemoteException;
}
