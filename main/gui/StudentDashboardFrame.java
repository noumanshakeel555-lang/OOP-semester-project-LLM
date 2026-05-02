package main.gui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import main.dao.AttemptDAO;
import main.model.*;
import main.service.ClassService;

/**
 * Student home screen — two tabs:
 *
 *  📋 My Quizzes  — shows class quizzes; already-attempted ones are locked
 *  📊 My Results  — full history table (score, %, pending marks, date)
 */
public class StudentDashboardFrame extends JFrame {

    private final Student      student;
    private final ClassService classService  = new ClassService();
    private final AttemptDAO   attemptDAO    = new AttemptDAO();

    private final SchoolClass  enrolledClass;
    private final List<Quiz>   quizzes;
    /** IDs of quizzes this student has already attempted — loaded once on open. */
    private final Set<Integer> attemptedIds;

    public StudentDashboardFrame(Student student) {
        this.student       = student;
        this.enrolledClass = classService.getClassForStudent(student);
        this.quizzes       = classService.getQuizzesForStudent(student);
        this.attemptedIds  = attemptDAO.attemptedQuizIds(student.getId());

        setTitle("QuizPro — Student");
        setSize(860, 640);
        setMinimumSize(new Dimension(680, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG);
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildTabbedBody(), BorderLayout.CENTER);

        setVisible(true);
    }

    // ── Header ────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.SIDEBAR_BG);
        header.setBorder(BorderFactory.createEmptyBorder(16, 28, 16, 28));

        // Left: logo + class badge
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel logo = new JLabel("📚");
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        JLabel appName = new JLabel("QuizPro");
        appName.setFont(UITheme.SUBHEAD_FONT);
        appName.setForeground(Color.WHITE);
        left.add(logo);
        left.add(appName);

        if (enrolledClass != null) {
            left.add(Box.createHorizontalStrut(8));
            left.add(UITheme.badge(
                "🏫  " + enrolledClass.getName(),
                new Color(61, 111, 255, 60),
                new Color(180, 200, 255)));
        }

        // Right: user chip + logout
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(UITheme.badge(
            "🎓  " + student.getUsername(),
            new Color(255, 255, 255, 30), Color.WHITE));
        JButton logoutBtn = UITheme.roundBtn("Logout", UITheme.DANGER, Color.WHITE);
        logoutBtn.setPreferredSize(new Dimension(90, 32));
        logoutBtn.addActionListener(e -> { dispose(); new LoginFrame(); });
        right.add(logoutBtn);

        header.add(left,  BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ── Tabbed body ───────────────────────────────────────────────────────
    private JTabbedPane buildTabbedBody() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.SUBHEAD_FONT);
        tabs.setBackground(UITheme.BG);
        tabs.setForeground(UITheme.TEXT);
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Tab 1 — My Quizzes
        tabs.addTab("  📋  My Quizzes  ", buildQuizzesTab());

        // Tab 2 — My Results
        tabs.addTab("  📊  My Results  ", buildResultsTab());

        return tabs;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TAB 1 — MY QUIZZES
    // ═══════════════════════════════════════════════════════════════════════
    private JPanel buildQuizzesTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Section heading row
        JPanel sectionRow = new JPanel(new BorderLayout());
        sectionRow.setOpaque(false);
        sectionRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        String headText = enrolledClass != null
            ? enrolledClass.getName() + " — Quizzes"
            : "Available Quizzes";
        sectionRow.add(UITheme.headingLabel(headText), BorderLayout.WEST);

        long pending = quizzes.stream()
            .filter(q -> !attemptedIds.contains(q.getId())).count();
        sectionRow.add(UITheme.mutedLabel(
            pending + " remaining · " + attemptedIds.size() + " completed"),
            BorderLayout.EAST);
        panel.add(sectionRow, BorderLayout.NORTH);

        // Content
        if (enrolledClass == null && quizzes.isEmpty()) {
            panel.add(buildNotEnrolledPanel(), BorderLayout.CENTER);
        } else if (quizzes.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setBackground(UITheme.BG);
            empty.add(UITheme.mutedLabel("No quizzes assigned to your class yet."));
            panel.add(empty, BorderLayout.CENTER);
        } else {
            panel.add(UITheme.scrollPane(buildQuizGrid()), BorderLayout.CENTER);
        }

        return panel;
    }

    // ── Not-enrolled placeholder ──────────────────────────────────────────
    private JPanel buildNotEnrolledPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(UITheme.BG);

        JPanel card = UITheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(420, 230));

        JLabel icon = new JLabel("🏫", JLabel.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = UITheme.headingLabel("Not Enrolled in a Class");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel(
            "<html><div style='text-align:center;font-family:Segoe UI;font-size:13px;"
            + "color:#8A92A8;'>Ask your teacher to enrol you in a class.<br>"
            + "Your quizzes will appear here once you are added.</div></html>",
            JLabel.CENTER);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(icon);
        card.add(Box.createVerticalStrut(12));
        card.add(title);
        card.add(Box.createVerticalStrut(10));
        card.add(sub);
        outer.add(card);
        return outer;
    }

    // ── Quiz card grid ────────────────────────────────────────────────────
    private JPanel buildQuizGrid() {
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 16, 16));
        grid.setBackground(UITheme.BG);
        for (Quiz quiz : quizzes) {
            boolean done = attemptedIds.contains(quiz.getId());
            grid.add(buildQuizCard(quiz, done));
        }
        return grid;
    }

    /**
     * @param done  true = student already attempted this quiz → show locked card
     */
    private JPanel buildQuizCard(Quiz quiz, boolean done) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 5; i >= 1; i--) {
                    g2.setColor(new Color(0, 0, 60, done ? 3 : 6));
                    g2.fillRoundRect(i, i, getWidth()-i, getHeight()-i, 18, 18);
                }
                // Slightly grey background for completed cards
                g2.setColor(done ? new Color(0xF8F9FC) : UITheme.SURFACE);
                g2.fillRoundRect(0, 0, getWidth()-5, getHeight()-5, 18, 18);
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(240, 200));
        wrapper.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Top accent bar — green for done, gradient for available
        JPanel accent = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                if (done) {
                    g2.setColor(UITheme.SUCCESS);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                } else {
                    g2.setPaint(new GradientPaint(
                        0, 0, UITheme.PRIMARY, getWidth(), 0, UITheme.ACCENT));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }
                g2.dispose();
            }
        };
        accent.setOpaque(false);
        accent.setPreferredSize(new Dimension(Integer.MAX_VALUE, 5));

        JLabel titleLbl = new JLabel(quiz.getTitle());
        titleLbl.setFont(UITheme.SUBHEAD_FONT);
        titleLbl.setForeground(done ? UITheme.TEXT_MUTED : UITheme.TEXT);

        int qCount    = quiz.getQuestions().size();
        int totalMark = quiz.getQuestions().stream().mapToInt(Question::getMarks).sum();
        JLabel qCountLbl = UITheme.mutedLabel("📝  " + qCount + " question" + (qCount==1?"":"s"));
        JLabel marksLbl  = UITheme.mutedLabel("⭐  " + totalMark + " marks total");

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(accent);
        content.add(Box.createVerticalStrut(12));
        content.add(titleLbl);
        content.add(Box.createVerticalStrut(4));
        content.add(qCountLbl);
        content.add(Box.createVerticalStrut(2));
        content.add(marksLbl);
        content.add(Box.createVerticalGlue());
        content.add(Box.createVerticalStrut(12));

        if (done) {
            // ── Completed badge — no button ──
            JPanel doneBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            doneBadge.setOpaque(false);
            doneBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel doneLabel = new JLabel("✅  Completed — see My Results");
            doneLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            doneLabel.setForeground(UITheme.SUCCESS);
            doneBadge.add(doneLabel);
            content.add(doneBadge);
        } else {
            // ── Start button ──
            JButton startBtn = UITheme.primaryBtn("Start Quiz →");
            startBtn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 36));
            startBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            startBtn.addActionListener(e -> { dispose(); new QuizAttemptFrame(quiz, student); });
            content.add(startBtn);
        }

        wrapper.add(content);

        // Hover only for available quizzes
        if (!done) {
            wrapper.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    wrapper.setBorder(BorderFactory.createCompoundBorder(
                        new UITheme.RoundBorder(UITheme.PRIMARY, 2, 18),
                        BorderFactory.createEmptyBorder(16, 18, 16, 18)));
                    wrapper.repaint();
                }
                @Override public void mouseExited(MouseEvent e) {
                    wrapper.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
                    wrapper.repaint();
                }
            });
        }

        return wrapper;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TAB 2 — MY RESULTS
    // ═══════════════════════════════════════════════════════════════════════
    private JPanel buildResultsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Section heading
        JPanel sectionRow = new JPanel(new BorderLayout());
        sectionRow.setOpaque(false);
        sectionRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
        sectionRow.add(UITheme.headingLabel("My Results"), BorderLayout.WEST);
        panel.add(sectionRow, BorderLayout.NORTH);

        // Load all attempts for this student
        List<AttemptRecord> records = attemptDAO.findByStudentId(student.getId());

        if (records.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setBackground(UITheme.BG);
            JPanel card = UITheme.card();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setPreferredSize(new Dimension(420, 200));

            JLabel icon = new JLabel("📊", JLabel.CENTER);
            icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel msg = UITheme.headingLabel("No attempts yet");
            msg.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel sub = UITheme.mutedLabel("Complete a quiz to see your scores here.");
            sub.setAlignmentX(Component.CENTER_ALIGNMENT);

            card.add(icon);
            card.add(Box.createVerticalStrut(10));
            card.add(msg);
            card.add(Box.createVerticalStrut(6));
            card.add(sub);
            empty.add(card);
            panel.add(empty, BorderLayout.CENTER);
            return panel;
        }

        // Build summary stats row
        panel.add(buildStatsSummary(records), BorderLayout.CENTER);
        return panel;
    }

    /** Stats cards + results table combined into one scrollable panel. */
    private JPanel buildStatsSummary(List<AttemptRecord> records) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 20));
        wrapper.setBackground(UITheme.BG);

        // ── Top stats cards ──────────────────────────────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 14, 0));
        statsRow.setOpaque(false);

        // Average score
        double avgPct = records.stream()
            .mapToDouble(r -> r.getResult().getPercentage())
            .average().orElse(0);

        // Best score
        double bestPct = records.stream()
            .mapToDouble(r -> r.getResult().getPercentage())
            .max().orElse(0);

        // Pending count
        long pendingCount = records.stream()
            .filter(r -> r.getResult().hasPending()).count();

        statsRow.add(buildStatCard("📈  Average", String.format("%.1f%%", avgPct),
            UITheme.PRIMARY));
        statsRow.add(buildStatCard("🏆  Best Score", String.format("%.1f%%", bestPct),
            UITheme.SUCCESS));
        statsRow.add(buildStatCard("⏳  Awaiting Marks",
            pendingCount == 0 ? "All done ✅" : pendingCount + " quiz" + (pendingCount==1?"":"zes"),
            pendingCount == 0 ? UITheme.SUCCESS : UITheme.WARNING));

        wrapper.add(statsRow, BorderLayout.NORTH);

        // ── Results table ────────────────────────────────────────────────
        String[] cols = {"Quiz", "Score", "Percentage", "Status", "Violations", "Date"};
        Object[][] data = new Object[records.size()][6];
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy  HH:mm");

        for (int i = 0; i < records.size(); i++) {
            AttemptRecord r    = records.get(i);
            Result        res  = r.getResult();
            data[i][0] = r.getQuizTitle();
            data[i][1] = res.getScore() + " / " + res.getTotalMarks();
            data[i][2] = String.format("%.1f%%", res.getPercentage());
            data[i][3] = res.hasPending()
                ? "⏳ " + res.getPendingMarks() + " marks pending"
                : getGradeLabel(res.getPercentage());
            data[i][4] = r.getViolations() > 0 ? "⚠ " + r.getViolations() : "—";
            data[i][5] = sdf.format(new Date(r.getTimestamp()));
        }

        JTable table = new JTable(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        // Style the table
        table.setFont(UITheme.BODY_FONT);
        table.setForeground(UITheme.TEXT);
        table.setBackground(UITheme.SURFACE);
        table.setRowHeight(48);
        table.setShowHorizontalLines(true);
        table.setGridColor(UITheme.BORDER);
        table.setSelectionBackground(new Color(0xE8EEFF));
        table.setSelectionForeground(UITheme.TEXT);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBorder(BorderFactory.createEmptyBorder());

        table.getTableHeader().setFont(UITheme.SUBHEAD_FONT);
        table.getTableHeader().setBackground(UITheme.BG);
        table.getTableHeader().setForeground(UITheme.TEXT_MUTED);
        table.getTableHeader().setBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER));

        // Column widths
        int[] widths = {0, 90, 90, 200, 80, 160};
        for (int i = 0; i < widths.length; i++) {
            if (widths[i] > 0) {
                table.getColumnModel().getColumn(i).setMaxWidth(widths[i]);
                table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            }
        }

        // Colour the Percentage column cells by grade
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                if (!sel) {
                    String pctStr = val.toString().replace("%", "").trim();
                    try {
                        double pct = Double.parseDouble(pctStr);
                        setForeground(pct >= 70 ? UITheme.SUCCESS
                                    : pct >= 50 ? UITheme.WARNING
                                    :             UITheme.DANGER);
                    } catch (NumberFormatException ignored) {
                        setForeground(UITheme.TEXT);
                    }
                }
                return this;
            }
        });

        wrapper.add(UITheme.scrollPane(table), BorderLayout.CENTER);

        // Hint
        JLabel hint = UITheme.mutedLabel(
            "Pending marks will update once your teacher reviews your written answers.");
        hint.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        wrapper.add(hint, BorderLayout.SOUTH);

        return wrapper;
    }

    /** A single coloured stat card. */
    private JPanel buildStatCard(String label, String value, Color accent) {
        JPanel card = UITheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(0, 90));

        // Coloured left accent stripe
        JPanel stripe = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(accent);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        stripe.setOpaque(false);
        stripe.setPreferredSize(new Dimension(4, 0));
        stripe.setMaximumSize(new Dimension(4, Integer.MAX_VALUE));
        stripe.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = UITheme.mutedLabel(label);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 22));
        val.setForeground(accent);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lbl);
        card.add(Box.createVerticalStrut(6));
        card.add(val);

        // Wrap in BorderLayout panel to show left stripe
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(stripe, BorderLayout.WEST);
        wrapper.add(card,   BorderLayout.CENTER);
        return wrapper;
    }

    /** Returns a grade label string based on percentage. */
    private String getGradeLabel(double pct) {
        if (pct >= 90) return "🏆  Excellent";
        if (pct >= 70) return "👍  Good";
        if (pct >= 50) return "✔  Passing";
        return "💪  Needs work";
    }

    // ── WrapLayout (for quiz card grid) ───────────────────────────────────
    static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override public Dimension preferredLayoutSize(Container t) { return layout(t, true); }
        @Override public Dimension minimumLayoutSize(Container t)   { return layout(t, false); }

        private Dimension layout(Container target, boolean pref) {
            synchronized (target.getTreeLock()) {
                int tw = target.getSize().width;
                if (tw == 0) tw = Integer.MAX_VALUE;
                int hg = getHgap(), vg = getVgap();
                Insets ins = target.getInsets();
                int maxW = tw - (ins.left + ins.right + hg * 2);
                int w = 0, h = 0, rowH = 0, rowW = 0;
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = pref ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowW + d.width > maxW && rowW > 0) {
                        w = Math.max(w, rowW); h += rowH + vg; rowW = 0; rowH = 0;
                    }
                    rowW += d.width + hg;
                    rowH = Math.max(rowH, d.height);
                }
                return new Dimension(Math.max(w, rowW),
                    h + rowH + ins.top + ins.bottom + vg * 2);
            }
        }
    }
}
