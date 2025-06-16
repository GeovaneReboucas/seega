package src;

import java.rmi.Naming;
import java.rmi.RemoteException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import src.ui.ClientUI;
import src.ui.ConnectionDialog;

public class Client {
    private SeegaServer server;
    private SeegaClientImpl client;
    private ClientUI clientUI;
    private int clientId;

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
            // Conecta ao servidor RMI
            server = (SeegaServer) Naming.lookup("//" + serverIp + "/SeegaServer");

            // Cria o cliente RMI temporário para registro
            client = new SeegaClientImpl(null, 0, server);

            // Registra o cliente no servidor e obtém o ID
            clientId = server.registerClient(client);

            // Cria a interface do usuário com o ID correto
            clientUI = new ClientUI(clientId, this);

            // Atualiza a referência da UI no cliente RMI
            client.setClientUI(clientUI);

            // Se for o primeiro cliente, solicita a escolha do jogador inicial
            if (clientId == 1) {
                client.promptForStartingPlayer();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Não foi possível conectar ao servidor.\nVerifique o IP e a porta e tente novamente.",
                    "Erro de Conexão",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public void sendMove(int row, int col) {
        client.makeMove(row, col);
    }

    public void sendMove(int fromRow, int fromCol, int toRow, int toCol) {
        client.movePiece(fromRow, fromCol, toRow, toCol);
    }

    public void sendMessage(String message) {
        try {
            server.broadcastMessage(clientId + ": " + message);
        } catch (RemoteException e) {
            e.printStackTrace();
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