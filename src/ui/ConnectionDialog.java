package src.ui;

import javax.swing.*;

import src.utils.Constants;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ConnectionDialog extends JDialog {
    private JTextField ipField;
    private JTextField portField;
    private boolean confirmed = false;
    private String ip;
    private int port;

    public ConnectionDialog(JFrame parent) {
        super(parent, "Configuracao de Conexao", true);
        setSize(300, 200);
        setLocationRelativeTo(parent);
        setResizable(false);
        
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        panel.add(new JLabel("IP do Servidor:"));
        ipField = new JTextField(Constants.SERVER_IP);
        panel.add(ipField);
        
        panel.add(new JLabel("Porta:"));
        portField = new JTextField(String.valueOf(Constants.SERVER_PORT));
        panel.add(portField);
        
        JButton confirmButton = new JButton("Confirmar");
        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ip = ipField.getText().trim();
                    port = Integer.parseInt(portField.getText().trim());
                    confirmed = true;
                    dispose();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(ConnectionDialog.this, 
                            Constants.ENTER_VALID_PORT, 
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);
        
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public String getIp() {
        return ip;
    }
    
    public int getPort() {
        return port;
    }
}