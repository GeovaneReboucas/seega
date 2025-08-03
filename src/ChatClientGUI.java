import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ChatClientGUI extends JFrame {
    private JTextField usernameField, latitudeField, longitudeField, radiusField;
    private JButton loginButton;
    private JCheckBox onlineStatusCheckbox;
    private JTabbedPane tabbedPane;
    private JPanel loginPanel, mainPanel, settingsPanel;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private String loggedInUser;
    private boolean isLoggedIn = false;

    private JList<String> nearbyContactsList;
    private DefaultListModel<String> nearbyContactsListModel;
    private JList<String> chatHistoryContactsList;
    private DefaultListModel<String> chatHistoryContactsListModel;
    private Map<String, StringBuilder> chatHistories; // Armazena o histórico de chat por contato

    private JPanel dynamicChatPanel; // Painel para exibir o chat do contato selecionado
    private JTextArea currentChatArea; // Area de texto do chat atual
    private JTextField currentMessageInput; // Campo de entrada de mensagem do chat atual
    private JButton currentSendButton; // Botão de envio do chat atual
    private String currentChatContact; // Contato com o qual o chat está aberto
    private JLabel userHeaderLabel;

    public ChatClientGUI() {
        super("Location Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // Cria o painel do header primeiro
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        headerPanel.setBackground(new Color(240, 240, 240));
        
        userHeaderLabel = new JLabel();
        userHeaderLabel.setFont(new Font("Arial", Font.BOLD, 14));
        headerPanel.add(userHeaderLabel);
        
        add(headerPanel, BorderLayout.NORTH);

        // Inicializa os históricos de chat
        chatHistories = new HashMap<>();

        // Cria todos os painéis necessários
        createLoginPanel();
        createMainPanel(); // Isso criará o settingsPanel também
        
        // Mostra inicialmente a tela de login
        add(loginPanel, BorderLayout.CENTER);
        
        setVisible(true);
    }

    private void createLoginPanel() {
        loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBorder(BorderFactory.createTitledBorder("Configuração Inicial do Usuário"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Nome do usuário
        gbc.gridx = 0; gbc.gridy = 0;
        loginPanel.add(new JLabel("Nome do Usuário:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField("", 15);
        loginPanel.add(usernameField, gbc);

        // Latitude
        gbc.gridx = 0; gbc.gridy = 1;
        loginPanel.add(new JLabel("Latitude:"), gbc);
        gbc.gridx = 1;
        latitudeField = new JTextField("-23.550520", 15);
        loginPanel.add(latitudeField, gbc);

        // Longitude
        gbc.gridx = 0; gbc.gridy = 2;
        loginPanel.add(new JLabel("Longitude:"), gbc);
        gbc.gridx = 1;
        longitudeField = new JTextField("-46.633308", 15);
        loginPanel.add(longitudeField, gbc);

        // Raio de comunicação (agora editável)
        gbc.gridx = 0; gbc.gridy = 3;
        loginPanel.add(new JLabel("Raio de Comunicação (km):"), gbc);
        gbc.gridx = 1;
        radiusField = new JTextField("10.0", 15);
        loginPanel.add(radiusField, gbc);

        // Botão de login
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        loginButton = new JButton("Entrar no Sistema");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                login();
            }
        });
        loginPanel.add(loginButton, gbc);
    }

    private void createMainPanel() {
        mainPanel = new JPanel(new BorderLayout());
        
        // Criar o painel com abas
        tabbedPane = new JTabbedPane();
        
        // Aba do Chat
        JPanel chatPanel = createChatPanel();
        tabbedPane.addTab("Chat", chatPanel);
        
        // Aba de Configurações (garante que settingsPanel é criado)
        settingsPanel = new JPanel(new GridBagLayout());
        createSettingsPanel();
        tabbedPane.addTab("Configurações", settingsPanel);
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        
        // Painel de Contatos
        JPanel contactsPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        contactsPanel.setPreferredSize(new Dimension(200, 0)); // Largura fixa para a lista de contatos
        contactsPanel.setBorder(BorderFactory.createTitledBorder("Contatos"));

        // Contatos Próximos
        nearbyContactsListModel = new DefaultListModel<>();
        nearbyContactsList = new JList<>(nearbyContactsListModel);
        nearbyContactsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        nearbyContactsList.setBorder(BorderFactory.createTitledBorder("Contatos Proximos"));
        nearbyContactsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selectedContact = nearbyContactsList.getSelectedValue();
                    if (selectedContact != null) {
                        // Remove o (Online) ou (Offline) do nome do contato
                        selectedContact = selectedContact.split(" \\(")[0];
                        openChatPanel(selectedContact);
                    }
                }
            }
        });
        contactsPanel.add(new JScrollPane(nearbyContactsList));

        // Histórico de Mensagens
        chatHistoryContactsListModel = new DefaultListModel<>();
        chatHistoryContactsList = new JList<>(chatHistoryContactsListModel);
        chatHistoryContactsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chatHistoryContactsList.setBorder(BorderFactory.createTitledBorder("Historico de Mensagens"));
        chatHistoryContactsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String selectedContact = chatHistoryContactsList.getSelectedValue();
                    if (selectedContact != null) {
                        openChatPanel(selectedContact);
                    }
                }
            }
        });
        contactsPanel.add(new JScrollPane(chatHistoryContactsList));

        chatPanel.add(contactsPanel, BorderLayout.WEST);

        // Painel de chat dinâmico (inicialmente vazio ou com mensagem de boas-vindas)
        dynamicChatPanel = new JPanel(new BorderLayout());
        dynamicChatPanel.setBorder(BorderFactory.createTitledBorder("Selecione um contato para conversar"));
        chatPanel.add(dynamicChatPanel, BorderLayout.CENTER);
        
        return chatPanel;
    }

    private void createSettingsPanel() {
        settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Configuracoes do Usuario"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Nome do usuário (somente leitura)
        gbc.gridx = 0; gbc.gridy = 0;
        settingsPanel.add(new JLabel("Nome do Usuario:"), gbc);
        gbc.gridx = 1;
        JTextField usernameDisplayField = new JTextField(15);
        usernameDisplayField.setEditable(false);
        usernameDisplayField.setBackground(Color.LIGHT_GRAY);
        settingsPanel.add(usernameDisplayField, gbc);

        // Latitude (editável)
        gbc.gridx = 0; gbc.gridy = 1;
        settingsPanel.add(new JLabel("Latitude:"), gbc);
        gbc.gridx = 1;
        JTextField latitudeUpdateField = new JTextField(15);
        settingsPanel.add(latitudeUpdateField, gbc);

        // Longitude (editável)
        gbc.gridx = 0; gbc.gridy = 2;
        settingsPanel.add(new JLabel("Longitude:"), gbc);
        gbc.gridx = 1;
        JTextField longitudeUpdateField = new JTextField(15);
        settingsPanel.add(longitudeUpdateField, gbc);

        // Status Online/Offline (editável)
        gbc.gridx = 0; gbc.gridy = 3;
        settingsPanel.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        JCheckBox statusUpdateCheckbox = new JCheckBox("Online", true); // Inicia marcado
        settingsPanel.add(statusUpdateCheckbox, gbc);

        // Raio de comunicação (editável)
        gbc.gridx = 0; gbc.gridy = 4;
        settingsPanel.add(new JLabel("Raio de Comunicacao (km):"), gbc);
        gbc.gridx = 1;
        JTextField radiusUpdateField = new JTextField(15);
        settingsPanel.add(radiusUpdateField, gbc);

        // Botão de atualização consolidado
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JButton updateInfoButton = new JButton("Atualizar Informacoes");
        updateInfoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAllUserInfo(
                    latitudeUpdateField.getText(),
                    longitudeUpdateField.getText(),
                    statusUpdateCheckbox.isSelected(),
                    radiusUpdateField.getText()
                );
            }
        });
        settingsPanel.add(updateInfoButton, gbc);
        
        // Armazenar referências para os campos de configuração
        settingsPanel.putClientProperty("usernameField", usernameDisplayField);
        settingsPanel.putClientProperty("latitudeField", latitudeUpdateField);
        settingsPanel.putClientProperty("longitudeField", longitudeUpdateField);
        settingsPanel.putClientProperty("statusCheckbox", statusUpdateCheckbox);
        settingsPanel.putClientProperty("radiusField", radiusUpdateField);
    }

    private void connectToServer() {
        try {
            // Se o socket já existe e está conectado, fecha-o antes de criar um novo
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            socket = new Socket("localhost", 5555);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(this::listenForServerMessages).start();
            System.out.println("Conectado ao servidor.\n");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar ao servidor: " + e.getMessage());
        }
    }

    private void login() {
        String username = usernameField.getText().trim();
        String lat = latitudeField.getText().trim();
        String lon = longitudeField.getText().trim();
        String radius = radiusField.getText().trim();

        if (username.isEmpty() || lat.isEmpty() || lon.isEmpty() || radius.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha todos os campos de login.");
            return;
        }

        try {
            Double.parseDouble(lat);
            Double.parseDouble(lon);
            Double.parseDouble(radius);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valores numéricos inválidos para latitude, longitude ou raio.");
            return;
        }

        if (socket == null || socket.isClosed()) {
            connectToServer();
        }

        loggedInUser = username;
        // Envia true como status online fixo
        writer.println("LOGIN|" + username + "|" + lat + "|" + lon + "|" + radius);
    }

    private void updateUserHeader() {
        if (loggedInUser != null) {
            userHeaderLabel.setText("Usuario: " + loggedInUser);
            userHeaderLabel.setIcon(new ImageIcon("user_icon.png")); // Opcional: adicione um ícone
        }
    }

    private void switchToMainInterface() {
        remove(loginPanel);
        
        // Garante que os painéis principais estão criados
        if (mainPanel == null) {
            createMainPanel();
        }
        
        add(mainPanel, BorderLayout.CENTER);
        updateUserHeader();
        
        // Só tenta acessar os campos se settingsPanel foi criado
        if (settingsPanel != null) {
            JTextField usernameDisplayField = (JTextField) settingsPanel.getClientProperty("usernameField");
            JTextField latitudeUpdateField = (JTextField) settingsPanel.getClientProperty("latitudeField");
            JTextField longitudeUpdateField = (JTextField) settingsPanel.getClientProperty("longitudeField");
            JCheckBox statusUpdateCheckbox = (JCheckBox) settingsPanel.getClientProperty("statusCheckbox");
            JTextField radiusUpdateField = (JTextField) settingsPanel.getClientProperty("radiusField");
            
            if (usernameDisplayField != null) usernameDisplayField.setText(loggedInUser);
            if (latitudeUpdateField != null) latitudeUpdateField.setText(latitudeField.getText());
            if (longitudeUpdateField != null) longitudeUpdateField.setText(longitudeField.getText());
            if (statusUpdateCheckbox != null) statusUpdateCheckbox.setSelected(true); // Sempre online
            if (radiusUpdateField != null) radiusUpdateField.setText(radiusField.getText());
        }
        
        isLoggedIn = true;
        revalidate();
        repaint();
    }

    private void sendMessage(String recipient, String content) {
        if (!isLoggedIn) return;
        
        if (recipient.isEmpty() || content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha destinatario e conteudo da mensagem.");
            return;
        }

        String messageType = "SYNC";
        writer.println("SEND_MESSAGE|" + content + "|" + recipient + "|" + messageType);
        
        // Adiciona a mensagem ao histórico do chat
        appendMessageToChatHistory(recipient, "Voce: " + content + "\n");
        
        // Atualiza a área de chat se for o contato atual
        if (currentChatArea != null && recipient.equals(currentChatContact)) {
            currentChatArea.append("Voce: " + content + "\n");
        }
    }

    private void updateAllUserInfo(String lat, String lon, boolean isOnline, String radius) {
        if (!isLoggedIn) return;

        // Atualizar Localização
        try {
            Double.parseDouble(lat);
            Double.parseDouble(lon);
            writer.println("UPDATE_LOCATION|" + lat + "|" + lon);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valores invalidos para latitude ou longitude.");
        }

        // Atualizar Status
        writer.println("UPDATE_STATUS|" + isOnline);
        
        // Removido o CHECK_PENDING forçado aqui

        // Atualizar Raio
        try {
            Double.parseDouble(radius);
            writer.println("UPDATE_RADIUS|" + radius);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor invalido para o raio de comunicação.");
        }
        JOptionPane.showMessageDialog(this, "Informacoes atualizadas com sucesso!");
    }

    private void listenForServerMessages() {
        try {
            String serverMessage;
            while ((serverMessage = reader.readLine()) != null) {
                processServerMessage(serverMessage);
            }
        } catch (IOException e) {
            if (isLoggedIn) {
                SwingUtilities.invokeLater(() -> {
                    // Não mais escreve no chatArea principal
                });
            }
            disconnect();
        }
    }

    private void processServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split("\\|");
            String command = parts[0];

            switch (command) {
                case "LOGIN_SUCCESS":
                    switchToMainInterface();
                    // Não mais escreve no chatArea principal
                    break;
                case "MESSAGE":
                    String sender = parts[1];
                    String content = parts[2];
                    String timestamp = parts[3];
                    appendMessageToChatHistory(sender, sender + ": " + content + " (" + timestamp + ")\n");
                    
                    // Se o chat com este remetente estiver aberto, atualiza a área de chat
                    if (currentChatArea != null && sender.equals(currentChatContact)) {
                        currentChatArea.append(sender + ": " + content + " (" + timestamp + ")\n");
                    }
                    break;
                case "CONTACTS":
                    nearbyContactsListModel.clear();
                    for (int i = 1; i < parts.length; i++) {
                        String[] contactInfo = parts[i].split(",");
                        if (contactInfo.length >= 4) {
                            String name = contactInfo[0];
                            boolean isOnline = Boolean.parseBoolean(contactInfo[3]);
                            nearbyContactsListModel.addElement(name + (isOnline ? " (Online)" : " (Offline)"));
                        }
                    }
                    break;
                default:
                    // Mensagens do servidor que não se encaixam nos comandos acima podem ser ignoradas ou logadas em outro lugar
                    // if (isLoggedIn) {
                    //     chatArea.append("Servidor: " + message + "\n");
                    // }
            }
        });
    }

    private void appendMessageToChatHistory(String contactName, String message) {
        chatHistories.computeIfAbsent(contactName, k -> new StringBuilder()).append(message);
        if (!chatHistoryContactsListModel.contains(contactName)) {
            chatHistoryContactsListModel.addElement(contactName);
        }
    }

    private void openChatPanel(String contactName) {
        currentChatContact = contactName;
        dynamicChatPanel.removeAll(); // Limpa o painel anterior
        dynamicChatPanel.setBorder(BorderFactory.createTitledBorder("Chat com " + contactName));

        currentChatArea = new JTextArea();
        currentChatArea.setEditable(false);
        currentChatArea.setText(chatHistories.getOrDefault(contactName, new StringBuilder()).toString());
        JScrollPane scrollPane = new JScrollPane(currentChatArea);
        dynamicChatPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        currentMessageInput = new JTextField();
        currentSendButton = new JButton("Enviar");

        currentSendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String content = currentMessageInput.getText().trim();
                if (!content.isEmpty()) {
                    sendMessage(contactName, content);
                    currentMessageInput.setText("");
                }
            }
        });
        
        currentMessageInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String content = currentMessageInput.getText().trim();
                if (!content.isEmpty()) {
                    sendMessage(contactName, content);
                    currentMessageInput.setText("");
                }
            }
        });

        inputPanel.add(currentMessageInput, BorderLayout.CENTER);
        inputPanel.add(currentSendButton, BorderLayout.EAST);
        dynamicChatPanel.add(inputPanel, BorderLayout.SOUTH);

        dynamicChatPanel.revalidate();
        dynamicChatPanel.repaint();
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

