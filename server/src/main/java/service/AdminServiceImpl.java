package service;

import enums.UserRole;
import model.User;
import remote.AdminSerivce;
import repository.FileRepository;
import repository.UserRepository;
import repository.impl.FileRepositoryImpl;
import repository.impl.UserRepositoryImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class AdminServiceImpl extends UnicastRemoteObject implements AdminSerivce {

    private UserRepository userRepository;
    private FileRepository fileRepository;
    private final AuthServiceImpl authService = new AuthServiceImpl();

    public AdminServiceImpl() throws RemoteException {
        userRepository = new UserRepositoryImpl();
        fileRepository = new FileRepositoryImpl();
    }

    @Override
    public boolean registerUser(String username, String password) throws RemoteException {
        try {
            if (username != null && password != null) {
                userRepository.save(new User(username, password, UserRole.USER));
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    public void deleteUser(String sessionId, String username) throws RemoteException {
        User user = authService.getUser(sessionId);

        if (user == null) {
            throw new SecurityException("Not logged in");
        }

        if (user.getRole() != UserRole.ADMIN) {
            throw new SecurityException("No permission");
        }
        if (username != null) {
            userRepository.delete(username);
        }
    }
}
