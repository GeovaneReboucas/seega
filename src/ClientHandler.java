import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Server server;
    private BufferedReader reader;
    private PrintWriter writer;
    private String userName;

    public ClientHandler(Socket clientSocket, Server server) {
        this.clientSocket = clientSocket;
        this.server = server;
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Erro ao criar streams: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                System.out.println("DEBUG: Comando recebido de " + userName + ": " + inputLine);
                processCommand(inputLine);
            }
        } catch (IOException e) {
            System.err.println("Erro na comunicação com cliente: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void processCommand(String command) {
        String[] parts = command.split("\\|");
        String action = parts[0];

        switch (action) {
            case "LOGIN":
                if (parts.length >= 4) { // Agora são 4 partes (sem o status)
                    String name = parts[1];
                    double lat = Double.parseDouble(parts[2]);
                    double lon = Double.parseDouble(parts[3]);
                    double radius = Double.parseDouble(parts[4]);
                    
                    // Sempre cria como online
                    User user = new User(name, new Location(lat, lon), true, radius);
                    this.userName = name;
                    server.registerUser(user, this);
                    writer.println("LOGIN_SUCCESS|" + name);
                    System.out.println("DEBUG: Login processado para " + name);
                    
                    // Forçar atualização de status para online
                    server.updateStatus(name, true);
                }
                break;
                
            case "SEND_MESSAGE":
                if (parts.length >= 4) {
                    String content = parts[1];
                    String recipient = parts[2];
                    String messageType = parts[3];
                    
                    Message.MessageType type = messageType.equals("SYNC") ? 
                        Message.MessageType.SYNCHRONOUS : Message.MessageType.ASYNCHRONOUS;
                    
                    Message message = new Message(content, userName, recipient, type);
                    server.sendMessage(message);
                    System.out.println("DEBUG: Mensagem processada de " + userName + " para " + recipient);
                }
                break;
                
            case "UPDATE_LOCATION":
                if (parts.length >= 3) {
                    double lat = Double.parseDouble(parts[1]);
                    double lon = Double.parseDouble(parts[2]);
                    server.updateLocation(userName, new Location(lat, lon));
                    System.out.println("DEBUG: Localização atualizada para " + userName);
                }
                break;
                
            case "UPDATE_STATUS":
                if (parts.length >= 2) {
                    boolean isOnline = Boolean.parseBoolean(parts[1]);
                    System.out.println("Status atualizado para " + userName + ": " + (isOnline ? "ONLINE" : "OFFLINE"));
                    server.updateStatus(userName, isOnline);
                    
                    if (isOnline) {
                        System.out.println("Verificando mensagens pendentes para " + userName);
                        server.sendPendingMessages(userName);
                    }
                }
                break;

            case "CHECK_PENDING":
                if (parts.length >= 2) {
                    String userToCheck = parts[1];
                    server.sendPendingMessages(userToCheck);
                }
                break;

            case "UPDATE_RADIUS":
                if (parts.length >= 2) {
                    double newRadius = Double.parseDouble(parts[1]);
                    server.updateCommunicationRadius(userName, newRadius);
                    System.out.println("DEBUG: Raio atualizado para " + userName);
                }
                break;
                
            default:
                System.out.println("Comando desconhecido: " + action);
        }
    }

    public void sendSynchronousMessage(Message message) {
        // Inclui o tipo da mensagem no envio
        writer.println("MESSAGE|" + 
                    message.getSender() + "|" + 
                    message.getContent() + "|" + 
                    message.getTimestamp() + "|" + 
                    message.getType());
    }

    public void sendContactList(List<User> contacts) {
        StringBuilder contactsStr = new StringBuilder("CONTACTS");
        for (User contact : contacts) {
            contactsStr.append("|").append(contact.getName())
                      .append(",").append(contact.getLocation().getLatitude())
                      .append(",").append(contact.getLocation().getLongitude())
                      .append(",").append(contact.isOnline());
        }
        writer.println(contactsStr.toString());
    }

    public String getUserName() {
        return userName;
    }

    private void disconnect() {
        if (userName != null) {
            User user = server.getRegisteredUser(userName);
            if (user != null) {
                // Ao desconectar, definimos o status como offline
                server.updateStatus(user.getName(), false);
                System.out.println("DEBUG: Usuário " + userName + " desconectado e marcado como offline.");
            }
        }
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close(); // Verifica se o socket não está fechado antes de tentar fechar
        } catch (IOException e) {
            System.err.println("Erro ao fechar conexão: " + e.getMessage());
        }
    }

    public PrintWriter getWriter() {
        return writer;
    }

}

