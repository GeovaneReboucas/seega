package src.handlers;

import java.io.*;
import java.net.*;
import src.services.BroadcastService;
import src.services.ServerService;
import src.utils.Constants;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private int clientId;
    private ServerService serverService;
    private BroadcastService broadcastService;

    public ClientHandler(Socket socket, int clientId, ServerService serverService, 
                        BroadcastService broadcastService) throws IOException {
        this.socket = socket;
        this.clientId = clientId;
        this.serverService = serverService;
        this.broadcastService = broadcastService;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);

        out.println(Constants.ID_PREFIX + clientId);
    }

    public void sendInitialBoard() {
        String[][] board = serverService.getBoard();
        for (int row = 0; row < Constants.BOARD_SIZE; row++) {
            for (int col = 0; col < Constants.BOARD_SIZE; col++) {
                if (!board[row][col].isEmpty()) {
                    int player = board[row][col].equals(Constants.PLAYER_1_SYMBOL) ? 1 : 2;
                    out.println(Constants.MOVE_PREFIX + player + ":" + row + ":" + col);
                }
            }
        }
    }

    public void run() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.startsWith(Constants.STARTING_PLAYER_PREFIX)) {
                    int startingPlayer = Integer.parseInt(msg.split(":")[1]);
                    serverService.setStartingPlayer(startingPlayer);
                    broadcastService.broadcastMessage(Constants.STARTING_PLAYER_PREFIX + startingPlayer);
                } else if (msg.startsWith(Constants.MOVE_PREFIX)) {
                    String[] parts = msg.split(":");
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);
                    serverService.makeMove(clientId, row, col);
                } else if (msg.startsWith(Constants.MOVE_PIECE_PREFIX)) {
                    String[] parts = msg.split(":");
                    int fromRow = Integer.parseInt(parts[1]);
                    int fromCol = Integer.parseInt(parts[2]);
                    int toRow = Integer.parseInt(parts[3]);
                    int toCol = Integer.parseInt(parts[4]);
                    serverService.movePiece(clientId, fromRow, fromCol, toRow, toCol);
                } else if (msg.equals("/desistir")) {
                    int winner = (clientId == 1) ? 2 : 1;
                    broadcastService.broadcastMessage(String.format(Constants.RESIGN_MESSAGE, clientId));
                    broadcastService.broadcastGameOver(winner);
                    break;
                } else {
                    broadcastService.broadcastMessage("Cliente " + clientId + ": " + msg);
                }
            }
        } catch (IOException e) {
            System.out.println(String.format(Constants.CLIENT_DISCONNECT_MESSAGE, clientId));
            int winner = (clientId == 1) ? 2 : 1;
            broadcastService.broadcastMessage(String.format(Constants.DISCONNECT_MESSAGE, clientId));
            broadcastService.broadcastGameOver(winner);
        } finally {
            broadcastService.removeClient(this);
        }
    }

    public PrintWriter getOut() {
        return out;
    }

    public int getClientId() {
        return clientId;
    }

    public Socket getSocket() {
        return socket;
    }
}