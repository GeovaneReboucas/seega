package mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;

import javax.jms.Connection;
import javax.jms.Queue;

import java.util.ArrayList;
import java.util.List;

public class MessageConsumer {
    private Connection connection;
    private Session session;
    private javax.jms.MessageConsumer consumer;
    private List<String> messages;

    public MessageConsumer(String brokerUrl, String queueName) throws JMSException {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue destination = session.createQueue(queueName);
        consumer = session.createConsumer(destination);
        messages = new ArrayList<>();
    }

    public List<String> receiveMessages() throws JMSException {
        List<String> receivedMessages = new ArrayList<>();
        javax.jms.Message message;
        while ((message = consumer.receiveNoWait()) != null) {
            if (message instanceof TextMessage) {
                String text = ((TextMessage) message).getText();
                receivedMessages.add(text);
                System.out.println("Mensagem recebida da fila: " + text);
            }
        }
        return receivedMessages;
    }

    public void close() throws JMSException {
        consumer.close();
        session.close();
        connection.close();
    }
}

