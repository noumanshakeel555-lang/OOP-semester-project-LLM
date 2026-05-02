package main.gui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import main.dao.AttemptDAO;
import main.model.*;
import main.service.ClassService;
import main.service.QuizService;

public class TeacherDashboardFrame extends JFrame {

    private final Teacher      teacher;
    private final ClassService classService = new ClassService();
    private final QuizService  quizService  = new QuizService();
    private final AttemptDAO   attemptDAO   = new AttemptDAO();

    private JPanel contentArea;

    public TeacherDashboardFrame(Teacher teacher) {
        this.teacher = teacher;

        setTitle("QuizPro — Teacher");
        setSize(980, 680);
        setMinimumSize(new Dimension(760, 520));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG);
        setContentPane(root);

        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(UITheme.BG);
        main.add(buildTopBar(), BorderLayout.NORTH);

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(UITheme.BG);
        main.add(contentArea, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);

        showMyQuizzes();
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SIDEBAR
    // ─────────────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sb = new JPanel();
        sb.setBackground(UITheme.SIDEBAR_BG);
        sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));
        sb.setPreferredSize(new Dimension(220, Integer.MAX_VALUE));
        sb.setBorder(BorderFactory.createEmptyBorder(24, 0, 24, 0));

        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoRow.setOpaque(false);
        JLabel logo = new JLabel("📚  QuizPro");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logo.setForeground(Color.WHITE);
        logoRow.add(logo);

        JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        chipRow.setOpaque(false);
        chipRow.add(UITheme.badge("  Teacher  ",
            new Color(255,255,255,20), new Color(255,255,255,180)));

        sb.add(logoRow);
        sb.add(Box.createVerticalStrut(4));
        sb.add(chipRow);
        sb.add(Box.createVerticalStrut(28));

        JLabel navLbl = new JLabel("  NAVIGATION");
        navLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        navLbl.setForeground(new Color(255,255,255,60));
        navLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        sb.add(navLbl);
        sb.add(Box.createVerticalStrut(8));

        sb.add(navItem("📋  My Quizzes",       this::showMyQuizzes));
        sb.add(Box.createVerticalStrut(4));
        sb.add(navItem("🏫  My Classes",        this::showMyClasses));
        sb.add(Box.createVerticalStrut(4));
        sb.add(navItem("📊  Student Results",   this::showStudentResults));
        sb.add(Box.createVerticalStrut(4));
        sb.add(navItem("✏️  Mark Answers",       this::showMarkAnswers));
        sb.add(Box.createVerticalStrut(4));
        sb.add(navItem("➕  Create New Quiz",   this::showCreateQuiz));
        sb.add(Box.createVerticalGlue());
        sb.add(navItem("🚪  Logout", () -> { dispose(); new LoginFrame(); }));
        return sb;
    }

    private JButton navItem(String label, Runnable action) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover() || getModel().isPressed()) {
                    g2.setColor(UITheme.SIDEBAR_HOVER);
                    g2.fillRoundRect(8, 2, getWidth()-16, getHeight()-4, 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(UITheme.SIDEBAR_FONT);
        btn.setForeground(new Color(200,210,255));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(14, 24, 14, 24)));
        JLabel greet = UITheme.headingLabel("Hello, " + teacher.getUsername() + " 👋");
        JLabel sub   = UITheme.mutedLabel("Manage your classes, quizzes, and student progress.");
        sub.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(greet);
        text.add(sub);
        bar.add(text, BorderLayout.WEST);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PANEL 1 — MY QUIZZES
    // ─────────────────────────────────────────────────────────────────────
    private void showMyQuizzes() {
        contentArea.removeAll();
        JPanel panel = padded();
        panel.add(sectionHeader("My Quizzes", "Create Quiz", this::showCreateQuiz),
                  BorderLayout.NORTH);

        List<Quiz> quizzes = quizService.getAllQuizzes();
        if (quizzes.isEmpty()) {
            panel.add(emptyState("No quizzes yet — create your first one!"),
                      BorderLayout.CENTER);
        } else {
            String[] cols = {"#", "Quiz Title", "Class", "Questions"};
            Object[][] data = new Object[quizzes.size()][4];
            for (int i = 0; i < quizzes.size(); i++) {
                Quiz q = quizzes.get(i);
                String className = "—";
                if (q.getClassId() != 0) {
                    SchoolClass sc = classService.findById(q.getClassId());
                    if (sc != null) className = sc.getName();
                }
                data[i] = new Object[]{i+1, q.getTitle(), className,
                                       q.getQuestions().size() + " Qs"};
            }
            panel.add(UITheme.scrollPane(
                styledTable(data, cols, new int[]{40, 0, 180, 80})),
                BorderLayout.CENTER);
        }
        swap(panel);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PANEL 2 — MY CLASSES
    // ─────────────────────────────────────────────────────────────────────
    private void showMyClasses() {
        contentArea.removeAll();
        JPanel panel = padded();
        panel.add(sectionHeader("My Classes", "Create Class", this::createClassDialog),
                  BorderLayout.NORTH);

        List<SchoolClass> classes = classService.getClassesForTeacher(teacher);
        if (classes.isEmpty()) {
            panel.add(emptyState("No classes yet — create one to get started!"),
                      BorderLayout.CENTER);
            swap(panel);
            return;
        }

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(220);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setBackground(UITheme.BG);

        DefaultListModel<SchoolClass> listModel = new DefaultListModel<>();
        classes.forEach(listModel::addElement);
        JList<SchoolClass> classList = new JList<>(listModel);
        classList.setFont(UITheme.BODY_FONT);
        classList.setBackground(UITheme.SURFACE);
        classList.setForeground(UITheme.TEXT);
        classList.setSelectionBackground(new Color(0xE8EEFF));
        classList.setSelectionForeground(UITheme.TEXT);
        classList.setCellRenderer(new ClassListRenderer());
        classList.setFixedCellHeight(48);
        classList.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JScrollPane listScroll = UITheme.scrollPane(classList);
        listScroll.setPreferredSize(new Dimension(220, 0));

        JPanel detailHolder = new JPanel(new BorderLayout());
        detailHolder.setBackground(UITheme.BG);
        detailHolder.add(emptyState("Select a class on the left to view details."),
                         BorderLayout.CENTER);

        classList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            SchoolClass selected = classList.getSelectedValue();
            if (selected == null) return;
            detailHolder.removeAll();
            detailHolder.add(buildClassDetail(selected, this::showMyClasses),
                             BorderLayout.CENTER);
            detailHolder.revalidate();
            detailHolder.repaint();
        });

        split.setLeftComponent(listScroll);
        split.setRightComponent(detailHolder);
        panel.add(split, BorderLayout.CENTER);
        swap(panel);
    }

    private JPanel buildClassDetail(SchoolClass sc, Runnable refresh) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));

        JPanel hdr = new JPanel();
        hdr.setOpaque(false);
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        hdr.add(UITheme.headingLabel(sc.getName()));
        hdr.add(Box.createVerticalStrut(4));
        hdr.add(UITheme.mutedLabel(
            sc.getStudentIds().size() + " student" + (sc.getStudentIds().size()==1?"":"s") +
            "  ·  " + sc.getQuizIds().size() + " quiz" +
            (sc.getQuizIds().size()==1?"":"zes") + " assigned"));
        panel.add(hdr, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.BODY_FONT);
        tabs.setBackground(UITheme.BG);
        tabs.addTab("  Students  ", buildStudentsTab(sc, refresh));
        tabs.addTab("  Quizzes   ", buildQuizzesTab(sc, refresh));
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStudentsTab(SchoolClass sc, Runnable refresh) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(UITheme.BG);
        p.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        JButton addBtn = UITheme.primaryBtn("+ Enrol Student");
        addBtn.setPreferredSize(new Dimension(160, 36));
        addBtn.addActionListener(e -> {
            String uname = JOptionPane.showInputDialog(this,
                "Enter the student's username:", "Enrol Student",
                JOptionPane.PLAIN_MESSAGE);
            if (uname == null || uname.isBlank()) return;
            try {
                classService.enrollStudentByUsername(sc, uname.trim());
                JOptionPane.showMessageDialog(this,
                    "✅  " + uname + " enrolled in " + sc.getName());
                refresh.run();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "⚠  " + ex.getMessage(),
                    "Error", JOptionPane.WARNING_MESSAGE);
            }
        });
        toolbar.add(addBtn);
        p.add(toolbar, BorderLayout.NORTH);

        List<Student> students = classService.getStudentsInClass(sc);
        if (students.isEmpty()) {
            p.add(emptyState("No students enrolled yet."), BorderLayout.CENTER);
        } else {
            String[] cols = {"#", "Username", "ID"};
            Object[][] data = new Object[students.size()][3];
            for (int i = 0; i < students.size(); i++) {
                Student s = students.get(i);
                data[i] = new Object[]{i+1, s.getUsername(), s.getId()};
            }
            p.add(UITheme.scrollPane(styledTable(data, cols, new int[]{40, 0, 60})),
                  BorderLayout.CENTER);
        }
        return p;
    }

    private JPanel buildQuizzesTab(SchoolClass sc, Runnable refresh) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(UITheme.BG);
        p.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        JButton assignBtn = UITheme.primaryBtn("+ Assign Quiz");
        assignBtn.setPreferredSize(new Dimension(150, 36));
        assignBtn.addActionListener(e -> {
            List<Quiz> available = classService.getAllQuizzes().stream()
                .filter(q -> !sc.hasQuiz(q.getId())).toList();
            if (available.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No more quizzes to assign. Create one first!",
                    "No Quizzes", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Quiz chosen = (Quiz) JOptionPane.showInputDialog(this,
                "Select a quiz to assign to " + sc.getName() + ":",
                "Assign Quiz", JOptionPane.PLAIN_MESSAGE,
                null, available.toArray(), available.get(0));
            if (chosen == null) return;
            classService.assignQuizToClass(sc, chosen);
            JOptionPane.showMessageDialog(this,
                "✅  \"" + chosen.getTitle() + "\" assigned to " + sc.getName());
            refresh.run();
        });
        toolbar.add(assignBtn);
        p.add(toolbar, BorderLayout.NORTH);

        List<Quiz> assigned = classService.getQuizzesForClass(sc);
        if (assigned.isEmpty()) {
            p.add(emptyState("No quizzes assigned yet."), BorderLayout.CENTER);
        } else {
            String[] cols = {"#", "Quiz Title", "Questions", "Total Marks"};
            Object[][] data = new Object[assigned.size()][4];
            for (int i = 0; i < assigned.size(); i++) {
                Quiz q = assigned.get(i);
                int marks = q.getQuestions().stream().mapToInt(Question::getMarks).sum();
                data[i] = new Object[]{i+1, q.getTitle(),
                                       q.getQuestions().size() + " Qs", marks};
            }
            p.add(UITheme.scrollPane(
                styledTable(data, cols, new int[]{40, 0, 90, 90})),
                BorderLayout.CENTER);
        }
        return p;
    }

    private void createClassDialog() {
        String name = JOptionPane.showInputDialog(this,
            "Enter class name (e.g. \"Grade 10 — Section A\"):",
            "Create Class", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        classService.createClass(name.trim(), teacher);
        JOptionPane.showMessageDialog(this,
            "✅  Class \"" + name + "\" created!", "Success",
            JOptionPane.INFORMATION_MESSAGE);
        showMyClasses();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PANEL 3 — STUDENT RESULTS
    // ─────────────────────────────────────────────────────────────────────
    private void showStudentResults() {
        contentArea.removeAll();
        JPanel panel = padded();
        panel.add(sectionHeader("Student Results", null, null), BorderLayout.NORTH);

        List<Integer> myStudentIds = getMyStudentIds();
        List<AttemptRecord> records = attemptDAO.findByStudentIds(myStudentIds);

        if (records.isEmpty()) {
            panel.add(emptyState("No students have attempted any quizzes yet."),
                      BorderLayout.CENTER);
            swap(panel);
            return;
        }

        records.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        String[] cols = {"Student", "Quiz", "Score", "%", "Pending", "Violations"};
        Object[][] data = new Object[records.size()][6];
        for (int i = 0; i < records.size(); i++) {
            AttemptRecord r = records.get(i);
            Result res = r.getResult();
            data[i][0] = r.getStudentName();
            data[i][1] = r.getQuizTitle();
            data[i][2] = res.getScore() + " / " + res.getTotalMarks();
            data[i][3] = String.format("%.1f%%", res.getPercentage());
            data[i][4] = res.hasPending() ? "⏳ " + res.getPendingMarks() + " marks" : "✅ Done";
            data[i][5] = r.getViolations() > 0 ? "⚠ " + r.getViolations() : "—";
        }

        JTable table = new JTable(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        styleTable(table, new int[]{140, 160, 80, 70, 110, 80});
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) openAnswerReview(records.get(row), false);
            }
        });

        panel.add(UITheme.scrollPane(table), BorderLayout.CENTER);
        JLabel hint = UITheme.mutedLabel(
            "Click any row to view answers. Use ✏️ Mark Answers to grade written responses.");
        hint.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        panel.add(hint, BorderLayout.SOUTH);
        swap(panel);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PANEL 4 — MARK ANSWERS  (subjective grading)
    // ─────────────────────────────────────────────────────────────────────
    private void showMarkAnswers() {
        contentArea.removeAll();
        JPanel panel = padded();
        panel.add(sectionHeader("Mark Written Answers", null, null), BorderLayout.NORTH);

        List<Integer> myStudentIds = getMyStudentIds();
        List<AttemptRecord> pending = attemptDAO.findPendingSubjective(myStudentIds);

        if (pending.isEmpty()) {
            JPanel empty = emptyState("✅  All written answers have been marked!");
            panel.add(empty, BorderLayout.CENTER);
            swap(panel);
            return;
        }

        String[] cols = {"Student", "Quiz", "MCQ Score", "Pending Marks", "Mark Now"};
        Object[][] data = new Object[pending.size()][5];
        for (int i = 0; i < pending.size(); i++) {
            AttemptRecord r = pending.get(i);
            Result res = r.getResult();
            data[i][0] = r.getStudentName();
            data[i][1] = r.getQuizTitle();
            data[i][2] = res.getScore() + " / " + (res.getTotalMarks() - res.getPendingMarks());
            data[i][3] = "⏳ " + res.getPendingMarks() + " marks";
            data[i][4] = "Grade →";
        }

        JTable table = new JTable(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        styleTable(table, new int[]{140, 160, 100, 110, 80});
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) openAnswerReview(pending.get(row), true);
            }
        });

        panel.add(UITheme.scrollPane(table), BorderLayout.CENTER);
        JLabel hint = UITheme.mutedLabel(
            "Click any row to open the grading dialog for that student's attempt.");
        hint.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        panel.add(hint, BorderLayout.SOUTH);
        swap(panel);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ANSWER REVIEW DIALOG  (view only OR grade mode)
    // ─────────────────────────────────────────────────────────────────────
    /**
     * @param gradeMode true = show mark spinners for ungraded subjective answers
     *                  false = read-only view
     */
    private void openAnswerReview(AttemptRecord record, boolean gradeMode) {
        Quiz quiz = quizService.findById(record.getQuizId());

        JDialog dialog = new JDialog(this,
            (gradeMode ? "Grade — " : "Answers — ")
            + record.getStudentName() + " · " + record.getQuizTitle(), true);
        dialog.setSize(660, 600);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        dialog.setContentPane(root);

        // ── Header ──
        JPanel hdr = new JPanel();
        hdr.setOpaque(false);
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        JLabel title = UITheme.headingLabel(record.getStudentName() + "'s Answers");

        // Load real awarded marks from DB
        Map<Integer, Integer> awardedMarks = attemptDAO.loadAwardedMarks(record.getId());

        JLabel sub = UITheme.mutedLabel(record.getQuizTitle()
            + "  ·  Score: " + record.getResult().getScore()
            + " / " + record.getResult().getTotalMarks()
            + (record.getResult().hasPending()
               ? "  (⏳ " + record.getResult().getPendingMarks() + " marks pending)"
               : "  ✅"));
        hdr.add(title);
        hdr.add(Box.createVerticalStrut(4));
        hdr.add(sub);
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        root.add(hdr, BorderLayout.NORTH);

        // ── Answer rows ──
        JPanel listPanel = new JPanel();
        listPanel.setBackground(UITheme.BG);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        Map<Integer, String> answers = record.getAnswers();

        // Map to hold spinners for grading (questionId -> JSpinner)
        Map<Integer, JSpinner> spinners = new java.util.LinkedHashMap<>();

        if (quiz == null || quiz.getQuestions().isEmpty()) {
            listPanel.add(UITheme.mutedLabel(
                "(Question details not available — legacy record)"));
        } else {
            int idx = 1;
            for (Question q : quiz.getQuestions()) {
                String studentAnswer = answers.getOrDefault(q.getId(), "(no answer given)");
                boolean isSubjective  = q instanceof SubjectiveQuestion;

                // Get the real awarded marks from DB
                Integer awarded = awardedMarks.get(q.getId());
                // -1 = pending subjective, null = not found (shouldn't happen)
                int displayScore;
                boolean isPending;
                if (isSubjective) {
                    isPending    = (awarded == null || awarded == -1);
                    displayScore = isPending ? 0 : awarded;
                } else {
                    // MCQ: manual_score holds the auto-evaluated score
                    isPending    = false;
                    displayScore = (awarded != null && awarded >= 0)
                                   ? awarded : q.evaluate(studentAnswer);
                }

                // Card for this question
                JPanel card = UITheme.card();
                card.setLayout(new BorderLayout(12, 0));
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, isSubjective ? 160 : 120));
                card.setAlignmentX(Component.LEFT_ALIGNMENT);

                // Coloured left indicator
                final boolean pending = isPending;
                final int     scored  = displayScore;
                JPanel indicator = new JPanel() {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                            RenderingHints.VALUE_ANTIALIAS_ON);
                        Color c = pending            ? UITheme.WARNING
                                : scored >= q.getMarks() ? UITheme.SUCCESS
                                : scored > 0         ? UITheme.WARNING
                                :                     UITheme.DANGER;
                        g2.setColor(c);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        g2.dispose();
                    }
                };
                indicator.setOpaque(false);
                indicator.setPreferredSize(new Dimension(6, 0));

                // Text area
                JPanel text = new JPanel();
                text.setOpaque(false);
                text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

                JLabel qLbl = new JLabel("Q" + idx + ".  " + q.getQuestionText());
                qLbl.setFont(UITheme.SUBHEAD_FONT);
                qLbl.setForeground(UITheme.TEXT);

                JLabel aLbl = new JLabel("<html><i>Answer:</i>  "
                    + studentAnswer.replace("<", "&lt;") + "</html>");
                aLbl.setFont(UITheme.BODY_FONT);
                aLbl.setForeground(UITheme.TEXT_MUTED);

                text.add(qLbl);
                text.add(Box.createVerticalStrut(4));
                text.add(aLbl);
                text.add(Box.createVerticalStrut(6));

                if (isSubjective && gradeMode) {
                    // ── Grading row with spinner ──
                    JPanel gradeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                    gradeRow.setOpaque(false);

                    if (isPending) {
                        JLabel gradeLbl = UITheme.colorLabel(
                            "⏳  Award marks (0 – " + q.getMarks() + "):",
                            UITheme.WARNING);
                        gradeLbl.setFont(UITheme.SMALL_FONT);

                        JSpinner spinner = new JSpinner(
                            new SpinnerNumberModel(0, 0, q.getMarks(), 1));
                        spinner.setFont(UITheme.BODY_FONT);
                        spinner.setPreferredSize(new Dimension(70, 30));
                        spinners.put(q.getId(), spinner);

                        gradeRow.add(gradeLbl);
                        gradeRow.add(spinner);
                    } else {
                        JLabel gradedLbl = UITheme.colorLabel(
                            "✅  Marked: " + displayScore + " / " + q.getMarks() + " marks",
                            UITheme.SUCCESS);
                        gradedLbl.setFont(UITheme.SMALL_FONT);
                        gradeRow.add(gradedLbl);
                    }
                    text.add(gradeRow);

                } else {
                    // ── View-only marks label ──
                    String marksText = isSubjective && isPending
                        ? "⏳  Pending teacher review"
                        : displayScore + " / " + q.getMarks() + " marks";
                    Color marksColor = isSubjective && isPending ? UITheme.WARNING
                                     : displayScore >= q.getMarks() ? UITheme.SUCCESS
                                     : UITheme.DANGER;
                    JLabel sLbl = new JLabel(marksText);
                    sLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    sLbl.setForeground(marksColor);
                    text.add(sLbl);
                }

                card.add(indicator, BorderLayout.WEST);
                card.add(text,      BorderLayout.CENTER);
                listPanel.add(card);
                listPanel.add(Box.createVerticalStrut(10));
                idx++;
            }
        }

        root.add(UITheme.scrollPane(listPanel), BorderLayout.CENTER);

        // ── Footer buttons ──
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        if (gradeMode && !spinners.isEmpty()) {
            JButton saveBtn = UITheme.primaryBtn("💾  Save Marks");
            saveBtn.addActionListener(e -> {
                boolean anySaved = false;
                for (Map.Entry<Integer, JSpinner> entry : spinners.entrySet()) {
                    int questionId   = entry.getKey();
                    int marksAwarded = (int) entry.getValue().getValue();
                    try {
                        attemptDAO.awardSubjectiveMarks(
                            record.getId(), questionId, marksAwarded);
                        anySaved = true;
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog,
                            "Failed to save marks for Q" + questionId
                            + ":\n" + ex.getMessage(),
                            "Save Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                if (anySaved) {
                    JOptionPane.showMessageDialog(dialog,
                        "✅  Marks saved successfully!\n"
                        + "The student's score has been updated.",
                        "Saved", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    // Refresh the Mark Answers panel
                    showMarkAnswers();
                }
            });
            footer.add(saveBtn);
        }

        JButton closeBtn = UITheme.secondaryBtn("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  PANEL 5 — CREATE QUIZ
    // ─────────────────────────────────────────────────────────────────────
    private void showCreateQuiz() {
        contentArea.removeAll();
        JPanel panel = padded();
        panel.add(UITheme.headingLabel("Create a New Quiz"), BorderLayout.NORTH);
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UITheme.BG);
        center.add(new QuizCreatePanel(teacher, classService, quizService,
                                       this::showMyQuizzes));
        panel.add(center, BorderLayout.CENTER);
        swap(panel);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  INNER — QuizCreatePanel
    // ─────────────────────────────────────────────────────────────────────
    static class QuizCreatePanel extends JPanel {

        private final Teacher      teacher;
        private final ClassService classService;
        private final QuizService  quizService;
        private final Runnable     onDone;

        QuizCreatePanel(Teacher t, ClassService cs, QuizService qs, Runnable onDone) {
            this.teacher = t; this.classService = cs;
            this.quizService = qs; this.onDone = onDone;
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            buildUI();
        }

        private void buildUI() {
            JPanel card = UITheme.card();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setPreferredSize(new Dimension(420, 340));

            JLabel icon = new JLabel("📝", JLabel.CENTER);
            icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
            icon.setAlignmentX(CENTER_ALIGNMENT);
            JLabel title = UITheme.headingLabel("Quiz Wizard");
            title.setAlignmentX(CENTER_ALIGNMENT);
            JLabel sub = UITheme.mutedLabel("Build a quiz and assign it to a class.");
            sub.setAlignmentX(CENTER_ALIGNMENT);
            JButton startBtn = UITheme.primaryBtn("🚀  Launch Quiz Builder");
            startBtn.setPreferredSize(new Dimension(240, 44));
            startBtn.setMaximumSize(new Dimension(240, 44));
            startBtn.setAlignmentX(CENTER_ALIGNMENT);
            startBtn.addActionListener(e -> runWizard());

            card.add(icon);
            card.add(Box.createVerticalStrut(10));
            card.add(title);
            card.add(Box.createVerticalStrut(6));
            card.add(sub);
            card.add(Box.createVerticalStrut(24));
            card.add(startBtn);
            add(card);
        }

        private void runWizard() {
            Window parent = SwingUtilities.getWindowAncestor(this);

            String quizTitle = JOptionPane.showInputDialog(parent,
                "Enter Quiz Title:", "Step 1 — Title", JOptionPane.PLAIN_MESSAGE);
            if (quizTitle == null || quizTitle.isBlank()) return;

            String countStr = JOptionPane.showInputDialog(parent,
                "How many questions?", "Step 2 — Count", JOptionPane.PLAIN_MESSAGE);
            if (countStr == null) return;
            int count;
            try { count = Integer.parseInt(countStr.trim()); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent, "Please enter a valid number.");
                return;
            }
            if (count < 1) return;

            // Pick a class
            List<SchoolClass> myClasses = classService.getClassesForTeacher(teacher);
            SchoolClass targetClass = null;
            if (!myClasses.isEmpty()) {
                Object[] options = new Object[myClasses.size() + 1];
                options[0] = "— No class (unrestricted) —";
                for (int i = 0; i < myClasses.size(); i++) options[i+1] = myClasses.get(i);
                Object chosen = JOptionPane.showInputDialog(parent,
                    "Assign this quiz to a class (optional):",
                    "Step 3 — Class", JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]);
                if (chosen == null) return;
                if (chosen instanceof SchoolClass sc) targetClass = sc;
            }

            Quiz quiz = new Quiz(0, quizTitle.trim(),
                targetClass != null ? targetClass.getId() : 0);

            for (int i = 0; i < count; i++) {
                String qText = JOptionPane.showInputDialog(parent,
                    "Question " + (i+1) + " of " + count + ":",
                    "Enter Question", JOptionPane.PLAIN_MESSAGE);
                if (qText == null || qText.isBlank()) { i--; continue; }

                String[] types = {"MCQ (Multiple Choice)", "Subjective (Written)"};
                int typeChoice = JOptionPane.showOptionDialog(parent,
                    "Select type for Q" + (i+1), "Question Type",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, types, types[0]);

                if (typeChoice == 0) {
                    java.util.List<String> opts = new java.util.ArrayList<>();
                    for (int j = 0; j < 4; j++) {
                        String opt = JOptionPane.showInputDialog(parent,
                            "Option " + (j+1) + " for Q" + (i+1) + ":",
                            "MCQ Options", JOptionPane.PLAIN_MESSAGE);
                        opts.add(opt == null ? "Option " + (j+1) : opt.trim());
                    }
                    String correct = JOptionPane.showInputDialog(parent,
                        "Correct answer (exact option text):",
                        "Correct Answer", JOptionPane.PLAIN_MESSAGE);
                    quiz.addQuestion(new MCQQuestion(i, qText.trim(), 5, opts,
                        correct == null ? opts.get(0) : correct.trim()));
                } else {
                    quiz.addQuestion(new SubjectiveQuestion(i, qText.trim(), 10));
                }
            }

            quizService.createQuiz(quiz);
            if (targetClass != null) {
                classService.assignQuizToClass(targetClass, quiz);
            }

            JOptionPane.showMessageDialog(parent,
                "✅  Quiz \"" + quizTitle + "\" created with " + count + " questions!"
                + (targetClass != null ? "\nAssigned to: " + targetClass.getName() : ""),
                "Quiz Created", JOptionPane.INFORMATION_MESSAGE);
            onDone.run();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /** All student IDs from classes this teacher owns. */
    private List<Integer> getMyStudentIds() {
        return classService.getClassesForTeacher(teacher).stream()
            .flatMap(c -> c.getStudentIds().stream())
            .distinct().toList();
    }

    private void swap(JPanel panel) {
        contentArea.add(panel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    private JPanel padded() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UITheme.BG);
        p.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        return p;
    }

    private JPanel sectionHeader(String title, String btnLabel, Runnable btnAction) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        row.add(UITheme.headingLabel(title), BorderLayout.WEST);
        if (btnLabel != null && btnAction != null) {
            JButton btn = UITheme.primaryBtn(btnLabel);
            btn.setPreferredSize(new Dimension(140, 36));
            btn.addActionListener(e -> btnAction.run());
            row.add(btn, BorderLayout.EAST);
        }
        return row;
    }

    private JPanel emptyState(String msg) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UITheme.BG);
        JLabel l = UITheme.mutedLabel(msg);
        l.setFont(UITheme.BODY_FONT);
        p.add(l);
        return p;
    }

    private JTable styledTable(Object[][] data, String[] cols, int[] colWidths) {
        JTable t = new JTable(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        styleTable(t, colWidths);
        return t;
    }

    private void styleTable(JTable table, int[] colWidths) {
        table.setFont(UITheme.BODY_FONT);
        table.setForeground(UITheme.TEXT);
        table.setBackground(UITheme.SURFACE);
        table.setRowHeight(44);
        table.setShowHorizontalLines(true);
        table.setGridColor(UITheme.BORDER);
        table.setSelectionBackground(new Color(0xE8EEFF));
        table.setSelectionForeground(UITheme.TEXT);
        table.getTableHeader().setFont(UITheme.SUBHEAD_FONT);
        table.getTableHeader().setBackground(UITheme.BG);
        table.getTableHeader().setForeground(UITheme.TEXT_MUTED);
        table.getTableHeader().setBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER));
        table.setBorder(BorderFactory.createEmptyBorder());
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        for (int i = 0; i < colWidths.length; i++) {
            if (colWidths[i] > 0) {
                table.getColumnModel().getColumn(i).setMaxWidth(colWidths[i]);
                table.getColumnModel().getColumn(i).setPreferredWidth(colWidths[i]);
            }
        }
    }

    static class ClassListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, focused);
            if (value instanceof SchoolClass sc) {
                setText("<html><b>" + sc.getName() + "</b><br>"
                    + "<font color='gray' size='-2'>"
                    + sc.getStudentIds().size() + " students · "
                    + sc.getQuizIds().size() + " quizzes</font></html>");
            }
            setBackground(selected ? new Color(0xE8EEFF) : UITheme.SURFACE);
            setForeground(UITheme.TEXT);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
            return this;
        }
    }
}
