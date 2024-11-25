/**
 * Copyright (C) 2024 SkyTech Services, LLC. All rights reserved.
 */

package org.opentravel.otm.eitool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Properties subclass that automatically saves and loads its content to/from a file.
 */
public class PersistentProperties extends Properties {

    private static final long serialVersionUID = -7989912540264560112L;

    public static final File OTA2_FOLDER = new File( System.getProperty( "user.home" ), "/.ota2" );

    private File saveFile;

    /**
     * Constructor that specifies the name of the file in the <code>~/.ota2</code> directory where the information will
     * be saved. If a file of the name already exists, its contents will be loaded by this constructor.
     * 
     * @param filename the name of the file where properties will be persisted
     * @throws IOException thrown if an error occurrs while loading an existing file
     */
    public PersistentProperties(String filename) throws IOException {
        this.saveFile = new File( OTA2_FOLDER, filename );

        if (this.saveFile.exists()) {
            try (InputStream is = new FileInputStream( this.saveFile )) {
                this.load( is );
            }
        }
    }

    /**
     * Saves all properties to the persistent file.
     * 
     * @throws IOException thrown if an error occurrs while saving the file
     */
    public void saveProperties() throws IOException {
        try (OutputStream os = new FileOutputStream( this.saveFile )) {
            this.store( os, null );
        }
    }

}
