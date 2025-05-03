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
    private JLabel turnLabel = new JLabel("Aguardando início do jogo...");
    private JButton[][] boardButtons = new JButton[5][5];
    private Client client;
    private int clientId;
    private PrintWriter out;
    private boolean centerBlocked = true;

    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean isPositioningPhase = true;

    public ClientUI(int clientId, PrintWriter out, Client client) {
        this.clientId = clientId;
        this.out = out;
        this.client = client;

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

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(100, 100));
                button.setBackground(Color.WHITE);
                button.setFocusPainted(false);
                button.setFont(new Font("SansSerif", Font.BOLD, 24));

                // Marca o centro como bloqueado inicialmente
                if (row == 2 && col == 2) {
                    button.setBackground(Color.RED);
                    button.setEnabled(false);
                }

                final int r = row;
                final int c = col;
                button.addActionListener(e -> {
                    if (isPositioningPhase) {
                        // Fase de posicionamento (turnos 1-24)
                        if (button.getText().isEmpty() && !(centerBlocked && r == 2 && c == 2)) {
                            client.sendMove(r, c);
                        }
                    } else {
                        // Fase de movimentação (turno 25+)
                        String currentSymbol = (clientId == 1) ? "O" : "X";

                        if (selectedRow == -1 && selectedCol == -1) {
                            // Selecionar uma peça para mover
                            if (button.getText().equals(currentSymbol)) {
                                selectedRow = r;
                                selectedCol = c;
                                button.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
                            }
                        } else {
                            // Tentar mover a peça selecionada para esta posição
                            if (button.getText().isEmpty() && isValidMove(selectedRow, selectedCol, r, c)) {
                                // Enviar o movimento para o servidor
                                client.sendMove(selectedRow, selectedCol, r, c);
                            }
                            // Deselecionar independentemente do movimento ser válido ou não
                            boardButtons[selectedRow][selectedCol]
                                    .setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                            selectedRow = -1;
                            selectedCol = -1;
                        }
                    }
                });

                boardButtons[row][col] = button;
                gamePanel.add(button);
            }
        }

        // Painel de informações
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(new Color(32, 41, 59));
        infoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        turnLabel.setForeground(Color.WHITE);
        turnLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        infoPanel.add(turnLabel, BorderLayout.CENTER);

        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setPreferredSize(new Dimension(800, 200));
        chatPanel.setBackground(new Color(32, 41, 59));
        chatPanel.add(scrollPane, BorderLayout.CENTER);
        chatPanel.add(inputField, BorderLayout.SOUTH);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(24, 31, 44));
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(gamePanel, BorderLayout.CENTER);
        mainPanel.add(chatPanel, BorderLayout.SOUTH);

        frame.setContentPane(mainPanel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void updateCenterBlock(boolean blocked) {
        SwingUtilities.invokeLater(() -> {
            centerBlocked = blocked;
            JButton centerButton = boardButtons[2][2];
            if (blocked) {
                centerButton.setBackground(Color.RED);
                centerButton.setEnabled(false);
                centerButton.setText("");
            } else {
                centerButton.setBackground(Color.WHITE);
                centerButton.setEnabled(true);
            }
        });
    }

    public void appendMessage(String msg) {
        chatArea.append(msg + "\n");
    }

    public void updateTurnInfo(int currentPlayer, int turnNumber) {
        SwingUtilities.invokeLater(() -> {
            if (turnNumber >= 25) {
                isPositioningPhase = false;
            }

            // Limpar seleção quando o turno muda
            if (selectedRow != -1 && selectedCol != -1) {
                boardButtons[selectedRow][selectedCol].setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                selectedRow = -1;
                selectedCol = -1;
            }

            if (currentPlayer == clientId) {
                turnLabel.setText("SEU TURNO (Turno " + turnNumber + ")" +
                        (isPositioningPhase ? "" : " - Selecione uma peça para mover"));
                turnLabel.setForeground(Color.GREEN);
            } else {
                turnLabel.setText("Turno do oponente (Turno " + turnNumber + ")");
                turnLabel.setForeground(Color.RED);
            }

            // Habilitar/desabilitar botões conforme o turno
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    String currentSymbol = (clientId == 1) ? "O" : "X";
                    boolean isMyPiece = boardButtons[row][col].getText().equals(currentSymbol);

                    if (isPositioningPhase) {
                        boardButtons[row][col].setEnabled(
                                boardButtons[row][col].getText().isEmpty() &&
                                        !(centerBlocked && row == 2 && col == 2) &&
                                        currentPlayer == clientId);
                    } else {
                        boardButtons[row][col].setEnabled(
                                (isMyPiece || boardButtons[row][col].getText().isEmpty()) &&
                                        currentPlayer == clientId);
                    }
                }
            }
        });
    }

    // Método para verificar se um movimento é válido
    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        // Verifica se é uma peça do jogador atual
        String currentSymbol = (clientId == 1) ? "O" : "X";
        if (!boardButtons[fromRow][fromCol].getText().equals(currentSymbol)) {
            return false;
        }

        // Verifica se a casa de destino está vazia
        if (!boardButtons[toRow][toCol].getText().isEmpty()) {
            return false;
        }

        // Verifica se é um movimento adjacente (horizontal ou vertical)
        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);

        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1);
    }

    // No método updateBoard do ClientUI.java, modifique para:
    public void updateBoard(int player, int row, int col) {
        SwingUtilities.invokeLater(() -> {
            String symbol = (player == 1) ? "O" : "X";
            boardButtons[row][col].setText(symbol);
            boardButtons[row][col].setForeground(player == 1 ? Color.BLUE : Color.RED);
            // Não desabilitar o botão na fase de movimentação
            boardButtons[row][col].setEnabled(!isPositioningPhase || boardButtons[row][col].getText().isEmpty());
        });
    }

    public void movePieceOnBoard(int player, int fromRow, int fromCol, int toRow, int toCol) {
        SwingUtilities.invokeLater(() -> {
            // Limpa a posição original
            boardButtons[fromRow][fromCol].setText("");
            boardButtons[fromRow][fromCol].setEnabled(true);
            boardButtons[fromRow][fromCol].setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            
            // Coloca a peça na nova posição
            String symbol = (player == 1) ? "O" : "X";
            boardButtons[toRow][toCol].setText(symbol);
            boardButtons[toRow][toCol].setForeground(player == 1 ? Color.BLUE : Color.RED);
            boardButtons[toRow][toCol].setEnabled(false);
        });
    }    

    public void capturePiece(int row, int col) {
        SwingUtilities.invokeLater(() -> {
            boardButtons[row][col].setText("");
            boardButtons[row][col].setEnabled(true);
            boardButtons[row][col].setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        });
    }

    public void gameOver() {
        SwingUtilities.invokeLater(() -> {
            turnLabel.setText("FIM DE JOGO!");
            turnLabel.setForeground(Color.ORANGE);
            
            // Desabilita todos os botões
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    boardButtons[row][col].setEnabled(false);
                }
            }
        });
    }

}