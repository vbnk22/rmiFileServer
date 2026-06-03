package repository.impl;

import dto.UserDTO;
import repository.UserRepository;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;

public class UserRepositoryImpl implements UserRepository {
    List<UserDTO> users;

    public UserRepositoryImpl() throws RemoteException {
        users = new ArrayList<UserDTO>();
    }

    @Override
    public void save(UserDTO user) throws MissingResourceException {
        if (!users.contains(user) && user != null) {
            users.add(user);
        } else {
            throw new IllegalArgumentException("User is null or already exists");
        }
    }

    @Override
    public UserDTO findByUsername(String username) {
        for (UserDTO user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
            else throw new NoSuchElementException("User cannot be found");
        }
        return null;
    }

    @Override
    public void delete(String username) {
        if (users.contains(username) && username != null) {
            users.remove(username);
        }
        else throw new NoSuchElementException("User cannot be found and deleted");
    }
}
