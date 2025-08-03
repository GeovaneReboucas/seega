import java.util.ArrayList;
import java.util.List;

public class User {
    private String name;
    private Location location;
    private boolean isOnline;
    private double communicationRadius;
    private List<User> contacts;

    public User(String name, Location location, boolean isOnline, double communicationRadius) {
        this.name = name;
        this.location = location;
        this.isOnline = isOnline;
        this.communicationRadius = communicationRadius;
        this.contacts = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public double getCommunicationRadius() {
        return communicationRadius;
    }

    public void setCommunicationRadius(double communicationRadius) {
        this.communicationRadius = communicationRadius;
    }

    public List<User> getContacts() {
        return contacts;
    }

    public void addContact(User user) {
        if (!contacts.contains(user)) {
            contacts.add(user);
        }
    }

    public void removeContact(User user) {
        contacts.remove(user);
    }

    @Override
    public String toString() {
        return "User{" +
               "name='" + name + '\'' +
               ", location=" + location +
               ", isOnline=" + isOnline +
               ", communicationRadius=" + communicationRadius +
               '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return name.equals(user.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

