package src.entities;

import java.io.*;
import java.net.*;
import javax.swing.SwingUtilities;

import src.services.ClientService;
import src.ui.ClientUI;
import src.utils.Constants;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ClientUI clientUI;
    private int clientId;
    private ClientService clientService;

    public Client() {
        initializeConnection();
    }

    private void initializeConnection() {
        try {
            socket = new Socket(Constants.SERVER_IP, Constants.SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String idMessage = in.readLine();
            if (idMessage != null && idMessage.startsWith(Constants.ID_PREFIX)) {
                clientId = Integer.parseInt(idMessage.substring(Constants.ID_PREFIX.length()));
            }

            clientUI = new ClientUI(clientId, out, this);
            clientService = new ClientService(clientUI, clientId, out);

            if (clientId == 1) {
                clientService.promptForStartingPlayer();
            }

            startMessageReceiver();

        } catch (IOException e) {
            clientService.handleConnectionError(e);
        }
    }

    private void startMessageReceiver() {
        new Thread(this::receiveMessages).start();
    }

    private void receiveMessages() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                clientService.processMessage(msg);
            }
        } catch (IOException e) {
            clientService.handleDisconnection();
        }
    }

    public void sendMove(int row, int col) {
        if (clientService.isMyTurn()) {
            out.println(Constants.MOVE_PREFIX + row + ":" + col);
        }
    }

    public void sendMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (clientService.isMyTurn()) {
            out.println(Constants.MOVE_PIECE_PREFIX + fromRow + ":" + fromCol + ":" + toRow + ":" + toCol);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}