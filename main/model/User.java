package main.model;

public abstract class User {
    protected int id;
    protected String username;
    protected String password;
    protected String role;

    public User(int id, String username, String password, String role) {
        this.id       = id;
        this.username = username;
        this.password = password;
        this.role     = role;
    }

    public int    getId()       { return id; }
    public String getUsername() { return username; }
    public String getRole()     { return role; }
    public String getPassword() { return password; }

    /** Called by UserDAO after insertion to stamp the generated ID. */
    public void setId(int id)   { this.id = id; }

    public void setPassword(String password) {
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        this.password = password;
    }

    public abstract void displayDashboard();
}
