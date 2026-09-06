package com.islandpacific.monitoring.credtool;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.islandpacific.monitoring.common.CredentialProtector;

/**
 * Swing UI for CredTool: paste a credential, encrypt it with DPAPI (machine
 * scope), copy the DPAPI(...) value into a .properties file.
 */
public class CredToolUI {

    public static void launch() {
        SwingUtilities.invokeLater(CredToolUI::buildAndShow);
    }

    private static void buildAndShow() {
        JFrame frame = new JFrame("Island Pacific - Credential Encryption Tool");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPasswordField input = new JPasswordField(40);
        JCheckBox show = new JCheckBox("Show");
        char echoChar = input.getEchoChar();
        show.addActionListener(e -> input.setEchoChar(show.isSelected() ? (char) 0 : echoChar));

        JTextArea output = new JTextArea(6, 40);
        output.setEditable(false);
        output.setLineWrap(true);

        JButton encrypt = new JButton("Encrypt");
        JButton copy = new JButton("Copy to Clipboard");
        copy.setEnabled(false);

        encrypt.addActionListener(e -> {
            String value = new String(input.getPassword());
            if (value.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Enter or paste a value to encrypt.",
                        "Nothing to encrypt", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                output.setText(CredentialProtector.protect(value));
                copy.setEnabled(true);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(frame, "Encryption failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(output.getText()), null);
            copy.setText("Copied!");
            new javax.swing.Timer(1500, t -> {
                copy.setText("Copy to Clipboard");
                ((javax.swing.Timer) t.getSource()).stop();
            }).start();
        });

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        form.add(new JLabel("Value to encrypt:"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        form.add(input, c);
        c.gridx = 2; c.fill = GridBagConstraints.NONE; c.weightx = 0;
        form.add(show, c);

        c.gridx = 1; c.gridy = 1;
        form.add(encrypt, c);

        c.gridx = 0; c.gridy = 2;
        form.add(new JLabel("Encrypted value:"), c);
        c.gridx = 1; c.gridwidth = 2; c.fill = GridBagConstraints.BOTH; c.weightx = 1; c.weighty = 1;
        form.add(new JScrollPane(output), c);

        c.gridx = 1; c.gridy = 3; c.gridwidth = 1; c.fill = GridBagConstraints.NONE; c.weightx = 0; c.weighty = 0;
        form.add(copy, c);

        JLabel note = new JLabel("  Encrypted on this machine only - paste the DPAPI(...) value into the .properties file.");

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(form, BorderLayout.CENTER);
        frame.getContentPane().add(note, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
