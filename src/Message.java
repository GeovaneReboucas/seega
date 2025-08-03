import java.time.LocalDateTime;

public class Message {
    public enum MessageType {
        SYNCHRONOUS,
        ASYNCHRONOUS
    }

    private String content;
    private String sender;
    private String recipient;
    private MessageType type;
    private LocalDateTime timestamp;

    public Message(String content, String sender, String recipient, MessageType type) {
        this.content = content;
        this.sender = sender;
        this.recipient = recipient;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public String getContent() {
        return content;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public MessageType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Message{" +
               "content='" + content + '\'' +
               ", sender='" + sender + '\'' +
               ", recipient='" + recipient + '\'' +
               ", type=" + type +
               ", timestamp=" + timestamp +
               '}';
    }
}

