package service;

import model.User;
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
    private Map<String, User> sessions;

    protected AuthServiceImpl() throws RemoteException {
        userRepository = new UserRepositoryImpl();
        sessions = new ConcurrentHashMap<>();
    }

    @Override
    public String authenticate(String username, String password) throws RemoteException {
        try {
            User user = userRepository.findByUsername(username);
            if (user != null && user.getPassword().equals(password)) {
                userRepository.save(user);
                String sessionId = UUID.randomUUID().toString();
                sessions.put(sessionId, user);
                return sessionId;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "";
    }

    public User getUser(String sessionId) {
        return sessions.get(sessionId);
    }
}
