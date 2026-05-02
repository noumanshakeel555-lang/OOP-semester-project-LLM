package main.gui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

/**
 * Central design system for QuizPro.
 * All colors, fonts, and component factories live here so every
 * screen stays visually consistent.
 */
public class UITheme {

    // ───────────────────────────────────────────────────────────────────
    //  PALETTE
    // ───────────────────────────────────────────────────────────────────
    public static final Color BG             = new Color(0xF0F4FF);
    public static final Color SIDEBAR_BG     = new Color(0x0F1535);
    public static final Color SIDEBAR_HOVER  = new Color(0x1E2A50);
    public static final Color PRIMARY        = new Color(0x3D6FFF);
    public static final Color PRIMARY_DARK   = new Color(0x1A3FB7);
    public static final Color ACCENT         = new Color(0x845EFF);
    public static final Color SURFACE        = Color.WHITE;
    public static final Color TEXT           = new Color(0x1C2035);
    public static final Color TEXT_MUTED     = new Color(0x8A92A8);
    public static final Color SUCCESS        = new Color(0x00C48C);
    public static final Color DANGER         = new Color(0xFF4757);
    public static final Color WARNING        = new Color(0xFFB300);
    public static final Color BORDER         = new Color(0xDDE4FF);
    public static final Color QUIZ_CARD_BG   = new Color(0xF7F9FF);

    // ───────────────────────────────────────────────────────────────────
    //  TYPOGRAPHY
    // ───────────────────────────────────────────────────────────────────
    public static final Font DISPLAY_FONT  = new Font("Segoe UI", Font.BOLD,   36);
    public static final Font TITLE_FONT    = new Font("Segoe UI", Font.BOLD,   24);
    public static final Font HEADING_FONT  = new Font("Segoe UI", Font.BOLD,   18);
    public static final Font SUBHEAD_FONT  = new Font("Segoe UI", Font.BOLD,   15);
    public static final Font BODY_FONT     = new Font("Segoe UI", Font.PLAIN,  14);
    public static final Font SMALL_FONT    = new Font("Segoe UI", Font.PLAIN,  12);
    public static final Font BUTTON_FONT   = new Font("Segoe UI", Font.BOLD,   14);
    public static final Font SIDEBAR_FONT  = new Font("Segoe UI", Font.BOLD,   13);
    public static final Font MONO_FONT     = new Font("Consolas",  Font.PLAIN,  13);

    // ───────────────────────────────────────────────────────────────────
    //  BUTTONS
    // ───────────────────────────────────────────────────────────────────
    public static JButton primaryBtn(String text)   { return roundBtn(text, PRIMARY,   Color.WHITE); }
    public static JButton secondaryBtn(String text) { return roundBtn(text, new Color(0xE8EEFF), PRIMARY); }
    public static JButton dangerBtn(String text)    { return roundBtn(text, DANGER,    Color.WHITE); }
    public static JButton successBtn(String text)   { return roundBtn(text, SUCCESS,   Color.WHITE); }
    public static JButton accentBtn(String text)    { return roundBtn(text, ACCENT,    Color.WHITE); }

    public static JButton roundBtn(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color paint = getModel().isPressed()  ? bg.darker()
                            : getModel().isRollover() ? bg.brighter()
                                                      : bg;
                g2.setColor(paint);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(fg);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth()  - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        btn.setFont(BUTTON_FONT);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(180, 42));
        return btn;
    }

    // ───────────────────────────────────────────────────────────────────
    //  INPUT FIELDS
    // ───────────────────────────────────────────────────────────────────
    public static JTextField textField() {
        JTextField f = new JTextField(20);
        styleInput(f);
        return f;
    }

    public static JPasswordField passwordField() {
        JPasswordField f = new JPasswordField(20);
        styleInput(f);
        return f;
    }

    public static JTextArea textArea(int rows, int cols) {
        JTextArea a = new JTextArea(rows, cols);
        a.setFont(BODY_FONT);
        a.setForeground(TEXT);
        a.setBackground(SURFACE);
        a.setCaretColor(PRIMARY);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createCompoundBorder(
            new RoundBorder(BORDER, 1, 10),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        return a;
    }

    private static void styleInput(JTextField f) {
        f.setFont(BODY_FONT);
        f.setBackground(SURFACE);
        f.setForeground(TEXT);
        f.setCaretColor(PRIMARY);
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundBorder(BORDER, 1, 10),
            BorderFactory.createEmptyBorder(9, 14, 9, 14)
        ));
        f.setPreferredSize(new Dimension(260, 44));
    }

    // ───────────────────────────────────────────────────────────────────
    //  PANELS
    // ───────────────────────────────────────────────────────────────────
    /** Left-to-right gradient panel (transparent layout). */
    public static JPanel gradientPanel(Color c1, Color c2) {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                                    RenderingHints.VALUE_RENDER_QUALITY);
                g2.setPaint(new GradientPaint(0, 0, c1, 0, getHeight(), c2));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        p.setOpaque(true);
        return p;
    }

    /** White rounded card with soft drop shadow. */
    public static JPanel card() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                // layered shadow
                for (int i = 6; i >= 1; i--) {
                    g2.setColor(new Color(0, 0, 30, 5));
                    g2.fillRoundRect(i, i, getWidth() - i, getHeight() - i, 20, 20);
                }
                g2.setColor(SURFACE);
                g2.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 6, 20, 20);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));
        return p;
    }

    /** Flat coloured badge/chip. */
    public static JLabel badge(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text, JLabel.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(fg);
        l.setOpaque(false);
        l.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        return l;
    }

    // ───────────────────────────────────────────────────────────────────
    //  LABELS
    // ───────────────────────────────────────────────────────────────────
    public static JLabel displayLabel(String t) {
        JLabel l = new JLabel(t, JLabel.CENTER); l.setFont(DISPLAY_FONT); l.setForeground(TEXT); return l;
    }
    public static JLabel titleLabel(String t) {
        JLabel l = new JLabel(t, JLabel.CENTER); l.setFont(TITLE_FONT); l.setForeground(TEXT); return l;
    }
    public static JLabel headingLabel(String t) {
        JLabel l = new JLabel(t); l.setFont(HEADING_FONT); l.setForeground(TEXT); return l;
    }
    public static JLabel subheadLabel(String t) {
        JLabel l = new JLabel(t); l.setFont(SUBHEAD_FONT); l.setForeground(TEXT); return l;
    }
    public static JLabel bodyLabel(String t) {
        JLabel l = new JLabel(t); l.setFont(BODY_FONT); l.setForeground(TEXT); return l;
    }
    public static JLabel mutedLabel(String t) {
        JLabel l = new JLabel(t); l.setFont(SMALL_FONT); l.setForeground(TEXT_MUTED); return l;
    }
    public static JLabel colorLabel(String t, Color c) {
        JLabel l = new JLabel(t); l.setFont(BODY_FONT); l.setForeground(c); return l;
    }

    // ───────────────────────────────────────────────────────────────────
    //  PROGRESS BAR
    // ───────────────────────────────────────────────────────────────────
    public static JProgressBar progressBar(int max) {
        JProgressBar pb = new JProgressBar(0, max) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                // track
                g2.setColor(BORDER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                // fill
                if (getValue() > 0) {
                    int w = (int) ((getWidth() * (double) getValue()) / getMaximum());
                    g2.setColor(PRIMARY);
                    g2.fillRoundRect(0, 0, w, getHeight(), getHeight(), getHeight());
                }
                g2.dispose();
            }
        };
        pb.setBorderPainted(false);
        pb.setOpaque(false);
        pb.setPreferredSize(new Dimension(Integer.MAX_VALUE, 8));
        return pb;
    }

    // ───────────────────────────────────────────────────────────────────
    //  SEPARATOR
    // ───────────────────────────────────────────────────────────────────
    public static JSeparator separator() {
        JSeparator s = new JSeparator();
        s.setForeground(BORDER);
        s.setBackground(BG);
        return s;
    }

    // ───────────────────────────────────────────────────────────────────
    //  SCROLL PANE
    // ───────────────────────────────────────────────────────────────────
    public static JScrollPane scrollPane(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setBackground(BG);
        sp.getViewport().setBackground(BG);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // ───────────────────────────────────────────────────────────────────
    //  GLOBAL INIT
    // ───────────────────────────────────────────────────────────────────
    public static void apply() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}
        UIManager.put("Panel.background",          BG);
        UIManager.put("OptionPane.background",     SURFACE);
        UIManager.put("OptionPane.messageFont",    BODY_FONT);
        UIManager.put("OptionPane.buttonFont",     BUTTON_FONT);
        UIManager.put("ScrollPane.background",     BG);
        UIManager.put("Viewport.background",       BG);
        UIManager.put("ComboBox.font",             BODY_FONT);
        UIManager.put("RadioButton.font",          BODY_FONT);
        UIManager.put("CheckBox.font",             BODY_FONT);
    }

    // ───────────────────────────────────────────────────────────────────
    //  INNER: RoundBorder
    // ───────────────────────────────────────────────────────────────────
    public static class RoundBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        public RoundBorder(Color c, int t, int r) {
            this.color = c; this.thickness = t; this.radius = r;
        }

        @Override
        public void paintBorder(Component c, Graphics g,
                                int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.draw(new RoundRectangle2D.Double(
                x + 0.5, y + 0.5, w - 1, h - 1, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override public boolean isBorderOpaque() { return false; }
    }
}
