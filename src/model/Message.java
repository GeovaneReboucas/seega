package model;
import java.io.Serializable;

public class Message implements Serializable {
    private String sender;
    private String recipient;
    private String content;

    public Message(String sender, String recipient, String content) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
    }

    // Getters e Setters
    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return sender + " to " + recipient + ": " + content;
    }

    public String toJson() {
        return String.format("{\"sender\":\"%s\",\"recipient\":\"%s\",\"content\":\"%s\"}",
                sender, recipient, content);
    }

    public static Message fromJson(String json) {
        String[] parts = json.replaceAll("[{}\"]", "").split(",");
        String sender = parts[0].split(":")[1];
        String recipient = parts[1].split(":")[1];
        String content = parts[2].split(":")[1];
        return new Message(sender, recipient, content);
    }

}