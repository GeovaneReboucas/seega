package src.services;

import java.util.*;

import src.handlers.ClientHandler;
import src.services.BroadcastService;
import src.utils.Constants;

public class ServerService {
    private static List<ClientHandler> clients;
    private static int currentTurn = Constants.INITIAL_TURN;
    private static int currentPlayer = 1;
    private static int movesInCurrentBlock = 0;
    private static String[][] board;
    private static int lastPlayerToPlace = 0;
    private static boolean startingPlayerChosen = false;
    private static BroadcastService broadcastService;

    public ServerService(List<ClientHandler> clients, BroadcastService broadcastService) {
        ServerService.clients = clients;
        ServerService.broadcastService = broadcastService;
        initializeBoard();
    }

    private void initializeBoard() {
        board = new String[Constants.BOARD_SIZE][Constants.BOARD_SIZE];
        for (int i = 0; i < Constants.BOARD_SIZE; i++) {
            Arrays.fill(board[i], "");
        }
        // Bloqueia o centro inicialmente
        board[Constants.CENTER_ROW][Constants.CENTER_COL] = "BLOCKED";
    }

    public synchronized void setStartingPlayer(int player) {
        currentPlayer = player;
        startingPlayerChosen = true;
    }

    public synchronized boolean hasValidMoves(int player) {
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

    public synchronized boolean makeMove(int player, int row, int col) {
        if (player != currentPlayer)
            return false;

        // Verifica se está tentando jogar no centro bloqueado
        if (currentTurn <= Constants.PLACEMENT_PHASE_END_TURN && 
            row == Constants.CENTER_ROW && col == Constants.CENTER_COL) {
            return false;
        }

        if (board[row][col].isEmpty()) {
            board[row][col] = (player == 1) ? Constants.PLAYER_1_SYMBOL : Constants.PLAYER_2_SYMBOL;
            movesInCurrentBlock++;

            if (currentTurn == Constants.PLACEMENT_PHASE_END_TURN) {
                board[Constants.CENTER_ROW][Constants.CENTER_COL] = "";
                broadcastService.broadcastCenterBlocked(false);
            }

            currentTurn++;
            checkPhaseTransition();

            if (currentTurn <= Constants.PLACEMENT_PHASE_END_TURN) {
                if (movesInCurrentBlock >= Constants.MOVES_PER_BLOCK) {
                    currentPlayer = (currentPlayer == 1) ? 2 : 1;
                    movesInCurrentBlock = 0;
                    lastPlayerToPlace = player;
                }
            } else {
                currentPlayer = (currentPlayer == 1) ? 2 : 1;
                movesInCurrentBlock = 0;
            }

            broadcastService.broadcastMove(player, row, col);
            broadcastService.broadcastTurnInfo(currentPlayer, currentTurn);
            return true;
        }
        return false;
    }

    public synchronized boolean movePiece(int player, int fromRow, int fromCol, int toRow, int toCol) {
        if (player != currentPlayer || currentTurn < Constants.MOVEMENT_PHASE_START_TURN) {
            return false;
        }

        String playerSymbol = (player == 1) ? Constants.PLAYER_1_SYMBOL : Constants.PLAYER_2_SYMBOL;

        if (!board[fromRow][fromCol].equals(playerSymbol)) {
            return false;
        }

        if (!board[toRow][toCol].isEmpty()) {
            return false;
        }

        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);

        if ((rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)) {
            board[fromRow][fromCol] = "";
            board[toRow][toCol] = playerSymbol;

            boolean captureOccurred = checkCaptures(player, toRow, toCol);

            if (!captureOccurred) {
                int nextPlayer = (currentPlayer == 1) ? 2 : 1;

                if (!hasValidMoves(nextPlayer)) {
                    broadcastService.broadcastAutoPass(nextPlayer);

                    if (!hasValidMoves(currentPlayer)) {
                        broadcastService.broadcastGameOver();
                        return true;
                    }

                    currentTurn++;
                    broadcastService.broadcastPieceMove(player, fromRow, fromCol, toRow, toCol);
                    broadcastService.broadcastTurnInfo(currentPlayer, currentTurn);
                    return true;
                }

                currentPlayer = nextPlayer;
            }

            currentTurn++;
            broadcastService.broadcastPieceMove(player, fromRow, fromCol, toRow, toCol);
            broadcastService.broadcastTurnInfo(currentPlayer, currentTurn);
            return true;
        }

        return false;
    }

    private void checkPhaseTransition() {
        if (currentTurn == Constants.MOVEMENT_PHASE_START_TURN) {
            currentPlayer = lastPlayerToPlace;
            broadcastService.broadcastTurnInfo(currentPlayer, currentTurn);
        }
    }

    private synchronized boolean checkCaptures(int player, int movedToRow, int movedToCol) {
        String playerSymbol = (player == 1) ? Constants.PLAYER_1_SYMBOL : Constants.PLAYER_2_SYMBOL;
        String opponentSymbol = (player == 1) ? Constants.PLAYER_2_SYMBOL : Constants.PLAYER_1_SYMBOL;
        boolean captureOccurred = false;

        int[][] directions = { { -1, 0 }, { 0, 1 }, { 1, 0 }, { 0, -1 } };

        for (int[] dir : directions) {
            int adjacentRow = movedToRow + dir[0];
            int adjacentCol = movedToCol + dir[1];

            if (adjacentRow >= 0 && adjacentRow < Constants.BOARD_SIZE && 
                adjacentCol >= 0 && adjacentCol < Constants.BOARD_SIZE) {
                
                if (board[adjacentRow][adjacentCol].equals(opponentSymbol)) {
                    int oppositeRow = adjacentRow + dir[0];
                    int oppositeCol = adjacentCol + dir[1];

                    if (oppositeRow >= 0 && oppositeRow < Constants.BOARD_SIZE && 
                        oppositeCol >= 0 && oppositeCol < Constants.BOARD_SIZE) {
                        
                        if (board[oppositeRow][oppositeCol].equals(playerSymbol)) {
                            if (!(adjacentRow == Constants.CENTER_ROW && 
                                  adjacentCol == Constants.CENTER_COL)) {
                                board[adjacentRow][adjacentCol] = "";
                                broadcastService.broadcastCapture(adjacentRow, adjacentCol);
                                captureOccurred = true;
                            }
                        }
                    }
                }
            }
        }

        checkGameEnd();
        return captureOccurred;
    }

    private synchronized void checkGameEnd() {
        boolean player1HasPieces = false;
        boolean player2HasPieces = false;

        for (int row = 0; row < Constants.BOARD_SIZE; row++) {
            for (int col = 0; col < Constants.BOARD_SIZE; col++) {
                if (board[row][col].equals(Constants.PLAYER_1_SYMBOL)) {
                    player1HasPieces = true;
                } else if (board[row][col].equals(Constants.PLAYER_2_SYMBOL)) {
                    player2HasPieces = true;
                }
            }
        }

        if (!player1HasPieces || !player2HasPieces) {
            int winner = !player1HasPieces ? 2 : 1;
            broadcastService.broadcastGameOver(winner);
        }
    }

    public String[][] getBoard() {
        return board;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public boolean getStartingPlayerChosen() {
        return startingPlayerChosen;
    }
}