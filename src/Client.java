package src;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Client {
    private static final Color mainPainelBackgroundColor = new Color(24, 31, 44);
    private static final Color chatBackgroundColor = new Color(32, 41, 59);

    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private JFrame frame = new JFrame("Seega");
    private JTextArea chatArea = new JTextArea(20, 40);
    private JTextField inputField = new JTextField(40);

    public Client() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Recebe o ID do cliente do servidor
            String idMessage = in.readLine();
            int clientId = 0;
            if (idMessage != null && idMessage.startsWith("ID:")) {
                clientId = Integer.parseInt(idMessage.substring(3));
            }

            // Configuração visual do chatArea
            chatArea.setEditable(false);
            chatArea.setBackground(chatBackgroundColor);
            chatArea.setForeground(Color.WHITE);
            chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
            chatArea.setBorder(BorderFactory.createLineBorder(new Color(180, 200, 230)));

            // ScrollPane com padding
            JScrollPane scrollPane = new JScrollPane(chatArea);

            // Campo de entrada com placeholder e borda arredondada
            inputField.setFont(new Font("SansSerif", Font.PLAIN, 14));
            inputField.setBackground(new Color(245, 250, 255));
            inputField.setForeground(Color.GRAY);
            inputField.setText("Digite sua mensagem...");

            inputField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(180, 200, 230), 1, true), // true = borda arredondada
                    new EmptyBorder(5, 10, 5, 10)));

            // Simula placeholder
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

            // Painel principal com padding
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
            mainPanel.setBackground(mainPainelBackgroundColor);
            mainPanel.add(scrollPane, BorderLayout.CENTER);
            mainPanel.add(inputField, BorderLayout.SOUTH);

            frame.setTitle("Seega - Cliente " + clientId);
            frame.setContentPane(mainPanel);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null); // centraliza a janela
            frame.setVisible(true);

            new Thread(this::receiveMessages).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                chatArea.append(msg + "\n");
            }
        } catch (IOException e) {
            System.out.println("Conexão encerrada.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
