package admin;

public class QueueInfo {
    private String name;
    private long messageCount;
    private int consumerCount;
    private int producerCount;

    public QueueInfo(String name, long messageCount, int consumerCount, int producerCount) {
        this.name = name;
        this.messageCount = messageCount;
        this.consumerCount = consumerCount;
        this.producerCount = producerCount;
    }

    // Getters
    public String getName() {
        return name;
    }

    public long getMessageCount() {
        return messageCount;
    }

    public int getConsumerCount() {
        return consumerCount;
    }

    public int getProducerCount() {
        return producerCount;
    }

    @Override
    public String toString() {
        return String.format("%s (Msgs: %d, Consumers: %d, Producers: %d)",
                name, messageCount, consumerCount, producerCount);
    }
}