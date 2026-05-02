package main.model;

public class Teacher extends User {

    public Teacher(int id, String username, String password) {
        super(id, username, password, "TEACHER");
    }

    @Override
    public void displayDashboard() {
        System.out.println("Teacher Dashboard Loaded...");
    }
}