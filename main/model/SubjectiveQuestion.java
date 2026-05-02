package main.model;

public class SubjectiveQuestion extends Question {

    public SubjectiveQuestion(int id, String questionText, int marks) {
        super(id, questionText, marks);
    }

    /**
     * Subjective questions are NOT auto-scored.
     * Always returns 0 — the teacher manually assigns marks
     * through the Answer Review panel in the Teacher Dashboard.
     */
    @Override
    public int evaluate(String answer) {
        return 0;
    }
}
