package main.dao;

import main.db.DatabaseManager;
import main.model.*;
import java.sql.*;
import java.util.*;

public class QuizDAO {

    // ── Write ──────────────────────────────────────────────────────────────

    public void save(Quiz quiz) {
        DatabaseManager.beginTransaction();
        try {
            // 1. Insert quiz row
            String quizSql = "INSERT INTO quizzes (title, class_id) VALUES (?, ?)";
            try (PreparedStatement ps = DatabaseManager.get().prepareStatement(quizSql)) {
                ps.setString(1, quiz.getTitle());
                ps.setInt(2, quiz.getClassId());
                ps.executeUpdate();
                quiz.setId(DatabaseManager.lastInsertId());
            }

            // 2. Insert each question + its options
            int pos = 0;
            for (Question q : quiz.getQuestions()) {
                saveQuestion(q, quiz.getId(), pos++);
            }

            DatabaseManager.commit();
        } catch (Exception e) {
            DatabaseManager.rollback();
            throw new RuntimeException("QuizDAO.save: " + e.getMessage(), e);
        }
    }

    public void updateClassId(int quizId, int classId) {
        String sql = "UPDATE quizzes SET class_id = ? WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, classId);
            ps.setInt(2, quizId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("QuizDAO.updateClassId: " + e.getMessage(), e);
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────

    public List<Quiz> findAll() {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quizzes ORDER BY id";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapQuiz(rs));
        } catch (SQLException e) {
            throw new RuntimeException("QuizDAO.findAll: " + e.getMessage(), e);
        }
        return list;
    }

    public Quiz findById(int id) {
        String sql = "SELECT * FROM quizzes WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapQuiz(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("QuizDAO.findById: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Quiz> findStrictByClassId(int classId) {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quizzes WHERE class_id = ? ORDER BY id";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapQuiz(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("QuizDAO.findStrictByClassId: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Quiz> findUnassigned() {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quizzes WHERE class_id = 0 ORDER BY id";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapQuiz(rs));
        } catch (SQLException e) {
            throw new RuntimeException("QuizDAO.findUnassigned: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Mappers ────────────────────────────────────────────────────────────

    private Quiz mapQuiz(ResultSet rs) throws SQLException {
        int    id      = rs.getInt("id");
        String title   = rs.getString("title");
        int    classId = rs.getInt("class_id");
        Quiz quiz = new Quiz(id, title, classId);
        loadQuestions(quiz);
        return quiz;
    }

    private void loadQuestions(Quiz quiz) throws SQLException {
        String sql = "SELECT * FROM questions WHERE quiz_id = ? ORDER BY position";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, quiz.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) quiz.addQuestion(mapQuestion(rs));
            }
        }
    }

    private Question mapQuestion(ResultSet rs) throws SQLException {
        int    id      = rs.getInt("id");
        String type    = rs.getString("type");
        String text    = rs.getString("question_text");
        int    marks   = rs.getInt("marks");
        String correct = rs.getString("correct_answer");

        if ("MCQ".equals(type)) {
            List<String> options = loadOptions(id);
            return new MCQQuestion(id, text, marks, options, correct);
        } else {
            return new SubjectiveQuestion(id, text, marks);
        }
    }

    private List<String> loadOptions(int questionId) throws SQLException {
        List<String> opts = new ArrayList<>();
        String sql = "SELECT option_text FROM question_options WHERE question_id = ? ORDER BY position";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) opts.add(rs.getString("option_text"));
            }
        }
        return opts;
    }

    // ── Question write ─────────────────────────────────────────────────────

    private void saveQuestion(Question q, int quizId, int position) throws SQLException {
        String sql = "INSERT INTO questions (quiz_id, position, type, question_text, marks, correct_answer) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, quizId);
            ps.setInt(2, position);
            ps.setString(3, q instanceof MCQQuestion ? "MCQ" : "SUBJECTIVE");
            ps.setString(4, q.getQuestionText());
            ps.setInt(5, q.getMarks());
            ps.setString(6, q instanceof MCQQuestion mcq ? mcq.getCorrectAnswer() : null);
            ps.executeUpdate();
            int dbId = DatabaseManager.lastInsertId();
            if (q instanceof MCQQuestion mcq) {
                saveOptions(dbId, mcq.getOptions());
            }
        }
    }

    private void saveOptions(int questionId, List<String> options) throws SQLException {
        String sql = "INSERT INTO question_options (question_id, position, option_text) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            for (int i = 0; i < options.size(); i++) {
                ps.setInt(1, questionId);
                ps.setInt(2, i);
                ps.setString(3, options.get(i));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
