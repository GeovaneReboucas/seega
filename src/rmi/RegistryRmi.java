package src.rmi;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class RegistryRmi {
    public static void main(String args[]) {
        try {
            LocateRegistry.createRegistry(1099);
            SeegaServerImpl servidor = new SeegaServerImpl();
            String serverIp = (args.length > 0) ? args[0] : "localhost";
            String serverUrl = "//" + serverIp + "/SeegaServer";
            Naming.rebind(serverUrl, servidor);

            System.out.println("Servidor Seega registrado com sucesso!");
            System.out.println("Server URL: " + serverUrl);
        } catch (Exception e) {
            System.out.println("Erro ao registrar o servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}