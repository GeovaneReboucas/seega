package src;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class Registrador {
    public static void main(String args[]) {
        try {
            // Cria o registro RMI na porta padrão 1099
            LocateRegistry.createRegistry(1099);

            // Cria a instância do servidor
            SeegaServerImpl servidor = new SeegaServerImpl();

            // Registra o servidor no RMI Registry
            Naming.rebind("//localhost/SeegaServer", servidor);

            System.out.println("Servidor Seega registrado com sucesso!");

        } catch (Exception e) {
            System.out.println("Erro ao registrar o servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}