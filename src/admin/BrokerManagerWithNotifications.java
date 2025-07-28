package admin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

/**
 * Classe responsável pelo gerenciamento do broker ActiveMQ com notificações
 * Permite ao administrador gerenciar filas, tópicos e usuarios
 * Envia notificações para clientes conectados sobre mudanças
 */
public class BrokerManagerWithNotifications {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String USER_TOPIC = "USERS";
    private static final String ALL_TOPICS_TOPIC = "ALL_TOPICS";
    private static final String ADMIN_NOTIFICATIONS_TOPIC = "ADMIN_NOTIFICATIONS";

    private Connection connection;
    private Session session;
    private Set<String> activeUsers;
    private Set<String> activeTopic;
    private MessageProducer userTopicProducer;
    private MessageProducer allTopicsProducer;
    private MessageProducer adminNotificationsProducer;

    public BrokerManagerWithNotifications() {
        this.activeUsers = new HashSet<>();
        this.activeTopic = new HashSet<>();
        initialize();
    }

    private void initialize() {
        try {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Cria produtores para notificações
            Topic userTopic = session.createTopic(USER_TOPIC);
            userTopicProducer = session.createProducer(userTopic);

            Topic allTopicsTopic = session.createTopic(ALL_TOPICS_TOPIC);
            allTopicsProducer = session.createProducer(allTopicsTopic);

            Topic adminNotificationsTopic = session.createTopic(ADMIN_NOTIFICATIONS_TOPIC);
            adminNotificationsProducer = session.createProducer(adminNotificationsTopic);

            // Adicionar consumidor para sincronizacao
            Queue syncQueue = session.createQueue("SYNC_USERS");
            MessageConsumer syncConsumer = session.createConsumer(syncQueue);

            syncConsumer.setMessageListener(message -> {
                if (message instanceof TextMessage) {
                    System.out.println("Sincronizando usuarios com o servidor...");
                    // Não precisa fazer nada, apenas ativa a sincronizacao
                }
            });

            Queue syncRequestQueue = session.createQueue("SYNC_USERS");
            MessageConsumer syncRequestConsumer = session.createConsumer(syncRequestQueue);

            syncRequestConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage &&
                            ((TextMessage) message).getText().equals("REQUEST_SYNC")) {

                        System.out.println("[SYNC] Enviando lista de usuarios por solicitacao");
                        Queue responseQueue = session.createQueue("SYNC_USERS");
                        MessageProducer producer = session.createProducer(responseQueue);
                        String userList = String.join(",", activeUsers);
                        producer.send(session.createTextMessage(userList));
                    }
                } catch (JMSException e) {
                    System.err.println("Erro ao responder sincronizacao: " + e.getMessage());
                }
            });

            Queue userCheckQueue = session.createQueue("USER_CHECK");
            MessageConsumer userCheckConsumer = session.createConsumer(userCheckQueue);

            userCheckConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        String userName = ((TextMessage) message).getText();
                        Queue replyQueue = session.createQueue("USER_CHECK_REPLY");
                        MessageProducer replyProducer = session.createProducer(replyQueue);

                        if (activeUsers.contains(userName)) {
                            replyProducer.send(session.createTextMessage("EXISTS"));
                        } else {
                            replyProducer.send(session.createTextMessage("NOT_EXISTS"));
                        }
                    }
                } catch (JMSException e) {
                    System.err.println("Erro ao verificar usuario: " + e.getMessage());
                }
            });

            System.out.println("BrokerManager com notificações inicializado com sucesso");

        } catch (JMSException e) {
            throw new RuntimeException("Erro ao inicializar BrokerManager", e);
        }
    }

    public boolean createQueue(String queueName) {
        try {
            session.createQueue(queueName);
            System.out.println("Fila criada: " + queueName);

            // Notifica sobre nova fila
            sendAdminNotification("QUEUE_CREATED:" + queueName);

            return true;
        } catch (JMSException e) {
            System.err.println("Erro ao criar fila " + queueName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean createTopic(String topicName) {
        try {
            session.createTopic(topicName);
            activeTopic.add(topicName);
            System.out.println("Topico criado: " + topicName);

            // Notifica todos os clientes sobre o novo topico
            TextMessage newTopicMsg = session.createTextMessage("NEW_TOPIC:" + topicName);
            allTopicsProducer.send(newTopicMsg);

            // Notifica administradores
            sendAdminNotification("TOPIC_CREATED:" + topicName);

            return true;
        } catch (JMSException e) {
            System.err.println("Erro ao criar topico " + topicName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean removeTopic(String topicName) {
        activeTopic.remove(topicName);
        System.out.println("Topico removido (simulado): " + topicName);

        // Notifica todos os clientes sobre remocao do topico
        try {
            TextMessage removeTopicMsg = session.createTextMessage("REMOVE_TOPIC:" + topicName);
            allTopicsProducer.send(removeTopicMsg);
        } catch (JMSException e) {
            System.err.println("Erro ao notificar remocao de topico: " + e.getMessage());
        }

        // Notifica administradores
        sendAdminNotification("TOPIC_REMOVED:" + topicName);

        return true;
    }

    public List<QueueInfo> listQueues() {
        List<QueueInfo> queues = new ArrayList<>();

        // Filas conhecidas do sistema
        String[] knownQueues = { "CONNECTIONS", "TOPIC_SUBSCRIPTIONS", "GROUP_CREATION" };
        for (String queueName : knownQueues) {
            queues.add(new QueueInfo(queueName, 0, 0, 0));
        }

        // Adiciona filas de usuarios ativos
        for (String userName : activeUsers) {
            queues.add(new QueueInfo("QUEUE." + userName, 0, 0, 0));
        }

        return queues;
    }

    public List<TopicInfo> listTopics() {
        List<TopicInfo> topics = new ArrayList<>();

        // Tópicos conhecidos do sistema
        String[] knownTopics = { "USERS", "ALL_TOPICS", "GROUP_TOPIC", "ADMIN_NOTIFICATIONS" };
        for (String topicName : knownTopics) {
            topics.add(new TopicInfo(topicName, 0, 0, 0));
        }

        // Adiciona tópicos criados dinamicamente
        for (String topicName : activeTopic) {
            topics.add(new TopicInfo(topicName, 0, 0, 0));
        }

        return topics;
    }

    public QueueInfo getQueueInfo(String queueName) {
        return new QueueInfo(queueName, 0, 0, 0);
    }

    public boolean addUser(String userName) {
        try {
            if (activeUsers.contains(userName)) {
                return false;
            }

            // Cria fila pessoal
            session.createQueue("QUEUE." + userName);
            activeUsers.add(userName);

            // notificacao simples para o servidor
            Queue queue = session.createQueue("USER_CREATION");
            MessageProducer producer = session.createProducer(queue);
            TextMessage message = session.createTextMessage(userName);
            producer.send(message);

            return true;
        } catch (JMSException e) {
            System.err.println("Erro ao criar usuario: " + e.getMessage());
            return false;
        }
    }

    public List<String> listUsers() {
        return new ArrayList<>(activeUsers);
    }

    public BrokerStats getBrokerStats() {
        return new BrokerStats(0, 0, 0, listQueues().size(), listTopics().size());
    }

    private void sendAdminNotification(String notification) {
        try {
            TextMessage adminMsg = session.createTextMessage(notification);
            adminNotificationsProducer.send(adminMsg);
        } catch (JMSException e) {
            System.err.println("Erro ao enviar notificacao administrativa: " + e.getMessage());
        }
    }

    public void syncExistingUsers(List<String> existingUsers) {
        activeUsers.clear();
        activeUsers.addAll(existingUsers);
        System.out.println("usuarios sincronizados: " + existingUsers.size());
    }

    public void syncExistingTopics(List<String> existingTopics) {
        activeTopic.clear();
        activeTopic.addAll(existingTopics);
        System.out.println("Topicos sincronizados: " + existingTopics.size());
    }

    public void notifyUserAdded(String userName) {
        try {
            Queue notificationQueue = session.createQueue("USER_ADDED_NOTIFICATION");
            MessageProducer producer = session.createProducer(notificationQueue);
            TextMessage message = session.createTextMessage(userName);
            producer.send(message);
        } catch (JMSException e) {
            System.err.println("Erro ao notificar adicao de usuario: " + e.getMessage());
        }
    }

    public void notifyUserListUpdate() {
        try {
            Topic adminTopic = session.createTopic("ADMIN_UPDATES");
            MessageProducer producer = session.createProducer(adminTopic);
            TextMessage message = session.createTextMessage("USER_LIST_UPDATE");
            producer.send(message);
        } catch (JMSException e) {
            System.err.println("Erro ao notificar atualizacao: " + e.getMessage());
        }
    }

    /**
     * Fecha as conexões
     */
    public void close() {
        try {
            if (userTopicProducer != null)
                userTopicProducer.close();
            if (allTopicsProducer != null)
                allTopicsProducer.close();
            if (adminNotificationsProducer != null)
                adminNotificationsProducer.close();
            if (session != null)
                session.close();
            if (connection != null)
                connection.close();
        } catch (JMSException e) {
            System.err.println("Erro ao fechar conexões: " + e.getMessage());
        }
    }
}
