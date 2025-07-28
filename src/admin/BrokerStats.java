package admin;

public class BrokerStats {
    private long totalMessages;
    private int totalConsumers;
    private int totalProducers;
    private int queueCount;
    private int topicCount;

    public BrokerStats(long totalMessages, int totalConsumers, int totalProducers,
            int queueCount, int topicCount) {
        this.totalMessages = totalMessages;
        this.totalConsumers = totalConsumers;
        this.totalProducers = totalProducers;
        this.queueCount = queueCount;
        this.topicCount = topicCount;
    }

    // Getters
    public long getTotalMessages() {
        return totalMessages;
    }

    public int getTotalConsumers() {
        return totalConsumers;
    }

    public int getTotalProducers() {
        return totalProducers;
    }

    public int getQueueCount() {
        return queueCount;
    }

    public int getTopicCount() {
        return topicCount;
    }

    @Override
    public String toString() {
        return String.format("Broker Stats - Messages: %d, Consumers: %d, Producers: %d, Queues: %d, Topics: %d",
                totalMessages, totalConsumers, totalProducers, queueCount, topicCount);
    }
}
