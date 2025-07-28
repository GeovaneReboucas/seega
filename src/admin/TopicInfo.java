package admin;

public class TopicInfo {
    private String name;
    private int consumerCount;
    private int producerCount;
    private long messageCount;

    public TopicInfo(String name, int consumerCount, int producerCount, long messageCount) {
        this.name = name;
        this.consumerCount = consumerCount;
        this.producerCount = producerCount;
        this.messageCount = messageCount;
    }

    // Getters
    public String getName() {
        return name;
    }

    public int getConsumerCount() {
        return consumerCount;
    }

    public int getProducerCount() {
        return producerCount;
    }

    public long getMessageCount() {
        return messageCount;
    }

    @Override
    public String toString() {
        return String.format("%s (Consumers: %d, Producers: %d, Msgs: %d)",
                name, consumerCount, producerCount, messageCount);
    }
}
