package admin;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
import javax.swing.SwingWorker;
import javax.swing.Timer;

import org.apache.activemq.ActiveMQConnectionFactory;

public class AdminUI {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private BrokerManagerWithNotifications brokerManager;
    private JFrame frame;

    // Componentes para filas
    private DefaultListModel<QueueInfo> queueListModel;

    // Componentes para Topicos
    private DefaultListModel<TopicInfo> topicListModel;
    private JList<TopicInfo> topicList;
    private JTextField topicNameField;
    private JTextArea topicDetailsArea;

    // Componentes para usuarios
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private JTextField userNameField;
    private JTextArea userDetailsArea;

    // Componentes para estatísticas
    private JLabel statsLabel;
    private Timer refreshTimer;

    private Connection adminConn;
    private Session adminSession;

    public AdminUI() {
        this.brokerManager = new BrokerManagerWithNotifications();
        initialize();
        startAutoRefresh();
    }

    private void initialize() {
        frame = new JFrame("Administrador do Broker ActiveMQ");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLocationRelativeTo(null);

        try {
            // Configuração da conexão JMS para atualizações
            ConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);
            Connection adminConn = cf.createConnection();
            Session adminSession = adminConn.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Topic adminTopic = adminSession.createTopic("ADMIN_UPDATES");
            MessageConsumer adminConsumer = adminSession.createConsumer(adminTopic);
            adminConn.start();

            adminConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage && ((TextMessage) message).getText().equals("REFRESH")) {
                        refreshUsers(); // Atualiza a lista completa
                    }
                } catch (JMSException e) {
                    System.err.println("Erro ao processar atualização: " + e.getMessage());
                }
            });

            // Armazena a conexão para fechar posteriormente
            this.adminConn = adminConn;
            this.adminSession = adminSession;

        } catch (JMSException e) {
            System.err.println("Erro ao configurar conexão JMS: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame,
                    "Erro ao conectar com o servidor de mensagens",
                    "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
        }

        // Painel principal com abas
        JTabbedPane tabbedPane = new JTabbedPane();

        // Aba de Topicos
        tabbedPane.addTab("Topicos", createTopicsPanel());

        // Aba de Usuarios
        tabbedPane.addTab("Usuarios", createUsersPanel());

        // Aba de Estatisticas
        tabbedPane.addTab("Estatisticas", createStatsPanel());

        frame.add(tabbedPane, BorderLayout.CENTER);

        frame.setVisible(true);

        // Carrega dados iniciais
        refreshAllData();
    }

    private JPanel createTopicsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Painel esquerdo - Lista de Topicos
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Topicos Existentes"));

        topicListModel = new DefaultListModel<>();
        topicList = new JList<>(topicListModel);
        topicList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        topicList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateTopicDetails();
            }
        });

        leftPanel.add(new JScrollPane(topicList), BorderLayout.CENTER);

        // Painel de controles para Topicos
        JPanel topicControlPanel = new JPanel(new FlowLayout());
        topicNameField = new JTextField(15);
        JButton createTopicBtn = new JButton("Criar Topico");
        JButton removeTopicBtn = new JButton("Remover Topico");
        JButton refreshTopicsBtn = new JButton("Atualizar");

        createTopicBtn.addActionListener(e -> createTopic());
        removeTopicBtn.addActionListener(e -> removeTopic());
        refreshTopicsBtn.addActionListener(e -> refreshTopics());

        topicControlPanel.add(new JLabel("Nome:"));
        topicControlPanel.add(topicNameField);
        topicControlPanel.add(createTopicBtn);
        topicControlPanel.add(removeTopicBtn);
        topicControlPanel.add(refreshTopicsBtn);

        leftPanel.add(topicControlPanel, BorderLayout.SOUTH);

        // Painel direito - Detalhes do topico
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Detalhes do Topico"));

        topicDetailsArea = new JTextArea();
        topicDetailsArea.setEditable(false);
        rightPanel.add(new JScrollPane(topicDetailsArea), BorderLayout.CENTER);

        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(400);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Painel esquerdo - Lista de usuarios
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Usuarios Ativos"));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateUserDetails();
            }
        });

        leftPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        // Painel de controles para usuarios
        JPanel userControlPanel = new JPanel(new FlowLayout());
        userNameField = new JTextField(15);
        JButton addUserBtn = new JButton("Adicionar Usuario");

        addUserBtn.addActionListener(e -> addUser());

        userControlPanel.add(new JLabel("Nome:"));
        userControlPanel.add(userNameField);
        userControlPanel.add(addUserBtn);

        leftPanel.add(userControlPanel, BorderLayout.SOUTH);

        // Painel direito - Detalhes do usuario
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Detalhes do Usuario"));

        userDetailsArea = new JTextArea();
        userDetailsArea.setEditable(false);
        rightPanel.add(new JScrollPane(userDetailsArea), BorderLayout.CENTER);

        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(400);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Título
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 20, 10);

        // Area de estatísticas
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(10, 10, 10, 10);

        statsLabel = new JLabel();
        statsLabel.setVerticalAlignment(JLabel.TOP);
        JScrollPane statsScrollPane = new JScrollPane(statsLabel);
        statsScrollPane.setBorder(BorderFactory.createTitledBorder("Informacoes Gerais"));
        panel.add(statsScrollPane, gbc);

        // Botão de atualização
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        JButton refreshStatsBtn = new JButton("Atualizar Estatisticas");
        refreshStatsBtn.addActionListener(e -> refreshStats());
        panel.add(refreshStatsBtn, gbc);

        return panel;
    }

    private void refreshQueues() {
        SwingUtilities.invokeLater(() -> {
            queueListModel.clear();
            List<QueueInfo> queues = brokerManager.listQueues();
            for (QueueInfo queue : queues) {
                queueListModel.addElement(queue);
            }
        });
    }

    // Métodos para gerenciamento de Topicos
    private void createTopic() {
        String topicName = topicNameField.getText().trim();
        if (topicName.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Digite um nome para o topico", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (brokerManager.createTopic(topicName)) {
            topicNameField.setText("");
            refreshTopics();
            JOptionPane.showMessageDialog(frame, "Topico criado com sucesso!", "Sucesso",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(frame, "Erro ao criar topico", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeTopic() {
        TopicInfo selected = topicList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(frame, "Selecione um topico para remover", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(frame,
                "Tem certeza que deseja remover o topico '" + selected.getName() + "'?",
                "Confirmar Remoção", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (brokerManager.removeTopic(selected.getName())) {
                refreshTopics();
                JOptionPane.showMessageDialog(frame, "Topico removido com sucesso!", "Sucesso",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame, "Erro ao remover topico", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void refreshTopics() {
        SwingUtilities.invokeLater(() -> {
            topicListModel.clear();
            List<TopicInfo> topics = brokerManager.listTopics();
            for (TopicInfo topic : topics) {
                topicListModel.addElement(topic);
            }
        });
    }

    private void updateTopicDetails() {
        TopicInfo selected = topicList.getSelectedValue();
        if (selected != null) {
            StringBuilder details = new StringBuilder();
            details.append("Nome: ").append(selected.getName()).append("\n");
            details.append("Consumidores: ").append(selected.getConsumerCount()).append("\n");
            details.append("Produtores: ").append(selected.getProducerCount()).append("\n");
            details.append("Mensagens enviadas: ").append(selected.getMessageCount()).append("\n");

            topicDetailsArea.setText(details.toString());
        } else {
            topicDetailsArea.setText("");
        }
    }

    // Métodos para gerenciamento de usuarios
    private void addUser() {
        String userName = userNameField.getText().trim();
        if (userName.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Digite um nome para o usuario", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Adiciona imediatamente na lista local (otimista)
        SwingUtilities.invokeLater(() -> {
            userListModel.addElement(userName);
            userNameField.setText("");
        });

        // Operação assíncrona para confirmação real
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return brokerManager.addUser(userName);
            }

            @Override
            protected void done() {
                try {
                    if (get()) { // Se foi criado com sucesso
                        JOptionPane.showMessageDialog(frame,
                                "Usuario " + userName + " criado com sucesso!",
                                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        // Remove da lista se falhou
                        SwingUtilities.invokeLater(() -> {
                            userListModel.removeElement(userName);
                            JOptionPane.showMessageDialog(frame,
                                    "Falha ao criar usuario " + userName,
                                    "Erro", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        userListModel.removeElement(userName);
                        JOptionPane.showMessageDialog(frame,
                                "Erro: " + e.getMessage(),
                                "Erro", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }
        }.execute();
    }

    private void refreshUsers() {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return brokerManager.listUsers();
            }

            @Override
            protected void done() {
                try {
                    List<String> users = get();
                    userListModel.clear();
                    users.forEach(userListModel::addElement);
                    System.out.println("Lista de usuarios atualizada: " + users);
                } catch (Exception e) {
                    System.err.println("Erro ao atualizar lista: " + e.getMessage());
                    JOptionPane.showMessageDialog(frame,
                            "Erro ao atualizar lista de usuarios",
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void updateUserDetails() {
        String selected = userList.getSelectedValue();
        if (selected != null) {
            StringBuilder details = new StringBuilder();
            details.append("Nome do usuario: ").append(selected).append("\n");
            details.append("Fila pessoal: QUEUE.").append(selected).append("\n");
            userDetailsArea.setText(details.toString());
        } else {
            userDetailsArea.setText("");
        }
    }

    // Métodos para estatísticas
    private void refreshStats() {
        SwingUtilities.invokeLater(() -> {
            BrokerStats stats = brokerManager.getBrokerStats();

            StringBuilder statsText = new StringBuilder();
            statsText.append("<html>");
            statsText.append("<br>");
            statsText.append("<b>Total de Mensagens:</b> ").append(stats.getTotalMessages()).append("<br>");
            statsText.append("<b>Total de Consumidores:</b> ").append(stats.getTotalConsumers()).append("<br>");
            statsText.append("<b>Total de Produtores:</b> ").append(stats.getTotalProducers()).append("<br>");
            statsText.append("<b>Numero de Filas:</b> ").append(stats.getQueueCount()).append("<br>");
            statsText.append("<b>Numero de Topicos:</b> ").append(stats.getTopicCount()).append("<br>");
            statsText.append("<br>");
            statsText.append("<h3>Usuarios Ativos</h3>");

            List<String> users = brokerManager.listUsers();
            if (users.isEmpty()) {
                statsText.append("Nenhum usuario ativo<br>");
            } else {
                for (String user : users) {
                    statsText.append("- ").append(user).append("<br>");
                }
            }

            statsText.append("<br>");
            statsText.append("<small>Ultima atualizacao: ").append(new java.util.Date()).append("</small>");
            statsText.append("</html>");

            statsLabel.setText(statsText.toString());
        });
    }

    private void refreshAllData() {
        refreshQueues();
        refreshTopics();
        refreshUsers();
        refreshStats();
    }

    private void startAutoRefresh() {
        // Atualiza automaticamente a cada 30 segundos
        refreshTimer = new Timer(30000, e -> refreshStats());
        refreshTimer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new AdminUI();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Erro ao iniciar o sistema de administracao:\n" + e.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }

    // Método para fechar recursos ao sair
    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        if (brokerManager != null) {
            brokerManager.close();
        }
        try {
            if (adminSession != null) {
                adminSession.close();
            }
            if (adminConn != null) {
                adminConn.close();
            }
        } catch (JMSException e) {
            System.err.println("Erro ao fechar conexoes: " + e.getMessage());
        }
        frame.dispose();
    }
}
