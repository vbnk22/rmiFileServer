package repository;

import dto.UserDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.MissingResourceException;

public interface UserRepository {
    void save(UserDTO user) throws MissingResourceException;
    UserDTO findByUsername(String username);
    void delete(String username);
}
