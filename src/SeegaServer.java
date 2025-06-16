package src;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SeegaServer extends Remote {
    void setStartingPlayer(int player) throws RemoteException;

    boolean makeMove(int player, int row, int col) throws RemoteException;

    boolean movePiece(int player, int fromRow, int fromCol, int toRow, int toCol) throws RemoteException;

    boolean hasValidMoves(int player) throws RemoteException;

    String[][] getBoard() throws RemoteException;

    int getCurrentPlayer() throws RemoteException;

    int getCurrentTurn() throws RemoteException;

    int registerClient(SeegaClient client) throws RemoteException;

    void broadcastMessage(String message) throws RemoteException;

    // void checkCaptures(int row, int col, int player) throws RemoteException;
}