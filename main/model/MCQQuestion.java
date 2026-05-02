package main.model;

import java.util.List;

public class MCQQuestion extends Question {

    private List<String> options;
    private String correctAnswer;

    public MCQQuestion(int id, String questionText, int marks,
                       List<String> options, String correctAnswer) {
        super(id, questionText, marks);
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
@Override
public int evaluate(String answer) {

    if (answer == null) return 0;

    String user = answer.trim().toLowerCase();
    String correct = correctAnswer.trim().toLowerCase();

    // 1. direct match
    if (user.equals(correct)) {
        return marks;
    }

    // 2. numeric input (1-4)
    try {
        int idx = Integer.parseInt(user) - 1;

        if (idx >= 0 && idx < options.size()) {
            String selected = options.get(idx).trim().toLowerCase();

            if (selected.equals(correct)) {
                return marks;
            }
        }
    } catch (Exception ignored) {}

    return 0;
}
    
}