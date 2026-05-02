package main.app;
import java.util.*;
import main.model.*;
import main.service.*;

public class MainApp {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        AuthService authService = new AuthService();
        QuizService quizService = new QuizService();
        AttemptService attemptService = new AttemptService();

        try {
            System.out.println("1. Login\n2. Signup");
            int choice = sc.nextInt();
            sc.nextLine();

            User user = null;

            // ✅ LOGIN / SIGNUP
            if(choice == 1) {
                System.out.print("Username: ");
                String u = sc.nextLine();

                System.out.print("Password: ");
                String p = sc.nextLine();

                user = authService.login(u, p);

            } else if(choice == 2) {
                System.out.print("Username: ");
                String u = sc.nextLine();

                System.out.print("Password: ");
                String p = sc.nextLine();

                System.out.print("Role (STUDENT/TEACHER): ");
                String r = sc.nextLine();

                authService.signup(u, p, r);
                System.out.println("Signup successful!");
                return;

            } else {
                System.out.println("Invalid choice");
                return;
            }

            user.displayDashboard();

            // =======================
            // 🎓 STUDENT FLOW
            // =======================
            if(user instanceof Student) {

                Student student = (Student) user;

                List<Quiz> quizzes = quizService.getAllQuizzes();

                if(quizzes.isEmpty()) {
                    System.out.println("No quizzes available");
                    return;
                }

                System.out.println("Available Quizzes:");
                for(int i = 0; i < quizzes.size(); i++) {
                    System.out.println((i+1) + ". " + quizzes.get(i).getTitle());
                }

                System.out.print("Select quiz: ");
                int qChoice = sc.nextInt();
                sc.nextLine();

                if(qChoice < 1 || qChoice > quizzes.size()) {
                    System.out.println("Invalid selection");
                    return;
                }

                Quiz selectedQuiz = quizzes.get(qChoice - 1);

                // ✅ FIXED METHOD CALL
                Result result = attemptService.startAttempt(selectedQuiz, student, sc);
                result.display();
            }

            // =======================
            // 🧑‍🏫 TEACHER FLOW
            // =======================
            else if(user instanceof Teacher) {

                System.out.println("1. Create Quiz");
                int ch = sc.nextInt();
                sc.nextLine();

                if(ch == 1) {

                    System.out.print("Enter quiz title: ");
                    String title = sc.nextLine();

                    Quiz quiz = new Quiz(0, title);

                    System.out.print("How many questions? ");
                    int n = sc.nextInt();
                    sc.nextLine();

                    for(int i = 0; i < n; i++) {

                        System.out.println("\nQuestion " + (i+1));
                        System.out.println("1. MCQ  2. Subjective");
                        int type = sc.nextInt();
                        sc.nextLine();

                        System.out.print("Enter question: ");
                        String qText = sc.nextLine();

                        if(type == 1) {
                            List<String> options = new ArrayList<>();

                            for(int j = 0; j < 4; j++) {
                                System.out.print("Option " + (j+1) + ": ");
                                options.add(sc.nextLine());
                            }

                            System.out.print("Correct answer: ");
                            String correct = sc.nextLine();

                            quiz.addQuestion(new MCQQuestion(i, qText, 5, options, correct));

                        } else if(type == 2) {
                            quiz.addQuestion(new SubjectiveQuestion(i, qText, 10));

                        } else {
                            System.out.println("Invalid type, skipping...");
                            i--;
                        }
                    }

                    quizService.createQuiz(quiz);
                    System.out.println("Quiz Created Successfully!");
                }
            }

        } catch(InputMismatchException e) {
            System.out.println("Invalid input type. Please enter correct values.");
        } catch(Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            sc.close();
        }
    }
}
