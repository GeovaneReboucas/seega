package src;

import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ClientUI clientUI;
    private int clientId;
    private boolean myTurn = false;

    public Client() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String idMessage = in.readLine();
            if (idMessage != null && idMessage.startsWith("ID:")) {
                clientId = Integer.parseInt(idMessage.substring(3));
            }

            clientUI = new ClientUI(clientId, out, this);
            new Thread(this::receiveMessages).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.startsWith("TURN:")) {
                    String[] parts = msg.split(":");
                    int player = Integer.parseInt(parts[1]);
                    myTurn = (player == clientId);
                    int turnNumber = Integer.parseInt(parts[2]);
                    clientUI.updateTurnInfo(player, turnNumber);

                    // Verifica se é turno 25 para desbloquear o centro
                    if (turnNumber == 25) {
                        clientUI.updateCenterBlock(false);
                    }
                } else if (msg.startsWith("AUTOPASS:")) {
                    int playerWhoCannotMove = Integer.parseInt(msg.split(":")[1]);
                    clientUI.appendMessage("Jogador " + playerWhoCannotMove
                            + " não tem movimentos válidos. Turno passado automaticamente.");
                } else if (msg.startsWith("GAMEOVER")) {
                    int winner = Integer.parseInt(msg.split(":")[1]);
                    clientUI.showGameResult(winner == clientId);
                } else if (msg.startsWith("MOVE:")) {
                    String[] parts = msg.split(":");
                    int player = Integer.parseInt(parts[1]);
                    int row = Integer.parseInt(parts[2]);
                    int col = Integer.parseInt(parts[3]);
                    clientUI.updateBoard(player, row, col);
                } else if (msg.startsWith("MOVEPIECE:")) {
                    String[] parts = msg.split(":");
                    int player = Integer.parseInt(parts[1]);
                    int fromRow = Integer.parseInt(parts[2]);
                    int fromCol = Integer.parseInt(parts[3]);
                    int toRow = Integer.parseInt(parts[4]);
                    int toCol = Integer.parseInt(parts[5]);
                    clientUI.movePieceOnBoard(player, fromRow, fromCol, toRow, toCol);
                } else if (msg.startsWith("CAPTURE:")) {
                    String[] parts = msg.split(":");
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);
                    clientUI.capturePiece(row, col);
                } else if (msg.startsWith("CENTER:")) {
                    boolean blocked = msg.endsWith("BLOCKED");
                    clientUI.updateCenterBlock(blocked);
                } else if (msg.contains("desistiu") || msg.contains("desconectou")) {
                    // Mostra a mensagem de desistência no chat
                    clientUI.appendMessage(msg);

                    // Determina se este cliente é o vencedor
                    boolean isWinner = !msg.contains("Jogador " + clientId);
                    if (isWinner) {
                        clientUI.showGameResult(true);
                    }
                } else {
                    clientUI.appendMessage(msg);
                }
            }
        } catch (IOException e) {
            System.out.println("Conexão encerrada.");
        }
    }

    public void sendMove(int row, int col) {
        if (myTurn) {
            out.println("MOVE:" + row + ":" + col);
        }
    }

    public void sendMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (myTurn) {
            out.println("MOVEPIECE:" + fromRow + ":" + fromCol + ":" + toRow + ":" + toCol);
        }
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(Client::new);
    }
}