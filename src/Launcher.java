package src;

import java.io.IOException;

public class Launcher {

    public static void main(String[] args) {
        // Inicia o Server em uma nova thread
        Thread serverThread = new Thread(() -> {
            try {
                // Cria e executa o servidor
                Server.main(new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Inicia os dois Clients em novas threads
        Thread clientThread1 = new Thread(() -> {
            try {
                // Cria e executa o primeiro cliente
                Client.main(new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread clientThread2 = new Thread(() -> {
            try {
                // Cria e executa o segundo cliente
                Client.main(new String[]{});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Inicia as threads
        serverThread.start();
        clientThread1.start();
        clientThread2.start();

        try {
            // Espera o servidor terminar antes de finalizar o launcher (opcional)
            serverThread.join();
            clientThread1.join();
            clientThread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
