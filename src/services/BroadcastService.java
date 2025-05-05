package src.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import src.handlers.ClientHandler;


public class BroadcastService {
    private List<ClientHandler> clients;

    public BroadcastService(List<ClientHandler> clients) {
        this.clients = clients;
    }

    public synchronized void broadcastTurnInfo(int currentPlayer, int currentTurn) {
        String turnInfo = "TURN:" + currentPlayer + ":" + currentTurn;
        broadcastMessage(turnInfo);
    }

    public synchronized void broadcastCenterBlocked(boolean blocked) {
        String blockMsg = "CENTER:" + (blocked ? "BLOCKED" : "UNBLOCKED");
        broadcastMessage(blockMsg);
    }

    public synchronized void broadcastPieceMove(int player, int fromRow, int fromCol, int toRow, int toCol) {
        String moveMsg = "MOVEPIECE:" + player + ":" + fromRow + ":" + fromCol + ":" + toRow + ":" + toCol;
        broadcastMessage(moveMsg);
    }

    public synchronized void broadcastMove(int player, int row, int col) {
        String moveMsg = "MOVE:" + player + ":" + row + ":" + col;
        broadcastMessage(moveMsg);
    }

    public synchronized void broadcastCapture(int row, int col) {
        String captureMsg = "CAPTURE:" + row + ":" + col;
        broadcastMessage(captureMsg);
    }

    public synchronized void broadcastAutoPass(int playerWhoCannotMove) {
        String passMsg = "AUTOPASS:" + playerWhoCannotMove;
        broadcastMessage(passMsg);
    }

    public synchronized void broadcastGameOver() {
        broadcastMessage("GAMEOVER");
    }

    public synchronized void broadcastGameOver(int winner) {
        String gameOverMsg = "GAMEOVER:" + winner;
        broadcastMessage(gameOverMsg);
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            try {
                PrintWriter out = new PrintWriter(client.getSocket().getOutputStream(), true);
                out.println(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}