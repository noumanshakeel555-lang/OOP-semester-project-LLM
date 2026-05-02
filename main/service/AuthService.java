package main.service;

import main.dao.UserDAO;
import main.model.*;

public class AuthService {

    private UserDAO userDAO = new UserDAO();

    public User login(String username, String password) {

        if (username == null || password == null)
            throw new RuntimeException("Username or password cannot be null");

        User user = userDAO.findByUsername(username);

        if (user == null)
            throw new RuntimeException("User not found");

        if (!user.getPassword().equals(password))
            throw new RuntimeException("Invalid password");

        return user;
    }

    public void signup(String username, String password, String role) {

        if (username.length() < 3)
            throw new RuntimeException("Username too short");

        if (password.length() < 6)
            throw new RuntimeException("Weak password");

        if (userDAO.findByUsername(username) != null)
            throw new RuntimeException("User already exists");

        if (role.equalsIgnoreCase("STUDENT"))
            userDAO.save(new Student(0, username, password));

        else if (role.equalsIgnoreCase("TEACHER"))
            userDAO.save(new Teacher(0, username, password));

        else
            throw new RuntimeException("Invalid role");
    }
}