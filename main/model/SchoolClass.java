package main.model;

import java.util.*;

/**
 * Represents a school class / section (e.g. "Grade 10 — Section A").
 *
 * A class has:
 *  - one owning teacher
 *  - a set of enrolled student IDs
 *  - a set of quiz IDs assigned to it
 */
public class SchoolClass {

    private int          id;
    private String       name;
    private int          teacherId;
    private List<Integer> studentIds;
    private List<Integer> quizIds;

    public SchoolClass(int id, String name, int teacherId) {
        this.id         = id;
        this.name       = name;
        this.teacherId  = teacherId;
        this.studentIds = new ArrayList<>();
        this.quizIds    = new ArrayList<>();
    }

    // ── Getters ────────────────────────────────────────────────────────────
    public int    getId()        { return id; }
    public String getName()      { return name; }
    public int    getTeacherId() { return teacherId; }

    public List<Integer> getStudentIds() { return Collections.unmodifiableList(studentIds); }
    public List<Integer> getQuizIds()    { return Collections.unmodifiableList(quizIds); }

    // ── Mutators ───────────────────────────────────────────────────────────
    public void setId(int id) { this.id = id; }

    public void enrollStudent(int studentId) {
        if (!studentIds.contains(studentId)) studentIds.add(studentId);
    }

    public void removeStudent(int studentId) { studentIds.remove(Integer.valueOf(studentId)); }

    public void assignQuiz(int quizId) {
        if (!quizIds.contains(quizId)) quizIds.add(quizId);
    }

    public void unassignQuiz(int quizId) { quizIds.remove(Integer.valueOf(quizId)); }

    public boolean hasStudent(int studentId) { return studentIds.contains(studentId); }
    public boolean hasQuiz(int quizId)       { return quizIds.contains(quizId); }

    @Override public String toString() { return name; }
}
