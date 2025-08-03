// package com.locationchat.client;

// import com.locationchat.model.Location;
// import com.locationchat.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatClientGUI extends JFrame {
    private JTextField usernameField, latitudeField, longitudeField, radiusField, recipientField, messageContentField;
    private JTextArea chatArea, contactsArea;
    private JButton loginButton, sendMessageButton, updateLocationButton, updateStatusButton, updateRadiusButton;
    private JCheckBox onlineStatusCheckbox;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private String loggedInUser;
    private ScheduledExecutorService scheduler;

    public ChatClientGUI() {
        super("Location Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // Painel de Controle
        JPanel controlPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controle do Usuário"));

        usernameField = new JTextField("Alice");
        latitudeField = new JTextField("-23.550520");
        longitudeField = new JTextField("-46.633308");
        radiusField = new JTextField("10.0");
        onlineStatusCheckbox = new JCheckBox("Online", true);
        loginButton = new JButton("Login");

        controlPanel.add(new JLabel("Usuário:"));
        controlPanel.add(usernameField);
        controlPanel.add(new JLabel("Latitude:"));
        controlPanel.add(latitudeField);
        controlPanel.add(new JLabel("Longitude:"));
        controlPanel.add(longitudeField);
        controlPanel.add(new JLabel("Raio (km):"));
        controlPanel.add(radiusField);
        controlPanel.add(onlineStatusCheckbox);
        controlPanel.add(loginButton);

        updateLocationButton = new JButton("Atualizar Localização");
        updateStatusButton = new JButton("Atualizar Status");
        updateRadiusButton = new JButton("Atualizar Raio");

        controlPanel.add(updateLocationButton);
        controlPanel.add(updateStatusButton);
        controlPanel.add(updateRadiusButton);

        add(controlPanel, BorderLayout.NORTH);

        // Painel de Chat e Contatos
        JPanel chatAndContactsPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(BorderFactory.createTitledBorder("Chat"));

        contactsArea = new JTextArea();
        contactsArea.setEditable(false);
        JScrollPane contactsScrollPane = new JScrollPane(contactsArea);
        contactsScrollPane.setBorder(BorderFactory.createTitledBorder("Contatos Próximos"));

        chatAndContactsPanel.add(chatScrollPane);
        chatAndContactsPanel.add(contactsScrollPane);

        add(chatAndContactsPanel, BorderLayout.CENTER);

        // Painel de Envio de Mensagens
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBorder(BorderFactory.createTitledBorder("Enviar Mensagem"));

        JPanel messageInputPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        recipientField = new JTextField();
        messageContentField = new JTextField();
        messageInputPanel.add(new JLabel("Destinatário:"));
        messageInputPanel.add(recipientField);
        messageInputPanel.add(new JLabel("Mensagem:"));
        messageInputPanel.add(messageContentField);

        sendMessageButton = new JButton("Enviar");

        messagePanel.add(messageInputPanel, BorderLayout.CENTER);
        messagePanel.add(sendMessageButton, BorderLayout.EAST);

        add(messagePanel, BorderLayout.SOUTH);

        addListeners();
        setVisible(true);
    }

    private void addListeners() {
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });

        sendMessageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        updateLocationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateLocation();
            }
        });

        updateStatusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStatus();
            }
        });

        updateRadiusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateRadius();
            }
        });
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(this::listenForServerMessages).start();
            chatArea.append("Conectado ao servidor.\n");
        } catch (IOException e) {
            chatArea.append("Erro ao conectar ao servidor: " + e.getMessage() + "\n");
        }
    }

    private void login() {
        String username = usernameField.getText();
        String lat = latitudeField.getText();
        String lon = longitudeField.getText();
        String radius = radiusField.getText();

        if (username.isEmpty() || lat.isEmpty() || lon.isEmpty() || radius.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha todos os campos de login.");
            return;
        }

        if (socket == null || socket.isClosed()) {
            connectToServer();
        }

        loggedInUser = username;
        writer.println("LOGIN|" + username + "|" + lat + "|" + lon + "|" + radius);
        chatArea.append("Tentando logar como " + username + "...\n");
    }

    private void sendMessage() {
        String recipient = recipientField.getText();
        String content = messageContentField.getText();

        if (recipient.isEmpty() || content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha destinatário e conteúdo da mensagem.");
            return;
        }

        // Decidir se é síncrona ou assíncrona. Por enquanto, sempre síncrona se online, assíncrona se offline.
        // A lógica de raio será tratada no servidor.
        String messageType = "SYNC"; // Será decidido no servidor se é SYNC ou ASYNC
        writer.println("SEND_MESSAGE|" + content + "|" + recipient + "|" + messageType);
        chatArea.append("Você para " + recipient + ": " + content + "\n");
        messageContentField.setText("");
    }

    private void updateLocation() {
        String lat = latitudeField.getText();
        String lon = longitudeField.getText();
        if (loggedInUser != null && !lat.isEmpty() && !lon.isEmpty()) {
            writer.println("UPDATE_LOCATION|" + lat + "|" + lon);
            chatArea.append("Localização atualizada para: " + lat + ", " + lon + "\n");
        }
    }

    private void updateStatus() {
        boolean isOnline = onlineStatusCheckbox.isSelected();
        if (loggedInUser != null) {
            writer.println("UPDATE_STATUS|" + isOnline);
            chatArea.append("Status atualizado para: " + (isOnline ? "Online" : "Offline") + "\n");
        }
    }

    private void updateRadius() {
        String radius = radiusField.getText();
        if (loggedInUser != null && !radius.isEmpty()) {
            writer.println("UPDATE_RADIUS|" + radius);
            chatArea.append("Raio de comunicação atualizado para: " + radius + " km\n");
        }
    }

    private void listenForServerMessages() {
        try {
            String serverMessage;
            while ((serverMessage = reader.readLine()) != null) {
                processServerMessage(serverMessage);
            }
        } catch (IOException e) {
            chatArea.append("Conexão com o servidor perdida: " + e.getMessage() + "\n");
            disconnect();
        }
    }

    private void processServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split("\\|");
            String command = parts[0];

            switch (command) {
                case "LOGIN_SUCCESS":
                    chatArea.append("Login bem-sucedido como " + parts[1] + "\n");
                    break;
                case "MESSAGE":
                    chatArea.append(parts[1] + ": " + parts[2] + " (" + parts[3] + ")\n");
                    break;
                case "CONTACTS":
                    contactsArea.setText("Contatos Próximos:\n");
                    for (int i = 1; i < parts.length; i++) {
                        String[] contactInfo = parts[i].split(",");
                        if (contactInfo.length >= 4) {
                            String name = contactInfo[0];
                            String lat = contactInfo[1];
                            String lon = contactInfo[2];
                            boolean isOnline = Boolean.parseBoolean(contactInfo[3]);
                            contactsArea.append(name + " (Lat: " + lat + ", Lon: " + lon + ", Online: " + isOnline + ")\n");
                        }
                    }
                    break;
                default:
                    chatArea.append("Servidor: " + message + "\n");
            }
        });
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (reader != null) reader.close();
            if (writer != null) writer.close();
        } catch (IOException e) {
            System.err.println("Erro ao desconectar: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClientGUI::new);
    }
}

