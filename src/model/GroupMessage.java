package model;
public class GroupMessage extends Message {
    private String topicName;

    public GroupMessage(String sender, String topicName, String content) {
        super(sender, null, content);
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }

    @Override
    public String toJson() {
        return String.format("{\"sender\":\"%s\",\"topic\":\"%s\",\"content\":\"%s\"}",
                getSender(), topicName, getContent());
    }

    public static GroupMessage fromJson(String json) {
        String[] parts = json.replaceAll("[{}\"]", "").split(",");
        String sender = parts[0].split(":")[1];
        String topic = parts[1].split(":")[1];
        String content = parts[2].split(":")[1];
        return new GroupMessage(sender, topic, content);
    }
}