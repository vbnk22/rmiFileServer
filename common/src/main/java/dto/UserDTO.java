package dto;

import enums.UserRole;

import java.io.Serializable;

public class UserDTO implements Serializable {
    private String username;
    private String password;
    private UserRole role;

    public UserDTO() {}

    public UserDTO(String username, String password, UserRole role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public UserRole getRole() {
        return role;
    }
}