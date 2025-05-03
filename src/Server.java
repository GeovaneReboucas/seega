package src;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static int clientCounter = 0;
    private static int currentTurn = 1;
    private static int currentPlayer = 1; // 1 ou 2
    private static int movesInCurrentBlock = 0;
    private static String[][] board = new String[5][5]; // Para rastrear as peças
    private static final int CENTER_ROW = 2;
    private static final int CENTER_COL = 2;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor iniciado na porta " + PORT);

        // Inicializa o tabuleiro
        for (int i = 0; i < 5; i++) {
            Arrays.fill(board[i], "");
        }
        // Bloqueia o centro inicialmente
        board[CENTER_ROW][CENTER_COL] = "BLOCKED";

        while (true) {
            Socket clientSocket = serverSocket.accept();
            clientCounter++;
            ClientHandler handler = new ClientHandler(clientSocket, clientCounter);
            clients.add(handler);
            new Thread(handler).start();

            // Se for o segundo jogador, inicia o jogo
            if (clientCounter == 2) {
                broadcastTurnInfo();
                broadcastCenterBlocked(true);
            }
        }
    }

    private static synchronized void broadcastTurnInfo() {
        String turnInfo = "TURN:" + currentPlayer + ":" + currentTurn;
        for (ClientHandler client : clients) {
            try {
                PrintWriter out = new PrintWriter(client.socket.getOutputStream(), true);
                out.println(turnInfo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static synchronized void broadcastCenterBlocked(boolean blocked) {
        String blockMsg = "CENTER:" + (blocked ? "BLOCKED" : "UNBLOCKED");
        for (ClientHandler client : clients) {
            try {
                PrintWriter out = new PrintWriter(client.socket.getOutputStream(), true);
                out.println(blockMsg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized boolean makeMove(int player, int row, int col) {
        // Verifica se é o turno do jogador e se a célula está vazia
        if (player != currentPlayer)
            return false;

        // Verifica se está tentando jogar no centro bloqueado
        if (currentTurn <= 24 && row == CENTER_ROW && col == CENTER_COL) {
            return false;
        }

        if (board[row][col].isEmpty()) {
            // Coloca a peça no tabuleiro
            board[row][col] = (player == 1) ? "O" : "X";

            // Atualiza o estado do jogo
            movesInCurrentBlock++;
            currentTurn++;

            // Verifica se precisa desbloquear o centro
            if (currentTurn == 25) {
                board[CENTER_ROW][CENTER_COL] = "";
                broadcastCenterBlocked(false);
            }

            // Verifica se precisa mudar o jogador
            if (currentTurn <= 24) {
                // Turnos 1-24: alterna a cada 2 jogadas
                if (movesInCurrentBlock >= 2) {
                    currentPlayer = (currentPlayer == 1) ? 2 : 1;
                    movesInCurrentBlock = 0;
                }
            } else {
                // A partir do turno 25: alterna a cada jogada
                currentPlayer = (currentPlayer == 1) ? 2 : 1;
                movesInCurrentBlock = 0;
            }

            // Notifica todos os clientes sobre a jogada e o novo turno
            broadcastMove(player, row, col);
            broadcastTurnInfo();
            return true;
        }
        return false;
    }

    private static synchronized void broadcastMove(int player, int row, int col) {
        String moveMsg = "MOVE:" + player + ":" + row + ":" + col;
        for (ClientHandler client : clients) {
            try {
                PrintWriter out = new PrintWriter(client.socket.getOutputStream(), true);
                out.println(moveMsg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private int clientId;

        ClientHandler(Socket socket, int clientId) throws IOException {
            this.socket = socket;
            this.clientId = clientId;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);

            out.println("ID:" + clientId);

            // Envia o estado inicial do tabuleiro se for o segundo jogador
            if (clientId == 2 && !clients.isEmpty()) {
                sendInitialBoard();
            }
        }

        private void sendInitialBoard() {
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    if (!board[row][col].isEmpty()) {
                        int player = board[row][col].equals("O") ? 1 : 2;
                        out.println("MOVE:" + player + ":" + row + ":" + col);
                    }
                }
            }
        }

        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.startsWith("MOVE:")) {
                        // Formato: MOVE:row:col
                        String[] parts = msg.split(":");
                        int row = Integer.parseInt(parts[1]);
                        int col = Integer.parseInt(parts[2]);
                        makeMove(clientId, row, col);
                    } else {
                        broadcast("Cliente " + clientId + ": " + msg);
                    }
                }
            } catch (IOException e) {
                System.out.println("Cliente " + clientId + " desconectado.");
                clients.remove(this);
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