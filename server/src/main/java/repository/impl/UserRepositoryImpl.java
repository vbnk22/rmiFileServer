package repository.impl;

import dto.UserDTO;
import enums.UserRole;
import repository.UserRepository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserRepositoryImpl implements UserRepository {

    private final List<UserDTO> users = new CopyOnWriteArrayList<>();
    private final Path usersCsv = Paths.get("storage/users.csv");

    public UserRepositoryImpl() throws RemoteException {
        try {
            Files.createDirectories(Paths.get("storage"));
            loadUsersFromCsv();
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize user storage", e);
        }
    }

    private void loadUsersFromCsv() throws IOException {
        if (!Files.exists(usersCsv)) return;
        try (BufferedReader br = Files.newBufferedReader(usersCsv, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 3);
                if (parts.length != 3) continue;
                UserRole role = UserRole.valueOf(parts[2]);
                users.add(new UserDTO(parts[0], parts[1], role));
            }
        }
        System.out.println("[Storage] Załadowano " + users.size() + " użytkowników z CSV.");
    }

    private synchronized void saveUsersToCsv() {
        try (BufferedWriter bw = Files.newBufferedWriter(usersCsv, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (UserDTO u : users) {
                bw.write(u.getUsername() + "," + u.getPassword() + "," + u.getRole().name());
                bw.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error saving users to CSV", e);
        }
    }

    @Override
    public synchronized void save(UserDTO user) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        boolean exists = users.stream().anyMatch(u -> u.getUsername().equals(user.getUsername()));
        if (exists) throw new IllegalArgumentException("User already exists: " + user.getUsername());
        users.add(user);
        saveUsersToCsv();
    }

    @Override
    public UserDTO findByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    @Override
    public synchronized void delete(String username) {
        boolean removed = users.removeIf(u -> u.getUsername().equals(username));
        if (!removed) throw new NoSuchElementException("User not found: " + username);
        saveUsersToCsv();
    }
}
