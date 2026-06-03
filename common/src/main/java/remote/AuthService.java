package remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthService extends Remote {
    String authenticate(String username, String password) throws RemoteException;
    String getUsername(String sessionId) throws RemoteException;
}
