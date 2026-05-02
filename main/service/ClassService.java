package main.service;

import main.dao.*;
import main.model.*;
import java.util.*;

public class ClassService {

    private final ClassDAO classDAO = new ClassDAO();
    private final QuizDAO  quizDAO  = new QuizDAO();
    private final UserDAO  userDAO  = new UserDAO();

    // ── Class CRUD ────────────────────────────────────────────────────────

    public SchoolClass createClass(String name, Teacher teacher) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Class name cannot be blank.");
        SchoolClass sc = new SchoolClass(0, name.trim(), teacher.getId());
        return classDAO.save(sc);
    }

    public List<SchoolClass> getClassesForTeacher(Teacher teacher) {
        return classDAO.findByTeacherId(teacher.getId());
    }

    public SchoolClass findById(int classId) {
        return classDAO.findById(classId);
    }

    // ── Enrolment ─────────────────────────────────────────────────────────

    public void enrollStudentByUsername(SchoolClass sc, String username) {
        User user = userDAO.findByUsername(username);
        if (user == null)
            throw new RuntimeException("No user found with username: " + username);
        if (!(user instanceof Student))
            throw new RuntimeException(username + " is not a student.");

        // Remove from any previous class first
        classDAO.removeStudentFromAll(user.getId());

        // Persist enrolment
        classDAO.enrollStudent(sc.getId(), user.getId());
    }

    public void removeStudentFromClass(SchoolClass sc, int studentId) {
        classDAO.removeStudent(sc.getId(), studentId);
    }

    public List<Student> getStudentsInClass(SchoolClass sc) {
        List<Student> result = new ArrayList<>();
        for (int sid : sc.getStudentIds()) {
            User u = userDAO.findById(sid);
            if (u instanceof Student s) result.add(s);
        }
        return result;
    }

    // ── Quiz assignment ───────────────────────────────────────────────────

    public void assignQuizToClass(SchoolClass sc, Quiz quiz) {
        // Update quiz's class_id in DB
        quizDAO.updateClassId(quiz.getId(), sc.getId());
        quiz.setClassId(sc.getId());

        // Record the assignment in the join table
        classDAO.assignQuiz(sc.getId(), quiz.getId());
    }

    public void unassignQuizFromClass(SchoolClass sc, Quiz quiz) {
        quizDAO.updateClassId(quiz.getId(), 0);
        quiz.setClassId(0);
        classDAO.unassignQuiz(sc.getId(), quiz.getId());
    }

    public List<Quiz> getQuizzesForClass(SchoolClass sc) {
        return quizDAO.findStrictByClassId(sc.getId());
    }

    // ── Student-facing ────────────────────────────────────────────────────

    public List<Quiz> getQuizzesForStudent(Student student) {
        SchoolClass sc = classDAO.findByStudentId(student.getId());
        if (sc != null) return getQuizzesForClass(sc);
        return quizDAO.findUnassigned();   // fallback for unenrolled students
    }

    public SchoolClass getClassForStudent(Student student) {
        return classDAO.findByStudentId(student.getId());
    }

    public List<Quiz> getUnassignedQuizzes() {
        return quizDAO.findUnassigned();
    }

    public List<Quiz> getAllQuizzes() {
        return quizDAO.findAll();
    }
}
