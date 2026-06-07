package repository.impl;

import dto.UserDTO;
import repository.UserRepository;

import java.rmi.RemoteException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserRepositoryImpl implements UserRepository {

    private final List<UserDTO> users = new CopyOnWriteArrayList<>();

    public UserRepositoryImpl() throws RemoteException {}

    @Override
    public void save(UserDTO user) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        boolean exists = users.stream().anyMatch(u -> u.getUsername().equals(user.getUsername()));
        if (exists) throw new IllegalArgumentException("User already exists: " + user.getUsername());
        users.add(user);
    }

    @Override
    public UserDTO findByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void delete(String username) {
        boolean removed = users.removeIf(u -> u.getUsername().equals(username));
        if (!removed) throw new NoSuchElementException("User not found: " + username);
    }
}
