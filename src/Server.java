import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 5555;
    private Map<String, ClientHandler> onlineUsers;
    private Map<String, User> registeredUsers;
    private MessageQueueManager mqManager;

    public Server() {
        onlineUsers = new ConcurrentHashMap<>();
        registeredUsers = new ConcurrentHashMap<>();
        mqManager = new MessageQueueManager();
        // Adicionar alguns usuários de exemplo para teste
        registeredUsers.put("Alice", new User("Alice", new Location(-23.550520, -46.633308), false, 10.0));
        registeredUsers.put("Bob", new User("Bob", new Location(-23.561356, -46.656030), false, 15.0));
        registeredUsers.put("Charlie", new User("Charlie", new Location(-23.545580, -46.647560), false, 5.0));
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado na porta " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket);
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    public synchronized void registerUser(User user, ClientHandler handler) {
        // Se o usuário já está registrado, atualiza o handler para o novo socket
        if (registeredUsers.containsKey(user.getName())) {
            User existingUser = registeredUsers.get(user.getName());
            existingUser.setOnline(true);
            onlineUsers.put(user.getName(), handler); // Atualiza o handler para o novo socket
            System.out.println("Usuário " + user.getName() + " reconectado. Handler atualizado: " + handler);
        } else {
            onlineUsers.put(user.getName(), handler);
            registeredUsers.put(user.getName(), user);
            user.setOnline(true);
            System.out.println("Novo usuário " + user.getName() + " online. Handler: " + handler);
        }
        updateContactsForOnlineUsers();
        // Ao logar, verificar se há mensagens pendentes
        sendPendingMessages(user.getName());
    }

    public void unregisterUser(User user) {
        // Não remove do onlineUsers aqui, pois o updateStatus já lida com isso
        // onlineUsers.remove(user.getName()); 
        if (registeredUsers.containsKey(user.getName())) {
            registeredUsers.get(user.getName()).setOnline(false);
        }
        System.out.println("Usuário " + user.getName() + " está offline (desconectado).");
        updateContactsForOnlineUsers();
    }

    public void sendMessage(Message message) {
        String senderName = message.getSender();
        String recipientName = message.getRecipient();
        User senderUser = registeredUsers.get(senderName);
        User recipientUser = registeredUsers.get(recipientName);

        if (senderUser == null || recipientUser == null) {
            System.out.println("Remetente ou destinatário não encontrado.");
            return;
        }

        // Sempre envia para a fila se o remetente estiver offline
        if (!senderUser.isOnline()) {
            mqManager.sendAsyncMessage(message);
            System.out.println("Remetente offline. Mensagem enfileirada: " + senderName + " -> " + recipientName);
            return;
        }

        // Se chegou aqui, o remetente está online
        boolean recipientOnline = recipientUser.isOnline();
        boolean withinRadius = DistanceCalculator.isWithinRadius(
            senderUser.getLocation(), 
            recipientUser.getLocation(), 
            senderUser.getCommunicationRadius()
        );

        if (recipientOnline && withinRadius) {
            ClientHandler recipientHandler = onlineUsers.get(recipientName);
            if (recipientHandler != null) {
                recipientHandler.sendSynchronousMessage(message);
                System.out.println("Mensagem síncrona enviada: " + senderName + " -> " + recipientName);
            }
        } else {
            mqManager.sendAsyncMessage(message);
            System.out.println("Mensagem enfileirada. Motivo: " + 
                            (recipientOnline ? "Fora do raio" : "Destinatário offline"));
        }
    }

    public void updateLocation(String userName, Location newLocation) {
        User user = registeredUsers.get(userName);
        if (user != null) {
            user.setLocation(newLocation);
            System.out.println("Localização de " + userName + " atualizada para " + newLocation);
            updateContactsForOnlineUsers();
        }
    }

    public synchronized void updateStatus(String userName, boolean isOnline) {
        User user = registeredUsers.get(userName);
        
        if (user != null) {
            user.setOnline(isOnline);
            System.out.println("Status de " + userName + " atualizado para " + (isOnline ? "Online" : "Offline"));
            
            // Se estiver voltando online e não tiver handler, tentar encontrar um
            if (isOnline && !onlineUsers.containsKey(userName)) {
                // Procura por qualquer handler existente para este usuário
                for (Map.Entry<String, ClientHandler> entry : onlineUsers.entrySet()) {
                    if (entry.getValue().getUserName().equals(userName)) {
                        onlineUsers.put(userName, entry.getValue());
                        break;
                    }
                }
            }
            
            // Atualizar lista de contatos
            updateContactsForOnlineUsers();
            
            // Se estiver voltando online, verificar mensagens pendentes
            if (isOnline) {
                // Verifica mensagens pendentes para este usuário
                sendPendingMessages(userName);
                
                // Verifica se este usuário tem mensagens pendentes para outros
                for (User userRegister : registeredUsers.values()) {
                    if (userRegister.isOnline() && !userRegister.getName().equals(userName)) {
                        sendPendingMessages(userRegister.getName());
                    }
                }

            }
        }
    }

    public void sendPendingMessages(String userName) {
        User user = registeredUsers.get(userName);
        if (user == null || !user.isOnline()) return;

        List<String> pendingMessages = mqManager.receiveAsyncMessages(userName);
        ClientHandler handler = onlineUsers.get(userName);
        
        if (handler != null) {
            for (String msg : pendingMessages) {
                String[] parts = msg.split("\\|");
                if (parts.length >= 4) {
                    String sender = parts[0];
                    String content = parts[1];
                    String timestamp = parts[2];
                    String type = parts[3];
                    
                    // Cria e envia a mensagem independente do status do remetente
                    Message message = new Message(content, sender, userName, Message.MessageType.valueOf(type));
                    message.setTimestamp(LocalDateTime.parse(timestamp)); // Você precisará adicionar este método na classe Message
                    handler.sendSynchronousMessage(message);
                    System.out.println("Mensagem pendente entregue: " + sender + " -> " + userName);
                }
            }
        }
    }

    public void updateCommunicationRadius(String userName, double newRadius) {
        User user = registeredUsers.get(userName);
        if (user != null) {
            user.setCommunicationRadius(newRadius);
            System.out.println("Raio de comunicação de " + userName + " atualizado para " + newRadius + " km.");
            updateContactsForOnlineUsers();
        }
    }

    private void updateContactsForOnlineUsers() {
        for (ClientHandler handler : onlineUsers.values()) {
            User currentUser = registeredUsers.get(handler.getUserName());
            if (currentUser != null) {
                List<User> nearbyContacts = new ArrayList<>();
                for (User otherUser : registeredUsers.values()) {
                    if (!currentUser.equals(otherUser) && 
                        DistanceCalculator.isWithinRadius(currentUser.getLocation(), otherUser.getLocation(), currentUser.getCommunicationRadius())) {
                        nearbyContacts.add(otherUser);
                    }
                }
                currentUser.getContacts().clear();
                for (User contact : nearbyContacts) {
                    currentUser.addContact(contact);
                }
                handler.sendContactList(currentUser.getContacts());
            }
        }
    }

    public User getRegisteredUser(String userName) {
        return registeredUsers.get(userName);
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}