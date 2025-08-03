// package com.locationchat.mq;

// import model.Message;
import javax.jms.JMSException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class MessageQueueManager {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private Map<String, MessageProducer> producers;
    private Map<String, MessageConsumer> consumers;

    public MessageQueueManager() {
        producers = new ConcurrentHashMap<>();
        consumers = new ConcurrentHashMap<>();
    }

    public void sendAsyncMessage(Message message) {
        String queueName = "user." + message.getRecipient();
        try {
            MessageProducer producer = getOrCreateProducer(queueName);
            String messageText = message.getSender() + "|" + message.getContent() + "|" + message.getTimestamp();
            producer.send(messageText);
        } catch (JMSException e) {
            System.err.println("Erro ao enviar mensagem assíncrona: " + e.getMessage());
        }
    }

    public List<String> receiveAsyncMessages(String userName) {
        String queueName = "user." + userName;
        try {
            MessageConsumer consumer = getOrCreateConsumer(queueName);
            return consumer.receiveMessages();
        } catch (JMSException e) {
            System.err.println("Erro ao receber mensagens assíncronas: " + e.getMessage());
            return List.of();
        }
    }

    private MessageProducer getOrCreateProducer(String queueName) throws JMSException {
        if (!producers.containsKey(queueName)) {
            producers.put(queueName, new MessageProducer(BROKER_URL, queueName));
        }
        return producers.get(queueName);
    }

    private MessageConsumer getOrCreateConsumer(String queueName) throws JMSException {
        if (!consumers.containsKey(queueName)) {
            consumers.put(queueName, new MessageConsumer(BROKER_URL, queueName));
        }
        return consumers.get(queueName);
    }

    public void close() {
        for (MessageProducer producer : producers.values()) {
            try {
                producer.close();
            } catch (JMSException e) {
                System.err.println("Erro ao fechar producer: " + e.getMessage());
            }
        }
        for (MessageConsumer consumer : consumers.values()) {
            try {
                consumer.close();
            } catch (JMSException e) {
                System.err.println("Erro ao fechar consumer: " + e.getMessage());
            }
        }
    }
}

