package main.service;

import main.dao.QuizDAO;
import main.model.*;
import java.util.List;

public class QuizService {

    private final QuizDAO quizDAO = new QuizDAO();

    public void createQuiz(Quiz quiz) {
        if (quiz == null) throw new IllegalArgumentException("Quiz cannot be null.");
        quizDAO.save(quiz);
    }

    public List<Quiz> getAllQuizzes() {
        return quizDAO.findAll();
    }

    public List<Quiz> getQuizzesForClass(int classId) {
        return quizDAO.findStrictByClassId(classId);
    }

    public Quiz findById(int id) {
        return quizDAO.findById(id);
    }
}
