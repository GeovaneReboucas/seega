import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.JMSException;

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
        // registeredUsers.put("Alice", new User("Alice", new Location(-23.550520, -46.633308), false, 10.0));
        // registeredUsers.put("Bob", new User("Bob", new Location(-23.561356, -46.656030), false, 15.0));
        // registeredUsers.put("Charlie", new User("Charlie", new Location(-23.545580, -46.647560), false, 5.0));
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
        // Se o usuario já está registrado, atualiza o handler para o novo socket
        if (registeredUsers.containsKey(user.getName())) {
            User existingUser = registeredUsers.get(user.getName());
            existingUser.setOnline(true);
            onlineUsers.put(user.getName(), handler); // Atualiza o handler para o novo socket
            System.out.println("Usuario " + user.getName() + " reconectado. Handler atualizado: " + handler);
        } else {
            onlineUsers.put(user.getName(), handler);
            registeredUsers.put(user.getName(), user);
            user.setOnline(true);
            System.out.println("Novo usuario " + user.getName() + " online. Handler: " + handler);
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
        System.out.println("Usuario " + user.getName() + " está offline (desconectado).");
        updateContactsForOnlineUsers();
    }

    public void sendMessage(Message message) {
        String senderName = message.getSender();
        String recipientName = message.getRecipient();
        User senderUser = registeredUsers.get(senderName);
        User recipientUser = registeredUsers.get(recipientName);

        if (senderUser == null || recipientUser == null) {
            System.out.println("Remetente ou destinatario nao encontrado.");
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
        
        // Verifica se o destinatário está dentro do raio do remetente (para visibilidade)
        boolean withinSenderRadius = DistanceCalculator.isWithinRadius(
            senderUser.getLocation(), 
            recipientUser.getLocation(), 
            senderUser.getCommunicationRadius()
        );
        
        // Para comunicação síncrona, verifica se o remetente está dentro do raio do destinatário
        boolean withinRecipientRadius = recipientOnline ? 
            DistanceCalculator.isWithinRadius(
                recipientUser.getLocation(),
                senderUser.getLocation(),
                recipientUser.getCommunicationRadius()
            ) : false;

        if (recipientOnline && withinSenderRadius && withinRecipientRadius) {
            // Comunicação síncrona - ambos online e dentro dos respectivos raios
            ClientHandler recipientHandler = onlineUsers.get(recipientName);
            if (recipientHandler != null) {
                recipientHandler.sendSynchronousMessage(message);
                System.out.println("Mensagem sincrona enviada: " + senderName + " -> " + recipientName);
            }
        } else {
            // Comunicação assíncrona
            mqManager.sendAsyncMessage(message);
            System.out.println("Mensagem enfileirada. Motivo: " + 
                            (!recipientOnline ? "Destinatario offline" : 
                            (!withinSenderRadius ? "Fora do raio do remetente" : 
                            "Fora do raio do destinatario")));
        }
    }

    public void updateLocation(String userName, Location newLocation) {
        User user = registeredUsers.get(userName);
        if (user != null) {
            user.setLocation(newLocation);
            System.out.println("Localizacao de " + userName + " atualizada para " + newLocation);
            
            // Verifica mensagens pendentes para este usuário
            sendPendingMessages(userName);
            
            // Verifica mensagens de outros usuários que podem agora ver este usuário
            checkAndDeliverPendingMessages();
            
            updateContactsForOnlineUsers();
        }
    }


    public synchronized void updateStatus(String userName, boolean isOnline) {
        User user = registeredUsers.get(userName);
        
        if (user != null) {
            user.setOnline(isOnline);
            System.out.println("Status de " + userName + " atualizado para " + (isOnline ? "Online" : "Offline"));
            
            if (isOnline) {
                // Verifica mensagens pendentes para este usuário
                sendPendingMessages(userName);
                
                // Verifica mensagens de outros usuários que podem ver este usuário
                checkAndDeliverPendingMessages();
            }
            
            updateContactsForOnlineUsers();
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
                    
                    User senderUser = registeredUsers.get(sender);
                    if (senderUser != null && isMutuallyVisible(user, senderUser)) {
                        Message message = new Message(content, sender, userName, Message.MessageType.valueOf(type));
                        message.setTimestamp(LocalDateTime.parse(timestamp));
                        handler.sendSynchronousMessage(message);
                        System.out.println("Mensagem pendente entregue (síncrona): " + sender + " -> " + userName);
                    } else {
                        // Mantém na fila se não houver visibilidade mútua
                        mqManager.sendAsyncMessage(new Message(content, sender, userName, Message.MessageType.ASYNCHRONOUS));
                    }
                }
            }
        }
    }

    public void updateCommunicationRadius(String userName, double newRadius) {
        User user = registeredUsers.get(userName);
        if (user != null) {
            user.setCommunicationRadius(newRadius);
            System.out.println("Raio de comunicacao de " + userName + " atualizado para " + newRadius + " km.");
            
            // Verifica mensagens pendentes para este usuário
            sendPendingMessages(userName);
            
            // Verifica mensagens de outros usuários que podem agora ver este usuário
            checkAndDeliverPendingMessages();
            
            updateContactsForOnlineUsers();
        }
    }


    public synchronized void updateContactsForOnlineUsers() {
        for (ClientHandler handler : onlineUsers.values()) {
            User currentUser = registeredUsers.get(handler.getUserName());
            if (currentUser != null) {
                List<User> visibleContacts = new ArrayList<>();
                
                for (User otherUser : registeredUsers.values()) {
                    if (!currentUser.equals(otherUser)) {
                        // Verifica se está dentro do raio do usuário atual
                        boolean withinCurrentUserRadius = DistanceCalculator.isWithinRadius(
                            currentUser.getLocation(), 
                            otherUser.getLocation(), 
                            currentUser.getCommunicationRadius()
                        );
                        
                        // Cria cópia do usuário com status ajustado
                        User contactCopy = new User(otherUser.getName(), 
                                                otherUser.getLocation(),
                                                isMutuallyVisible(currentUser, otherUser),
                                                otherUser.getCommunicationRadius());
                        
                        if (withinCurrentUserRadius) {
                            visibleContacts.add(contactCopy);
                        }
                    }
                }
                currentUser.getContacts().clear();
                currentUser.getContacts().addAll(visibleContacts);
                handler.sendContactList(currentUser.getContacts());
            }
        }
    }

    private boolean isMutuallyVisible(User user1, User user2) {
        boolean user1CanSeeUser2 = DistanceCalculator.isWithinRadius(
            user1.getLocation(),
            user2.getLocation(),
            user1.getCommunicationRadius()
        );
        
        boolean user2CanSeeUser1 = DistanceCalculator.isWithinRadius(
            user2.getLocation(),
            user1.getLocation(),
            user2.getCommunicationRadius()
        );
        
        return user1.isOnline() && user2.isOnline() && user1CanSeeUser2 && user2CanSeeUser1;
    }

    public void checkAndDeliverPendingMessages() {
        for (User user : registeredUsers.values()) {
            if (user.isOnline()) {
                sendPendingMessages(user.getName());
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