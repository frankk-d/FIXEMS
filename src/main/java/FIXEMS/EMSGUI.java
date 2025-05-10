package FIXEMS;


import quickfix.field.OrdStatus;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

public class EMSGUI extends JFrame {
    private AiEMSApplication application;
    private OrderTableModel tableModel;
    private JTable table;
    private JButton btnReplace, btnAcknowledge, btnCancel, btnFill;
    private JTextArea consoleTextArea;

    public EMSGUI(AiEMSApplication application) {
        this.application = application;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("FIX EMS");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        // ========== Button Styling with Hover Effects ==========
        Color buttonGray = new Color(255, 253, 253);
        Color darkGrey = new Color(239, 239, 239);
        // ========== Button Panel ==========
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setBackground(buttonGray);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10)); // Top spacing added

        btnReplace = createHoverButton("Accept Replace Request", buttonGray, darkGrey);
        btnAcknowledge = createHoverButton("Acknowledge Order", buttonGray, darkGrey);
        btnCancel = createHoverButton("Accept Cancel Request", buttonGray, darkGrey);
        btnFill = createHoverButton("Fill Order", buttonGray, darkGrey);

        // Set uniform font while keeping default styling
        Font buttonFont = new Font("SansSerif", Font.PLAIN, 16);
        btnReplace.setFont(buttonFont);
        btnAcknowledge.setFont(buttonFont);
        btnCancel.setFont(buttonFont);
        btnFill.setFont(buttonFont);

        // Add components
        controlPanel.add(btnReplace);
        controlPanel.add(btnAcknowledge);
        controlPanel.add(btnCancel);
        controlPanel.add(btnFill);

        // ========== Table Configurat  ion ==========
        tableModel = new OrderTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // ========== Table Outline Enhancement ==========
        table.setShowGrid(true);
        table.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1)); // Always visible border
        // Table styling
        table.setFont(new Font("SansSerif", Font.PLAIN, 16));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 16));
        table.setRowHeight(28); // Improved row height
        table.setGridColor(new Color(60, 60, 60));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));

        JScrollPane scrollPane = new JScrollPane(table);
        // ========== Remove Table Border & Set Backgrounds ==========
        // Replace existing table border setup with:
        Border tableBorder = BorderFactory.createEmptyBorder(15, 15, 15, 15); // Padding only
        scrollPane.setBorder(tableBorder);
        scrollPane.getViewport().setBackground(buttonGray); // Set scroll pane background
        table.setBackground(buttonGray); // Set table background

        // ========== Set Global Background ==========
        getContentPane().setBackground(buttonGray); // Main window background
        scrollPane.setBorder(tableBorder);

        // ========== Layout Assembly ==========
        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(controlPanel, BorderLayout.CENTER);

        add(northContainer, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);


        // ========== Table Styling Adjustments ==========
        table.setBackground(UIManager.getColor("Table.background")); // Default background
        table.setForeground(UIManager.getColor("Table.foreground"));
        table.setGridColor(new Color(100, 100, 100)); // Darker grid lines

        // ========== Console Area Improvements ==========
        consoleTextArea = new JTextArea();
        consoleTextArea.setEditable(false);
        consoleTextArea.setFont(new Font("SansSerif", Font.PLAIN, 16)); // Uniform 16px font
        consoleTextArea.setBackground(new Color(245, 245, 245));

        JScrollPane consoleScrollPane = new JScrollPane(consoleTextArea);
        consoleScrollPane.setPreferredSize(new Dimension(0, 150));
        consoleScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(15, 0, 0, 0), // Top spacing
                BorderFactory.createTitledBorder("System Output")
        ));

        // ========== Status Bar ==========
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.SOUTH);

        // ========== Console Border Styling ==========
        Border consoleBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "System Output",
                TitledBorder.LEFT,
                TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", Font.BOLD, 16) // Match UI font size
        );
        consoleScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(15, 0, 0, 0),
                consoleBorder
        ));

        // Redirect system output streams
        redirectSystemStreams();

        // ========== Layout Assembly ==========
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(consoleScrollPane, BorderLayout.SOUTH);

        add(northContainer, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        // Add action listeners
        btnReplace.addActionListener(this::handleReplace);
        btnAcknowledge.addActionListener(this::handleAcknowledge);
        btnCancel.addActionListener(this::handleCancel);
        btnFill.addActionListener(this::handleFill);

        northContainer.setBackground(buttonGray); // Button panel container
        mainPanel.setBackground(buttonGray); // Central area
        consoleScrollPane.getViewport().setBackground(Color.WHITE); // Keep console white for contrast

        // Refresh data
        Timer timer = new Timer(1000, e -> updateTableData());
        timer.start();
    }

    private JButton createStyledButton(String text, Font font) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setFocusPainted(false);
        button.setBackground(new Color(240, 240, 240));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        return button;
    }

    private void handleReplace(ActionEvent e) {
        Order order = getSelectedOrder();
        if (order != null) {
            try {
                application.createCancelReplaceExecutionOrder(order);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Replace Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleAcknowledge(ActionEvent e) {
        Order order = getSelectedOrder();
        if (order != null) {
            try {
                application.createAcknowledgeExecutionOrder(order);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Acknowledge Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleCancel(ActionEvent e) {
        Order order = getSelectedOrder();
        if (order != null) {
            try {
                application.createCancelExecutionOrder(order);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cancel Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private JButton createHoverButton(String text, Color normal, Color hover) {
        JButton button = new JButton(text) {
            // Ensure background painting works with rounded borders
            @Override
            protected void paintComponent(Graphics g) {
                if (!isOpaque() && getBackground().getAlpha() < 255) {
                    super.paintComponent(g);
                    return;
                }
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                super.paintComponent(g2d);
                g2d.dispose();
            }
        };
        button.setFont(new Font("SansSerif", Font.PLAIN, 16));
        button.setFocusPainted(false);
        button.setBackground(normal);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setOpaque(false); // Required for custom painting
        button.setBorder(new RoundedBorder(15, Color.GRAY)); // Rounded border with 15px radius
        button.setContentAreaFilled(false); // Prevent default background fill

        // Hover effects
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(hover);
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            public void mouseExited(MouseEvent evt) {
                button.setBackground(normal);
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        return button;
    }
    private void handleFill(ActionEvent e) {
        Order order = getSelectedOrder();
        if (order == null) return;

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        Font inputFont = new Font("SansSerif", Font.PLAIN, 16);

        JLabel amountLabel = new JLabel("Amount to fill:");
        amountLabel.setFont(inputFont);
        JTextField amountField = new JTextField();
        amountField.setFont(inputFont);

        JLabel priceLabel = new JLabel("Price:");
        priceLabel.setFont(inputFont);
        JTextField priceField = new JTextField();
        priceField.setFont(inputFont);

        inputPanel.add(amountLabel);
        inputPanel.add(amountField);
        inputPanel.add(priceLabel);
        inputPanel.add(priceField);

        int result = JOptionPane.showConfirmDialog(
                this, inputPanel, "Fill Order Details",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                long amount = Long.parseLong(amountField.getText());
                double price = Double.parseDouble(priceField.getText());
                application.createFillExecutionOrder(order, amount, price);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid numeric input", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Fill Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // In EMSGUI.java
    private void updateTableData() {
        // Preserve selection
        Order selectedOrder = getSelectedOrder();
        String preservedClOrdId = (selectedOrder != null) ? selectedOrder.getClOrdID() : null;

        // Update model
        List<Order> orders = application.getOrders().getOrderList();
        tableModel.setOrders(orders);

        // Restore selection
        if (preservedClOrdId != null) {
            for (int i = 0; i < orders.size(); i++) {
                if (orders.get(i).getClOrdID().equals(preservedClOrdId)) {
                    table.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
    }
    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(final int b) throws IOException {
                SwingUtilities.invokeLater(() -> {
                    consoleTextArea.append(String.valueOf((char) b));
                    consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
                });
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                final String text = new String(b, off, len);
                SwingUtilities.invokeLater(() -> {
                    consoleTextArea.append(text);
                    consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
                });
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }
    // Update getSelectedOrder() to handle model-row conversion
    private Order getSelectedOrder() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        return tableModel.orders.get(modelRow);
    }

    private void scrollToVisible(int rowIndex) {
        table.scrollRectToVisible(table.getCellRect(rowIndex, 0, true));
    }
    private static class OrderTableModel extends AbstractTableModel {
        private List<Order> orders;
        private final String[] columnNames = {
                "Status", "Symbol", "Order ID", "Orig Order ID", "Type", "Quantity",
                "Remaining", "Order Type", "Limit Price", "Stop Price","Avg Price"
        };

        public void setOrders(List<Order> orders) {
            this.orders = orders;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return orders != null ? orders.size() : 0;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            Order order = orders.get(row);
            switch (column) {
                case 0: return getStatus(order);
                case 1: return order.getSymbol();
                case 2: return order.getClOrdID();
                case 3: return order.getOrigClOrdID(); // New column: OrigClOrdID
                case 4: return getOrderType(order);
                case 5: return order.getOrderQty();
                case 6: return order.getLeavesQty();
                case 7: return String.valueOf(order.getOrdType());
                case 8: return order.getPrice();       // New column: Limit Price
                case 9: return order.getStopPx();
                case 10: return order.getAvgPx();
                default: return null;
            }
        }
        private String getStatus(Order order) {
            if (order.getReceivedOrder()) return "RECEIVED";
            if (order.getReceivedCancelRequest()) return "CANCEL_REQ";
            if (order.getReceivedReplaceRequest()) return "REPLACE_REQ";
            switch (order.getOrdStatus()) {
                case OrdStatus.NEW: return "NEW";
                case OrdStatus.PARTIALLY_FILLED: return "PARTIALLY_FILLED";
                case OrdStatus.FILLED: return "FILLED";
                case OrdStatus.CANCELED: return "CANCELED";
                case OrdStatus.REPLACED: return "REPLACED";
                default: return "UNKNOWN";
            }
        }

        private String getOrderType(Order order) {
            if (order.getOrigClOrdID() != null && !order.getOrigClOrdID().isEmpty())
                return "UNKNOWN";
            return order.getOrdStatus() == OrdStatus.CANCELED ? "CANCEL" : "NEW";
        }
    }

    // ========== Custom Rounded Border Class ==========
    private static class RoundedBorder implements Border {
        private final int radius;
        private final Color color;

        public RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);
            g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2d.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius + 1, radius + 1, radius + 1, radius + 1);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }


    // New helper method
    private JPanel createStatusPanel() {
        Color buttonGray = new Color(255, 253, 253);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        panel.setBackground(buttonGray);

        // Border with subtle top separation
        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        );
        panel.setBorder(border);

        JLabel statusLabel = new JLabel();
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));

        // Initial update and timer for refresh
        updateConnectionStatus(statusLabel);
        Timer statusTimer = new Timer(1000, e -> updateConnectionStatus(statusLabel));
        statusTimer.start();

        panel.add(statusLabel);
        return panel;
    }

    // New status update logic
    private void updateConnectionStatus(JLabel statusLabel) {
        boolean isConnected = application.getLogOn();
        String omsName = application.getConnectionName();

        String text = isConnected
                ? String.format("Connected OMS: %s", omsName)
                : "Disconnected. Please connect to an OMS.";

        statusLabel.setText(text);
        statusLabel.setForeground(isConnected ? new Color(0, 120, 0) : Color.RED);
    }
}