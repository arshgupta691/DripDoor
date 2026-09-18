package com.dripdoor.view;

import com.dripdoor.config.ThemeConfig;
import com.dripdoor.model.Order;
import com.dripdoor.model.SavedAddress;
import com.dripdoor.service.AddressService;
import com.dripdoor.service.AuthService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

/**
 * Modal dialog that lets the user:
 *   a) Pick a previously saved address, OR
 *   b) Enter a new address (with option to save it)
 *
 * Shows saved-address panel first if the user has any saved.
 * Call isConfirmed() after showing; if true, address is set on the order.
 */
public class AddressDialog extends JDialog {

    private boolean confirmed = false;
    private SavedAddress selectedSaved = null;

    // New address fields
    private JTextField labelField, nameField, phoneField,
                       line1Field, line2Field, cityField,
                       stateField, pincodeField;
    private JCheckBox  saveCheckBox;

    // Panels for card switch
    private JPanel cardPanel;
    private CardLayout cards;
    private static final String CARD_SELECT = "SELECT";
    private static final String CARD_NEW    = "NEW";

    private final List<SavedAddress> saved;

    public AddressDialog(JFrame parent) {
        super(parent, "Delivery Address", true);
        this.saved = loadSaved();
        setSize(500, saved.isEmpty() ? 560 : 480);
        setLocationRelativeTo(parent);
        setResizable(false);
        initUI();
    }

    private List<SavedAddress> loadSaved() {
        var user = AuthService.getCurrentUser();
        if (user == null) return List.of();
        // Load from Firebase on first open (populates user.getSavedAddresses())
        if (user.getSavedAddresses().isEmpty()) {
            AddressService.loadAddresses(user);
        }
        return user.getSavedAddresses();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeConfig.WHITE);
        setContentPane(root);

        // Title bar
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(ThemeConfig.WHITE);
        titleBar.setBorder(new EmptyBorder(28, 36, 16, 36));

        JLabel title = new JLabel("Where should we deliver?");
        title.setFont(new Font("Georgia", Font.BOLD, 20));
        title.setForeground(ThemeConfig.DEEP_BLACK);
        titleBar.add(title, BorderLayout.WEST);
        root.add(titleBar, BorderLayout.NORTH);

        // Card panel
        cards     = new CardLayout();
        cardPanel = new JPanel(cards);
        cardPanel.setBackground(ThemeConfig.WHITE);

        if (!saved.isEmpty()) {
            cardPanel.add(buildSelectPanel(), CARD_SELECT);
        }
        cardPanel.add(buildNewPanel(), CARD_NEW);

        // Start on select panel if saved addresses exist, else new
        cards.show(cardPanel, saved.isEmpty() ? CARD_NEW : CARD_SELECT);

        root.add(cardPanel, BorderLayout.CENTER);
    }

    // ── Panel A: select from saved ─────────────────────────────────────────

    private JPanel buildSelectPanel() {
        JPanel p = new JPanel();
        p.setBackground(ThemeConfig.WHITE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(0, 36, 28, 36));

        JLabel hint = new JLabel("Choose a saved address or add a new one.");
        hint.setFont(new Font("Arial", Font.PLAIN, 12));
        hint.setForeground(Color.GRAY);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(hint);
        p.add(Box.createVerticalStrut(18));

        // Address cards (radio-button style)
        ButtonGroup group = new ButtonGroup();
        for (SavedAddress addr : saved) {
            JPanel row = buildSavedRow(addr, group);
            p.add(row);
            p.add(Box.createVerticalStrut(10));
        }

        p.add(Box.createVerticalStrut(8));

        // "Add new address" link button
        JButton addNew = new JButton("+ Add a new address");
        addNew.setFont(new Font("Arial", Font.BOLD, 12));
        addNew.setForeground(ThemeConfig.GOLD);
        addNew.setBackground(ThemeConfig.WHITE);
        addNew.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        addNew.setFocusPainted(false);
        addNew.setContentAreaFilled(false);
        addNew.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addNew.setAlignmentX(Component.LEFT_ALIGNMENT);
        addNew.addActionListener(e -> {
            setSize(500, 600);
            cards.show(cardPanel, CARD_NEW);
        });
        p.add(addNew);

        p.add(Box.createVerticalGlue());
        p.add(Box.createVerticalStrut(20));

        // Bottom buttons
        p.add(buildBtnRow(() -> {
            if (selectedSaved == null) {
                JOptionPane.showMessageDialog(this,
                        "Please select an address.", "No Address Selected",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            confirmed = true;
            dispose();
        }));

        return p;
    }

    private JPanel buildSavedRow(SavedAddress addr, ButtonGroup group) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(new Color(0xFA, 0xF8, 0xF2));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeConfig.BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JRadioButton radio = new JRadioButton();
        radio.setBackground(new Color(0xFA, 0xF8, 0xF2));
        group.add(radio);

        JPanel text = new JPanel();
        text.setBackground(new Color(0xFA, 0xF8, 0xF2));
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel labelLbl = new JLabel(addr.getLabel());
        labelLbl.setFont(new Font("Arial", Font.BOLD, 12));
        labelLbl.setForeground(ThemeConfig.DEEP_BLACK);

        JLabel detailLbl = new JLabel("<html>" +
                addr.getName() + " · " + addr.getPhone() + "<br>" +
                addr.getLine1() + (addr.getLine2() != null && !addr.getLine2().isBlank()
                        ? ", " + addr.getLine2() : "") + "<br>" +
                addr.getCity() + ", " + addr.getState() + " - " + addr.getPincode() +
                "</html>");
        detailLbl.setFont(new Font("Arial", Font.PLAIN, 11));
        detailLbl.setForeground(Color.GRAY);

        text.add(labelLbl);
        text.add(Box.createVerticalStrut(3));
        text.add(detailLbl);

        row.add(radio, BorderLayout.WEST);
        row.add(text,  BorderLayout.CENTER);

        // Click anywhere on row to select
        var selectAction = new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                radio.setSelected(true);
                selectedSaved = addr;
                row.setBackground(new Color(0xFF, 0xF8, 0xE8));
                text.setBackground(new Color(0xFF, 0xF8, 0xE8));
                detailLbl.setBackground(new Color(0xFF, 0xF8, 0xE8));
                labelLbl.setBackground(new Color(0xFF, 0xF8, 0xE8));
                row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeConfig.GOLD, 1, true),
                        BorderFactory.createEmptyBorder(12, 14, 12, 14)));
            }
        };
        row.addMouseListener(selectAction);
        radio.addActionListener(e -> {
            selectedSaved = addr;
        });

        return row;
    }

    // ── Panel B: new address form ──────────────────────────────────────────

    private JPanel buildNewPanel() {
        // Inner scrollable content panel
        JPanel inner = new JPanel();
        inner.setBackground(ThemeConfig.WHITE);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(new EmptyBorder(0, 36, 16, 36));

        // Back link (only shown if saved addresses exist)
        if (!saved.isEmpty()) {
            JButton back = new JButton("← Back to saved addresses");
            back.setFont(new Font("Arial", Font.PLAIN, 11));
            back.setForeground(ThemeConfig.GOLD);
            back.setBackground(ThemeConfig.WHITE);
            back.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            back.setFocusPainted(false);
            back.setContentAreaFilled(false);
            back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            back.setAlignmentX(Component.LEFT_ALIGNMENT);
            back.addActionListener(e -> {
                setSize(500, 480);
                cards.show(cardPanel, CARD_SELECT);
            });
            inner.add(back);
        }

        JLabel hint = new JLabel("Enter your delivery details.");
        hint.setFont(new Font("Arial", Font.PLAIN, 12));
        hint.setForeground(Color.GRAY);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(hint);
        inner.add(Box.createVerticalStrut(14));

        labelField   = addField(inner, "Label *",          "e.g. Home, Office, Mom's place");
        nameField    = addField(inner, "Full Name *",       "e.g. Priya Sharma");
        phoneField   = addField(inner, "Phone Number *",    "10-digit mobile number");
        line1Field   = addField(inner, "Address Line 1 *",  "House / Flat / Building");
        line2Field   = addField(inner, "Address Line 2",    "Area / Colony (optional)");
        cityField    = addField(inner, "City *",            "e.g. Mumbai");
        stateField   = addField(inner, "State *",           "e.g. Maharashtra");
        pincodeField = addField(inner, "Pincode *",         "6-digit pincode");

        inner.add(Box.createVerticalStrut(8));

        saveCheckBox = new JCheckBox("Save this address to my account");
        saveCheckBox.setFont(new Font("Arial", Font.PLAIN, 12));
        saveCheckBox.setBackground(ThemeConfig.WHITE);
        saveCheckBox.setForeground(ThemeConfig.DEEP_BLACK);
        saveCheckBox.setSelected(true);
        saveCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(saveCheckBox);

        // Wrap inner panel in a scroll pane
        JScrollPane scrollPane = new JScrollPane(inner,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(ThemeConfig.WHITE);
        scrollPane.getViewport().setBackground(ThemeConfig.WHITE);

        // Outer wrapper panel (BorderLayout) holds scroll area + fixed button row
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(ThemeConfig.WHITE);
        outer.add(scrollPane, BorderLayout.CENTER);

        JPanel btnWrapper = new JPanel();
        btnWrapper.setBackground(ThemeConfig.WHITE);
        btnWrapper.setLayout(new BoxLayout(btnWrapper, BoxLayout.Y_AXIS));
        btnWrapper.setBorder(new EmptyBorder(8, 36, 16, 36));
        btnWrapper.add(buildBtnRow(this::handleNewConfirm));
        outer.add(btnWrapper, BorderLayout.SOUTH);

        return outer;
    }

    private JTextField addField(JPanel parent, String label, String hint) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 11));
        lbl.setForeground(new Color(0x55, 0x55, 0x55));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(lbl);
        parent.add(Box.createVerticalStrut(3));

        JTextField field = new JTextField();
        field.setFont(new Font("Georgia", Font.PLAIN, 13));
        field.setToolTipText(hint);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCC, 0xCC, 0xCC)),
                BorderFactory.createEmptyBorder(5, 9, 5, 9)));
        parent.add(field);
        parent.add(Box.createVerticalStrut(8));
        return field;
    }

    private JPanel buildBtnRow(Runnable onConfirm) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row.setBackground(ThemeConfig.WHITE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JButton cancel = new JButton("Cancel");
        cancel.setFocusPainted(false);
        cancel.addActionListener(e -> dispose());
        row.add(cancel);

        JButton confirm = new JButton("CONFIRM ADDRESS");
        confirm.setFont(new Font("Arial", Font.BOLD, 12));
        confirm.setBackground(ThemeConfig.DEEP_BLACK);
        confirm.setForeground(ThemeConfig.WHITE);
        confirm.setFocusPainted(false);
        confirm.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        confirm.setOpaque(true);
        confirm.addActionListener(e -> onConfirm.run());
        row.add(confirm);

        return row;
    }

    private void handleNewConfirm() {
        if (labelField.getText().isBlank() || nameField.getText().isBlank()
                || phoneField.getText().isBlank() || line1Field.getText().isBlank()
                || cityField.getText().isBlank() || stateField.getText().isBlank()
                || pincodeField.getText().isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in all required fields (marked with *).",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!phoneField.getText().matches("\\d{10}")) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid 10-digit phone number.",
                    "Invalid Phone", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!pincodeField.getText().matches("\\d{6}")) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid 6-digit pincode.",
                    "Invalid Pincode", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Build the SavedAddress from form fields
        selectedSaved = new SavedAddress(
                labelField.getText().trim(),
                nameField.getText().trim(),
                phoneField.getText().trim(),
                line1Field.getText().trim(),
                line2Field.getText().trim(),
                cityField.getText().trim(),
                stateField.getText().trim(),
                pincodeField.getText().trim()
        );

        // Save to Firebase if checkbox is ticked
        if (saveCheckBox.isSelected() && AuthService.getCurrentUser() != null) {
            AddressService.saveAddress(AuthService.getCurrentUser(), selectedSaved);
        }

        confirmed = true;
        dispose();
    }

    /** Applies the chosen address onto the order. */
    public void applyToOrder(Order order) {
        if (selectedSaved != null) {
            selectedSaved.applyToOrder(order);
        }
    }

    public boolean isConfirmed() { return confirmed; }
}
