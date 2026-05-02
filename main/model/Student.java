package main.model;

public class Student extends User {

    public Student(int id, String username, String password) {
        super(id, username, password, "STUDENT");
    }

    @Override
    public void displayDashboard() {
        System.out.println("Student Dashboard Loaded...");
    }
}