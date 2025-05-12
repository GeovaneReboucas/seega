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
        int port;
        try {
            port = args.length > 0 ? parsePort(args[0]) : Constants.SERVER_PORT;
        } catch (IllegalArgumentException e) {
            System.err.println("Erro: " + e.getMessage());
            System.err.println("Usando porta padrão " + Constants.SERVER_PORT + " instead.");
            port = Constants.SERVER_PORT;
        }

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println(Constants.SERVER_START_MESSAGE + port);

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

    private static int parsePort(String portStr) throws IllegalArgumentException {
        try {
            int port = Integer.parseInt(portStr);
            if (port < 1024 || port > 65535) {
                throw new IllegalArgumentException("Porta deve estar entre 1024 e 65535");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Porta deve ser um número válido");
        }
    }
}