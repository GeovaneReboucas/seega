package client;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import model.ChatGroup;
import model.GroupMessage;
import model.Message;

public class ChatUI {
    private ChatClient chatClient;
    private JFrame frame;
    private JList<User> userList;
    private DefaultListModel<User> userListModel;
    private JTextArea chatArea;
    private JTextField messageField;
    private Map<String, List<Message>> conversationHistory;
    private Map<String, User> userMap = new HashMap<>();
    private User currentRecipient;

    private JList<ChatGroup> topicList;
    private DefaultListModel<ChatGroup> topicListModel;
    private JList<ChatGroup> subscribedTopicList;
    private DefaultListModel<ChatGroup> subscribedTopicListModel;
    private Map<String, List<GroupMessage>> topicConversationHistory;
    private ChatGroup currentTopic;
    private List<ChatGroup> subscribedTopics;

    public ChatUI(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.userListModel = new DefaultListModel<>();
        this.conversationHistory = new HashMap<>();
        this.topicConversationHistory = new HashMap<>();
        this.subscribedTopics = new ArrayList<>();

        try {
            initialize();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Erro ao iniciar a interface: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void initialize() {
        frame = new JFrame("Chat - " + chatClient.getUserName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);

        JSplitPane mainSplitPane = new JSplitPane();
        JPanel leftPanel = new JPanel(new BorderLayout());
        JTabbedPane listTabbedPane = new JTabbedPane();

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                User selectedUser = userList.getSelectedValue();
                if (selectedUser != null) {
                    currentRecipient = selectedUser;
                    currentTopic = null;
                    subscribedTopicList.clearSelection();
                    updateChatArea();
                }
            }
        });

        listTabbedPane.addTab("Usuarios", new JScrollPane(userList));

        JPanel groupsPanel = new JPanel(new BorderLayout());

        topicListModel = new DefaultListModel<>();
        topicList = new JList<>(topicListModel);

        subscribedTopicListModel = new DefaultListModel<>();
        subscribedTopicList = new JList<>(subscribedTopicListModel);

        JSplitPane groupsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(topicList),
                new JScrollPane(subscribedTopicList));
        groupsSplitPane.setResizeWeight(0.5);

        subscribedTopicList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ChatGroup selected = subscribedTopicList.getSelectedValue();
                if (selected != null) {
                    currentTopic = selected;
                    currentRecipient = null;
                    userList.clearSelection();
                    updateTopicChatArea();
                }
            }
        });

        groupsPanel.add(groupsSplitPane, BorderLayout.CENTER);

        // Painel de botões para grupos
        JPanel groupButtonsPanel = new JPanel(new GridLayout(1, 2));
        JButton subscribeButton = new JButton("Assinar");
        JButton createGroupButton = new JButton("Criar Grupo");

        subscribeButton.addActionListener(e -> {
            ChatGroup selected = topicList.getSelectedValue();
            if (selected != null) {
                chatClient.subscribeToTopic(selected.getName());
            }
        });

        createGroupButton.addActionListener(e -> {
            String groupName = JOptionPane.showInputDialog(frame,
                    "Digite o nome do novo grupo:",
                    "Criar Novo Grupo",
                    JOptionPane.PLAIN_MESSAGE);

            if (groupName != null && !groupName.trim().isEmpty()) {
                chatClient.createGroup(groupName);
            }
        });

        groupButtonsPanel.add(subscribeButton);
        groupButtonsPanel.add(createGroupButton);
        groupsPanel.add(groupButtonsPanel, BorderLayout.SOUTH);

        listTabbedPane.addTab("Grupos", groupsPanel);
        leftPanel.add(listTabbedPane, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        rightPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        JButton sendButton = new JButton("Enviar");

        messageField.addActionListener(e -> sendMessage());
        sendButton.addActionListener(e -> sendMessage());

        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        rightPanel.add(messagePanel, BorderLayout.SOUTH);

        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(rightPanel);
        frame.add(mainSplitPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    public void updateTopicList(List<ChatGroup> allTopics) {
        SwingUtilities.invokeLater(() -> {
            topicListModel.clear();
            for (ChatGroup topic : allTopics) {
                topicListModel.addElement(topic);
            }
        });
    }

    public void updateSubscribedTopics(List<ChatGroup> subscribedTopics) {
        SwingUtilities.invokeLater(() -> {
            // Atualiza a lista local de tópicos inscritos
            this.subscribedTopics = new ArrayList<>(subscribedTopics);

            subscribedTopicListModel.clear();
            for (ChatGroup topic : subscribedTopics) {
                subscribedTopicListModel.addElement(topic);
                topicConversationHistory.putIfAbsent(topic.getName(), new ArrayList<>());
            }
        });
    }

    public void displayTopicMessage(GroupMessage message) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Exibindo mensagem de grupo - Tópico: " + message.getTopicName() +
                    ", Remetente: " + message.getSender() +
                    ", Conteúdo: " + message.getContent());

            topicConversationHistory
                    .computeIfAbsent(message.getTopicName(), k -> new ArrayList<>())
                    .add(message);

            if (currentTopic != null && currentTopic.getName().equals(message.getTopicName())) {
                chatArea.append("[" + message.getTopicName() + "] " +
                        message.getSender() + ": " + message.getContent() + "\n");
            } else {
                for (ChatGroup topic : subscribedTopics) {
                    if (topic.getName().equals(message.getTopicName())) {
                        topic.incrementUnreadCount();
                        subscribedTopicList.repaint();
                        break;
                    }
                }
            }
        });
    }

    private void updateTopicChatArea() {
        SwingUtilities.invokeLater(() -> {
            chatArea.setText("");
            if (currentTopic != null) {
                List<GroupMessage> messages = topicConversationHistory.getOrDefault(
                        currentTopic.getName(), new ArrayList<>());

                for (GroupMessage msg : messages) {
                    chatArea.append("[" + msg.getTopicName() + "] " +
                            msg.getSender() + ": " + msg.getContent() + "\n");
                }
            }
        });
    }

    private void sendMessage() {
        String messageText = messageField.getText();
        if (messageText.isEmpty())
            return;

        if (currentRecipient != null) {
            // Envia mensagem privada
            chatClient.sendMessage(currentRecipient.getName(), messageText);

            Message sentMessage = new Message(chatClient.getUserName(),
                    currentRecipient.getName(),
                    messageText);
            addMessageToHistory(sentMessage);

            chatArea.append("Voce: " + messageText + "\n");
        } else if (currentTopic != null) {
            chatClient.sendTopicMessage(currentTopic.getName(), messageText);

            GroupMessage groupMessage = new GroupMessage(chatClient.getUserName(),
                    currentTopic.getName(),
                    messageText);
            topicConversationHistory.computeIfAbsent(currentTopic.getName(),
                    k -> new ArrayList<>())
                    .add(groupMessage);
        }

        messageField.setText("");
    }

    private void addMessageToHistory(Message message) {
        String otherUser = message.getSender().equals(chatClient.getUserName())
                ? message.getRecipient()
                : message.getSender();

        conversationHistory.computeIfAbsent(otherUser, k -> new ArrayList<>()).add(message);
    }

    public void updateUserList(List<User> users) {
        SwingUtilities.invokeLater(() -> {
            User selected = userList.getSelectedValue();

            userListModel.clear();
            for (User user : users) {
                if (!user.getName().startsWith("REQUEST_") &&
                        !user.getName().startsWith("SYNC_") &&
                        !user.getName().equals(chatClient.getUserName())) {
                    userListModel.addElement(user);
                }
            }

            // Restaura a seleção se ainda existir
            if (selected != null) {
                for (int i = 0; i < userListModel.size(); i++) {
                    if (userListModel.get(i).getName().equals(selected.getName())) {
                        userList.setSelectedIndex(i);
                        break;
                    }
                }
            }
        });
    }

    public void displayMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            addMessageToHistory(message);

            if (!(message instanceof GroupMessage)) {
                String sender = message.getSender();

                if (currentRecipient == null || !currentRecipient.getName().equals(sender)) {
                    User senderUser = userMap.get(sender);
                    if (senderUser != null) {
                        senderUser.incrementUnreadCount();
                        userList.repaint();
                    }
                }

                if (currentRecipient != null &&
                        (message.getSender().equals(currentRecipient.getName()) ||
                                message.getRecipient().equals(currentRecipient.getName()))) {

                    String prefix = message.getSender().equals(chatClient.getUserName())
                            ? "Voce"
                            : message.getSender();

                    chatArea.append(prefix + ": " + message.getContent() + "\n");
                }
            }
        });
    }

    private void updateChatArea() {
        SwingUtilities.invokeLater(() -> {
            chatArea.setText("");
            if (currentRecipient != null) {
                currentRecipient.resetUnreadCount();
                userList.repaint();

                List<Message> messages = conversationHistory.getOrDefault(currentRecipient.getName(),
                        new ArrayList<>());
                for (Message msg : messages) {
                    if (msg.getSender().equals(chatClient.getUserName())) {
                        chatArea.append("Voce: " + msg.getContent() + "\n");
                    } else {
                        chatArea.append(currentRecipient.getName() + ": " + msg.getContent() + "\n");
                    }
                }
            }
        });
    }

    public JFrame getFrame() {
        return frame;
    }

}