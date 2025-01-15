/**
 * Copyright (C) 2025 SkyTech Services, LLC. All rights reserved.
 */

package org.opentravel.otm.eitool;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Launches a dialog to collect the user's credentials for the OpenTravel OTM repository.
 */
public class OTMCredentialsDialogUI {

    /**
     * Displays a login dialog to collect the username and password for the OpenTravel OTM repository. If successful,
     * the returned array will contain two elements with the username and password provided by the user. If the user
     * cancels, null will be returned instead of the array of strings.
     * 
     * @return String[]
     */
    public static String[] showDialog() {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        // Create fields for message and panel layout
        JLabel messageLabel = new JLabel( "Please enter your credentials for the OpenTravel OTM Repository" );
        messageLabel.setForeground( Color.BLACK );

        // Panel to hold input fields and message
        JPanel panel = new JPanel( new GridLayout( 0, 1 ) );
        panel.add( messageLabel );
        panel.add( new JLabel( "Username:" ) );
        panel.add( usernameField );
        panel.add( new JLabel( "Password:" ) );
        panel.add( passwordField );

        // Create a custom OK button
        JButton okButton = new JButton( "OK" );
        okButton.setEnabled( false ); // Initially disabled

        // Create a dialog with custom buttons
        JOptionPane optionPane = new JOptionPane( panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION );
        JDialog dialog = optionPane.createDialog( "Invalid Login Credentials" );
        dialog.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );

        // Enable OK button only if both fields are valid
        DocumentListener inputListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateInputs();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateInputs();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateInputs();
            }

            private void validateInputs() {
                String username = usernameField.getText().trim();
                String password = new String( passwordField.getPassword() ).trim();
                boolean isValid = !username.isEmpty() && !username.contains( " " ) && !password.isEmpty()
                    && !password.contains( " " );

                okButton.setEnabled( isValid );
            }
        };

        // Attach the listener to input fields
        usernameField.getDocument().addDocumentListener( inputListener );
        passwordField.getDocument().addDocumentListener( inputListener );

        // Add custom button functionality
        optionPane.setOptions( new Object[] {okButton, "Cancel"} );
        okButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                optionPane.setValue( JOptionPane.OK_OPTION ); // Explicitly set the value
                dialog.dispose();
            }
        } );

        dialog.setVisible( true );

        // Process the user input after the dialog closes
        Object selectedValue = optionPane.getValue();

        if (selectedValue instanceof Integer && (int) selectedValue == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            char[] password = passwordField.getPassword();
            String passwordString = new String( password ); // Convert password char array to String

            java.util.Arrays.fill( password, ' ' ); // Clear password array for security
            return new String[] {username, passwordString};
        }
        return null; // User canceled or closed the dialog
    }

}
