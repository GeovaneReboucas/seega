import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

    public void registerUser(User user, ClientHandler handler) {
        onlineUsers.put(user.getName(), handler);
        registeredUsers.put(user.getName(), user);
        user.setOnline(true);
        System.out.println("Usuário " + user.getName() + " está online. Handler: " + handler);
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
        String recipientName = message.getRecipient();
        ClientHandler recipientHandler = onlineUsers.get(recipientName);
        User senderUser = registeredUsers.get(message.getSender());
        User recipientUser = registeredUsers.get(recipientName);

        if (senderUser == null || recipientUser == null) {
            System.out.println("Remetente ou destinatário não encontrado.");
            return;
        }

        if (message.getType() == Message.MessageType.SYNCHRONOUS) {
            if (recipientHandler != null && recipientUser.isOnline()) {
                // Verificar raio de comunicação
                if (DistanceCalculator.isWithinRadius(senderUser.getLocation(), recipientUser.getLocation(), senderUser.getCommunicationRadius())) {
                    recipientHandler.sendSynchronousMessage(message);
                    System.out.println("Mensagem síncrona enviada de " + message.getSender() + " para " + message.getRecipient());
                } else {
                    System.out.println("Usuário " + recipientName + " fora do raio de comunicação de " + senderUser.getName() + ". Enviando como assíncrona.");
                    mqManager.sendAsyncMessage(message);
                }
            } else {
                System.out.println("Usuário " + recipientName + " offline ou não encontrado para mensagem síncrona. Enviando como assíncrona.");
                mqManager.sendAsyncMessage(message);
            }
        } else if (message.getType() == Message.MessageType.ASYNCHRONOUS) {
            mqManager.sendAsyncMessage(message);
            System.out.println("Mensagem assíncrona de " + message.getSender() + " para " + message.getRecipient() + " enviada para a fila.");
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

    public void updateStatus(String userName, boolean isOnline) {
        User user = registeredUsers.get(userName);
        
        System.out.println("DEBUG: updateStatus chamado para " + userName + " com isOnline = " + isOnline);

        if (user != null) {
            user.setOnline(isOnline);
            System.out.println("Status de " + userName + " atualizado para " + (isOnline ? "Online" : "Offline") + ". (Interno: " + user.isOnline() + ")");
            
            if (isOnline) {
                // Se o usuário está ficando online, adiciona ou atualiza o handler no mapa onlineUsers
                // Isso é crucial para que o servidor saiba que o usuário está ativo e pode receber mensagens síncronas
                // e para que as mensagens pendentes sejam enviadas.
                // O handler é passado no registerUser, mas se o status for atualizado via UI, precisamos garantir que o handler esteja lá.
                // No entanto, o handler só existe se o socket estiver conectado. Se o usuário estava offline e o socket foi fechado,
                // ele precisaria se reconectar para ter um handler válido.
                // Por enquanto, vamos assumir que o handler ainda está ativo se o usuário está atualizando o status via UI.
                // Se o handler for null aqui, significa que o cliente se desconectou e tentou atualizar o status sem reconectar.
                ClientHandler handler = onlineUsers.get(userName);
                if (handler != null) {
                    sendPendingMessages(userName);
                    System.out.println("DEBUG: Mensagens pendentes enviadas para " + userName);
                } else {
                    System.out.println("DEBUG: Handler para " + userName + " é NULL. Não é possível enviar mensagens pendentes.");
                }
            } else {
                // Se o usuário está ficando offline, remove o handler do mapa onlineUsers
                onlineUsers.remove(userName);
                System.out.println("DEBUG: Handler para " + userName + " removido de onlineUsers.");
            }
            
            // Sempre atualizar a lista de contatos para refletir o novo status
            updateContactsForOnlineUsers();
        }
    }

    private void sendPendingMessages(String userName) {
        List<String> pendingMessages = mqManager.receiveAsyncMessages(userName);
        ClientHandler handler = onlineUsers.get(userName);
        if (handler != null) {
            System.out.println("DEBUG: Tentando enviar " + pendingMessages.size() + " mensagens pendentes para " + userName);
            for (String msg : pendingMessages) {
                // Formato da mensagem: sender|content|timestamp
                String[] msgParts = msg.split("\\|");
                if (msgParts.length >= 3) {
                    String sender = msgParts[0];
                    String content = msgParts[1];
                    // Reconstruir a mensagem para enviar ao cliente
                    Message asyncMessage = new Message(content, sender, userName, Message.MessageType.ASYNCHRONOUS);
                    handler.sendSynchronousMessage(asyncMessage); // Reutilizando o método de envio síncrono para entregar a mensagem
                    System.out.println("DEBUG: Mensagem pendente enviada: de " + sender + " para " + userName + ": " + content);
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

