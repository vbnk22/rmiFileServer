package remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthService extends Remote {
    public boolean authenticate(String username, String password) throws RemoteException;
}
