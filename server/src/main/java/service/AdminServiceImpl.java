package service;

import dto.UserDTO;
import enums.UserRole;
import remote.AdminSerivce;
import remote.AuthService;
import repository.UserRepository;
import repository.impl.UserRepositoryImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class AdminServiceImpl extends UnicastRemoteObject implements AdminSerivce {

    private UserRepository userRepository;
    private AuthService authService;

    public AdminServiceImpl(UserRepositoryImpl userRepository, AuthServiceImpl authService) throws RemoteException {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    public boolean registerUser(String username, String password) throws RemoteException {
        try {
            if (username != null && password != null) {
                userRepository.save(new UserDTO(username, password, UserRole.USER));
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    public void deleteUser(String sessionId, String username) throws RemoteException {
        UserDTO user = userRepository.findByUsername(authService.getUsername(sessionId));

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
