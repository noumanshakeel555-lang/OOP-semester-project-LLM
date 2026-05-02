package main.model;

import java.util.*;

public class Attempt {

    private Map<Integer, String> answers = new LinkedHashMap<>();

    public Attempt(Quiz quiz) {
    }

    public void answerQuestion(Question q, String answer) {
        answers.put(q.getId(), answer);
    }

    public Map<Integer, String> getAnswers() {
        return answers;
    }
}