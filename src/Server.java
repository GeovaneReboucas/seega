package src;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static int clientCounter = 0;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor iniciado na porta " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            clientCounter++;
            ClientHandler handler = new ClientHandler(clientSocket, clientCounter);
            clients.add(handler);
            new Thread(handler).start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private int clientId;

        ClientHandler(Socket socket, int clientId) throws IOException {
            this.socket = socket;
            this.clientId = clientId;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("ID:" + clientId);
        }
        

        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    broadcast("Cliente " + clientId + ": " + msg);
                }
            } catch (IOException e) {
                System.out.println("Cliente " + clientId + " desconectado.");
            }
        }

        private void broadcast(String msg) {
            for (ClientHandler client : clients) {
                try {
                    PrintWriter out = new PrintWriter(client.socket.getOutputStream(), true);
                    out.println(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
