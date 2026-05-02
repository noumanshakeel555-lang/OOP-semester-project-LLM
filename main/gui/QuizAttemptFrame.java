package main.gui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.text.DefaultEditorKit;
import main.dao.AttemptDAO;
import main.model.*;

/**
 * Quiz attempt screen — full anti-cheat suite.
 *
 * Layer 1 — Fullscreen + always-on-top
 * Layer 2 — Key blocker (Alt+Tab, Win+Tab, Win+D, Win+Ctrl+D,
 *            Win+Ctrl+Left/Right, Escape, Alt+F4, Cmd+Q)
 * Layer 3 — Focus watcher (any app/desktop switch = violation)
 * Layer 4 — Blackout overlay (opaque JWindow covers the screen
 *            the instant focus is lost, hides quiz content)
 * Layer 5 — Clipboard wiper (background thread nukes clipboard
 *            every 300 ms — makes paste attacks useless)
 * Layer 6 — Paste blocker on text areas (Ctrl+V / right-click
 *            context menu disabled on the answer field)
 * Layer 7 — Warning overlay + violation dots + auto-submit at 3
 */
public class QuizAttemptFrame extends JFrame {

    // ── Anti-cheat config ─────────────────────────────────────────────────
    private static final int  MAX_VIOLATIONS = 3;
    private int               violations     = 0;
    private boolean           quizEnded      = false;
    private boolean           warningVisible = false;

    private GraphicsDevice    graphicsDevice;
    private KeyEventDispatcher keyBlocker;

    /** Solid-black panel at DRAG_LAYER — covers quiz content when focus is lost. */
    private JPanel            blackoutPanel;
    /** Background thread that wipes the system clipboard every 300 ms. */
    private Thread            clipboardWiper;
    private volatile boolean  wipingClipboard = true;

    // ── Quiz state ────────────────────────────────────────────────────────
    private final Quiz                quiz;
    private final Student             student;
    private final List<Question>      questions;
    private int                       currentIndex = 0;
    private final Map<Integer,String> answers = new LinkedHashMap<>();

    // ── Timer ─────────────────────────────────────────────────────────────
    private static final int TIME_LIMIT_SECONDS = 120;
    private int              secondsLeft         = TIME_LIMIT_SECONDS;
    private Timer            countdownTimer;

    // ── UI refs ───────────────────────────────────────────────────────────
    private JLabel       timerLabel;
    private JLabel       qCounterLabel;
    private JProgressBar progressBar;
    private JPanel       questionCard;
    private ButtonGroup  optionGroup;
    private JTextArea    subjectiveArea;
    private JButton      prevBtn;
    private JButton      nextBtn;
    private JPanel       warningOverlay;
    private JLabel       warningLabel;
    private Timer        overlayDismissTimer;

    // ── Constructor ───────────────────────────────────────────────────────
    public QuizAttemptFrame(Quiz quiz, Student student) {
        this.quiz      = quiz;
        this.student   = student;
        this.questions = quiz.getQuestions();

        setTitle("QuizPro — " + quiz.getTitle());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setUndecorated(true);

        buildUI();
        enterFullscreen();
        installKeyBlocker();
        installFocusWatcher();
        startClipboardWiper();
        startTimer();
        loadQuestion();
        setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UI BUILD
    // ═══════════════════════════════════════════════════════════════════════

    private void buildUI() {
        JLayeredPane layered = new JLayeredPane();
        setContentPane(layered);

        JPanel main = new JPanel(new BorderLayout()) {
            @Override public void setBounds(int x, int y, int w, int h) {
                super.setBounds(x, y, w, h);
                if (warningOverlay != null)
                    warningOverlay.setBounds(0, 0, w, 80);
            }
        };
        main.setBackground(UITheme.BG);
        main.add(buildHeader(),  BorderLayout.NORTH);
        main.add(buildBody(),    BorderLayout.CENTER);
        main.add(buildNavBar(),  BorderLayout.SOUTH);

        main.setBounds(0, 0, 1920, 1080);
        layered.add(main, JLayeredPane.DEFAULT_LAYER);

        buildWarningOverlay();
        layered.add(warningOverlay, JLayeredPane.PALETTE_LAYER);

        // Blackout lives at DRAG_LAYER — above everything including the warning banner.
        // Being inside the same JFrame means it works in OS exclusive fullscreen mode
        // (a separate JWindow cannot appear over a fullscreen window on most platforms).
        buildBlackoutPanel();
        layered.add(blackoutPanel, JLayeredPane.DRAG_LAYER);

        layered.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int w = layered.getWidth(), h = layered.getHeight();
                main.setBounds(0, 0, w, h);
                warningOverlay.setBounds(0, 0, w, 80);
                blackoutPanel.setBounds(0, 0, w, h);
            }
        });
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(UITheme.SIDEBAR_BG);
        h.setBorder(BorderFactory.createEmptyBorder(14, 28, 14, 28));

        JLabel title = new JLabel(quiz.getTitle());
        title.setFont(UITheme.SUBHEAD_FONT);
        title.setForeground(Color.WHITE);

        JPanel centre = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centre.setOpaque(false);
        centre.add(UITheme.badge("🛡  Anti-Cheat Active",
            new Color(255, 71, 87, 40), new Color(255, 150, 150)));

        timerLabel = new JLabel("⏱  " + formatTime(secondsLeft));
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        timerLabel.setForeground(new Color(0x90E0FF));

        h.add(title,      BorderLayout.WEST);
        h.add(centre,     BorderLayout.CENTER);
        h.add(timerLabel, BorderLayout.EAST);
        return h;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(UITheme.BG);
        body.setBorder(BorderFactory.createEmptyBorder(20, 60, 0, 60));

        JPanel progressRow = new JPanel(new BorderLayout(12, 0));
        progressRow.setOpaque(false);
        progressBar   = UITheme.progressBar(questions.size());
        qCounterLabel = UITheme.mutedLabel("Question 1 of " + questions.size());
        progressRow.add(progressBar,   BorderLayout.CENTER);
        progressRow.add(qCounterLabel, BorderLayout.EAST);
        body.add(progressRow, BorderLayout.NORTH);

        questionCard = new JPanel(new BorderLayout());
        questionCard.setOpaque(false);
        body.add(questionCard, BorderLayout.CENTER);
        return body;
    }

    private JPanel buildNavBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER),
            BorderFactory.createEmptyBorder(14, 60, 14, 60)));

        prevBtn = UITheme.secondaryBtn("← Previous");
        prevBtn.setEnabled(false);
        prevBtn.addActionListener(e -> {
            saveCurrentAnswer();
            currentIndex--;
            loadQuestion();
        });

        nextBtn = UITheme.primaryBtn("Next →");
        nextBtn.addActionListener(e -> {
            saveCurrentAnswer();
            if (currentIndex >= questions.size() - 1) {
                submitQuiz(false);
            } else {
                currentIndex++;
                loadQuestion();
            }
        });

        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightGroup.setOpaque(false);
        rightGroup.add(buildViolationIndicator());
        rightGroup.add(nextBtn);

        bar.add(prevBtn,     BorderLayout.WEST);
        bar.add(rightGroup,  BorderLayout.EAST);
        return bar;
    }

    private JPanel buildViolationIndicator() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        p.add(UITheme.mutedLabel("Warnings: "));
        for (int i = 0; i < MAX_VIOLATIONS; i++) {
            JLabel dot = new JLabel("●");
            dot.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            dot.setForeground(UITheme.BORDER);
            dot.setName("viol_" + i);
            p.add(dot);
        }
        return p;
    }

    private void refreshViolationDots() {
        updateDots(getContentPane());
    }

    private void updateDots(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JLabel lbl && lbl.getName() != null
                    && lbl.getName().startsWith("viol_")) {
                int idx = Integer.parseInt(lbl.getName().substring(5));
                lbl.setForeground(idx < violations ? UITheme.DANGER : UITheme.BORDER);
            }
            if (comp instanceof Container ct) updateDots(ct);
        }
    }

    private void buildWarningOverlay() {
        warningOverlay = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0,
                    new Color(220, 30, 30, 230), 0, getHeight(),
                    new Color(180, 0, 0, 200)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        warningOverlay.setLayout(new GridBagLayout());
        warningOverlay.setOpaque(false);
        warningOverlay.setVisible(false);

        warningLabel = new JLabel();
        warningLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        warningLabel.setForeground(Color.WHITE);
        warningLabel.setHorizontalAlignment(SwingConstants.CENTER);
        warningOverlay.add(warningLabel);
    }

    private void showWarning(String message) {
        warningVisible = true;
        warningLabel.setText(message);
        warningOverlay.setVisible(true);

        Timer shake = new Timer(40, null);
        final int[] tick = {0};
        shake.addActionListener(e -> {
            tick[0]++;
            int offset = (tick[0] % 2 == 0) ? 4 : -4;
            warningOverlay.setBounds(offset, 0,
                getLayeredPane().getWidth() - Math.abs(offset), 80);
            if (tick[0] >= 8) {
                warningOverlay.setBounds(0, 0, getLayeredPane().getWidth(), 80);
                shake.stop();
            }
        });
        shake.start();

        if (overlayDismissTimer != null) overlayDismissTimer.stop();
        overlayDismissTimer = new Timer(2500, e -> {
            warningOverlay.setVisible(false);
            warningVisible = false;
        });
        overlayDismissTimer.setRepeats(false);
        overlayDismissTimer.start();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANTI-CHEAT LAYER 4 — BLACKOUT PANEL (in-frame, works in fullscreen)
    //
    //  A separate JWindow CANNOT appear over an OS exclusive-fullscreen window
    //  on Windows/Linux. So instead we place a solid-black JPanel at
    //  JLayeredPane.DRAG_LAYER (above everything) inside the same frame.
    //  It is invisible by default and made visible the instant focus is lost.
    // ═══════════════════════════════════════════════════════════════════════

    private void buildBlackoutPanel() {
        blackoutPanel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        blackoutPanel.setOpaque(true);
        blackoutPanel.setBackground(Color.BLACK);
        blackoutPanel.setBounds(0, 0, 1920, 1080);

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel("⛔", JLabel.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Quiz Paused", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg = new JLabel(
            "<html><div style='text-align:center;color:#CCCCCC;"
            + "font-family:Segoe UI;font-size:16pt;'>"
            + "You left the quiz window.<br>"
            + "This has been recorded as a violation.<br><br>"
            + "<span style='color:#FF4757;font-weight:bold;'>"
            + "Return to the quiz immediately.</span>"
            + "</div></html>", JLabel.CENTER);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(icon);
        inner.add(Box.createVerticalStrut(14));
        inner.add(title);
        inner.add(Box.createVerticalStrut(14));
        inner.add(msg);

        blackoutPanel.add(inner);
        blackoutPanel.setVisible(false);
    }

    private void showBlackout() {
        if (quizEnded) return;
        SwingUtilities.invokeLater(() -> {
            if (blackoutPanel != null) {
                // Resize to fill the whole frame
                Container cp = getContentPane();
                blackoutPanel.setBounds(0, 0, cp.getWidth(), cp.getHeight());
                blackoutPanel.setVisible(true);
                blackoutPanel.repaint();
            }
        });
    }

    private void hideBlackout() {
        SwingUtilities.invokeLater(() -> {
            if (blackoutPanel != null) blackoutPanel.setVisible(false);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANTI-CHEAT LAYER 5 — CLIPBOARD WIPER
    //  Runs on a background daemon thread.
    //  Every 300 ms: checks if the clipboard has any text content.
    //  If it does, immediately replaces it with an empty string.
    //  This means anything the student copies from another window is
    //  gone before they can switch back and paste it.
    // ═══════════════════════════════════════════════════════════════════════

    private void startClipboardWiper() {
        // Wipe once immediately to clear anything already in the clipboard
        wipeClipboard();

        clipboardWiper = new Thread(() -> {
            while (wipingClipboard && !quizEnded) {
                wipeClipboard();
                try { Thread.sleep(300); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }, "clipboard-wiper");
        clipboardWiper.setDaemon(true);   // won't prevent JVM exit
        clipboardWiper.start();
    }

    private void wipeClipboard() {
        try {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            // Only wipe if there's actually something there — avoids spurious errors
            if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                // Replace contents with empty string
                StringSelection empty = new StringSelection("");
                cb.setContents(empty, empty);
            }
        } catch (Exception ignored) {
            // IllegalStateException if clipboard is owned by another app momentarily
        }
    }

    private void stopClipboardWiper() {
        wipingClipboard = false;
        if (clipboardWiper != null) clipboardWiper.interrupt();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANTI-CHEAT LAYER 2 — KEY BLOCKER (expanded)
    //  Blocks at JVM level before any component processes the key event.
    //  NOTE: Win+Tab Task View is a kernel-level OS shortcut on Windows
    //  and cannot be fully blocked from Java. However:
    //    - The focus listener fires immediately when focus is lost
    //    - The blackout window covers the screen
    //    - The clipboard wiper runs every 300 ms
    //  So even if the student gets to Task View, they gain nothing.
    // ═══════════════════════════════════════════════════════════════════════

    private void enterFullscreen() {
        graphicsDevice = GraphicsEnvironment
            .getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (graphicsDevice.isFullScreenSupported()) {
            graphicsDevice.setFullScreenWindow(this);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
        setAlwaysOnTop(true);
        requestFocus();
    }

    private void exitFullscreen() {
        setAlwaysOnTop(false);
        if (graphicsDevice != null && graphicsDevice.getFullScreenWindow() == this) {
            graphicsDevice.setFullScreenWindow(null);
        }
    }

    /**
     * We track the Windows key state ourselves because Java does NOT expose
     * VK_WINDOWS as a modifier bit in getModifiersEx(). Without this flag,
     * Win+D / Win+Tab / Win+Ctrl+D are invisible to modifier checks.
     *
     * Strategy:
     *  - KEY_PRESSED  VK_WINDOWS → set windowsKeyDown = true, consume event
     *  - KEY_RELEASED VK_WINDOWS → set windowsKeyDown = false
     *  - Any KEY_PRESSED while windowsKeyDown == true → consume and block
     *    (this catches Win+Tab, Win+D, Win+Ctrl+D, Win+Ctrl+Left/Right, etc.)
     */
    private volatile boolean windowsKeyDown = false;

    private void installKeyBlocker() {
        keyBlocker = e -> {
            if (quizEnded) return false;

            int  code = e.getKeyCode();
            int  id   = e.getID();   // KEY_PRESSED, KEY_RELEASED, KEY_TYPED
            int  mods = e.getModifiersEx();

            boolean ctrl = (mods & InputEvent.CTRL_DOWN_MASK) != 0;
            boolean alt  = (mods & InputEvent.ALT_DOWN_MASK)  != 0;
            boolean meta = (mods & InputEvent.META_DOWN_MASK) != 0;

            // ── Track Windows key state ─────────────────────────────────
            if (code == KeyEvent.VK_WINDOWS) {
                if (id == KeyEvent.KEY_PRESSED) {
                    windowsKeyDown = true;
                } else if (id == KeyEvent.KEY_RELEASED) {
                    windowsKeyDown = false;
                }
                // Always consume the Windows key itself
                e.consume();
                return true;
            }

            // macOS Cmd key
            if (code == KeyEvent.VK_META) {
                e.consume();
                return true;
            }

            // ── While Windows key is held, block EVERYTHING ─────────────
            // This catches: Win+Tab, Win+D, Win+Ctrl+D,
            //               Win+Ctrl+Left/Right, Win+Ctrl+F4, Win+R, etc.
            if (windowsKeyDown) {
                e.consume();
                return true;
            }

            // ── Standard modifier-based blocks ──────────────────────────

            // Alt+Tab / Cmd+Tab — app switching
            if ((alt || meta) && code == KeyEvent.VK_TAB) {
                e.consume(); return true;
            }

            // Alt+F4 — close window
            if (alt && code == KeyEvent.VK_F4) {
                e.consume(); return true;
            }

            // Cmd+Q — quit on macOS
            if (meta && code == KeyEvent.VK_Q) {
                e.consume(); return true;
            }

            // Escape — might exit fullscreen on some systems
            if (code == KeyEvent.VK_ESCAPE) {
                e.consume(); return true;
            }

            // PrintScreen — app-level block (OS-level PrtSc can't be blocked)
            if (code == KeyEvent.VK_PRINTSCREEN) {
                e.consume(); return true;
            }

            return false;
        };

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(keyBlocker);
    }

    private void removeKeyBlocker() {
        if (keyBlocker != null) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .removeKeyEventDispatcher(keyBlocker);
            keyBlocker = null;
        }
        windowsKeyDown = false;   // clear in case Win key was held during submit
    }

    // ── Focus watcher ─────────────────────────────────────────────────────
    private void installFocusWatcher() {
        addWindowFocusListener(new WindowFocusListener() {
            @Override public void windowGainedFocus(WindowEvent e) {
                setAlwaysOnTop(true);
                toFront();
                hideBlackout();
                // Wipe whatever they may have copied while away
                wipeClipboard();
            }

            @Override public void windowLostFocus(WindowEvent e) {
                if (quizEnded) return;

                // Slam the blackout immediately
                showBlackout();

                violations++;
                refreshViolationDots();

                // Try to reclaim focus
                SwingUtilities.invokeLater(() -> { toFront(); requestFocus(); });

                if (violations >= MAX_VIOLATIONS) {
                    SwingUtilities.invokeLater(() -> {
                        showWarning("❌  Too many violations — quiz is being auto-submitted!");
                        new Timer(1800, ev -> {
                            ((Timer) ev.getSource()).stop();
                            submitQuiz(true);
                        }) {{ setRepeats(false); }}.start();
                    });
                } else {
                    int remaining = MAX_VIOLATIONS - violations;
                    SwingUtilities.invokeLater(() ->
                        showWarning("⚠  WARNING " + violations + "/" + MAX_VIOLATIONS
                            + "  —  App switch detected!  ("
                            + remaining + " warning" + (remaining == 1 ? "" : "s")
                            + " left before auto-submit)")
                    );
                }
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  QUESTION LOADING
    // ═══════════════════════════════════════════════════════════════════════

    private void loadQuestion() {
        questionCard.removeAll();
        Question q = questions.get(currentIndex);

        qCounterLabel.setText("Question " + (currentIndex + 1) + " of " + questions.size());
        progressBar.setValue(currentIndex + 1);
        prevBtn.setEnabled(currentIndex > 0);
        nextBtn.setText(currentIndex == questions.size() - 1 ? "Submit ✓" : "Next →");

        JPanel card = UITheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badges.setOpaque(false);
        badges.setAlignmentX(Component.LEFT_ALIGNMENT);
        badges.add(UITheme.badge("Q" + (currentIndex + 1), new Color(0xE8EEFF), UITheme.PRIMARY));
        badges.add(UITheme.badge(q.getMarks() + " marks",  new Color(0xE6FFF6), UITheme.SUCCESS));
        if (q instanceof SubjectiveQuestion) {
            badges.add(UITheme.badge("Teacher Marked", new Color(0xFFF3CD), UITheme.WARNING));
        }

        JTextArea qText = new JTextArea(q.getQuestionText());
        qText.setFont(new Font("Segoe UI", Font.BOLD, 17));
        qText.setForeground(UITheme.TEXT);
        qText.setOpaque(false);
        qText.setEditable(false);
        qText.setLineWrap(true);
        qText.setWrapStyleWord(true);
        qText.setAlignmentX(Component.LEFT_ALIGNMENT);
        qText.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        card.add(badges);
        card.add(Box.createVerticalStrut(14));
        card.add(qText);
        card.add(Box.createVerticalStrut(16));
        card.add(UITheme.separator());
        card.add(Box.createVerticalStrut(14));

        if (q instanceof MCQQuestion mcq) {
            buildMcqOptions(card, mcq);
        } else {
            buildSubjectiveInput(card);
        }

        questionCard.add(card, BorderLayout.CENTER);
        questionCard.revalidate();
        questionCard.repaint();
        restoreAnswer(q);
    }

    // ── MCQ radio buttons ─────────────────────────────────────────────────
    private void buildMcqOptions(JPanel card, MCQQuestion mcq) {
        optionGroup    = new ButtonGroup();
        subjectiveArea = null;
        String[] letters = {"A", "B", "C", "D"};
        List<String> opts = mcq.getOptions();

        for (int i = 0; i < opts.size(); i++) {
            final String opt    = opts.get(i);
            final String letter = (i < letters.length) ? letters[i] : String.valueOf(i + 1);

            JRadioButton radio = new JRadioButton(letter + ".  " + opt) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    Color bg = isSelected()            ? new Color(0xE8EEFF)
                             : getModel().isRollover() ? new Color(0xF5F7FF)
                             : UITheme.SURFACE;
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    if (isSelected()) {
                        g2.setColor(UITheme.PRIMARY);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            radio.setFont(UITheme.BODY_FONT);
            radio.setForeground(UITheme.TEXT);
            radio.setOpaque(false);
            radio.setContentAreaFilled(false);
            radio.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            radio.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            radio.setAlignmentX(Component.LEFT_ALIGNMENT);
            radio.addActionListener(e ->
                answers.put(questions.get(currentIndex).getId(), opt));
            optionGroup.add(radio);
            card.add(radio);
            card.add(Box.createVerticalStrut(6));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ANTI-CHEAT LAYER 6 — PASTE BLOCKER ON TEXT AREA
    //  Removes paste/copy/cut from the subjective answer text area.
    //  Students must type their own answers — cannot paste copied text.
    // ─────────────────────────────────────────────────────────────────────
    private void buildSubjectiveInput(JPanel card) {
        optionGroup = null;

        // ── Amber "Teacher Marked" notice ──────────────────────────────────
        JPanel pendingBanner = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xFFF3CD));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(0xFFB300));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
            }
        };
        pendingBanner.setLayout(new BoxLayout(pendingBanner, BoxLayout.Y_AXIS));
        pendingBanner.setOpaque(false);
        pendingBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        pendingBanner.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        pendingBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel pendingTitle = new JLabel("⏳  Marks Awarded After Teacher Review");
        pendingTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pendingTitle.setForeground(new Color(0x856404));

        JLabel pendingDesc = new JLabel(
            "Your answer will be read and graded by your teacher. "
            + "You will see the marks in My Results once graded.");
        pendingDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pendingDesc.setForeground(new Color(0x856404));

        pendingBanner.add(pendingTitle);
        pendingBanner.add(Box.createVerticalStrut(4));
        pendingBanner.add(pendingDesc);

        card.add(pendingBanner);
        card.add(Box.createVerticalStrut(12));

        // ── Answer text area ────────────────────────────────────────────────
        JLabel hint = UITheme.mutedLabel("Write your answer below  (copy-paste is disabled):");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(hint);
        card.add(Box.createVerticalStrut(8));

        subjectiveArea = UITheme.textArea(6, 60);

        // ── Block paste / copy / cut via InputMap ──
        InputMap  im = subjectiveArea.getInputMap();
        ActionMap am = subjectiveArea.getActionMap();

        // Dead action — does nothing
        Action doNothing = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                // intentionally empty — blocks the default paste/copy/cut
            }
        };

        // Ctrl+V / Cmd+V — paste
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "blocked-paste");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.META_DOWN_MASK), "blocked-paste");
        am.put("blocked-paste", doNothing);

        // Ctrl+C / Cmd+C — copy
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "blocked-copy");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK), "blocked-copy");
        am.put("blocked-copy", doNothing);

        // Ctrl+X / Cmd+X — cut
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK), "blocked-cut");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.META_DOWN_MASK), "blocked-cut");
        am.put("blocked-cut", doNothing);

        // Ctrl+A / Cmd+A — select all (also blocks then copying)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "blocked-sela");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.META_DOWN_MASK), "blocked-sela");
        am.put("blocked-sela", doNothing);

        // Disable the right-click context menu (which shows Copy/Paste/Cut)
        subjectiveArea.setComponentPopupMenu(null);
        subjectiveArea.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { if (e.isPopupTrigger()) e.consume(); }
            @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) e.consume(); }
        });

        subjectiveArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane sp = UITheme.scrollPane(subjectiveArea);
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setPreferredSize(new Dimension(Integer.MAX_VALUE, 160));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        card.add(sp);
    }

    // ── Save / restore ────────────────────────────────────────────────────
    private void saveCurrentAnswer() {
        if (subjectiveArea != null) {
            String txt = subjectiveArea.getText().trim();
            if (!txt.isEmpty())
                answers.put(questions.get(currentIndex).getId(), txt);
        }
    }

    private void restoreAnswer(Question q) {
        String saved = answers.get(q.getId());
        if (saved == null) return;
        if (q instanceof MCQQuestion && optionGroup != null) {
            Enumeration<AbstractButton> btns = optionGroup.getElements();
            while (btns.hasMoreElements()) {
                JRadioButton rb = (JRadioButton) btns.nextElement();
                String raw = rb.getText().replaceFirst("^[A-D]\\.\\s+", "");
                if (raw.equals(saved)) { rb.setSelected(true); break; }
            }
        } else if (subjectiveArea != null) {
            subjectiveArea.setText(saved);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SUBMIT
    // ═══════════════════════════════════════════════════════════════════════

    private void submitQuiz(boolean autoSubmit) {
        if (quizEnded) return;

        // ── MCQ validation — must answer every MCQ before submitting ──────
        if (!autoSubmit) {
            saveCurrentAnswer();
            List<String> unanswered = new ArrayList<>();
            for (int i = 0; i < questions.size(); i++) {
                Question q = questions.get(i);
                if (q instanceof MCQQuestion) {
                    String ans = answers.get(q.getId());
                    if (ans == null || ans.trim().isEmpty()) {
                        unanswered.add("Q" + (i + 1));
                    }
                }
            }
            if (!unanswered.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "<html><div style='font-family:Segoe UI;'>"
                    + "<b>⚠  Please answer all MCQ questions before submitting.</b><br><br>"
                    + "Unanswered: " + String.join(", ", unanswered)
                    + "<br><br>Written (subjective) questions can be left blank,<br>"
                    + "but every multiple-choice question must be selected.</div></html>",
                    "Incomplete Answers",
                    JOptionPane.WARNING_MESSAGE);
                return;   // do NOT submit — let student go back and answer
            }
        }

        quizEnded = true;

        stopTimer();
        removeKeyBlocker();
        stopClipboardWiper();
        hideBlackout();
        saveCurrentAnswer();

        int score = 0, pendingMarks = 0;
        for (Question q : questions) {
            String ans = answers.getOrDefault(q.getId(), "");
            if (q instanceof SubjectiveQuestion) {
                if (!ans.trim().isEmpty()) pendingMarks += q.getMarks();
            } else {
                if (!ans.isEmpty()) score += q.evaluate(ans);
            }
        }

        int    total  = questions.stream().mapToInt(Question::getMarks).sum();
        Result result = new Result(score, total, pendingMarks);

        exitFullscreen();

        try {
            new AttemptDAO().saveFullAttempt(student, quiz, answers, result, violations);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Your attempt was completed but could not be saved to the database.\n\n"
                + "Please tell your teacher immediately.\n\nError: " + e.getMessage(),
                "Save Error", JOptionPane.ERROR_MESSAGE);
        }

        showResultDialog(result, autoSubmit);
    }

    private void showResultDialog(Result result, boolean autoSubmit) {
        double pct   = result.getPercentage();
        String grade = pct >= 90 ? "Excellent! 🏆"
                     : pct >= 70 ? "Good job! 👍"
                     : pct >= 50 ? "Passing ✔"
                     :             "Keep practising 💪";

        String pendingNote = result.hasPending()
            ? "<p style='color:#FFB300;font-size:12pt;margin:10px 0 0;'>"
              + "⏳  " + result.getPendingMarks() + " mark"
              + (result.getPendingMarks() == 1 ? "" : "s")
              + " pending — your teacher will mark your written answers.</p>"
            : "";

        String cheatNote = autoSubmit
            ? "<p style='color:#FF4757;font-weight:bold;margin:8px 0 0;'>"
              + "⚠  Auto-submitted due to " + violations + " tab-switch violation"
              + (violations == 1 ? "" : "s") + ".</p>"
            : "";

        String msg = "<html><div style='font-family:Segoe UI;text-align:center;'>"
            + "<h2 style='margin:0;'>Quiz Complete!</h2>"
            + "<p style='color:gray;margin:4px 0 14px;'>Here is how you did:</p>"
            + "<h1 style='font-size:36pt;margin:4px 0;color:#3D6FFF;'>"
            + result.getScore() + " / " + result.getTotalMarks() + "</h1>"
            + "<p style='font-size:14pt;margin:4px 0;'>"
            + String.format("%.1f", pct) + "% (MCQ only)</p>"
            + "<p style='font-size:13pt;margin:8px 0 0;'>" + grade + "</p>"
            + pendingNote + cheatNote
            + "</div></html>";

        JOptionPane.showMessageDialog(this, msg, "Your Result",
            autoSubmit ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);

        dispose();
        new StudentDashboardFrame(student);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TIMER
    // ═══════════════════════════════════════════════════════════════════════

    private void startTimer() {
        countdownTimer = new Timer(1000, e -> {
            secondsLeft--;
            timerLabel.setText("⏱  " + formatTime(secondsLeft));
            if (secondsLeft <= 30) timerLabel.setForeground(UITheme.DANGER);
            if (secondsLeft <= 0) {
                stopTimer();
                showWarning("⏰  Time is up! Submitting your answers...");
                new Timer(1500, ev -> {
                    ((Timer) ev.getSource()).stop();
                    saveCurrentAnswer();
                    submitQuiz(false);
                }) {{ setRepeats(false); }}.start();
            }
        });
        countdownTimer.start();
    }

    private void stopTimer() {
        if (countdownTimer != null && countdownTimer.isRunning()) countdownTimer.stop();
    }

    private String formatTime(int secs) {
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }
}
