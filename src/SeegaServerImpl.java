package src;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import src.utils.Constants;

public class SeegaServerImpl extends UnicastRemoteObject implements SeegaServer {
    private static final long serialVersionUID = 1L;
    private String[][] board;
    private int currentPlayer;
    private int currentTurn;
    private int movesInCurrentBlock;
    private boolean startingPlayerChosen;
    private List<SeegaClient> clients;
    private int nextClientId;

    public SeegaServerImpl() throws RemoteException {
        super();
        initializeBoard();
        currentTurn = Constants.INITIAL_TURN;
        currentPlayer = 1;
        movesInCurrentBlock = 0;
        startingPlayerChosen = false;
        clients = new ArrayList<>();
        nextClientId = 1;
    }

    private void initializeBoard() {
        board = new String[Constants.BOARD_SIZE][Constants.BOARD_SIZE];
        for (int i = 0; i < Constants.BOARD_SIZE; i++) {
            for (int j = 0; j < Constants.BOARD_SIZE; j++) {
                board[i][j] = "";
            }
        }
        // Bloqueia o centro inicialmente
        board[Constants.CENTER_ROW][Constants.CENTER_COL] = "BLOCKED";
    }

    @Override
    public synchronized int registerClient(SeegaClient client) throws RemoteException {
        int clientId = nextClientId++;
        clients.add(client);

        // Envia o estado atual do jogo para o novo cliente
        client.updateBoard(board);
        client.setCurrentPlayer(currentPlayer);
        client.setCurrentTurn(currentTurn);

        // Se for o segundo cliente e o jogador inicial ainda não foi escolhido,
        // solicita ao primeiro cliente que escolha
        if (clients.size() == 2 && !startingPlayerChosen) {
            clients.get(0).promptForStartingPlayer();
            // Notifica todos os clientes que o jogo começou
            for (SeegaClient c : clients) {
                c.showMessage("Jogo iniciado! Aguardando escolha do jogador inicial...");
            }
        }

        return clientId;
    }

    private void notifyAllClients() throws RemoteException {
        for (SeegaClient client : clients) {
            client.updateBoard(board);
            client.setCurrentPlayer(currentPlayer);
            client.setCurrentTurn(currentTurn);
        }
    }

    @Override
    public synchronized void setStartingPlayer(int player) throws RemoteException {
        currentPlayer = player;
        startingPlayerChosen = true;

        // Notifica todos os clientes sobre o jogador inicial
        for (SeegaClient client : clients) {
            client.showMessage("Jogador " + player + " começa!");
        }

        notifyAllClients();
    }

    @Override
    public synchronized boolean makeMove(int player, int row, int col) throws RemoteException {
        if (player != currentPlayer) {
            return false;
        }

        // Verifica se está tentando jogar no centro bloqueado durante a fase de
        // posicionamento
        if (currentTurn <= Constants.PLACEMENT_PHASE_END_TURN &&
                row == Constants.CENTER_ROW && col == Constants.CENTER_COL) {
            return false;
        }

        if (board[row][col].isEmpty()) {
            board[row][col] = (player == 1) ? Constants.PLAYER_1_SYMBOL : Constants.PLAYER_2_SYMBOL;
            movesInCurrentBlock++;

            // Remove o desbloqueio do centro aqui - será feito apenas no
            // checkPhaseTransition
            currentTurn++;
            checkPhaseTransition();

            if (currentTurn <= Constants.PLACEMENT_PHASE_END_TURN) {
                if (movesInCurrentBlock >= Constants.MOVES_PER_BLOCK) {
                    currentPlayer = (currentPlayer == 1) ? 2 : 1;
                    movesInCurrentBlock = 0;
                }
            } else {
                currentPlayer = (currentPlayer == 1) ? 2 : 1;
                movesInCurrentBlock = 0;
            }

            notifyAllClients();
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean movePiece(int player, int fromRow, int fromCol, int toRow, int toCol)
            throws RemoteException {
        if (player != currentPlayer || currentTurn < Constants.MOVEMENT_PHASE_START_TURN) {
            return false;
        }

        String playerSymbol = (player == 1) ? Constants.PLAYER_1_SYMBOL : Constants.PLAYER_2_SYMBOL;

        if (!board[fromRow][fromCol].equals(playerSymbol) || !board[toRow][toCol].isEmpty()) {
            return false;
        }

        // Verifica movimento adjacente
        if (Math.abs(fromRow - toRow) + Math.abs(fromCol - toCol) != 1) {
            return false;
        }

        // Executa o movimento
        board[fromRow][fromCol] = "";
        board[toRow][toCol] = playerSymbol;

        // Verifica capturas
        boolean captureOccurred = checkCaptures(player, toRow, toCol);

        if (!captureOccurred) {
            int nextPlayer = (currentPlayer == 1) ? 2 : 1;

            // Verifica se o próximo jogador tem movimentos válidos
            if (!hasValidMoves(nextPlayer)) {
                broadcastMessage("Jogador " + nextPlayer + " não tem movimentos válidos! Turno passado.");

                // Se nem o jogador atual tem movimentos, fim de jogo
                if (!hasValidMoves(currentPlayer)) {
                    broadcastMessage("Nenhum jogador tem movimentos válidos!");
                    checkGameEnd();
                    return true;
                }

                // Mantém o mesmo jogador para o próximo turno
                currentTurn++;
                notifyAllClients();
                return true;
            }

            currentPlayer = nextPlayer;

        }

        currentTurn++;
        notifyAllClients();
        return true;
    }

    @Override
    public synchronized boolean hasValidMoves(int player) throws RemoteException {
        String playerSymbol = (player == 1) ? Constants.PLAYER_1_SYMBOL : Constants.PLAYER_2_SYMBOL;

        for (int row = 0; row < Constants.BOARD_SIZE; row++) {
            for (int col = 0; col < Constants.BOARD_SIZE; col++) {
                if (board[row][col].equals(playerSymbol)) {
                    // Verifica todas as direções possíveis
                    if (row > 0 && board[row - 1][col].isEmpty())
                        return true;
                    if (row < Constants.BOARD_SIZE - 1 && board[row + 1][col].isEmpty())
                        return true;
                    if (col > 0 && board[row][col - 1].isEmpty())
                        return true;
                    if (col < Constants.BOARD_SIZE - 1 && board[row][col + 1].isEmpty())
                        return true;
                }
            }
        }
        return false;
    }

    @Override
    public synchronized String[][] getBoard() throws RemoteException {
        return board;
    }

    @Override
    public synchronized int getCurrentPlayer() throws RemoteException {
        return currentPlayer;
    }

    @Override
    public synchronized int getCurrentTurn() throws RemoteException {
        return currentTurn;
    }

    private void checkPhaseTransition() throws RemoteException {
        if (currentTurn == Constants.MOVEMENT_PHASE_START_TURN) {
            // Desbloqueia o centro apenas uma vez, no início da fase de movimentação
            if (board[Constants.CENTER_ROW][Constants.CENTER_COL].equals("BLOCKED")) {
                board[Constants.CENTER_ROW][Constants.CENTER_COL] = "";
                for (SeegaClient client : clients) {
                    client.showMessage("Fase de movimentação iniciada! Centro desbloqueado.");
                }
            }
        }
    }

    @Override
    public synchronized void broadcastMessage(String message) throws RemoteException {
        for (SeegaClient client : clients) {
            client.showMessage(message);
        }
    }

    private void checkGameEnd() throws RemoteException {
        boolean player1HasPieces = false;
        boolean player2HasPieces = false;

        // Verifica peças restantes
        for (int row = 0; row < Constants.BOARD_SIZE; row++) {
            for (int col = 0; col < Constants.BOARD_SIZE; col++) {
                if (board[row][col].equals(Constants.PLAYER_1_SYMBOL)) {
                    player1HasPieces = true;
                } else if (board[row][col].equals(Constants.PLAYER_2_SYMBOL)) {
                    player2HasPieces = true;
                }
            }
        }

        // Determina o resultado
        if (!player1HasPieces || !player2HasPieces) {
            int winner = !player1HasPieces ? 2 : 1;
            broadcastMessage("Jogador 2 venceu!");
            for (SeegaClient client : clients) {
                client.showGameOver(winner);
            }
        }
    }

    private boolean checkCaptures(int player, int movedToRow, int movedToCol) throws RemoteException {
        String playerSymbol = (player == 1) ? Constants.PLAYER_1_SYMBOL : Constants.PLAYER_2_SYMBOL;
        String opponentSymbol = (player == 1) ? Constants.PLAYER_2_SYMBOL : Constants.PLAYER_1_SYMBOL;
        boolean captureOccurred = false;

        int[][] directions = { { -1, 0 }, { 0, 1 }, { 1, 0 }, { 0, -1 } };

        for (int[] dir : directions) {
            int adjacentRow = movedToRow + dir[0];
            int adjacentCol = movedToCol + dir[1];

            if (adjacentRow >= 0 && adjacentRow < Constants.BOARD_SIZE &&
                    adjacentCol >= 0 && adjacentCol < Constants.BOARD_SIZE) {

                // Verifica se é uma peça adversária E NÃO está no centro
                if (board[adjacentRow][adjacentCol].equals(opponentSymbol) &&
                        !(adjacentRow == Constants.CENTER_ROW && adjacentCol == Constants.CENTER_COL)) {

                    int oppositeRow = adjacentRow + dir[0];
                    int oppositeCol = adjacentCol + dir[1];

                    if (oppositeRow >= 0 && oppositeRow < Constants.BOARD_SIZE &&
                            oppositeCol >= 0 && oppositeCol < Constants.BOARD_SIZE &&
                            board[oppositeRow][oppositeCol].equals(playerSymbol)) {

                        board[adjacentRow][adjacentCol] = "";
                        for (SeegaClient client : clients) {
                            client.capturePiece(adjacentRow, adjacentCol);
                        }
                        broadcastMessage("Jogador " + player + " capturou uma peça!");
                        captureOccurred = true;
                    }
                }
            }
        }

        checkGameEnd();
        return captureOccurred;
    }

}