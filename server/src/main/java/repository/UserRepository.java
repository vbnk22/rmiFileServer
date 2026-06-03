package repository;

import model.User;

import java.util.MissingResourceException;

public interface UserRepository {
    void save(User user) throws MissingResourceException;
    User findByUsername(String username);
    void delete(String username);
}
