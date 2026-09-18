package com.dripdoor.view;

import com.dripdoor.config.AppConfig;
import com.dripdoor.config.ThemeConfig;
import com.dripdoor.controller.AIStylistController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;

/**
 * AI Stylist screen -- asks the user about their event and outfit,
 * then calls Gemini (via Firebase AI) to curate a personalised
 * jewellery bundle.
 */
public class AIStylistView extends JPanel {

    private static final List<String> EVENTS = List.of(
            "Wedding", "Gala / Black Tie", "Corporate Event",
            "Cocktail Party", "Beach Wedding", "Red Carpet",
            "Casual Chic", "Anniversary Dinner", "Festival / Sangeet",
            "Engagement Ceremony"
    );

    // Form fields
    private JComboBox<String> eventCombo;
    private JTextArea         outfitArea;
    private JTextField        notesField;

    // Output
    private JTextArea resultArea;
    private JButton   curateBtn;
    private JLabel    statusLabel;

    public AIStylistView() {
        setBackground(ThemeConfig.SOFT_GREY);
        setLayout(new BorderLayout());
        initUI();
    }

    // ── UI build ──────────────────────────────────────────────────────────

    private void initUI() {
        add(buildHeader(), BorderLayout.NORTH);

        JPanel form = buildForm();
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeConfig.WHITE);
        header.setBorder(new EmptyBorder(24, 32, 20, 32));

        JPanel block = new JPanel();
        block.setBackground(ThemeConfig.WHITE);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("AI Stylist");
        title.setFont(new Font("Georgia", Font.BOLD, 28));
        title.setForeground(ThemeConfig.DEEP_BLACK);
        block.add(title);

        JLabel sub = new JLabel("Powered by Gemini via Firebase AI");
        sub.setFont(new Font("Georgia", Font.ITALIC, 13));
        sub.setForeground(ThemeConfig.GOLD);
        block.add(sub);

        header.add(block, BorderLayout.WEST);
        return header;
    }

    private JPanel buildForm() {
        JPanel p = new JPanel();
        p.setBackground(ThemeConfig.WHITE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(32, 44, 44, 44));

        // -- Q1: Event ------------------------------------------------
        p.add(sectionLabel("1.  What is the occasion?"));
        p.add(Box.createVerticalStrut(8));

        eventCombo = new JComboBox<>(EVENTS.toArray(new String[0]));
        eventCombo.setFont(new Font("Georgia", Font.PLAIN, 14));
        eventCombo.setBackground(ThemeConfig.WHITE);
        eventCombo.setMaximumSize(new Dimension(380, 44));
        eventCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(eventCombo);

        p.add(Box.createVerticalStrut(28));

        // -- Q2: Outfit -----------------------------------------------
        p.add(sectionLabel("2.  What are you wearing?"));
        p.add(Box.createVerticalStrut(4));
        p.add(hintLabel("Describe your outfit -- colour, fabric, silhouette, neckline..."));
        p.add(Box.createVerticalStrut(8));

        outfitArea = new JTextArea(3, 40);
        outfitArea.setFont(new Font("Georgia", Font.PLAIN, 13));
        outfitArea.setForeground(Color.GRAY);
        outfitArea.setLineWrap(true);
        outfitArea.setWrapStyleWord(true);
        outfitArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCC, 0xCC, 0xCC)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        final String outfitPlaceholder =
                "e.g. Deep red silk saree with a sleeveless blouse and open back...";
        outfitArea.setText(outfitPlaceholder);
        outfitArea.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (outfitArea.getText().equals(outfitPlaceholder)) {
                    outfitArea.setText("");
                    outfitArea.setForeground(ThemeConfig.DEEP_BLACK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (outfitArea.getText().isBlank()) {
                    outfitArea.setText(outfitPlaceholder);
                    outfitArea.setForeground(Color.GRAY);
                }
            }
        });

        JScrollPane outfitScroll = new JScrollPane(outfitArea);
        outfitScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        outfitScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        outfitScroll.setBorder(BorderFactory.createEmptyBorder());
        p.add(outfitScroll);

        p.add(Box.createVerticalStrut(28));

        // -- Q3: Extra preferences ------------------------------------
        p.add(sectionLabel("3.  Any other preferences?  (optional)"));
        p.add(Box.createVerticalStrut(4));
        p.add(hintLabel("e.g. prefer gold over silver, no earrings, budget under Rs. 50,000..."));
        p.add(Box.createVerticalStrut(8));

        notesField = styledTextField(
                "e.g. I prefer minimal jewellery, rose gold only...");
        p.add(notesField);

        p.add(Box.createVerticalStrut(32));

        // -- Curate button --------------------------------------------
        curateBtn = buildDarkButton("CURATE MY LOOK  \u2728");
        curateBtn.addActionListener(e -> fetchRecommendation());
        p.add(curateBtn);

        p.add(Box.createVerticalStrut(10));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Georgia", Font.ITALIC, 12));
        statusLabel.setForeground(ThemeConfig.GOLD);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(statusLabel);

        p.add(Box.createVerticalStrut(28));

        // -- Results box ----------------------------------------------
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBackground(new Color(0xFA, 0xF8, 0xF2));
        resultsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.GOLD, 1, true),
                BorderFactory.createEmptyBorder(20, 22, 20, 22)));
        resultsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel resultsTitle = new JLabel("YOUR CURATED LOOK");
        resultsTitle.setFont(new Font("Arial", Font.BOLD, 10));
        resultsTitle.setForeground(ThemeConfig.GOLD);
        resultsPanel.add(resultsTitle, BorderLayout.NORTH);

        resultArea = new JTextArea(12, 40);
        resultArea.setFont(new Font("Georgia", Font.PLAIN, 14));
        resultArea.setForeground(ThemeConfig.DEEP_BLACK);
        resultArea.setBackground(new Color(0xFA, 0xF8, 0xF2));
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setEditable(false);
        resultArea.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        resultArea.setText("Describe your outfit above and Gemini will curate\na personalised jewellery bundle just for you.");

        JScrollPane rScroll = new JScrollPane(resultArea);
        rScroll.setBorder(BorderFactory.createEmptyBorder());
        resultsPanel.add(rScroll, BorderLayout.CENTER);

        p.add(resultsPanel);

        return p;
    }

    // ── Action ────────────────────────────────────────────────────────────

    private void fetchRecommendation() {
        String event  = (String) eventCombo.getSelectedItem();
        String outfit = getAreaValue(outfitArea,
                "e.g. Deep red silk saree with a sleeveless blouse and open back...");
        String notes  = getFieldValue(notesField,
                "e.g. I prefer minimal jewellery, rose gold only...");

        curateBtn.setEnabled(false);
        statusLabel.setText("Gemini is curating your look...");
        resultArea.setText("");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override protected String doInBackground() {
                return AIStylistController.getRecommendation(event, outfit, notes);
            }
            @Override protected void done() {
                try {
                    String raw     = get();
                    String cleaned = cleanMarkdown(raw);
                    resultArea.setText(cleaned);
                    resultArea.setCaretPosition(0);
                    statusLabel.setText("Your personalised look is ready.");
                } catch (Exception ex) {
                    resultArea.setText("Error: " + ex.getMessage());
                    statusLabel.setText("Something went wrong.");
                }
                curateBtn.setEnabled(true);
            }
        };
        worker.execute();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 13));
        l.setForeground(ThemeConfig.DEEP_BLACK);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel hintLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.PLAIN, 11));
        l.setForeground(new Color(0x99, 0x99, 0x99));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField styledTextField(String placeholder) {
        JTextField f = new JTextField(placeholder);
        f.setFont(new Font("Georgia", Font.ITALIC, 13));
        f.setForeground(Color.GRAY);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCC, 0xCC, 0xCC)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) {
                    f.setText("");
                    f.setFont(new Font("Georgia", Font.PLAIN, 13));
                    f.setForeground(ThemeConfig.DEEP_BLACK);
                }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isBlank()) {
                    f.setText(placeholder);
                    f.setFont(new Font("Georgia", Font.ITALIC, 13));
                    f.setForeground(Color.GRAY);
                }
            }
        });
        return f;
    }

    private JButton buildDarkButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBackground(ThemeConfig.DEEP_BLACK);
        btn.setForeground(ThemeConfig.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(13, 32, 13, 32));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(ThemeConfig.GOLD);
                    btn.setForeground(ThemeConfig.DEEP_BLACK);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(ThemeConfig.DEEP_BLACK);
                btn.setForeground(ThemeConfig.WHITE);
            }
        });
        return btn;
    }

    private String getFieldValue(JTextField f, String placeholder) {
        String v = f.getText().trim();
        return (v.isBlank() || v.equals(placeholder)) ? "" : v;
    }

    private String getAreaValue(JTextArea a, String placeholder) {
        String v = a.getText().trim();
        return (v.isBlank() || v.equals(placeholder)) ? "" : v;
    }

    private String cleanMarkdown(String text) {
        if (text == null) return "";
        return text
                .replaceAll("[\\u2610\\u2611\\u2612\\u25A1\\u25A0\\u2713\\u2714\\u2717\\u2718]", "")
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("__(.+?)__", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("_(.+?)_", "$1")
                .replaceAll("(?m)^#{1,6}\\s*", "")
                .replaceAll("(?m)^[\\-\\*]\\s+", "  - ")
                .replaceAll("(?m)(\\n\\s*){3,}", "\n\n")
                .replaceAll("(?m)^(\\d+\\.)", "\n$1")
                .trim();
    }
}
