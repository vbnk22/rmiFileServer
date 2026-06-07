package service;

import dto.UserDTO;
import enums.UserRole;
import remote.AuthService;
import repository.UserRepository;
import repository.impl.UserRepositoryImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthServiceImpl extends UnicastRemoteObject implements AuthService {

    private final UserRepository userRepository;
    // sessionId -> username
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    // sessionId -> role
    private final Map<String, UserRole> sessionRoles = new ConcurrentHashMap<>();

    public AuthServiceImpl(UserRepositoryImpl userRepository) throws RemoteException {
        this.userRepository = userRepository;
    }

    @Override
    public String authenticate(String username, String password) throws RemoteException {
        UserDTO user = userRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            String sessionId = UUID.randomUUID().toString();
            sessions.put(sessionId, user.getUsername());
            sessionRoles.put(sessionId, user.getRole());
            System.out.println("[Auth] Zalogowano: " + user.getUsername() + " [" + user.getRole() + "]");
            return sessionId;
        }
        System.out.println("[Auth] Nieudane logowanie dla: " + username);
        return "";
    }

    @Override
    public String getUsername(String sessionId) throws RemoteException {
        return sessions.get(sessionId);
    }

    @Override
    public UserRole getRole(String sessionId) throws RemoteException {
        return sessionRoles.get(sessionId);
    }
}
