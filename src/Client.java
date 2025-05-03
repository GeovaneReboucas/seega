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

    public Client() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String idMessage = in.readLine();
            int clientId = 0;
            if (idMessage != null && idMessage.startsWith("ID:")) {
                clientId = Integer.parseInt(idMessage.substring(3));
            }

            clientUI = new ClientUI(clientId, out);
            new Thread(this::receiveMessages).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                clientUI.appendMessage(msg);
            }
        } catch (IOException e) {
            System.out.println("Conexão encerrada.");
        }
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(Client::new);
    }
}
