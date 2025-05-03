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

            // Verifica se precisa desbloquear o centro (ANTES de incrementar o currentTurn)
            if (currentTurn == 24) { // Alterado para verificar no turno 24
                board[CENTER_ROW][CENTER_COL] = "";
                broadcastCenterBlocked(false);
            }

            currentTurn++; // Movido para depois da verificação do centro

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

    public static synchronized boolean movePiece(int player, int fromRow, int fromCol, int toRow, int toCol) {
        if (player != currentPlayer || currentTurn < 25) {
            return false;
        }

        String playerSymbol = (player == 1) ? "O" : "X";

        // Verifica se a peça original pertence ao jogador
        if (!board[fromRow][fromCol].equals(playerSymbol)) {
            return false;
        }

        // Verifica se a casa de destino está vazia
        if (!board[toRow][toCol].isEmpty()) {
            return false;
        }

        // Verifica se é um movimento adjacente (horizontal ou vertical)
        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);

        if ((rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)) {
            // Move a peça
            board[fromRow][fromCol] = "";
            board[toRow][toCol] = playerSymbol;

            // Verifica capturas
            checkCaptures(player, toRow, toCol);

            // Verifica se o próximo jogador tem movimentos válidos
            int nextPlayer = (currentPlayer == 1) ? 2 : 1;
            if (!hasValidMoves(nextPlayer)) {
                // Se não tiver movimentos, passa o turno automaticamente
                currentPlayer = nextPlayer;
                broadcastAutoPass(nextPlayer);
                nextPlayer = (currentPlayer == 1) ? 2 : 1;

                // Verifica novamente se o novo jogador tem movimentos
                if (!hasValidMoves(nextPlayer)) {
                    // Se nenhum jogador puder mover, fim de jogo
                    broadcastGameOver();
                    return true;
                }
            }

            // Atualiza o turno
            currentPlayer = nextPlayer;
            currentTurn++;

            // Notifica os clientes
            broadcastPieceMove(player, fromRow, fromCol, toRow, toCol);
            broadcastTurnInfo();
            return true;
        }

        return false;
    }

    private static synchronized void broadcastPieceMove(int player, int fromRow, int fromCol, int toRow, int toCol) {
        String moveMsg = "MOVEPIECE:" + player + ":" + fromRow + ":" + fromCol + ":" + toRow + ":" + toCol;
        for (ClientHandler client : clients) {
            try {
                PrintWriter out = new PrintWriter(client.socket.getOutputStream(), true);
                out.println(moveMsg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

    private static synchronized void checkCaptures(int player, int movedToRow, int movedToCol) {
        String playerSymbol = (player == 1) ? "O" : "X";
        String opponentSymbol = (player == 1) ? "X" : "O";

        // Verificar capturas nas 4 direções (cima, direita, baixo, esquerda)
        int[][] directions = { { -1, 0 }, { 0, 1 }, { 1, 0 }, { 0, -1 } };

        for (int[] dir : directions) {
            int adjacentRow = movedToRow + dir[0];
            int adjacentCol = movedToCol + dir[1];

            // Verifica se está dentro do tabuleiro
            if (adjacentRow >= 0 && adjacentRow < 5 && adjacentCol >= 0 && adjacentCol < 5) {
                // Se encontrou uma peça adversária adjacente
                if (board[adjacentRow][adjacentCol].equals(opponentSymbol)) {
                    int oppositeRow = adjacentRow + dir[0];
                    int oppositeCol = adjacentCol + dir[1];

                    // Verifica se há uma peça aliada do outro lado
                    if (oppositeRow >= 0 && oppositeRow < 5 && oppositeCol >= 0 && oppositeCol < 5) {
                        if (board[oppositeRow][oppositeCol].equals(playerSymbol)) {
                            // Verifica se não é a casa central
                            if (!(adjacentRow == CENTER_ROW && adjacentCol == CENTER_COL)) {
                                // Captura a peça adversária
                                board[adjacentRow][adjacentCol] = "";
                                broadcastCapture(adjacentRow, adjacentCol);
                            }
                        }
                    }
                }
            }
        }

        checkGameEnd();
    }

    private static synchronized void broadcastCapture(int row, int col) {
        String captureMsg = "CAPTURE:" + row + ":" + col;
        for (ClientHandler client : clients) {
            try {
                PrintWriter out = new PrintWriter(client.socket.getOutputStream(), true);
                out.println(captureMsg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static synchronized boolean hasValidMoves(int player) {
        String playerSymbol = (player == 1) ? "O" : "X";

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                if (board[row][col].equals(playerSymbol)) {
                    // Verifica se há pelo menos um movimento válido para esta peça
                    if (canPieceMove(row, col)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static synchronized boolean canPieceMove(int row, int col) {
        // Verifica as 4 direções possíveis (cima, direita, baixo, esquerda)
        int[][] directions = { { -1, 0 }, { 0, 1 }, { 1, 0 }, { 0, -1 } };

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            // Verifica se está dentro do tabuleiro e se a casa está vazia
            if (newRow >= 0 && newRow < 5 && newCol >= 0 && newCol < 5) {
                if (board[newRow][newCol].isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static synchronized void broadcastAutoPass(int playerWhoCannotMove) {
        String passMsg = "AUTOPASS:" + playerWhoCannotMove;
        for (ClientHandler client : clients) {
            try {
                PrintWriter out = new PrintWriter(client.socket.getOutputStream(), true);
                out.println(passMsg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static synchronized void broadcastGameOver() {
        String gameOverMsg = "GAMEOVER";
        for (ClientHandler client : clients) {
            try {
                PrintWriter out = new PrintWriter(client.socket.getOutputStream(), true);
                out.println(gameOverMsg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static synchronized void checkGameEnd() {
        boolean player1HasPieces = false;
        boolean player2HasPieces = false;

        // Verifica se cada jogador ainda tem peças
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                if (board[row][col].equals("O")) {
                    player1HasPieces = true;
                } else if (board[row][col].equals("X")) {
                    player2HasPieces = true;
                }
            }
        }

        // Determina o resultado
        if (!player1HasPieces || !player2HasPieces) {
            int winner = !player1HasPieces ? 2 : 1;
            broadcastGameOver(winner);
        }
    }

    private static synchronized void broadcastGameOver(int winner) {
        // Limpa o tabuleiro para uma nova partida
        for (int i = 0; i < 5; i++) {
            Arrays.fill(board[i], "");
        }
        board[CENTER_ROW][CENTER_COL] = "BLOCKED";

        currentTurn = 1;
        currentPlayer = 1;
        movesInCurrentBlock = 0;

        String gameOverMsg = "GAMEOVER:" + winner;
        for (ClientHandler client : clients) {
            try {
                PrintWriter out = new PrintWriter(client.socket.getOutputStream(), true);
                out.println(gameOverMsg);
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
                    } else if (msg.startsWith("MOVEPIECE:")) {
                        String[] parts = msg.split(":");
                        int fromRow = Integer.parseInt(parts[1]);
                        int fromCol = Integer.parseInt(parts[2]);
                        int toRow = Integer.parseInt(parts[3]);
                        int toCol = Integer.parseInt(parts[4]);
                        movePiece(clientId, fromRow, fromCol, toRow, toCol);
                    } else if (msg.equals("/desistir")) {
                        // O jogador atual desistiu
                        int winner = (clientId == 1) ? 2 : 1;
                        broadcast("Jogador " + clientId + " desistiu da partida!");
                        broadcastGameOver(winner);
                        break;
                    } else {
                        broadcast("Cliente " + clientId + ": " + msg);
                    }
                }
            } catch (IOException e) {
                System.out.println("Cliente " + clientId + " desconectado.");
                // Se um jogador desconectar, considerar como desistência
                int winner = (clientId == 1) ? 2 : 1;
                broadcast("Jogador " + clientId + " desconectou!");
                broadcastGameOver(winner);
            } finally {
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