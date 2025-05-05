package src.entities;

import java.io.*;
import java.net.*;
import java.util.*;

import src.handlers.ClientHandler;
import src.services.BroadcastService;
import src.services.ServerService;
import src.utils.Constants;

public class Server {
    private static int clientCounter = 0;
    private static ServerService serverService;
    private static BroadcastService broadcastService;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(Constants.SERVER_PORT);
        System.out.println(Constants.SERVER_START_MESSAGE + Constants.SERVER_PORT);

        List<ClientHandler> clients = new ArrayList<>();
        broadcastService = new BroadcastService(clients);
        serverService = new ServerService(clients, broadcastService);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            clientCounter++;
            ClientHandler handler = new ClientHandler(clientSocket, clientCounter, serverService, broadcastService);
            clients.add(handler);
            new Thread(handler).start();

            if (clientCounter == 2) {
                while (!serverService.getStartingPlayerChosen()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                broadcastService.broadcastTurnInfo(serverService.getCurrentPlayer(), serverService.getCurrentTurn());
                broadcastService.broadcastCenterBlocked(true);
            }
        }
    }
}