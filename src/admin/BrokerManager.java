package admin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;

public class BrokerManager {
    private static final String BROKER_URL = "tcp://localhost:61616";

    private Connection connection;
    private Session session;
    private Set<String> activeUsers;

    public BrokerManager() {
        this.activeUsers = new HashSet<>();
        initialize();
    }

    private void initialize() {
        try {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        } catch (JMSException e) {
            throw new RuntimeException("Erro ao inicializar BrokerManager", e);
        }
    }

    public boolean createQueue(String queueName) {
        try {
            session.createQueue(queueName);
            System.out.println("Fila criada: " + queueName);
            return true;
        } catch (JMSException e) {
            System.err.println("Erro ao criar fila " + queueName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean createTopic(String topicName) {
        try {
            session.createTopic(topicName);
            System.out.println("Topico criado: " + topicName);
            return true;
        } catch (JMSException e) {
            System.err.println("Erro ao criar topico " + topicName + ": " + e.getMessage());
            return false;
        }
    }

    public boolean removeTopic(String topicName) {
        return false;
    }

    public List<QueueInfo> listQueues() {
        List<QueueInfo> queues = new ArrayList<>();

        String[] knownQueues = { "CONNECTIONS", "TOPIC_SUBSCRIPTIONS", "GROUP_CREATION" };
        for (String queueName : knownQueues) {
            queues.add(new QueueInfo(queueName, 0, 0, 0));
        }

        for (String userName : activeUsers) {
            queues.add(new QueueInfo("QUEUE." + userName, 0, 0, 0));
        }
        return queues;
    }

    public List<TopicInfo> listTopics() {
        List<TopicInfo> topics = new ArrayList<>();
        String[] knownTopics = { "USERS", "ALL_TOPICS", "GROUP_TOPIC" };
        for (String topicName : knownTopics) {
            topics.add(new TopicInfo(topicName, 0, 0, 0));
        }
        return topics;
    }

    public QueueInfo getQueueInfo(String queueName) {
        return new QueueInfo(queueName, 0, 0, 0);
    }

    public boolean addUser(String userName) {
        if (activeUsers.contains(userName)) {
            System.err.println("Usuário já existe: " + userName);
            return false;
        }

        // Cria fila pessoal para o usuário
        String personalQueueName = "QUEUE." + userName;
        if (createQueue(personalQueueName)) {
            activeUsers.add(userName);
            System.out.println("Usuário adicionado: " + userName);
            return true;
        }

        return false;
    }

    public List<String> listUsers() {
        return new ArrayList<>(activeUsers);
    }

    public BrokerStats getBrokerStats() {
        return new BrokerStats(0, 0, 0, listQueues().size(), listTopics().size());
    }

    public void close() {
        try {
            if (session != null)
                session.close();
            if (connection != null)
                connection.close();
        } catch (JMSException e) {
            System.err.println("Erro ao fechar conexões: " + e.getMessage());
        }
    }

}
