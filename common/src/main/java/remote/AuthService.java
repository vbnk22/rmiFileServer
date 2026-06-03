package remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AuthService extends Remote {
    public String authenticate(String username, String password) throws RemoteException;
}
