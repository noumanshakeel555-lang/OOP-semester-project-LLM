package main.dao;

import main.db.DatabaseManager;
import main.model.*;
import java.sql.*;
import java.util.*;

public class AttemptDAO {

    // ── Write ──────────────────────────────────────────────────────────────

    public void saveFullAttempt(Student student, Quiz quiz,
                                Map<Integer, String> answers,
                                Result result, int violations) {
        DatabaseManager.beginTransaction();
        try {
            // 1. Insert attempt header row
            String sql =
                "INSERT INTO attempts " +
                "(student_id, quiz_id, score, total_marks, pending_marks, violations, attempted_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

            int attemptId;
            try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
                ps.setInt(1,  student.getId());
                ps.setInt(2,  quiz.getId());
                ps.setInt(3,  result.getScore());
                ps.setInt(4,  result.getTotalMarks());
                ps.setInt(5,  result.getPendingMarks());
                ps.setInt(6,  violations);
                ps.setLong(7, System.currentTimeMillis());
                ps.executeUpdate();
                attemptId = DatabaseManager.lastInsertId();
            }

            // 2. Insert each answer
            // MCQ  -> store auto-score in manual_score
            // Subj -> store -1 (pending teacher grading)
            String ansSql =
                "INSERT INTO attempt_answers (attempt_id, question_id, answer_text, manual_score) " +
                "VALUES (?, ?, ?, ?)";

            try (PreparedStatement ps = DatabaseManager.get().prepareStatement(ansSql)) {
                for (Question q : quiz.getQuestions()) {
                    String answerText = answers.getOrDefault(q.getId(), "");
                    int scored;
                    if (q instanceof SubjectiveQuestion) {
                        scored = -1;
                    } else {
                        scored = q.evaluate(answerText);
                    }
                    ps.setInt(1, attemptId);
                    ps.setInt(2, q.getId());
                    ps.setString(3, answerText);
                    ps.setInt(4, scored);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            DatabaseManager.commit();
        } catch (Exception e) {
            DatabaseManager.rollback();
            throw new RuntimeException("AttemptDAO.saveFullAttempt: " + e.getMessage(), e);
        }
    }

    /**
     * Teacher awards marks for one subjective answer.
     * Updates the answer row AND the attempt running score + pending_marks.
     */
    public void awardSubjectiveMarks(int attemptId, int questionId, int marksAwarded) {
        DatabaseManager.beginTransaction();
        try {
            // 1. Update the answer row
            String updAns =
                "UPDATE attempt_answers SET manual_score = ? " +
                "WHERE attempt_id = ? AND question_id = ?";
            try (PreparedStatement ps = DatabaseManager.get().prepareStatement(updAns)) {
                ps.setInt(1, marksAwarded);
                ps.setInt(2, attemptId);
                ps.setInt(3, questionId);
                ps.executeUpdate();
            }

            // 2. Get the max marks for this question
            int maxMarks = 0;
            String qSql = "SELECT marks FROM questions WHERE id = ?";
            try (PreparedStatement ps = DatabaseManager.get().prepareStatement(qSql)) {
                ps.setInt(1, questionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) maxMarks = rs.getInt("marks");
                }
            }

            // 3. Update attempt totals
            String updAttempt =
                "UPDATE attempts " +
                "SET score = score + ?, " +
                "    pending_marks = MAX(0, pending_marks - ?) " +
                "WHERE id = ?";
            try (PreparedStatement ps = DatabaseManager.get().prepareStatement(updAttempt)) {
                ps.setInt(1, marksAwarded);
                ps.setInt(2, maxMarks);
                ps.setInt(3, attemptId);
                ps.executeUpdate();
            }

            DatabaseManager.commit();
        } catch (Exception e) {
            DatabaseManager.rollback();
            throw new RuntimeException("AttemptDAO.awardSubjectiveMarks: " + e.getMessage(), e);
        }
    }

    /** Legacy console save — no answer map available. */
    public void saveAttempt(int studentId, int quizId, Result result) {
        String sql =
            "INSERT INTO attempts " +
            "(student_id, quiz_id, score, total_marks, pending_marks, violations, attempted_at) " +
            "VALUES (?, ?, ?, ?, ?, 0, ?)";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1,  studentId);
            ps.setInt(2,  quizId);
            ps.setInt(3,  result.getScore());
            ps.setInt(4,  result.getTotalMarks());
            ps.setInt(5,  result.getPendingMarks());
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("AttemptDAO.saveAttempt: " + e.getMessage(), e);
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────

    /** All attempts for a list of student IDs (teacher roster). */
    public List<AttemptRecord> findByStudentIds(List<Integer> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return Collections.emptyList();

        StringBuilder ph = new StringBuilder();
        for (int i = 0; i < studentIds.size(); i++) {
            if (i > 0) ph.append(",");
            ph.append("?");
        }
        String sql =
            "SELECT * FROM attempts WHERE student_id IN (" + ph + ") " +
            "ORDER BY attempted_at DESC";

        List<AttemptRecord> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            for (int i = 0; i < studentIds.size(); i++) ps.setInt(i + 1, studentIds.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAttempt(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("AttemptDAO.findByStudentIds: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Returns only attempts that still have at least one subjective answer
     * with manual_score = -1 (not yet graded).
     */
    public List<AttemptRecord> findPendingSubjective(List<Integer> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) return Collections.emptyList();

        StringBuilder ph = new StringBuilder();
        for (int i = 0; i < studentIds.size(); i++) {
            if (i > 0) ph.append(",");
            ph.append("?");
        }

        String sql =
            "SELECT DISTINCT a.* FROM attempts a " +
            "JOIN attempt_answers aa ON aa.attempt_id = a.id " +
            "JOIN questions q ON q.id = aa.question_id " +
            "WHERE a.student_id IN (" + ph + ") " +
            "  AND aa.manual_score = -1 " +
            "  AND q.type = 'SUBJECTIVE' " +
            "ORDER BY a.attempted_at DESC";

        List<AttemptRecord> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            for (int i = 0; i < studentIds.size(); i++) ps.setInt(i + 1, studentIds.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAttempt(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("AttemptDAO.findPendingSubjective: " + e.getMessage(), e);
        }
        return list;
    }

    /** How many subjective answers in this attempt are still ungraded. */
    public int countPending(int attemptId) {
        String sql =
            "SELECT COUNT(*) FROM attempt_answers aa " +
            "JOIN questions q ON q.id = aa.question_id " +
            "WHERE aa.attempt_id = ? " +
            "  AND aa.manual_score = -1 " +
            "  AND q.type = 'SUBJECTIVE'";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("AttemptDAO.countPending: " + e.getMessage(), e);
        }
    }

    /**
     * Per-question awarded marks for one attempt.
     * MCQ: auto-score stored at submit time.
     * Subjective: -1 = not yet graded, >= 0 = teacher's awarded marks.
     */
    public Map<Integer, Integer> loadAwardedMarks(int attemptId) {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT question_id, manual_score FROM attempt_answers WHERE attempt_id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    map.put(rs.getInt("question_id"), rs.getInt("manual_score"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("AttemptDAO.loadAwardedMarks: " + e.getMessage(), e);
        }
        return map;
    }

    public List<AttemptRecord> findByStudentId(int studentId) {
        String sql = "SELECT * FROM attempts WHERE student_id = ? ORDER BY attempted_at DESC";
        List<AttemptRecord> list = new ArrayList<>();
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAttempt(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("AttemptDAO.findByStudentId: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Returns true if the student has already submitted an attempt for this quiz.
     * Used to block re-attempts on the student dashboard.
     */
    public boolean hasAttempted(int studentId, int quizId) {
        String sql = "SELECT 1 FROM attempts WHERE student_id = ? AND quiz_id = ? LIMIT 1";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("AttemptDAO.hasAttempted: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a Set of quiz IDs this student has already attempted.
     * More efficient than calling hasAttempted() per quiz in a loop.
     */
    public java.util.Set<Integer> attemptedQuizIds(int studentId) {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        String sql = "SELECT DISTINCT quiz_id FROM attempts WHERE student_id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("quiz_id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("AttemptDAO.attemptedQuizIds: " + e.getMessage(), e);
        }
        return ids;
    }

    public List<AttemptRecord> findAll() {
        List<AttemptRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM attempts ORDER BY attempted_at DESC";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapAttempt(rs));
        } catch (SQLException e) {
            throw new RuntimeException("AttemptDAO.findAll: " + e.getMessage(), e);
        }
        return list;
    }

    // ── Mappers ────────────────────────────────────────────────────────────

    private AttemptRecord mapAttempt(ResultSet rs) throws SQLException {
        int  id         = rs.getInt("id");
        int  studentId  = rs.getInt("student_id");
        int  quizId     = rs.getInt("quiz_id");
        int  score      = rs.getInt("score");
        int  total      = rs.getInt("total_marks");
        int  pending    = rs.getInt("pending_marks");
        int  violations = rs.getInt("violations");
        long timestamp  = rs.getLong("attempted_at");

        User u    = new UserDAO().findById(studentId);
        Quiz quiz = new QuizDAO().findById(quizId);

        String studentName = (u    != null) ? u.getUsername() : "Student #" + studentId;
        String quizTitle   = (quiz != null) ? quiz.getTitle() : "Quiz #"   + quizId;

        Map<Integer, String> answers = loadAnswers(id);

        return new AttemptRecord(id, studentId, studentName,
            quizId, quizTitle, answers,
            new Result(score, total, pending), violations, timestamp);
    }

    private Map<Integer, String> loadAnswers(int attemptId) throws SQLException {
        Map<Integer, String> map = new LinkedHashMap<>();
        String sql = "SELECT question_id, answer_text FROM attempt_answers WHERE attempt_id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    map.put(rs.getInt("question_id"), rs.getString("answer_text"));
            }
        }
        return map;
    }
}
