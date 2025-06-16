package src;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import src.ui.ClientUI;
import src.utils.Constants;

public class SeegaClientImpl extends UnicastRemoteObject implements SeegaClient {
    private static final long serialVersionUID = 1L;
    private ClientUI clientUI;
    private int clientId;
    private SeegaServer server;

    public SeegaClientImpl(ClientUI clientUI, int clientId, SeegaServer server) throws RemoteException {
        super();
        this.clientUI = clientUI;
        this.clientId = clientId;
        this.server = server;
    }

    public void setClientUI(ClientUI clientUI) {
        this.clientUI = clientUI;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
        if (clientUI != null) {
            SwingUtilities.invokeLater(() -> {
                clientUI.setTitle("Seega - Cliente " + clientId);
            });
        }
    }

    @Override
    public void updateBoard(String[][] board) throws RemoteException {
        if (clientUI != null) {
            SwingUtilities.invokeLater(() -> {
                clientUI.updateBoard(board);
            });
        }
    }

    @Override
    public void setCurrentPlayer(int player) throws RemoteException {
        if (clientUI != null) {
            SwingUtilities.invokeLater(() -> {
                clientUI.setCurrentPlayer(player);
            });
        }
    }

    @Override
    public void setCurrentTurn(int turn) throws RemoteException {
        if (clientUI != null) {
            SwingUtilities.invokeLater(() -> {
                clientUI.setCurrentTurn(turn);
            });
        }
    }

    @Override
    public void showMessage(String message) throws RemoteException {
        if (clientUI != null) {
            SwingUtilities.invokeLater(() -> {
                clientUI.appendMessage(message);
            });
        }
    }

    @Override
    public void showGameOver(int winner) throws RemoteException {
        if (clientUI != null) {
            SwingUtilities.invokeLater(() -> {
                clientUI.showGameResult(winner == clientId);
                clientUI.disableResignButton();
            });
        }
    }

    @Override
    public void promptForStartingPlayer() throws RemoteException {
        if (clientUI != null) {
            SwingUtilities.invokeLater(() -> {
                int option = JOptionPane.showOptionDialog(null,
                        Constants.STARTING_PLAYER_QUESTION,
                        Constants.STARTING_PLAYER_TITLE,
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        Constants.PLAYER_OPTIONS,
                        Constants.DEFAULT_PLAYER_OPTION);

                int startingPlayer = (option == 0) ? 1 : 2;
                try {
                    server.setStartingPlayer(startingPlayer);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void makeMove(int row, int col) {
        try {
            if (server.getCurrentPlayer() == clientId) {
                server.makeMove(clientId, row, col);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        try {
            if (server.getCurrentPlayer() == clientId) {
                server.movePiece(clientId, fromRow, fromCol, toRow, toCol);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}