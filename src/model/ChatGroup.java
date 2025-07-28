package model;

import java.util.ArrayList;
import java.util.List;

public class ChatGroup {
    private String name;
    private int unreadCount = 0;
    private List<String> subscribers;

    public ChatGroup(String name) {
        this.name = name;
        this.subscribers = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<String> getSubscribers() {
        return subscribers;
    }

    public void addSubscriber(String userName) {
        if (!subscribers.contains(userName)) {
            subscribers.add(userName);
        }
    }

    public void removeSubscriber(String userName) {
        subscribers.remove(userName);
    }

    public void incrementUnreadCount() {
        unreadCount++;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void resetUnreadCount() {
        unreadCount = 0;
    }

    @Override
    public String toString() {
        return name;
    }

}