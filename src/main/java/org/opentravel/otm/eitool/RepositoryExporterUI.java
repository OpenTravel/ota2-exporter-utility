/**
 * Copyright (C) 2024 SkyTech Services, LLC. All rights reserved.
 */

package org.opentravel.otm.eitool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * UI that allows the user to configure and launch the export of the OpenTravel OTM repository to a GitHub repository.
 */
public class RepositoryExporterUI {

    private static final String EI_SETTINGS_PROPERTIES = ".ei-settings.properties";
    private static final String ORG_NAME = "orgName";
    private static final String GH_REPO_NAME = "ghRepoName";
    private static final String GH_ACCESS_TOKEN = "ghAccessToken";
    private static final String WINDOW_SIZE_HEIGHT = "window.size.height";
    private static final String WINDOW_SIZE_WIDTH = "window.size.width";
    private static final String WINDOW_LOCATION_Y = "window.location.y";
    private static final String WINDOW_LOCATION_X = "window.location.x";

    private static final Dimension MIN_FRAME_SIZE = new Dimension( 500, 250 );

    private PersistentProperties frameProps;
    private JFrame frame;
    private JTextField orgNameField;
    private JTextField ghRepoNameField;
    private JTextField ghAccessTokenField;
    private JButton exportButton;
    private JTextField statusTextField;
    private JProgressBar progressBar;
    private Color defaultTextColor;

    /**
     * Default constructor.
     */
    public RepositoryExporterUI() {
        try {
            this.frameProps = new PersistentProperties( EI_SETTINGS_PROPERTIES );

        } catch (IOException e) {
            System.out.println( "WARNING: Error loading application settings (using defaults)" );
            e.printStackTrace( System.out );
        }
    }

    /**
     * Called when the export button is clicked.
     */
    private void exportButtonClicked() {
        new Thread( () -> {
            ProgressMonitor monitor = new PMonitor();

            exportButton.setEnabled( false );

            try (RepositoryExporter exporter = new RepositoryExporter( orgNameField.getText(),
                ghRepoNameField.getText(), ghAccessTokenField.getText() )) {
                exporter.exportRepository( monitor );

            } catch (Exception e) {
                monitor.jobError( e.getMessage() );
                e.printStackTrace( System.out );
            }
        } ).start();
    }

    /**
     * Called just prior to the main window closing.
     */
    private void onClose() {
        try {
            frameProps.setProperty( ORG_NAME, orgNameField.getText() );
            frameProps.setProperty( GH_REPO_NAME, ghRepoNameField.getText() );
            frameProps.setProperty( GH_ACCESS_TOKEN, ghAccessTokenField.getText() );
            frameProps.setProperty( WINDOW_LOCATION_X, frame.getLocation().x + "" );
            frameProps.setProperty( WINDOW_LOCATION_Y, frame.getLocation().y + "" );
            frameProps.setProperty( WINDOW_SIZE_WIDTH, frame.getSize().width + "" );
            frameProps.setProperty( WINDOW_SIZE_HEIGHT, frame.getSize().height + "" );
            frameProps.saveProperties();

        } catch (IOException e) {
            System.out.println( "WARNING: Error saving application settings (using defaults)" );
            e.printStackTrace( System.out );
        }
    }

    /**
     * Validates the input fields to determine if the export button should be enabled.
     */
    private void validateForm() {
        boolean orgValid = (orgNameField.getText().length() > 0);
        boolean repoValid = (ghRepoNameField.getText().length() > 0);
        boolean tokenValid = (ghAccessTokenField.getText().length() > 0);

        exportButton.setEnabled( orgValid && repoValid && tokenValid );
    }

    /**
     * Initializes the form fields with any locally cached values.
     */
    protected void initForm() {
        orgNameField.setText( frameProps.getProperty( ORG_NAME, "OpenTravel" ) );
        ghRepoNameField.setText( frameProps.getProperty( GH_REPO_NAME, "" ) );
        ghAccessTokenField.setText( frameProps.getProperty( GH_ACCESS_TOKEN, "" ) );
    }

    /**
     * Initializes the components of the UI.
     * 
     * @return JFrame
     */
    protected JFrame initView() {
        // Create the main frame
        this.frame = new JFrame( "OTM Repository Exporter" );
        this.frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        this.frame.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        } );
        this.frame.setLocation( getWindowLocation() );
        this.frame.setSize( getWindowSize() );
        this.frame.setMinimumSize( MIN_FRAME_SIZE );
        this.frame.setLayout( new BorderLayout() );

        // Create the main panel to hold form components
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout( new GridBagLayout() );
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets( 5, 5, 5, 5 ); // Padding around components
        gbc.anchor = GridBagConstraints.WEST;

        // Create and add labels and text fields
        String[] labels = {"Organization Name", "GitHub Repository Name", "Access Token"};
        JTextField[] textFields = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            JLabel label = new JLabel( labels[i] );
            gbc.gridx = 0; // First column for labels
            gbc.gridy = i;
            gbc.anchor = GridBagConstraints.EAST; // Align labels to the right
            mainPanel.add( label, gbc );

            textFields[i] = new JTextField( 20 );
            textFields[i].getDocument().addDocumentListener( new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    validateForm();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    validateForm();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {}
            } );
            gbc.gridx = 1; // Second column for text fields
            gbc.anchor = GridBagConstraints.WEST; // Align text fields to the left
            mainPanel.add( textFields[i], gbc );
        }
        this.orgNameField = textFields[0];
        this.ghRepoNameField = textFields[1];
        this.ghAccessTokenField = textFields[2];

        // Create and add the button
        this.exportButton = new JButton( "Export OTM Repository" );
        this.exportButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportButtonClicked();
            }
        } );
        gbc.gridx = 0;
        gbc.gridy = labels.length; // Place below the last text field
        gbc.gridwidth = 2; // Span across two columns
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add( exportButton, gbc );

        // Add the main panel to the frame
        this.frame.add( mainPanel, BorderLayout.CENTER );

        // Add progress bar and status field
        JPanel statusPanel = new JPanel();
        this.progressBar = new JProgressBar();
        this.statusTextField = new JTextField();

        this.progressBar.setMaximum( 100 );
        this.statusTextField.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        this.statusTextField.setEditable( false );
        this.statusTextField.setBackground( frame.getBackground() );
        this.defaultTextColor = this.statusTextField.getForeground();
        statusPanel.setLayout( new BoxLayout( statusPanel, BoxLayout.Y_AXIS ) );
        statusPanel.add( this.progressBar );
        statusPanel.add( this.statusTextField );

        // Add the status bar to the frame
        this.frame.add( statusPanel, BorderLayout.SOUTH );

        // Initialize the content of the form
        initForm();
        validateForm();

        return this.frame;
    }

    /**
     * Returns the initial location of the main window.
     * 
     * @return Point
     */
    private Point getWindowLocation() {
        String xStr = frameProps.getProperty( WINDOW_LOCATION_X );
        String yStr = frameProps.getProperty( WINDOW_LOCATION_Y );
        int x = 0, y = 0;

        if ((xStr != null) && (yStr != null)) {
            try {
                x = Integer.parseInt( xStr );
                y = Integer.parseInt( yStr );

            } catch (NumberFormatException e) {
                // Ignore and continue with defaults
            }
        }
        return new Point( x, y );
    }

    /**
     * Returns the initial dimensions of the main window.
     * 
     * @return Dimension
     */
    private Dimension getWindowSize() {
        String widthStr = frameProps.getProperty( WINDOW_SIZE_WIDTH );
        String heightStr = frameProps.getProperty( WINDOW_SIZE_HEIGHT );
        int width = 0, height = 0;

        if ((widthStr != null) && (heightStr != null)) {
            try {
                width = Integer.parseInt( widthStr );
                height = Integer.parseInt( heightStr );

            } catch (NumberFormatException e) {
                // Ignore and continue with defaults
            }
        }
        return new Dimension( width, height );
    }

    private class PMonitor implements ProgressMonitor {

        /**
         * @see org.opentravel.otm.eitool.ProgressMonitor#jobStarted(java.lang.String)
         */
        @Override
        public void jobStarted(String message) {
            statusTextField.setText( message );
        }

        /**
         * @see org.opentravel.otm.eitool.ProgressMonitor#progress(double, java.lang.String)
         */
        @Override
        public void progress(double percentComplete, String message) {
            progressBar.setValue( (int) (percentComplete * 100.0) );
            statusTextField.setText( message );
        }

        /**
         * @see org.opentravel.otm.eitool.ProgressMonitor#jobComplete()
         */
        @Override
        public void jobComplete() {
            statusTextField.setText( "Export Completed Successfully!" );
            delayAndClearStatus();
        }

        /**
         * @see org.opentravel.otm.eitool.ProgressMonitor#jobError(java.lang.String)
         */
        @Override
        public void jobError(String message) {
            statusTextField.setText( "ERROR: " + message );
            statusTextField.setForeground( Color.RED );
            delayAndClearStatus();
        }

        /**
         * Delays for 5s before clearing the status message and progress bar.
         */
        private void delayAndClearStatus() {
            new Thread( () -> {
                try {
                    validateForm();
                    Thread.sleep( 5000 );
                    statusTextField.setText( "" );
                    statusTextField.setForeground( defaultTextColor );
                    progressBar.setValue( 0 );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } ).start();
        }

    }

    /**
     * Main method invoked from the command line.
     * 
     * @param args the command line arguments (ignored)
     */
    public static void main(String[] args) {
        try {
            JFrame frame = new RepositoryExporterUI().initView();

            frame.setVisible( true );

        } catch (Exception e) {
            e.printStackTrace( System.out );
        }
    }

}
