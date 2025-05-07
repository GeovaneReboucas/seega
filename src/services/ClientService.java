package src.services;

import java.io.*;
import java.net.*;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import src.ui.ClientUI;
import src.utils.Constants;

public class ClientService {
    private ClientUI clientUI;
    private int clientId;
    private boolean myTurn = false;
    private PrintWriter out;

    public ClientService(ClientUI clientUI, int clientId, PrintWriter out) {
        this.clientUI = clientUI;
        this.clientId = clientId;
        this.out = out;
    }

    public void promptForStartingPlayer() {
        SwingUtilities.invokeLater(() -> {
            int option = JOptionPane.showOptionDialog(null,
                    Constants.STARTING_PLAYER_QUESTION,
                    Constants.STARTING_PLAYER_TITLE,
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    Constants.PLAYER_OPTIONS,
                    Constants.DEFAULT_PLAYER_OPTION);

            int startingPlayer = (option == 0) ? 1 : 2;
            out.println(Constants.STARTING_PLAYER_PREFIX + startingPlayer);
        });
    }

    public void processMessage(String msg) {
        if (msg.startsWith(Constants.TURN_PREFIX)) {
            handleTurnMessage(msg);
        } else if (msg.startsWith(Constants.STARTING_PLAYER_PREFIX)) {
            handleStartingPlayerMessage(msg);
        } else if (msg.startsWith(Constants.AUTO_PASS_PREFIX)) {
            handleAutoPassMessage(msg);
        } else if (msg.startsWith(Constants.GAME_OVER_PREFIX)) {
            handleGameOverMessage(msg);
        } else if (msg.startsWith(Constants.MOVE_PREFIX)) {
            handleMoveMessage(msg);
        } else if (msg.startsWith(Constants.MOVE_PIECE_PREFIX)) {
            handleMovePieceMessage(msg);
        } else if (msg.startsWith(Constants.CAPTURE_PREFIX)) {
            handleCaptureMessage(msg);
        } else if (msg.startsWith(Constants.CENTER_PREFIX)) {
            handleCenterMessage(msg);
        } else if (msg.contains("desistiu") || msg.contains("desconectou")) {
            handleResignationMessage(msg);
        } else {
            clientUI.appendMessage(msg);
        }
    }

    public boolean isMyTurn() {
        return myTurn;
    }

    // Handle Methods
    public void handleTurnMessage(String msg) {
        String[] parts = msg.split(":");
        int player = Integer.parseInt(parts[1]);
        myTurn = (player == clientId);
        int turnNumber = Integer.parseInt(parts[2]);
        clientUI.updateTurnInfo(player, turnNumber);

        if (turnNumber == Constants.CENTER_UNLOCK_TURN) {
            clientUI.updateCenterBlock(false);
        }
    }

    public void handleStartingPlayerMessage(String msg) {
        int startingPlayer = Integer.parseInt(msg.split(":")[1]);
        clientUI.setStartingPlayer(startingPlayer);
    }

    public void handleAutoPassMessage(String msg) {
        int playerWhoCannotMove = Integer.parseInt(msg.split(":")[1]);
        clientUI.appendMessage("Jogador " + playerWhoCannotMove +
                Constants.AUTO_PASS_MESSAGE);
    }

    public void handleGameOverMessage(String msg) {
        int winner = Integer.parseInt(msg.split(":")[1]);
        clientUI.showGameResult(winner == clientId);
        clientUI.disableResignButton();
    }

    public void handleMoveMessage(String msg) {
        String[] parts = msg.split(":");
        int player = Integer.parseInt(parts[1]);
        int row = Integer.parseInt(parts[2]);
        int col = Integer.parseInt(parts[3]);
        clientUI.updateBoard(player, row, col);
    }

    public void handleMovePieceMessage(String msg) {
        String[] parts = msg.split(":");
        int player = Integer.parseInt(parts[1]);
        int fromRow = Integer.parseInt(parts[2]);
        int fromCol = Integer.parseInt(parts[3]);
        int toRow = Integer.parseInt(parts[4]);
        int toCol = Integer.parseInt(parts[5]);
        clientUI.movePieceOnBoard(player, fromRow, fromCol, toRow, toCol);
    }

    public void handleCaptureMessage(String msg) {
        String[] parts = msg.split(":");
        int row = Integer.parseInt(parts[1]);
        int col = Integer.parseInt(parts[2]);
        clientUI.capturePiece(row, col);
    }

    public void handleCenterMessage(String msg) {
        boolean blocked = msg.endsWith("BLOCKED");
        clientUI.updateCenterBlock(blocked);
    }

    public void handleResignationMessage(String msg) {
        clientUI.appendMessage(msg);
        boolean isWinner = !msg.contains("Jogador " + clientId);
        if (isWinner) {
            clientUI.showGameResult(true);
        }
        clientUI.disableResignButton();
    }

    public void handleConnectionError(IOException e) {
        System.err.println("Erro na conexão com o servidor: " + e.getMessage());
        e.printStackTrace();
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null,
                    Constants.CONNECTION_ERROR_MESSAGE,
                    Constants.CONNECTION_ERROR_TITLE,
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        });
    }

    public void handleDisconnection() {
        System.out.println("Conexão com o servidor encerrada.");
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null,
                    Constants.DISCONNECTION_MESSAGE,
                    Constants.CONNECTION_ERROR_TITLE,
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        });
    }

    //
}