package src.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import src.Client;
import src.utils.Constants;

public class ClientUI {
    private JFrame frame = new JFrame("Seega");
    private JTextArea chatArea = new JTextArea(20, 40);
    private JTextField inputField = new JTextField(40);
    private JLabel turnLabel = new JLabel("Aguardando inicio do jogo...");
    private JButton[][] boardButtons = new JButton[5][5];
    private JButton resignButton;
    private Client client;
    private int clientId;
    private boolean centerBlocked = true;

    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean isPositioningPhase = true;
    private int currentPlayer = 1;

    public ClientUI(int clientId, Client client) {
        this.clientId = clientId;
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
                client.sendMessage(msg);
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
                        String currentSymbol = (clientId == 1) ? Constants.PLAYER_1_SYMBOL : Constants.PLAYER_2_SYMBOL;

                        if (selectedRow == -1 && selectedCol == -1) {
                            // Selecionar uma peça para mover
                            if (button.getText().equals(currentSymbol)) {
                                selectedRow = r;
                                selectedCol = c;
                                button.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
                                updateAvailableMoves();
                            }
                        } else {
                            if (r == selectedRow && c == selectedCol) {
                                // Deseleciona
                                button.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                                selectedRow = -1;
                                selectedCol = -1;
                                updateAvailableMoves();
                            } else if (button.getText().isEmpty() && isValidMove(selectedRow, selectedCol, r, c)) {
                                // Tentar mover a peça selecionada para esta posição
                                client.sendMove(selectedRow, selectedCol, r, c);
                                // Limpa a seleção
                                boardButtons[selectedRow][selectedCol]
                                        .setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                                selectedRow = -1;
                                selectedCol = -1;
                                updateAvailableMoves();
                            } else {
                                // Se clicou em uma posição inválida, desmarca a seleção
                                boardButtons[selectedRow][selectedCol]
                                        .setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                                selectedRow = -1;
                                selectedCol = -1;
                                updateAvailableMoves();
                            }
                        }
                    }
                });

                boardButtons[row][col] = button;
                gamePanel.add(button);
            }
        }

        // Botão de desistência
        resignButton = new JButton("Desistir");
        resignButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        resignButton.setBackground(new Color(143, 56, 56));
        resignButton.setForeground(Color.WHITE);
        resignButton.setFocusPainted(false);
        resignButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    frame,
                    Constants.QUIT_MATCH_QUESTION,
                    Constants.CONFIRM_WITHDRAWAL,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    client.getServer().playerResigns(clientId);
                    appendMessage(Constants.GAVE_UP_THE_GAME);
                    resignButton.setEnabled(false);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame,
                            "Erro ao comunicar com o servidor",
                            "Erro",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Adicionar o botão ao painel de informações
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(32, 41, 59));
        buttonPanel.add(resignButton);

        // Modificar o infoPanel para incluir o buttonPanel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(new Color(32, 41, 59));
        infoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        infoPanel.add(turnLabel, BorderLayout.CENTER);
        infoPanel.add(buttonPanel, BorderLayout.EAST); // Adicionar o painel de botões

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
        String sanitized = new String(msg.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        chatArea.append(sanitized + "\n");
    }

    public void updateTurnInfo(int currentPlayer, int turnNumber) {
        SwingUtilities.invokeLater(() -> {
            // Atualiza a fase do jogo
            isPositioningPhase = turnNumber < Constants.MOVEMENT_PHASE_START_TURN;
            centerBlocked = isPositioningPhase;

            JButton centerButton = boardButtons[2][2];
            if (centerBlocked) {
                centerButton.setText("BLOCKED");
                centerButton.setBackground(Color.RED);
                centerButton.setForeground(Color.WHITE);
                centerButton.setEnabled(false);
            } else {
                centerButton.setText("");
                centerButton.setBackground(Color.WHITE);
                centerButton.setEnabled(true);
            }

            // Restante do método permanece igual...
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

            updateBoardState(currentPlayer);
        });
    }

    private void updateBoardState(int currentPlayer) {
        for (int row = 0; row < Constants.BOARD_SIZE; row++) {
            for (int col = 0; col < Constants.BOARD_SIZE; col++) {
                JButton button = boardButtons[row][col];
                String buttonText = button.getText();
                String currentSymbol = (clientId == 1) ? Constants.PLAYER_1_SYMBOL : Constants.PLAYER_2_SYMBOL;

                if (isPositioningPhase) {
                    // Fase de posicionamento
                    button.setEnabled(buttonText.isEmpty() &&
                            !(centerBlocked && row == Constants.CENTER_ROW && col == Constants.CENTER_COL) &&
                            currentPlayer == clientId);
                } else {
                    // Fase de movimentação - a casa central deve se comportar como qualquer outra
                    if (currentPlayer == clientId) {
                        if (selectedRow == -1 && selectedCol == -1) {
                            // Se nenhuma peça está selecionada, habilita apenas as peças do jogador
                            button.setEnabled(buttonText.equals(currentSymbol));
                        } else if (row == selectedRow && col == selectedCol) {
                            // A peça selecionada deve estar sempre habilitada
                            button.setEnabled(true);
                        } else {
                            // Se uma peça está selecionada, habilita apenas as casas vazias adjacentes
                            button.setEnabled(buttonText.isEmpty() && isValidMove(selectedRow, selectedCol, row, col));
                        }
                    } else {
                        button.setEnabled(false);
                    }
                }
            }
        }
    }

    private void updateAvailableMoves() {
        if (!isPositioningPhase && currentPlayer == clientId) {
            String currentSymbol = (clientId == 1) ? Constants.PLAYER_1_SYMBOL : Constants.PLAYER_2_SYMBOL;

            for (int row = 0; row < Constants.BOARD_SIZE; row++) {
                for (int col = 0; col < Constants.BOARD_SIZE; col++) {
                    JButton button = boardButtons[row][col];
                    String buttonText = button.getText();

                    if (selectedRow == -1 && selectedCol == -1) {
                        // Habilita todas as peças do jogador
                        button.setEnabled(buttonText.equals(currentSymbol));
                    } else {
                        // Habilita apenas casas vazias adjacentes
                        button.setEnabled(buttonText.isEmpty() &&
                                isValidMove(selectedRow, selectedCol, row, col));
                    }
                }
            }
        }
    }

    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        // Verifica se o movimento é adjacente (horizontal ou vertical)
        return (Math.abs(fromRow - toRow) + Math.abs(fromCol - toCol)) == 1;
    }

    // No método updateBoard do ClientUI.java, modifique para:
    public void updateBoard(String[][] board) {
        SwingUtilities.invokeLater(() -> {
            for (int row = 0; row < Constants.BOARD_SIZE; row++) {
                for (int col = 0; col < Constants.BOARD_SIZE; col++) {
                    JButton button = boardButtons[row][col];
                    String cellValue = board[row][col];

                    if (cellValue.equals("BLOCKED")) {
                        // Tratamento especial para a casa central bloqueada
                        button.setText("BLOCKED");
                        button.setBackground(Color.RED);
                        button.setForeground(Color.WHITE); // Texto branco para melhor contraste
                        button.setEnabled(false);
                    } else if (cellValue.isEmpty()) {
                        button.setText("");
                        button.setBackground(Color.WHITE);
                    } else {
                        button.setText(cellValue);
                        button.setBackground(Color.WHITE);
                        button.setForeground(cellValue.equals(Constants.PLAYER_1_SYMBOL) ? Color.BLUE : Color.RED);
                    }
                }
            }
            updateBoardState(currentPlayer);
        });
    }

    public void movePieceOnBoard(int player, int fromRow, int fromCol, int toRow, int toCol) {
        SwingUtilities.invokeLater(() -> {
            // Limpa a posição original
            boardButtons[fromRow][fromCol].setText("");
            boardButtons[fromRow][fromCol].setBackground(Color.WHITE);

            // Coloca a peça na nova posição
            String symbol = (player == 1) ? Constants.PLAYER_1_SYMBOL : Constants.PLAYER_2_SYMBOL;
            boardButtons[toRow][toCol].setText(symbol);
            boardButtons[toRow][toCol]
                    .setBackground(toRow == 2 && toCol == 2 && centerBlocked ? Color.RED : Color.WHITE);
            boardButtons[toRow][toCol].setForeground(player == 1 ? Color.BLUE : Color.RED);

            // Atualiza o estado do tabuleiro
            updateBoardState(currentPlayer);
        });
    }

    public void capturePiece(int row, int col) {
        SwingUtilities.invokeLater(() -> {
            boardButtons[row][col].setText("");
            boardButtons[row][col].setBackground(Color.WHITE);
            boardButtons[row][col].setEnabled(true);

            // Efeito visual para a captura
            boardButtons[row][col].setBackground(Color.YELLOW);
            Timer timer = new Timer(600, e -> {
                boardButtons[row][col].setBackground(Color.WHITE);
            });
            timer.setRepeats(false);
            timer.start();
        });

    }

    public void gameOver() {
        SwingUtilities.invokeLater(() -> {
            // Desabilita todos os botões
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    boardButtons[row][col].setEnabled(false);
                }
            }

            JOptionPane.showMessageDialog(frame,
                    "O jogo terminou em empate!",
                    "Fim de Jogo",
                    JOptionPane.INFORMATION_MESSAGE);

            turnLabel.setText("EMPATE!");
            turnLabel.setForeground(Color.ORANGE);
        });
    }

    public void showGameResult(boolean isWinner) {
        SwingUtilities.invokeLater(() -> {
            // Desabilita todos os botões
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    boardButtons[row][col].setEnabled(false);
                }
            }

            // Mostra mensagem de vitória/derrota
            String message = isWinner ? "Voce venceu! Parabens!" : "Voce perdeu. Tente novamente!";
            JOptionPane.showMessageDialog(frame,
                    message,
                    "Fim de Jogo",
                    JOptionPane.INFORMATION_MESSAGE);

            turnLabel.setText(isWinner ? "VOCE VENCEU!" : "VOCE PERDEU!");
            turnLabel.setForeground(isWinner ? Color.GREEN : Color.RED);
        });
    }

    public void setStartingPlayer(int player) {
        SwingUtilities.invokeLater(() -> {
            appendMessage("O jogador " + player + " foi escolhido para iniciar o jogo!");
        });
    }

    public void disableResignButton() {
        SwingUtilities.invokeLater(() -> resignButton.setEnabled(false));
    }

    public void enableResignButton() {
        SwingUtilities.invokeLater(() -> resignButton.setEnabled(true));
    }

    public void setCurrentPlayer(int player) {
        SwingUtilities.invokeLater(() -> {
            currentPlayer = player;
            turnLabel.setText("Turno do Jogador " + player);

            // Atualiza o estado dos botões do tabuleiro
            if (!isPositioningPhase) {
                for (int row = 0; row < Constants.BOARD_SIZE; row++) {
                    for (int col = 0; col < Constants.BOARD_SIZE; col++) {
                        JButton button = boardButtons[row][col];
                        String buttonText = button.getText();

                        if (player == clientId) {
                            // Se for o turno do jogador
                            if (selectedRow == -1 && selectedCol == -1) {
                                // Se nenhuma peça está selecionada, habilita apenas as peças do jogador
                                button.setEnabled(buttonText
                                        .equals(clientId == 1 ? Constants.PLAYER_1_SYMBOL : Constants.PLAYER_2_SYMBOL));
                            } else {
                                // Se uma peça está selecionada, habilita apenas as casas vazias adjacentes
                                button.setEnabled(
                                        buttonText.isEmpty() && isValidMove(selectedRow, selectedCol, row, col));
                            }
                        } else {
                            // Se não for o turno do jogador, desabilita todos os botões
                            button.setEnabled(false);
                        }
                    }
                }
            }
        });
    }

    public void setCurrentTurn(int turn) {
        SwingUtilities.invokeLater(() -> {
            isPositioningPhase = turn < Constants.MOVEMENT_PHASE_START_TURN;
            turnLabel.setText("Turno " + turn);

            // Atualiza o estado dos botões do tabuleiro
            for (int row = 0; row < Constants.BOARD_SIZE; row++) {
                for (int col = 0; col < Constants.BOARD_SIZE; col++) {
                    JButton button = boardButtons[row][col];
                    String buttonText = button.getText();

                    if (isPositioningPhase) {
                        // Na fase de posicionamento, habilita apenas casas vazias
                        button.setEnabled(buttonText.isEmpty()
                                && !(centerBlocked && row == Constants.CENTER_ROW && col == Constants.CENTER_COL));
                    } else {
                        // Na fase de movimentação
                        if (currentPlayer == clientId) {
                            // Se for o turno do jogador
                            if (selectedRow == -1 && selectedCol == -1) {
                                // Se nenhuma peça está selecionada, habilita apenas as peças do jogador
                                button.setEnabled(buttonText
                                        .equals(clientId == 1 ? Constants.PLAYER_1_SYMBOL : Constants.PLAYER_2_SYMBOL));
                            } else {
                                // Se uma peça está selecionada, habilita apenas as casas vazias adjacentes
                                button.setEnabled(
                                        buttonText.isEmpty() && isValidMove(selectedRow, selectedCol, row, col));
                            }
                        } else {
                            // Se não for o turno do jogador, desabilita todos os botões
                            button.setEnabled(false);
                        }
                    }
                }
            }
        });
    }

    public void setTitle(String title) {
        frame.setTitle(title);
    }
}