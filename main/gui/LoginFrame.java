package main.gui;

import java.awt.*;
import javax.swing.*;
import main.model.*;
import main.service.AuthService;

/**
 * Login screen.
 * Left half: dark gradient brand panel with app identity.
 * Right half: clean white card with sign-in / sign-up form.
 */
public class LoginFrame extends JFrame {

    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JLabel         statusLabel;

    private final AuthService authService = new AuthService();

    public LoginFrame() {
        setTitle("QuizPro");
        setSize(860, 560);
        setMinimumSize(new Dimension(700, 480));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridLayout(1, 2));
        setContentPane(root);

        root.add(buildBrandPanel());
        root.add(buildFormPanel());

        setVisible(true);
    }

    // ── Left brand panel ────────────────────────────────────────────────
    private JPanel buildBrandPanel() {
        JPanel panel = UITheme.gradientPanel(UITheme.SIDEBAR_BG, UITheme.PRIMARY_DARK);
        panel.setLayout(new GridBagLayout());

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setMaximumSize(new Dimension(280, 400));

        // icon
        JLabel icon = new JLabel("📚", JLabel.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 72));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        // app name
        JLabel name = new JLabel("QuizPro", JLabel.CENTER);
        name.setFont(UITheme.DISPLAY_FONT);
        name.setForeground(Color.WHITE);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        // tagline
        JLabel tag = new JLabel(
            "<html><div style='text-align:center;'>"
            + "Smart quizzes for<br>classrooms that move fast"
            + "</div></html>", JLabel.CENTER);
        tag.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tag.setForeground(new Color(255, 255, 255, 160));
        tag.setAlignmentX(Component.CENTER_ALIGNMENT);

        // feature chips
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        chips.setOpaque(false);
        for (String f : new String[]{"MCQ", "Subjective", "Live Timer"}) {
            JLabel chip = new JLabel(f);
            chip.setFont(new Font("Segoe UI", Font.BOLD, 11));
            chip.setForeground(new Color(255, 255, 255, 200));
            chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 1, true),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)
            ));
            chips.add(chip);
        }
        chips.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(icon);
        inner.add(Box.createVerticalStrut(10));
        inner.add(name);
        inner.add(Box.createVerticalStrut(10));
        inner.add(tag);
        inner.add(Box.createVerticalStrut(18));
        inner.add(chips);

        panel.add(inner);
        return panel;
    }

    // ── Right form panel ─────────────────────────────────────────────────
    private JPanel buildFormPanel() {
        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(UITheme.BG);

        JPanel card = UITheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(340, 420));

        // heading
        JLabel heading = UITheme.headingLabel("Welcome back 👋");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = UITheme.mutedLabel("Sign in to continue to QuizPro");
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        // username
        JLabel uLabel = UITheme.bodyLabel("Username");
        uLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField = UITheme.textField();
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // password
        JLabel pLabel = UITheme.bodyLabel("Password");
        pLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField = UITheme.passwordField();
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // status
        statusLabel = UITheme.colorLabel(" ", UITheme.DANGER);
        statusLabel.setFont(UITheme.SMALL_FONT);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // buttons
        JButton loginBtn  = UITheme.primaryBtn("Sign In");
        JButton signupBtn = UITheme.secondaryBtn("Create Account");
        loginBtn .setAlignmentX(Component.LEFT_ALIGNMENT);
        signupBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn .setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        signupBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        card.add(heading);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(22));
        card.add(uLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(14));
        card.add(pLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(6));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(8));
        card.add(signupBtn);

        bg.add(card);

        loginBtn .addActionListener(e -> handleLogin());
        signupBtn.addActionListener(e -> handleSignup());
        getRootPane().setDefaultButton(loginBtn);

        return bg;
    }

    // ── Actions ──────────────────────────────────────────────────────────
    private void handleLogin() {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());
        statusLabel.setText(" ");

        if (u.isEmpty() || p.isEmpty()) {
            statusLabel.setText("⚠  Please fill in all fields.");
            return;
        }

        try {
            User user = authService.login(u, p);
            dispose();
            if (user instanceof Student s) {
                new StudentDashboardFrame(s);
            } else if (user instanceof Teacher t) {
                new TeacherDashboardFrame(t);   // full dashboard, not just create frame
            }
        } catch (RuntimeException ex) {
            statusLabel.setText("✗  " + ex.getMessage());
        }
    }

    private void handleSignup() {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());
        statusLabel.setText(" ");

        if (u.isEmpty() || p.isEmpty()) {
            statusLabel.setText("⚠  Please fill in all fields.");
            return;
        }

        String[] roles = {"STUDENT", "TEACHER"};
        String role = (String) JOptionPane.showInputDialog(
            this, "Select your role:", "Create Account",
            JOptionPane.QUESTION_MESSAGE, null, roles, roles[0]);
        if (role == null) return;

        try {
            authService.signup(u, p, role);
            JOptionPane.showMessageDialog(
                this,
                "Account created! You can now sign in.",
                "Success ✓",
                JOptionPane.INFORMATION_MESSAGE
            );
        } catch (RuntimeException ex) {
            statusLabel.setText("✗  " + ex.getMessage());
        }
    }
}
