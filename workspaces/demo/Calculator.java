import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;

public class Calculator extends JFrame {

    private JLabel expressionLabel;
    private JTextField displayField;
    private double num1 = 0;
    private String operator = "";
    private boolean startNewNumber = true;
    private DecimalFormat df = new DecimalFormat("#.##########");

    // Colors
    private final Color COLOR_BG = new Color(28, 28, 30);
    private final Color COLOR_DISPLAY_BG = new Color(44, 44, 46);
    private final Color COLOR_BTN_NUM = new Color(58, 58, 60);
    private final Color COLOR_BTN_NUM_HOVER = new Color(72, 72, 74);
    private final Color COLOR_BTN_OP = new Color(255, 159, 10);
    private final Color COLOR_BTN_OP_HOVER = new Color(255, 179, 64);
    private final Color COLOR_BTN_SPECIAL = new Color(99, 99, 102);
    private final Color COLOR_BTN_SPECIAL_HOVER = new Color(142, 142, 147);
    private final Color COLOR_TEXT = Color.WHITE;

    public Calculator() {
        setTitle("Premium Calculator");
        setSize(360, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout(10, 10));

        // Display Panel
        JPanel displayPanel = new JPanel(new BorderLayout(5, 5));
        displayPanel.setBackground(COLOR_DISPLAY_BG);
        displayPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        expressionLabel = new JLabel(" ");
        expressionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        expressionLabel.setForeground(new Color(174, 174, 178));
        expressionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        displayPanel.add(expressionLabel, BorderLayout.NORTH);

        displayField = new JTextField("0");
        displayField.setFont(new Font("Segoe UI", Font.BOLD, 36));
        displayField.setForeground(COLOR_TEXT);
        displayField.setBackground(COLOR_DISPLAY_BG);
        displayField.setBorder(null);
        displayField.setEditable(false);
        displayField.setHorizontalAlignment(SwingConstants.RIGHT);
        displayPanel.add(displayField, BorderLayout.CENTER);

        add(displayPanel, BorderLayout.NORTH);

        // Buttons Panel
        JPanel buttonsPanel = new JPanel(new GridLayout(5, 4, 8, 8));
        buttonsPanel.setBackground(COLOR_BG);
        buttonsPanel.setBorder(new EmptyBorder(10, 15, 15, 15));

        String[] buttonLabels = {
            "C", "⌫", "%", "÷",
            "7", "8", "9", "×",
            "4", "5", "6", "-",
            "1", "2", "3", "+",
            "±", "0", ".", "="
        };

        for (String label : buttonLabels) {
            JButton button = createStyledButton(label);
            buttonsPanel.add(button);
        }

        add(buttonsPanel, BorderLayout.CENTER);

        // Keyboard integration
        setupKeyboardInput();

        // Prevent focus on click to keep keyboard input working
        setFocusable(true);
        requestFocusInWindow();
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 20));
        button.setForeground(COLOR_TEXT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);

        // Determine colors based on button type
        Color baseColor;
        Color hoverColor;

        if (text.matches("[0-9.]")) {
            baseColor = COLOR_BTN_NUM;
            hoverColor = COLOR_BTN_NUM_HOVER;
        } else if (text.matches("[÷×\\-+=]")) {
            baseColor = COLOR_BTN_OP;
            hoverColor = COLOR_BTN_OP_HOVER;
        } else {
            baseColor = COLOR_BTN_SPECIAL;
            hoverColor = COLOR_BTN_SPECIAL_HOVER;
        }

        button.setBackground(baseColor);

        // Hover & Press effects
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseColor);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(baseColor.darker());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.getBounds().contains(e.getPoint())) {
                    button.setBackground(hoverColor);
                } else {
                    button.setBackground(baseColor);
                }
            }
        });

        // Action logic
        button.addActionListener(e -> {
            handleInput(text);
            requestFocusInWindow(); // Keep keyboard focus
        });

        return button;
    }

    private void handleInput(String input) {
        if (input.matches("[0-9]")) {
            if (startNewNumber || displayField.getText().equals("0")) {
                displayField.setText(input);
                startNewNumber = false;
            } else {
                displayField.setText(displayField.getText() + input);
            }
        } else if (input.equals(".")) {
            if (startNewNumber) {
                displayField.setText("0.");
                startNewNumber = false;
            } else if (!displayField.getText().contains(".")) {
                displayField.setText(displayField.getText() + ".");
            }
        } else if (input.equals("C")) {
            num1 = 0;
            operator = "";
            displayField.setText("0");
            expressionLabel.setText(" ");
            startNewNumber = true;
        } else if (input.equals("⌫")) {
            String current = displayField.getText();
            if (current.length() > 0 && !startNewNumber) {
                if (current.length() == 1 || (current.length() == 2 && current.startsWith("-"))) {
                    displayField.setText("0");
                    startNewNumber = true;
                } else {
                    displayField.setText(current.substring(0, current.length() - 1));
                }
            }
        } else if (input.equals("±")) {
            double value = Double.parseDouble(displayField.getText());
            if (value != 0) {
                displayField.setText(df.format(-value));
            }
        } else if (input.equals("%")) {
            double value = Double.parseDouble(displayField.getText());
            displayField.setText(df.format(value / 100.0));
            startNewNumber = true;
        } else if (input.matches("[÷×\\-+]")) {
            if (!operator.isEmpty() && !startNewNumber) {
                calculate();
            }
            num1 = Double.parseDouble(displayField.getText());
            operator = input;
            expressionLabel.setText(df.format(num1) + " " + operator);
            startNewNumber = true;
        } else if (input.equals("=")) {
            if (!operator.isEmpty()) {
                calculate();
                operator = "";
                expressionLabel.setText(" ");
            }
        }
    }

    private void calculate() {
        double num2 = Double.parseDouble(displayField.getText());
        double result = 0;
        boolean valid = true;

        switch (operator) {
            case "+":
                result = num1 + num2;
                break;
            case "-":
                result = num1 - num2;
                break;
            case "×":
                result = num1 * num2;
                break;
            case "÷":
                if (num2 != 0) {
                    result = num1 / num2;
                } else {
                    valid = false;
                }
                break;
        }

        if (valid) {
            displayField.setText(df.format(result));
        } else {
            displayField.setText("Error");
        }
        startNewNumber = true;
    }

    private void setupKeyboardInput() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    char c = e.getKeyChar();
                    int code = e.getKeyCode();

                    if (Character.isDigit(c)) {
                        handleInput(String.valueOf(c));
                        return true;
                    } else if (c == '.') {
                        handleInput(".");
                        return true;
                    } else if (c == '+') {
                        handleInput("+");
                        return true;
                    } else if (c == '-') {
                        handleInput("-");
                        return true;
                    } else if (c == '*') {
                        handleInput("×");
                        return true;
                    } else if (c == '/') {
                        handleInput("÷");
                        return true;
                    } else if (code == KeyEvent.VK_ENTER || c == '=') {
                        handleInput("=");
                        return true;
                    } else if (code == KeyEvent.VK_BACK_SPACE) {
                        handleInput("⌫");
                        return true;
                    } else if (code == KeyEvent.VK_ESCAPE) {
                        handleInput("C");
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Set native look and feel or cross platform
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Keep default if system fails
            }
            new Calculator().setVisible(true);
        });
    }
}
