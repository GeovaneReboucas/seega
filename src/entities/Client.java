package src.entities;

import java.io.*;
import java.net.*;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import src.services.ClientService;
import src.ui.ClientUI;
import src.ui.ConnectionDialog;
import src.utils.Constants;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ClientUI clientUI;
    private int clientId;
    private ClientService clientService;

    public Client() {
        ConnectionDialog connectionDialog = new ConnectionDialog(null);
        connectionDialog.setVisible(true);

        if (connectionDialog.isConfirmed()) {
            initializeConnection(connectionDialog.getIp(), connectionDialog.getPort());
        } else {
            System.exit(0);
        }
    }

    private void initializeConnection(String serverIp, int serverPort) {
        try {
            socket = new Socket(serverIp, serverPort);
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
            if (clientService != null) {
                clientService.handleConnectionError(e);
            } else {
                JOptionPane.showMessageDialog(null,
                        "Não foi possível conectar ao servidor.\nVerifique o IP e a porta e tente novamente.",
                        "Erro de Conexão",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
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
        SwingUtilities.invokeLater(() -> {
            try {
                new Client();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Erro ao iniciar o cliente: " + e.getMessage(),
                        "Erro",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}