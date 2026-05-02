package main.model;

public abstract class Question {

    protected int id;
    protected String questionText;
    protected int marks;

    public Question(int id, String questionText, int marks) {
        this.id = id;
        this.questionText = questionText;
        this.marks = marks;
    }

    public int getId() {
        return id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public int getMarks() {
        return marks;
    }

    // 🔥 SINGLE SOURCE OF TRUTH
    public abstract int evaluate(String answer);
}