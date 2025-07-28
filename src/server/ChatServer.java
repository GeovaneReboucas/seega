package server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;

import client.User;
import model.ChatGroup;
import model.GroupMessage;

public class ChatServer {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String USER_TOPIC = "USERS";

    private List<ChatGroup> topics = new ArrayList<>();
    private List<User> users = new ArrayList<>();

    public static void main(String[] args) {
        new ChatServer().start();
    }

    public void start() {
        try {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
            Connection connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Solicita sincronização imediata ao iniciar
            Queue initSyncQueue = session.createQueue("SYNC_USERS");
            MessageProducer initProducer = session.createProducer(initSyncQueue);
            initProducer.send(session.createTextMessage("REQUEST_SYNC"));
            System.out.println("Solicitada sincronização inicial de usuarios");

            // Tópico para atualizações de usuarios
            Topic userTopic = session.createTopic(USER_TOPIC);
            MessageProducer userProducer = session.createProducer(userTopic);

            // Fila para conexões de usuarios
            Queue connectionQueue = session.createQueue("CONNECTIONS");
            MessageConsumer connectionConsumer = session.createConsumer(connectionQueue);

            connectionConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String userName = ((TextMessage) message).getText();
                        System.out.println("[DEBUG] Tentativa de conexao recebida para: " + userName);

                        Queue responseQueue = session.createQueue("CONNECTION_RESPONSE." + userName);
                        MessageProducer responseProducer = session.createProducer(responseQueue);

                        // Verifica se o usuario existe
                        if (users.stream().anyMatch(u -> u.getName().equals(userName))) {
                            // Envia confirmação de sucesso
                            TextMessage successMessage = session.createTextMessage("SUCCESS");
                            responseProducer.send(successMessage);

                            // Envia lista inicial de usuarios
                            Queue initialUsersQueue = session.createQueue("INITIAL_USERS." + userName);
                            MessageProducer initialProducer = session.createProducer(initialUsersQueue);

                            String usersList = users.stream()
                                    .map(User::getName)
                                    .filter(name -> !name.equals(userName))
                                    .collect(Collectors.joining(","));

                            TextMessage initialUsersMessage = session.createTextMessage(usersList);
                            initialProducer.send(initialUsersMessage);

                            System.out.println("usuario conectado: " + userName);
                        } else {
                            // usuario não encontrado
                            TextMessage errorMessage = session
                                    .createTextMessage("ERROR: usuario não encontrado. Contate o administrador.");
                            responseProducer.send(errorMessage);
                            System.out.println("Tentativa de conexao falhou - usuario nao existe: " + userName);
                        }
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            // No método start():
            Queue userCreationQueue = session.createQueue("USER_CREATION");
            MessageConsumer userCreationConsumer = session.createConsumer(userCreationQueue);

            // No método start(), atualize o listener:
            userCreationConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String userName = ((TextMessage) message).getText();

                        if (userName.startsWith("REQUEST_") || userName.startsWith("SYNC_")) {
                            return; // Ignora mensagens de sistema
                        }

                        if (users.stream().noneMatch(u -> u.getName().equals(userName))) {
                            users.add(new User(userName));
                            System.out.println("usuario registrado: " + userName);

                            // Notifica TODOS os clientes sobre o novo usuario
                            Topic userT = session.createTopic("USERS");
                            MessageProducer producer = session.createProducer(userT);
                            TextMessage updateMsg = session.createTextMessage("ADD:" + userName);
                            producer.send(updateMsg);

                            // Notifica os admins
                            Topic adminTopic = session.createTopic("ADMIN_UPDATES");
                            MessageProducer adminProducer = session.createProducer(adminTopic);
                            adminProducer.send(session.createTextMessage("REFRESH_USERS"));
                        }
                    }
                } catch (JMSException e) {
                    System.err.println("Erro ao processar criação de usuario: " + e.getMessage());
                }
            });

            // Fila para notificações de novos usuarios do AdminUI
            Queue userAddedQueue = session.createQueue("USER_ADDED_NOTIFICATION");
            MessageConsumer userAddedConsumer = session.createConsumer(userAddedQueue);

            userAddedConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String userName = ((TextMessage) message).getText();
                        if (users.stream().noneMatch(u -> u.getName().equals(userName))) {
                            users.add(new User(userName));
                            System.out.println("Novo usuario registrado via AdminUI: " + userName);

                            // Notifica todos os clientes sobre o novo usuario
                            TextMessage userUpdate = session.createTextMessage("ADD:" + userName);
                            userProducer.send(userUpdate);
                        }
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            // Adicione este consumidor no método start():
            Queue syncQueue = session.createQueue("SYNC_USERS");
            MessageConsumer syncConsumer = session.createConsumer(syncQueue);

            syncConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String userList = ((TextMessage) message).getText();
                        System.out.println("[SYNC] Recebida lista de usuarios: " + userList);

                        // Limpa e recria a lista de usuarios
                        users.clear();
                        Arrays.stream(userList.split(","))
                                .filter(name -> !name.trim().isEmpty())
                                .forEach(name -> users.add(new User(name)));

                        System.out.println("[SYNC] Lista atualizada: " +
                                users.stream().map(User::getName).collect(Collectors.joining(", ")));
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao sincronizar usuarios: " + e.getMessage());
                }
            });

            // Tópico para todos os tópicos/grupos
            Topic allTopicsTopic = session.createTopic("ALL_TOPICS");
            MessageProducer allTopicsProducer = session.createProducer(allTopicsTopic);

            // Fila para inscrições em tópicos
            Queue topicSubscriptionsQueue = session.createQueue("TOPIC_SUBSCRIPTIONS");
            MessageConsumer topicSubscriptionsConsumer = session.createConsumer(topicSubscriptionsQueue);

            topicSubscriptionsConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String text = ((TextMessage) message).getText();
                        if (text.startsWith("SUBSCRIBE:")) {
                            String[] parts = text.split(":");
                            String userName = parts[1];
                            String topicName = parts[2];

                            ChatGroup group = topics.stream()
                                    .filter(t -> t.getName().equals(topicName))
                                    .findFirst()
                                    .orElse(null);

                            if (group == null) {
                                group = new ChatGroup(topicName);
                                topics.add(group);
                                // Notifica sobre novo tópico
                                TextMessage newTopicMsg = session.createTextMessage("NEW_TOPIC:" + topicName);
                                allTopicsProducer.send(newTopicMsg);
                            }

                            group.addSubscriber(userName);

                            // Confirmação de assinatura
                            Topic messageTopic = session.createTopic("TOPIC_MESSAGES." + userName);
                            MessageProducer messageProducer = session.createProducer(messageTopic);
                            TextMessage confirmation = session.createTextMessage("SUBSCRIBED:" + topicName);
                            messageProducer.send(confirmation);

                            System.out.println("usuario " + userName + " inscrito no topico " + topicName);
                        }
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            // Fila para criação de grupos
            Queue groupCreationQueue = session.createQueue("GROUP_CREATION");
            MessageConsumer groupCreationConsumer = session.createConsumer(groupCreationQueue);

            groupCreationConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String text = ((TextMessage) message).getText();
                        if (text.startsWith("CREATE:")) {
                            String[] parts = text.split(":");
                            String creator = parts[1];
                            String groupName = parts[2];

                            // Verifica se grupo já existe
                            if (topics.stream().noneMatch(g -> g.getName().equals(groupName))) {
                                ChatGroup newGroup = new ChatGroup(groupName);
                                topics.add(newGroup);

                                // Notifica todos sobre o novo grupo
                                TextMessage newGroupMsg = session.createTextMessage("NEW_TOPIC:" + groupName);
                                allTopicsProducer.send(newGroupMsg);

                                // Auto-inscreve o criador
                                subscribeUserToGroup(creator, groupName, session);

                                System.out.println("Novo grupo criado: " + groupName + " por " + creator);
                            }
                        }
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            // Consumidor para mensagens de grupo
            Topic groupMessagesTopic = session.createTopic("GROUP_TOPIC");
            MessageConsumer groupMessagesConsumer = session.createConsumer(groupMessagesTopic);

            groupMessagesConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String json = ((TextMessage) message).getText();
                        GroupMessage groupMessage = GroupMessage.fromJson(json);

                        ChatGroup group = topics.stream()
                                .filter(g -> g.getName().equals(groupMessage.getTopicName()))
                                .findFirst()
                                .orElse(null);

                        if (group != null) {
                            // Envia para cada assinante
                            for (String subscriber : group.getSubscribers()) {
                                Topic subscriberTopic = session.createTopic("TOPIC_MESSAGES." + subscriber);
                                MessageProducer producer = session.createProducer(subscriberTopic);
                                TextMessage textMessage = session.createTextMessage(json);
                                producer.send(textMessage);
                            }
                        }
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            System.out.println("Servidor de chat iniciado. Aguardando conexões...");
            System.out.println(
                    "usuarios registrados: " + users.stream().map(User::getName).collect(Collectors.joining(", ")));

        } catch (JMSException e) {
            e.printStackTrace();
            System.err.println("Erro ao iniciar o servidor de chat: " + e.getMessage());
        }
    }

    private void sendGroupMessageToSubscribers(GroupMessage groupMessage, Session session) throws JMSException {
        ChatGroup group = topics.stream()
                .filter(g -> g.getName().equals(groupMessage.getTopicName()))
                .findFirst()
                .orElse(null);

        if (group != null) {
            System.out.println("Distribuindo mensagem para " + group.getSubscribers().size() + " assinantes");

            for (String subscriber : group.getSubscribers()) {
                Topic subscriberTopic = session.createTopic("TOPIC_MESSAGES." + subscriber);
                MessageProducer producer = session.createProducer(subscriberTopic);
                TextMessage textMessage = session.createTextMessage(groupMessage.toJson());
                producer.send(textMessage);

                System.out.println("Enviado para: " + subscriber);
            }
        } else {
            System.out.println("Grupo não encontrado: " + groupMessage.getTopicName());
        }
    }

    private void subscribeUserToGroup(String userName, String groupName, Session session) throws JMSException {
        ChatGroup group = topics.stream()
                .filter(g -> g.getName().equals(groupName))
                .findFirst()
                .orElse(null);

        if (group != null) {
            group.addSubscriber(userName);

            // Envia confirmação apenas para o usuario que se inscreveu
            Topic userTopic = session.createTopic("TOPIC_MESSAGES." + userName);
            MessageProducer userProducer = session.createProducer(userTopic);
            TextMessage confirmation = session.createTextMessage("SUBSCRIBED:" + groupName);
            userProducer.send(confirmation);
        }
    }

    private void syncUsersWithBroker() {
        try {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
            Connection connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Queue syncQueue = session.createQueue("SYNC_USERS");
            MessageProducer producer = session.createProducer(syncQueue);
            TextMessage message = session.createTextMessage("SYNC");
            producer.send(message);

            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

}