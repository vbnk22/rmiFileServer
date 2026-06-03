package repository.impl;

import model.User;
import repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;

public class UserRepositoryImpl implements UserRepository {
    List<User> users = new ArrayList<User>();

    @Override
    public void save(User user) throws MissingResourceException {
        if (!users.contains(user) && user != null) {
            users.add(user);
        } else {
            throw new IllegalArgumentException("User is null or already exists");
        }
    }

    @Override
    public User findByUsername(String username) {
        if (users.contains(username)) {
            return users.get(users.indexOf(username));
        }
        else throw new NoSuchElementException("User cannot be found");
    }

    @Override
    public void delete(String username) {
        if (users.contains(username) && username != null) {
            users.remove(username);
        }
        else throw new NoSuchElementException("User cannot be found and deleted");
    }
}
