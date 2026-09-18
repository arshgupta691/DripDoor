package com.dripdoor.view;

import com.dripdoor.config.AppConfig;
import com.dripdoor.config.ThemeConfig;
import com.dripdoor.model.Order;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 10-minute dispatcher dialog.
 * Uses a SwingWorker to simulate the curation progress bar (#DD-LUX-XXXX).
 *
 * Status progression:
 *   PENDING  -> set immediately on order placement (by OrderController)
 *   CURATING -> set when startDispatch() is called (start of progress bar)
 *   DISPATCHED -> set when progress bar reaches 50%
 *   DELIVERED  -> set when progress bar completes (100%)
 */
public class DispatcherView extends JDialog {

    private final Order      order;
    private JProgressBar     progressBar;
    private JLabel           statusLabel;
    private JLabel           timeLabel;
    private JButton          closeBtn;

    private static final List<String> STAGES = List.of(
            "Authenticating your order...",
            "Selecting premium pieces...",
            "Quality inspection in progress...",
            "Curating your Drip...",
            "Packaging in signature DripDoor box...",
            "Dispatching to carrier...",
            "Your Drip is on its way! \u2746"
    );

    public DispatcherView(JFrame parent, Order order) {
        super(parent, "DripDoor \u2014 Curating Your Drip", false);
        this.order = order;
        initUI();
    }

    private void initUI() {
        setSize(500, 380);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel root = new JPanel();
        root.setBackground(ThemeConfig.WHITE);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(40, 48, 36, 48));
        setContentPane(root);

        // Header
        JLabel header = new JLabel("Curating your Drip...");
        header.setFont(ThemeConfig.HEADER_FONT.deriveFont(20f));
        header.setForeground(ThemeConfig.DEEP_BLACK);
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(header);

        root.add(Box.createVerticalStrut(6));

        JLabel orderIdLabel = new JLabel("Order ID: #" + order.getOrderId());
        orderIdLabel.setFont(ThemeConfig.UI_BOLD);
        orderIdLabel.setForeground(ThemeConfig.GOLD);
        orderIdLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(orderIdLabel);

        root.add(Box.createVerticalStrut(30));

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(false);
        progressBar.setForeground(ThemeConfig.GOLD);
        progressBar.setBackground(ThemeConfig.BORDER);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setBorderPainted(false);
        root.add(progressBar);

        root.add(Box.createVerticalStrut(14));

        // Status label
        statusLabel = new JLabel("Preparing...");
        statusLabel.setFont(ThemeConfig.UI_FONT.deriveFont(Font.ITALIC));
        statusLabel.setForeground(Color.DARK_GRAY);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(statusLabel);

        root.add(Box.createVerticalStrut(8));

        // Time remaining
        timeLabel = new JLabel("Estimated: 10 min");
        timeLabel.setFont(ThemeConfig.SMALL_FONT);
        timeLabel.setForeground(Color.GRAY);
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(timeLabel);

        root.add(Box.createVerticalGlue());

        // Animated dots decoration
        JLabel deco = new JLabel("\u25e6  \u25e6  \u25e6");
        deco.setFont(ThemeConfig.SUB_FONT.deriveFont(22f));
        deco.setForeground(ThemeConfig.GOLD);
        deco.setAlignmentX(Component.CENTER_ALIGNMENT);
        root.add(deco);

        root.add(Box.createVerticalStrut(20));

        // Close (disabled until done)
        closeBtn = new JButton("Close");
        closeBtn.setFont(ThemeConfig.SMALL_FONT);
        closeBtn.setEnabled(false);
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> dispose());
        root.add(closeBtn);
    }

    /** Starts the SwingWorker that drives the dispatch simulation. */
    public void startDispatch() {

        // Step 1: Immediately mark as CURATING so the timeline reflects progress
        com.dripdoor.controller.OrderController.updateStatus(
                order, Order.Status.CURATING);

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {
                int totalSeconds = AppConfig.DISPATCH_SECONDS;
                for (int elapsed = 0; elapsed <= totalSeconds; elapsed++) {
                    if (isCancelled()) break;
                    publish(elapsed);
                    Thread.sleep(1000);
                }
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int elapsed = chunks.get(chunks.size() - 1);
                int percent = (elapsed * 100) / AppConfig.DISPATCH_SECONDS;
                progressBar.setValue(percent);

                // Step 2: Flip to DISPATCHED at 50% progress
                if (percent >= 50 && order.getStatus() == Order.Status.CURATING) {
                    com.dripdoor.controller.OrderController.updateStatus(
                            order, Order.Status.DISPATCHED);
                }

                // Stage label
                int stageIdx = Math.min((percent / 15), STAGES.size() - 1);
                statusLabel.setText(STAGES.get(stageIdx));

                // Time remaining
                int remaining = AppConfig.DISPATCH_SECONDS - elapsed;
                int mins = remaining / 60;
                int secs = remaining % 60;
                timeLabel.setText(String.format("Estimated: %d min %02d sec remaining", mins, secs));
            }

            @Override
            protected void done() {
                progressBar.setValue(100);
                statusLabel.setText("Your Drip has been delivered! \u2746");
                timeLabel.setText("Successfully delivered");
                closeBtn.setEnabled(true);
                // Step 3: Mark DELIVERED when timer completes
                com.dripdoor.controller.OrderController.updateStatus(
                        order, Order.Status.DELIVERED);
            }
        };

        worker.execute();
    }
}
