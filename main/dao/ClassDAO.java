package main.dao;

import main.db.DatabaseManager;
import main.model.SchoolClass;
import java.sql.*;
import java.util.*;

public class ClassDAO {

    // ── Write ──────────────────────────────────────────────────────────────

    public SchoolClass save(SchoolClass sc) {
        String sql = "INSERT INTO classes (name, teacher_id) VALUES (?, ?)";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setString(1, sc.getName());
            ps.setInt(2, sc.getTeacherId());
            ps.executeUpdate();
            sc.setId(DatabaseManager.lastInsertId());
        } catch (SQLException e) {
            throw new RuntimeException("ClassDAO.save: " + e.getMessage(), e);
        }
        return sc;
    }

    // ── Enrolment ──────────────────────────────────────────────────────────

    public void enrollStudent(int classId, int studentId) {
        String sql = "INSERT OR IGNORE INTO class_students (class_id, student_id) VALUES (?, ?)";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, classId);
            ps.setInt(2, studentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ClassDAO.enrollStudent: " + e.getMessage(), e);
        }
    }

    public void removeStudent(int classId, int studentId) {
        String sql = "DELETE FROM class_students WHERE class_id = ? AND student_id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, classId);
            ps.setInt(2, studentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ClassDAO.removeStudent: " + e.getMessage(), e);
        }
    }

    public void removeStudentFromAll(int studentId) {
        String sql = "DELETE FROM class_students WHERE student_id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ClassDAO.removeStudentFromAll: " + e.getMessage(), e);
        }
    }

    // ── Quiz assignment ────────────────────────────────────────────────────

    public void assignQuiz(int classId, int quizId) {
        String sql = "INSERT OR IGNORE INTO class_quizzes (class_id, quiz_id) VALUES (?, ?)";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, classId);
            ps.setInt(2, quizId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ClassDAO.assignQuiz: " + e.getMessage(), e);
        }
    }

    public void unassignQuiz(int classId, int quizId) {
        String sql = "DELETE FROM class_quizzes WHERE class_id = ? AND quiz_id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, classId);
            ps.setInt(2, quizId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ClassDAO.unassignQuiz: " + e.getMessage(), e);
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────

    public List<SchoolClass> findAll() {
        List<SchoolClass> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.get()
                .prepareStatement("SELECT * FROM classes ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapClass(rs));
        } catch (SQLException e) {
            throw new RuntimeException("ClassDAO.findAll: " + e.getMessage(), e);
        }
        return list;
    }

    public SchoolClass findById(int id) {
        String sql = "SELECT * FROM classes WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapClass(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("ClassDAO.findById: " + e.getMessage(), e);
        }
        return null;
    }

    public List<SchoolClass> findByTeacherId(int teacherId) {
        List<SchoolClass> list = new ArrayList<>();
        String sql = "SELECT * FROM classes WHERE teacher_id = ? ORDER BY name";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapClass(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("ClassDAO.findByTeacherId: " + e.getMessage(), e);
        }
        return list;
    }

    public SchoolClass findByStudentId(int studentId) {
        String sql = "SELECT c.* FROM classes c JOIN class_students cs ON c.id = cs.class_id WHERE cs.student_id = ? LIMIT 1";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapClass(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("ClassDAO.findByStudentId: " + e.getMessage(), e);
        }
        return null;
    }

    public boolean isEnrolled(int studentId, int classId) {
        String sql = "SELECT 1 FROM class_students WHERE class_id = ? AND student_id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, classId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("ClassDAO.isEnrolled: " + e.getMessage(), e);
        }
    }

    // ── Mapper ─────────────────────────────────────────────────────────────

    private SchoolClass mapClass(ResultSet rs) throws SQLException {
        int    id        = rs.getInt("id");
        String name      = rs.getString("name");
        int    teacherId = rs.getInt("teacher_id");
        SchoolClass sc   = new SchoolClass(id, name, teacherId);
        loadStudentIds(sc);
        loadQuizIds(sc);
        return sc;
    }

    private void loadStudentIds(SchoolClass sc) throws SQLException {
        String sql = "SELECT student_id FROM class_students WHERE class_id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, sc.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) sc.enrollStudent(rs.getInt("student_id"));
            }
        }
    }

    private void loadQuizIds(SchoolClass sc) throws SQLException {
        String sql = "SELECT quiz_id FROM class_quizzes WHERE class_id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, sc.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) sc.assignQuiz(rs.getInt("quiz_id"));
            }
        }
    }
}
