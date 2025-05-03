package src;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;

public class ClientUI {
    private JFrame frame = new JFrame("Seega");
    private JTextArea chatArea = new JTextArea(20, 40);
    private JTextField inputField = new JTextField(40);

    public ClientUI(int clientId, PrintWriter out) {
        frame.setTitle("Seega - Cliente " + clientId);

        chatArea.setEditable(false);
        chatArea.setBackground(new Color(32, 41, 59));
        chatArea.setForeground(Color.WHITE);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        chatArea.setBorder(BorderFactory.createLineBorder(new Color(180, 200, 230)));

        JScrollPane scrollPane = new JScrollPane(chatArea);

        inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        inputField.setBackground(new Color(245, 250, 255));
        inputField.setForeground(Color.GRAY);
        inputField.setText("Digite sua mensagem...");
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 200, 230), 1, true),
                new EmptyBorder(5, 10, 5, 10)));

        inputField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (inputField.getText().equals("Digite sua mensagem...")) {
                    inputField.setText("");
                    inputField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (inputField.getText().isEmpty()) {
                    inputField.setText("Digite sua mensagem...");
                    inputField.setForeground(Color.GRAY);
                }
            }
        });

        inputField.addActionListener(e -> {
            String msg = inputField.getText();
            if (!msg.trim().isEmpty() && !msg.equals("Digite sua mensagem...")) {
                out.println(msg);
                inputField.setText("");
            }
        });

        // Painel do jogo
        JPanel gamePanel = new JPanel(new GridLayout(5, 5, 5, 5));
        gamePanel.setPreferredSize(new Dimension(600, 600));
        gamePanel.setBackground(new Color(180, 200, 230));

        JButton[][] boardButtons = new JButton[5][5];
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(100, 100));
                button.setBackground(Color.WHITE);
                button.setFocusPainted(false);

                final int r = row;
                final int c = col;
                button.addActionListener(e -> {
                    System.out.println("Célula clicada: (" + r + "," + c + ")");
                });

                boardButtons[row][col] = button;
                gamePanel.add(button);
            }
        }

        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setPreferredSize(new Dimension(800, 200));
        chatPanel.setBackground(new Color(32, 41, 59));
        chatPanel.add(scrollPane, BorderLayout.CENTER);
        chatPanel.add(inputField, BorderLayout.SOUTH);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(24, 31, 44));
        mainPanel.add(gamePanel, BorderLayout.CENTER);
        mainPanel.add(chatPanel, BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void appendMessage(String msg) {
        chatArea.append(msg + "\n");
    }
}
