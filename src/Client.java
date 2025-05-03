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
                    // Formato: TURN:player:turnNumber
                    String[] parts = msg.split(":");
                    int player = Integer.parseInt(parts[1]);
                    myTurn = (player == clientId);
                    clientUI.updateTurnInfo(player, Integer.parseInt(parts[2]));
                } else if (msg.startsWith("MOVE:")) {
                    // Formato: MOVE:player:row:col
                    String[] parts = msg.split(":");
                    int player = Integer.parseInt(parts[1]);
                    int row = Integer.parseInt(parts[2]);
                    int col = Integer.parseInt(parts[3]);
                    clientUI.updateBoard(player, row, col);
                } else if (msg.startsWith("CENTER:")) {
                    boolean blocked = msg.endsWith("BLOCKED");
                    clientUI.updateCenterBlock(blocked);
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

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(Client::new);
    }
}