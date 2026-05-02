package main.dao;

import main.db.DatabaseManager;
import java.sql.*;

/**
 * Controls whether a student may attempt a given quiz.
 *
 * Rules (evaluated in order):
 *  1. Quiz does not exist              → deny
 *  2. Quiz has class_id = 0 (open)    → allow
 *  3. Student enrolled in quiz's class → allow
 *  4. Otherwise                        → deny
 */
public class AccessDAO {

    public boolean hasAccess(int quizId, int studentId) {
        // Step 1 + 2: check quiz existence and class_id
        String quizSql = "SELECT class_id FROM quizzes WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(quizSql)) {
            ps.setInt(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;         // quiz not found
                int classId = rs.getInt("class_id");
                if (classId == 0) return true;        // unrestricted quiz

                // Step 3: check enrolment
                return isEnrolled(studentId, classId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("AccessDAO.hasAccess: " + e.getMessage(), e);
        }
    }

    private boolean isEnrolled(int studentId, int classId) throws SQLException {
        String sql = "SELECT 1 FROM class_students WHERE class_id = ? AND student_id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, classId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
