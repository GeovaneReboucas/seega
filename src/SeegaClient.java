package src;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SeegaClient extends Remote {
    void updateBoard(String[][] board) throws RemoteException;

    void setCurrentPlayer(int player) throws RemoteException;

    void setCurrentTurn(int turn) throws RemoteException;

    void showMessage(String message) throws RemoteException;

    void showGameOver(int winner) throws RemoteException;

    void promptForStartingPlayer() throws RemoteException;
}