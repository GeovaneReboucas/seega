import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;

public class MessageProducer {
    private Connection connection;
    private Session session;
    private javax.jms.MessageProducer producer;

    public MessageProducer(String brokerUrl, String queueName) throws JMSException {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(queueName);
        producer = session.createProducer(destination);
    }

    public void send(String text) throws JMSException {
        TextMessage message = session.createTextMessage(text);
        producer.send(message);
        System.out.println("Mensagem enviada para a fila: " + text);
    }

    public void close() throws JMSException {
        producer.close();
        session.close();
        connection.close();
    }
}

