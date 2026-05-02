package main.model;

import java.util.*;

public class Quiz {
    private int id;
    private String title;
    private int classId;          // 0 = no restriction (legacy / unassigned)
    private List<Question> questions;

    /** Legacy constructor — no class restriction. */
    public Quiz(int id, String title) {
        this(id, title, 0);
    }

    /** Full constructor used when creating a class-assigned quiz. */
    public Quiz(int id, String title, int classId) {
        this.id        = id;
        this.title     = title;
        this.classId   = classId;
        this.questions = new ArrayList<>();
    }

    public int    getId()      { return id; }
    public String getTitle()   { return title; }
    public int    getClassId() { return classId; }

    /** Called by QuizDAO after insertion to stamp the generated ID. */
    public void setId(int id)           { this.id = id; }
    public void setClassId(int classId) { this.classId = classId; }

    public void addQuestion(Question q) {
        if (q == null) throw new IllegalArgumentException("Question cannot be null");
        questions.add(q);
    }

    public List<Question> getQuestions() { return questions; }

    @Override public String toString() { return title; }
}
