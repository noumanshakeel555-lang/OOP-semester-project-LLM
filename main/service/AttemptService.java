package main.service;
import main.model.MCQQuestion;
import java.util.*;
import main.dao.*;
import main.model.*;

public class AttemptService {

    private static final int MAX_WARNINGS = 3;
    private int warnings = 0;

    private AccessDAO accessDAO = new AccessDAO();
    private AttemptDAO attemptDAO = new AttemptDAO();

    private long questionStartTime;
    private String lastAnswer = "";

    public Result startAttempt(Quiz quiz, Student student, Scanner sc) {

        if (!accessDAO.hasAccess(quiz.getId(), student.getId())) {
            throw new RuntimeException("Access Denied to this quiz");
        }

        Attempt attempt = new Attempt(quiz);

        System.out.println("\nQuiz Started...");
        System.out.println("Questions: " + quiz.getQuestions().size());

        long startTime = System.currentTimeMillis();
        long duration = 2 * 60 * 1000;

        for (Question q : quiz.getQuestions()) {

            if (System.currentTimeMillis() - startTime > duration) {
                System.out.println("⏰ Time Up! Auto submitting...");
                break;
            }

            System.out.println("\nQuestion: " + q.getQuestionText());

if (q instanceof MCQQuestion mcq) {
    List<String> opts = mcq.getOptions();

    for (int i = 0; i < opts.size(); i++) {
        System.out.println((i + 1) + ". " + opts.get(i));
    }
}

            questionStartTime = System.currentTimeMillis();

            System.out.print("Your Answer: ");
            String ans = sc.nextLine().trim();

            // cheating check
            if (isSuspicious(ans)) {
                warnings++;
                System.out.println("⚠ Warning " + warnings + "/" + MAX_WARNINGS);

                if (warnings >= MAX_WARNINGS) {
                    System.out.println("❌ Auto-submitting due to violations...");
                    break;
                }
            }

            // 🔥 FIXED: ALWAYS STORE BY QUESTION ID
            attempt.answerQuestion(q, ans);
        }

        Result result = evaluate(attempt, quiz);

        attemptDAO.saveAttempt(student.getId(), quiz.getId(), result);

        System.out.println("\n====================");
        System.out.println("FINAL SCORE: " + result.getScore());
        System.out.println("====================");

        return result;
    }

    // -------------------------
    // CHEATING DETECTION (REALISTIC)
    // -------------------------
    private boolean isSuspicious(String answer) {

        long timeTaken = System.currentTimeMillis() - questionStartTime;

        if (answer == null || answer.trim().isEmpty())
            return true;

        if (timeTaken < 1500)
            return true;

        if (answer.equalsIgnoreCase(lastAnswer))
            return true;

        lastAnswer = answer;
        return false;
    }

    // -------------------------
    // FIXED EVALUATION
    // -------------------------
    private Result evaluate(Attempt attempt, Quiz quiz) {

        int score = 0;

        for (Question q : quiz.getQuestions()) {

            String ans = attempt.getAnswers().get(q.getId());

            if (ans != null && !ans.isEmpty()) {
                score += q.evaluate(ans);
            }
        }

        int totalMarks = quiz.getQuestions()
        .stream()
        .mapToInt(q -> q.getMarks())
        .sum();

return new Result(score, totalMarks);
    }
} 