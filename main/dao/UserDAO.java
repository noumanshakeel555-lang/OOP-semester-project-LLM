package main.dao;

import main.db.DatabaseManager;
import main.model.*;
import java.sql.*;
import java.util.*;

public class UserDAO {

    // ── Write ──────────────────────────────────────────────────────────────

    public void save(User user) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole());
            ps.executeUpdate();
            user.setId(DatabaseManager.lastInsertId());
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.save: " + e.getMessage(), e);
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ? COLLATE NOCASE";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findByUsername: " + e.getMessage(), e);
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findById: " + e.getMessage(), e);
        }
        return null;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.get()
                .prepareStatement("SELECT * FROM users ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findAll: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Student> findAllStudents() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role='STUDENT' ORDER BY username";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add((Student) map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("UserDAO.findAllStudents: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Mapper ─────────────────────────────────────────────────────────────

    private User map(ResultSet rs) throws SQLException {
        int    id   = rs.getInt("id");
        String u    = rs.getString("username");
        String p    = rs.getString("password");
        String role = rs.getString("role");
        return "STUDENT".equals(role)
            ? new Student(id, u, p)
            : new Teacher(id, u, p);
    }
}
