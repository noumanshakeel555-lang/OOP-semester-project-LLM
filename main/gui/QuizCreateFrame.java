package main.gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import main.model.*;
import main.service.QuizService;

/**
 * Standalone quiz-creation frame.
 * Used when a teacher logs in and is routed directly here (no dashboard yet shown).
 * After creating a quiz the teacher lands on TeacherDashboardFrame.
 */
public class QuizCreateFrame extends JFrame {

    private final Teacher     teacher;
    private final QuizService quizService = new QuizService();

    // ── Form fields ───────────────────────────────────────────────────────
    private JTextField         titleField;
    private JSpinner           countSpinner;
    private JPanel             questionsPanel;
    private JScrollPane        scrollPane;
    private final List<QuestionRow> questionRows = new ArrayList<>();
    private int                questionCount = 0;

    public QuizCreateFrame(Teacher teacher) {
        this.teacher = teacher;

        setTitle("QuizPro — Create Quiz");
        setSize(700, 620);
        setMinimumSize(new Dimension(580, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG);
        setContentPane(root);

        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildBody(),    BorderLayout.CENTER);
        root.add(buildFooter(),  BorderLayout.SOUTH);

        setVisible(true);
    }

    // ── Header ────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.SIDEBAR_BG);
        header.setBorder(BorderFactory.createEmptyBorder(16, 28, 16, 28));

        JLabel logo = new JLabel("📚  QuizPro  —  Quiz Builder");
        logo.setFont(UITheme.SUBHEAD_FONT);
        logo.setForeground(Color.WHITE);

        JLabel userChip = UITheme.badge(
            "🧑‍🏫  " + teacher.getUsername(),
            new Color(255, 255, 255, 25),
            Color.WHITE
        );

        header.add(logo,     BorderLayout.WEST);
        header.add(userChip, BorderLayout.EAST);
        return header;
    }

    // ── Body ──────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setBackground(UITheme.BG);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 28, 0, 28));

        // ── Quiz metadata card ──
        JPanel metaCard = UITheme.card();
        metaCard.setLayout(new BoxLayout(metaCard, BoxLayout.Y_AXIS));
        metaCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        metaCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel metaHeading = UITheme.subheadLabel("Quiz Details");
        metaHeading.setAlignmentX(Component.LEFT_ALIGNMENT);

        // title row
        JPanel titleRow = new JPanel(new BorderLayout(12, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel titleLbl = UITheme.bodyLabel("Quiz Title:");
        titleLbl.setPreferredSize(new Dimension(100, 32));
        titleField = UITheme.textField();
        titleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        titleRow.add(titleLbl,  BorderLayout.WEST);
        titleRow.add(titleField, BorderLayout.CENTER);

        // question count row
        JPanel countRow = new JPanel(new BorderLayout(12, 0));
        countRow.setOpaque(false);
        countRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel countLbl = UITheme.bodyLabel("Questions:");
        countLbl.setPreferredSize(new Dimension(100, 32));

        countSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 20, 1));
        countSpinner.setFont(UITheme.BODY_FONT);
        countSpinner.setPreferredSize(new Dimension(80, 36));
        ((JSpinner.DefaultEditor) countSpinner.getEditor()).getTextField().setFont(UITheme.BODY_FONT);

        JButton applyCountBtn = UITheme.secondaryBtn("Set Questions");
        applyCountBtn.setPreferredSize(new Dimension(140, 36));
        applyCountBtn.addActionListener(e -> applyQuestionCount());

        JPanel countRight = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        countRight.setOpaque(false);
        countRight.add(countSpinner);
        countRight.add(applyCountBtn);

        countRow.add(countLbl,   BorderLayout.WEST);
        countRow.add(countRight, BorderLayout.CENTER);

        metaCard.add(metaHeading);
        metaCard.add(Box.createVerticalStrut(14));
        metaCard.add(titleRow);
        metaCard.add(Box.createVerticalStrut(10));
        metaCard.add(countRow);

        body.add(metaCard);
        body.add(Box.createVerticalStrut(16));

        // ── Questions section ──
        JLabel qHeading = UITheme.subheadLabel("Questions");
        qHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(qHeading);
        body.add(Box.createVerticalStrut(10));

        questionsPanel = new JPanel();
        questionsPanel.setBackground(UITheme.BG);
        questionsPanel.setLayout(new BoxLayout(questionsPanel, BoxLayout.Y_AXIS));

        scrollPane = UITheme.scrollPane(questionsPanel);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        body.add(scrollPane);

        // trigger initial rows
        applyQuestionCount();

        return body;
    }

    // ── Footer ────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(14, 28, 14, 28)
        ));

        JButton cancelBtn = UITheme.secondaryBtn("Cancel");
        cancelBtn.addActionListener(e -> {
            dispose();
            new TeacherDashboardFrame(teacher);
        });

        JButton saveBtn = UITheme.primaryBtn("💾  Save Quiz");
        saveBtn.addActionListener(e -> saveQuiz());

        bar.add(cancelBtn, BorderLayout.WEST);
        bar.add(saveBtn,   BorderLayout.EAST);
        return bar;
    }

    // ── Apply question count ──────────────────────────────────────────────
    private void applyQuestionCount() {
        int target = (int) countSpinner.getValue();

        if (target == questionCount) return;

        if (target < questionCount) {
            // remove rows from the end
            while (questionCount > target) {
                questionCount--;
                questionsPanel.remove(questionsPanel.getComponentCount() - 1);
                questionRows.remove(questionRows.size() - 1);
            }
        } else {
            // add new rows
            while (questionCount < target) {
                QuestionRow row = new QuestionRow(questionCount + 1);
                questionRows.add(row);
                questionsPanel.add(row);
                questionsPanel.add(Box.createVerticalStrut(10));
                questionCount++;
            }
        }

        questionsPanel.revalidate();
        questionsPanel.repaint();
    }

    // ── Save quiz ─────────────────────────────────────────────────────────
    private void saveQuiz() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter a quiz title.", "Missing Title",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        Quiz quiz = new Quiz(0, title);

        for (int i = 0; i < questionRows.size(); i++) {
            QuestionRow row = questionRows.get(i);
            try {
                Question q = row.buildQuestion(i);
                quiz.addQuestion(q);
            } catch (IllegalStateException ex) {
                JOptionPane.showMessageDialog(this,
                    "Q" + (i + 1) + ": " + ex.getMessage(),
                    "Incomplete Question",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        quizService.createQuiz(quiz);
        JOptionPane.showMessageDialog(this,
            "✅  Quiz \"" + title + "\" saved with " + quiz.getQuestions().size() + " questions!",
            "Quiz Saved",
            JOptionPane.INFORMATION_MESSAGE);

        dispose();
        new TeacherDashboardFrame(teacher);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Inner class: one question row (type toggle + fields)
    // ═══════════════════════════════════════════════════════════════════════
    static class QuestionRow extends JPanel {

        private final int          number;
        private JTextField         questionTextField;
        private JComboBox<String>  typeCombo;
        private JPanel             typeSpecificPanel;

        // MCQ fields
        private JTextField[]       optionFields = new JTextField[4];
        private JTextField         correctField;

        // Subjective: no extra field needed (marks are fixed = 10)

        QuestionRow(int number) {
            this.number = number;
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
            setLayout(new BorderLayout());
            buildUI();
        }

        private void buildUI() {
            JPanel card = UITheme.card();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setAlignmentX(LEFT_ALIGNMENT);

            // row header: Q badge + type selector
            JPanel headerRow = new JPanel(new BorderLayout(12, 0));
            headerRow.setOpaque(false);
            headerRow.setAlignmentX(LEFT_ALIGNMENT);

            JLabel badge = UITheme.badge("Q" + number, new Color(0xE8EEFF), UITheme.PRIMARY);

            String[] types = {"MCQ (Multiple Choice)", "Subjective (Written)"};
            typeCombo = new JComboBox<>(types);
            typeCombo.setFont(UITheme.BODY_FONT);
            typeCombo.setPreferredSize(new Dimension(200, 36));
            typeCombo.addActionListener(e -> refreshTypePanel());

            headerRow.add(badge,     BorderLayout.WEST);
            headerRow.add(typeCombo, BorderLayout.EAST);

            // question text field
            JPanel qRow = new JPanel(new BorderLayout(10, 0));
            qRow.setOpaque(false);
            qRow.setAlignmentX(LEFT_ALIGNMENT);
            JLabel qLbl = UITheme.mutedLabel("Question:");
            qLbl.setPreferredSize(new Dimension(80, 32));
            questionTextField = UITheme.textField();
            questionTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            qRow.add(qLbl,               BorderLayout.WEST);
            qRow.add(questionTextField,  BorderLayout.CENTER);

            // type-specific area
            typeSpecificPanel = new JPanel();
            typeSpecificPanel.setOpaque(false);
            typeSpecificPanel.setAlignmentX(LEFT_ALIGNMENT);

            card.add(headerRow);
            card.add(Box.createVerticalStrut(12));
            card.add(qRow);
            card.add(Box.createVerticalStrut(10));
            card.add(typeSpecificPanel);

            add(card, BorderLayout.CENTER);

            refreshTypePanel();
        }

        private void refreshTypePanel() {
            typeSpecificPanel.removeAll();
            typeSpecificPanel.setLayout(new BoxLayout(typeSpecificPanel, BoxLayout.Y_AXIS));

            if (typeCombo.getSelectedIndex() == 0) {
                buildMcqPanel();
            } else {
                buildSubjectivePanel();
            }

            typeSpecificPanel.revalidate();
            typeSpecificPanel.repaint();
            // Notify parent to re-layout
            Container parent = getParent();
            if (parent != null) { parent.revalidate(); parent.repaint(); }
        }

        private void buildMcqPanel() {
            JLabel optLbl = UITheme.mutedLabel("Options (enter 4 choices):");
            optLbl.setAlignmentX(LEFT_ALIGNMENT);
            typeSpecificPanel.add(optLbl);
            typeSpecificPanel.add(Box.createVerticalStrut(6));

            String[] letters = {"A", "B", "C", "D"};
            for (int i = 0; i < 4; i++) {
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setOpaque(false);
                row.setAlignmentX(LEFT_ALIGNMENT);
                JLabel lbl = UITheme.mutedLabel(letters[i] + ".");
                lbl.setPreferredSize(new Dimension(24, 32));
                optionFields[i] = UITheme.textField();
                optionFields[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
                row.add(lbl,            BorderLayout.WEST);
                row.add(optionFields[i], BorderLayout.CENTER);
                typeSpecificPanel.add(row);
                typeSpecificPanel.add(Box.createVerticalStrut(6));
            }

            JPanel correctRow = new JPanel(new BorderLayout(8, 0));
            correctRow.setOpaque(false);
            correctRow.setAlignmentX(LEFT_ALIGNMENT);
            JLabel correctLbl = UITheme.colorLabel("✓ Correct Answer:", UITheme.SUCCESS);
            correctLbl.setPreferredSize(new Dimension(140, 32));
            correctLbl.setFont(UITheme.SMALL_FONT);
            correctField = UITheme.textField();
            correctField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            correctRow.add(correctLbl,  BorderLayout.WEST);
            correctRow.add(correctField, BorderLayout.CENTER);
            typeSpecificPanel.add(correctRow);
        }

        private void buildSubjectivePanel() {
            JLabel info = UITheme.mutedLabel(
                "Students will type a written answer.  Marks: 10");
            info.setAlignmentX(LEFT_ALIGNMENT);
            typeSpecificPanel.add(info);
        }

        /** Build the Question object from the row's fields. */
        public Question buildQuestion(int id) {
            String text = questionTextField.getText().trim();
            if (text.isEmpty())
                throw new IllegalStateException("Question text is empty.");

            if (typeCombo.getSelectedIndex() == 0) {
                // MCQ
                List<String> options = new ArrayList<>();
                for (JTextField f : optionFields) {
                    String opt = f.getText().trim();
                    if (opt.isEmpty())
                        throw new IllegalStateException("All 4 options must be filled.");
                    options.add(opt);
                }
                String correct = correctField.getText().trim();
                if (correct.isEmpty())
                    throw new IllegalStateException("Correct answer is required.");
                return new MCQQuestion(id, text, 5, options, correct);
            } else {
                return new SubjectiveQuestion(id, text, 10);
            }
        }
    }
}
