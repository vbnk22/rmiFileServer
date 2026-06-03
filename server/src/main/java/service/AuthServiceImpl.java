package service;

import model.User;
import remote.AuthService;
import repository.UserRepository;
import repository.impl.UserRepositoryImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class AuthServiceImpl extends UnicastRemoteObject implements AuthService {

    private UserRepository userRepository;

    protected AuthServiceImpl() throws RemoteException {
        userRepository = new UserRepositoryImpl();
    }

    @Override
    public boolean authenticate(String username, String password) throws RemoteException {
        try {
            User user = userRepository.findByUsername(username);
            if (user != null && user.getPassword().equals(password)) {
                userRepository.save(user);
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}
