package client;
public class User {
    private String name;
    private boolean online;
    private int unreadCount;

    public User(String name) {
        this.name = name;
        this.online = true;
        this.unreadCount = 0;
    }

    public String getName() {
        return name;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void incrementUnreadCount() {
        this.unreadCount++;
    }

    public void resetUnreadCount() {
        this.unreadCount = 0;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    @Override
    public String toString() {
        String status = "";
        if (unreadCount > 0) {
            status += " [" + unreadCount + " nova(s)]";
        }
        return name + status;
    }
}