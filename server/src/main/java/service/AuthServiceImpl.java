package service;

import dto.UserDTO;
import remote.AuthService;
import repository.UserRepository;
import repository.impl.UserRepositoryImpl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthServiceImpl extends UnicastRemoteObject implements AuthService {

    private UserRepository userRepository;
    private Map<String, String> sessions;

    public AuthServiceImpl(UserRepositoryImpl userRepository) throws RemoteException {
        this.userRepository = userRepository;
        sessions = new ConcurrentHashMap<>();
    }

    @Override
    public String authenticate(String username, String password) throws RemoteException {
        try {
            UserDTO user = userRepository.findByUsername(username);
            if (user != null && user.getPassword().equals(password)) {
                String sessionId = UUID.randomUUID().toString();
                sessions.put(sessionId, user.getUsername());
                System.out.println("Udane logowanie " + user.getUsername());
                return sessionId;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "";
    }

    @Override
    public String getUsername(String sessionId) throws RemoteException {
//        return userRepository.findByUsername(sessions.get(sessionId));
        return sessions.get(sessionId);
    }
}
