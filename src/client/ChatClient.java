package client;
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
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.activemq.ActiveMQConnectionFactory;

import model.ChatGroup;
import model.GroupMessage;
import model.Message;

public class ChatClient {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String USER_TOPIC = "USERS";
    private static final String CONNECTION_QUEUE = "CONNECTIONS";

    private String userName;
    private Connection connection;
    private Session session;
    private List<User> users = new ArrayList<>();
    private List<ChatGroup> topics = new ArrayList<>();
    private List<ChatGroup> subscribedTopics = new ArrayList<>();

    private ChatUI chatUI;

    public ChatClient(String userName) {
        this.userName = userName;
        this.chatUI = new ChatUI(this);
        initialize();
    }

    private void initialize() {
        try {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
            connection = connectionFactory.createConnection();
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Primeiro: Criar consumidor para lista inicial de usuários
            Queue initialUsersQueue = session.createQueue("INITIAL_USERS." + userName);
            MessageConsumer initialUsersConsumer = session.createConsumer(initialUsersQueue);

            initialUsersConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String usersList = ((TextMessage) message).getText();
                        if (!usersList.isEmpty()) {
                            List<User> filteredUsers = Arrays.stream(usersList.split(","))
                                .filter(name -> !name.startsWith("REQUEST_") && 
                                            !name.startsWith("SYNC_") &&
                                            !name.equals(userName))
                                .map(User::new)
                                .collect(Collectors.toList());
                            
                            users.clear();
                            users.addAll(filteredUsers);
                            chatUI.updateUserList(users);
                        }
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            // Depois: Registrar novo usuário
            Queue connectionQueue = session.createQueue(CONNECTION_QUEUE);
            MessageProducer connectionProducer = session.createProducer(connectionQueue);
            TextMessage connectionMessage = session.createTextMessage(userName);
            connectionProducer.send(connectionMessage);

            // Assinar atualizações de usuários
            Topic userTopic = session.createTopic(USER_TOPIC);
            MessageConsumer userConsumer = session.createConsumer(userTopic);

            // Substitua este bloco:
            userConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String text = ((TextMessage) message).getText();
                        if (text.startsWith("ADD:")) {
                            String newUserName = text.substring(4);
                            if (!newUserName.startsWith("REQUEST_") && 
                                !newUserName.startsWith("SYNC_") &&
                                !newUserName.equals(userName)) {
                                
                                users.add(new User(newUserName));
                                updateUserList(users); // Usa o novo método
                            }
                        }
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            // No método initialize():
            Topic usersT = session.createTopic("USERS");
            MessageConsumer usersTConsumer = session.createConsumer(usersT);

            usersTConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String text = ((TextMessage) message).getText();
                        if (text.startsWith("ADD:")) {
                            String newUserName = text.substring(4);
                            if (!newUserName.equals(userName) && 
                                !newUserName.startsWith("REQUEST_") &&
                                !newUserName.startsWith("SYNC_")) {
                                
                                SwingUtilities.invokeLater(() -> {
                                    // Atualiza a lista local
                                    if (users.stream().noneMatch(u -> u.getName().equals(newUserName))) {
                                        users.add(new User(newUserName));
                                        chatUI.updateUserList(users);
                                    }
                                });
                            }
                        }
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            // Criar fila para mensagens pessoais
            Queue personalQueue = session.createQueue("QUEUE." + userName);
            MessageConsumer messageConsumer = session.createConsumer(personalQueue);

            messageConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String json = ((TextMessage) message).getText();
                        Message chatMessage = Message.fromJson(json);
                        chatUI.displayMessage(chatMessage);
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            // Adicionar consumidor para tópicos
            Topic allTopicsTopic = session.createTopic("ALL_TOPICS");
            MessageConsumer topicsConsumer = session.createConsumer(allTopicsTopic);

            topicsConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String text = ((TextMessage) message).getText();
                        if (text.startsWith("NEW_TOPIC:")) {
                            String topicName = text.substring(10);
                            SwingUtilities.invokeLater(() -> {
                                ChatGroup newTopic = new ChatGroup(topicName);
                                // Verifica se já existe antes de adicionar
                                if (topics.stream().noneMatch(t -> t.getName().equals(topicName))) {
                                    topics.add(newTopic);
                                }
                                chatUI.updateTopicList(topics);
                            });
                        }
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            // Adicione este novo listener para confirmações de assinatura:
            MessageConsumer confirmationConsumer = session.createConsumer(
                    session.createTopic("TOPIC_MESSAGES." + userName));

            confirmationConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String text = ((TextMessage) message).getText();
                        if (text.startsWith("SUBSCRIBED:")) {
                            String topicName = text.substring(11);
                            SwingUtilities.invokeLater(() -> {
                                // Adiciona apenas à lista de inscritos
                                ChatGroup topic = topics.stream()
                                        .filter(t -> t.getName().equals(topicName))
                                        .findFirst()
                                        .orElse(null);

                                if (topic != null && !subscribedTopics.contains(topic)) {
                                    subscribedTopics.add(topic);
                                    chatUI.updateSubscribedTopics(subscribedTopics);
                                }
                            });
                        }
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            // Consumidor para mensagens de tópicos assinados
            Topic myTopicMessages = session.createTopic("TOPIC_MESSAGES." + userName);
            MessageConsumer topicMessagesConsumer = session.createConsumer(myTopicMessages);

            topicMessagesConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String text = ((TextMessage) message).getText();
                        if (text.startsWith("SUBSCRIBED:")) {
                            // Isso já está sendo tratado pelo confirmationConsumer
                            return;
                        }

                        System.out.println("Mensagem de grupo recebida: " + text);
                        GroupMessage topicMessage = GroupMessage.fromJson(text);
                        chatUI.displayTopicMessage(topicMessage);
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            // Adicionar listener para notificações administrativas
            Topic adminNotificationsTopic = session.createTopic("ADMIN_NOTIFICATIONS");
            MessageConsumer adminNotificationsConsumer = session.createConsumer(adminNotificationsTopic);

            adminNotificationsConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String notification = ((TextMessage) message).getText();
                        System.out.println("Notificação administrativa recebida: " + notification);
                        
                        SwingUtilities.invokeLater(() -> {
                            if (notification.startsWith("USER_ADDED:")) {
                                String newUserName = notification.substring(11);
                                if (
                                    !newUserName.startsWith("REQUEST_") && 
                                    !newUserName.startsWith("SYNC_") &&
                                    !newUserName.equals(userName)
                                ) {
                                    users.add(new User(newUserName));
                                    updateUserList(users);
                                }
                            } else if (notification.startsWith("USER_REMOVED:")) {
                                String removedUserName = notification.substring(13);
                                users.removeIf(u -> u.getName().equals(removedUserName));
                                chatUI.updateUserList(users);
                            } else if (notification.startsWith("TOPIC_CREATED:")) {
                                String newTopicName = notification.substring(14);
                                ChatGroup newTopic = new ChatGroup(newTopicName);
                                if (topics.stream().noneMatch(t -> t.getName().equals(newTopicName))) {
                                    topics.add(newTopic);
                                    chatUI.updateTopicList(topics);
                                }
                            } else if (notification.startsWith("TOPIC_REMOVED:")) {
                                String removedTopicName = notification.substring(14);
                                topics.removeIf(t -> t.getName().equals(removedTopicName));
                                subscribedTopics.removeIf(t -> t.getName().equals(removedTopicName));
                                chatUI.updateTopicList(topics);
                                chatUI.updateSubscribedTopics(subscribedTopics);
                            }
                        });
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String recipient, String content) {
        try {
            Queue destination = session.createQueue("QUEUE." + recipient);
            MessageProducer producer = session.createProducer(destination);

            Message chatMessage = new Message(userName, recipient, content);
            TextMessage textMessage = session.createTextMessage(chatMessage.toJson());

            producer.send(textMessage);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        while (true) {
            String userName = JOptionPane.showInputDialog("Digite seu nome:");
            if (userName == null || userName.trim().isEmpty()) {
                System.exit(0);
            }

            try {
                ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
                Connection tempConnection = connectionFactory.createConnection();
                tempConnection.start();
                Session tempSession = tempConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                // Envia solicitação de conexão simples
                Queue connectionQueue = tempSession.createQueue("CONNECTIONS");
                MessageProducer connectionProducer = tempSession.createProducer(connectionQueue);
                TextMessage connectionMessage = tempSession.createTextMessage(userName);
                connectionProducer.send(connectionMessage);

                // Cria consumidor para resposta
                Queue responseQueue = tempSession.createQueue("CONNECTION_RESPONSE." + userName);
                MessageConsumer responseConsumer = tempSession.createConsumer(responseQueue);

                // Aguarda resposta por 5 segundos
                TextMessage response = (TextMessage) responseConsumer.receive(5000);

                if (response != null) {
                    if (response.getText().equals("SUCCESS")) {
                        tempConnection.close();
                        new ChatClient(userName);
                        break;
                    } else {
                        JOptionPane.showMessageDialog(null,
                                response.getText().replace("ERROR: ", ""),
                                "Erro", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Timeout ao conectar com o servidor",
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }

                tempConnection.close();
            } catch (JMSException e) {
                JOptionPane.showMessageDialog(null,
                        "Erro ao conectar com o servidor: " + e.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void subscribeToTopic(String topicName) {
        try {
            if (subscribedTopics.stream().noneMatch(t -> t.getName().equals(topicName))) {
                Queue subscriptionQueue = session.createQueue("TOPIC_SUBSCRIPTIONS");
                MessageProducer subscriptionProducer = session.createProducer(subscriptionQueue);
                TextMessage subscriptionMessage = session.createTextMessage("SUBSCRIBE:" + userName + ":" + topicName);
                subscriptionProducer.send(subscriptionMessage);

                ChatGroup topic = topics.stream()
                        .filter(t -> t.getName().equals(topicName))
                        .findFirst()
                        .orElse(null);

                if (topic != null) {
                    subscribedTopics.add(topic);
                    chatUI.updateSubscribedTopics(subscribedTopics);
                }
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void sendTopicMessage(String topicName, String content) {
        try {
            if (subscribedTopics.stream().anyMatch(t -> t.getName().equals(topicName))) {
                Topic groupTopic = session.createTopic("GROUP_TOPIC");
                MessageProducer producer = session.createProducer(groupTopic);

                GroupMessage topicMessage = new GroupMessage(userName, topicName, content);
                TextMessage textMessage = session.createTextMessage(topicMessage.toJson());

                producer.send(textMessage);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void createGroup(String groupName) {
        try {
            // Verifica se o grupo já existe localmente
            if (topics.stream().anyMatch(t -> t.getName().equalsIgnoreCase(groupName))) {
                JOptionPane.showMessageDialog(chatUI.getFrame(),
                        "Já existe um grupo com este nome",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Queue groupCreationQueue = session.createQueue("GROUP_CREATION");
            MessageProducer producer = session.createProducer(groupCreationQueue);

            String messageText = "CREATE:" + userName + ":" + groupName;
            TextMessage message = session.createTextMessage(messageText);

            producer.send(message);

            // Remova a adição local temporária - vamos esperar a confirmação do servidor
            // Isso evita a duplicação
        } catch (JMSException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(chatUI.getFrame(),
                    "Erro ao criar grupo: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String getUserName() {
        return this.userName;
    }

    public void updateUserList(List<User> users) {
        chatUI.updateUserList(users);
    }
}