/**
 * Copyright (C) 2024 SkyTech Services, LLC. All rights reserved.
 */

package org.opentravel.otm.eitool;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.opentravel.schemacompiler.model.TLLibraryStatus;
import org.opentravel.schemacompiler.repository.RemoteRepository;
import org.opentravel.schemacompiler.repository.RepositoryException;
import org.opentravel.schemacompiler.repository.RepositoryFileManager;
import org.opentravel.schemacompiler.repository.RepositoryItem;
import org.opentravel.schemacompiler.repository.RepositoryItemType;
import org.opentravel.schemacompiler.repository.RepositoryManager;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Scans the remote OTM repository and ensures all relevant libraries for the export have been downloaded to the local
 * repository.
 */
public class RepositoryExporter implements AutoCloseable {

    private static final String OTM_REPOSITORY_ENDPOINT = "https://www.opentravelmodel.net";
    private static final String OTM_ROOT_NAMESPACE = "http://www.opentravel.org/OTM/";
    private static final List<String> EXCLUDED_NAMES = Arrays.asList( "strawman", "test", "demo" );
    private static final File EXPORTS_FOLDER = new File( System.getProperty( "java.io.tmpdir" ) );

    private RemoteRepository repository;
    private RepositoryFileManager fileManager;
    private File exportFolder;
    private String ghOwnerName;
    private boolean ownerIsOrganization;
    private String ghRepositoryName;
    private String ghAccessToken;

    /**
     * Constructor that specifies the identifying information of the export repository and the access token to use when
     * creating it.
     * 
     * @param ghOwnerName the name of the user or organization that will own the export repository
     * @param ownerIsOrganization flag indicating whether the owner is an organization or a user
     * @param ghRepositoryName the name of the repository to export
     * @param ghAccessToken the access token to use when creating the repository
     * @throws RepositoryException thrown if the repository already exists or cannot be created
     * @throws IOException thrown if an error occurs while writing data to the repository
     */
    public RepositoryExporter(String ghOwnerName, boolean ownerIsOrganization, String ghRepositoryName,
        String ghAccessToken) throws RepositoryException, IOException {
        this.repository = (RemoteRepository) RepositoryManager.getDefault().getRepository( "Opentravel" );
        this.fileManager = RepositoryManager.getDefault().getFileManager();
        this.ghOwnerName = ghOwnerName;
        this.ownerIsOrganization = ownerIsOrganization;
        this.ghRepositoryName = ghRepositoryName;
        this.ghAccessToken = ghAccessToken;

        EXPORTS_FOLDER.mkdirs();
        this.exportFolder = Files.createTempDirectory( EXPORTS_FOLDER.toPath(), "otm_export_" ).toFile();

    }

    /**
     * Tests the connection to the OpenTravel OTM repository and returns true if the connection and user authentication
     * was successful.
     * 
     * @return boolean
     */
    public boolean testOTMConnection() {
        boolean result = false;

        if (repository != null) {
            try {
                repository.getUserAuthorization( OTM_ROOT_NAMESPACE ); // Forces call with current credentials
                result = true;

            } catch (RepositoryException e) {
                // Ignore and return false
            }
        }
        return result;
    }

    /**
     * Updates (or creates) credentials for the OpenTravel OTM repository.
     * 
     * @param username the username that will be used to access the OTM repository
     * @param password the password credential for the OTM user
     * @throws RepositoryException thrown if the credentials are invalid
     */
    public void updateOTMCredentials(String username, String password) throws RepositoryException {
        RepositoryManager rm = RepositoryManager.getDefault();

        if (repository == null) {
            repository = rm.addRemoteRepository( OTM_REPOSITORY_ENDPOINT );
        }
        rm.setCredentials( repository, username, password );
    }

    /**
     * Orchestrates all actions required to create an export of the OpenTravel OTM repository.
     * 
     * @throws RepositoryException thrown if an error occurrs while accessing the OTM repository
     * @throws GitAPIException thrown if an error occurrs while initializing the local Git repository or
     *         creating/pushing-to remote
     * @throws IOException thrown if an error occurrs while creating the export
     */
    protected void exportRepository(ProgressMonitor monitor) throws RepositoryException, GitAPIException, IOException {
        List<RepositoryItem> itemList = scanRepository( monitor );

        if (monitor != null) {
            monitor.progress( 1.0, "Creating repository export..." );
        }
        copyFilesToExport( itemList );

        if (monitor != null) {
            monitor.progress( 1.0, "Pushing repository export to GitHub..." );
        }
        try {
            Git git = Git.init().setDirectory( exportFolder ).call();
            String remoteRepoUrl = createGitHubRepo();

            git.add().addFilepattern( "." ).call();
            git.commit().setMessage( "Initial commit" ).call();
            git.remoteAdd().setName( "origin" ).setUri( new org.eclipse.jgit.transport.URIish( remoteRepoUrl ) ).call();
            git.push().setCredentialsProvider( new UsernamePasswordCredentialsProvider( ghAccessToken, "" ) )
                .setRemote( "origin" ).call();

        } catch (GitAPIException | URISyntaxException e) {
            throw new IOException( "Error pushing export repository to GitHub", e );
        }

        if (monitor != null) {
            monitor.jobComplete();
        }
    }

    /**
     * Scans the remote repository and ensures that all libraries to be exported have been downloaded to the local file
     * system.
     * 
     * @param monitor the progress monitor for the job
     * @return List&lt;RepositoryItem&gt;
     * @throws RepositoryException
     */
    protected List<RepositoryItem> scanRepository(ProgressMonitor monitor) throws RepositoryException {
        List<String> namespaces = repository.listBaseNamespaces();
        List<RepositoryItem> allItems = new ArrayList<>();
        int counter = 0;

        if (monitor != null) {
            monitor.jobStarted( "Scanning remote repository..." );
        }

        // Collect the list of all repository items that should be included in the export
        for (String baseNS : namespaces) {
            if (isValidNamespace( baseNS )) {
                List<RepositoryItem> nsItems =
                    repository.listItems( baseNS, TLLibraryStatus.DRAFT, false, RepositoryItemType.LIBRARY );

                for (RepositoryItem item : nsItems) {
                    if (!isExcluded( item.getFilename() )) {
                        allItems.add( item );
                    }
                }
            }
        }

        // Force the download of all repository items to be exported
        for (RepositoryItem item : allItems) {
            if (monitor != null) {
                double percentComplete = ((double) ++counter) / ((double) allItems.size());

                monitor.progress( percentComplete, String.format( "Downloading: %s", item.getFilename() ) );
            }
            repository.downloadContent( item, true );
        }
        return allItems;
    }

    /**
     * Copies the files associated with each repository item (library) in the list to the export folder.
     * 
     * @param itemList the list of repository items to be exported
     * @throws RepositoryException thrown if the local copy of the library file cannot be located
     * @throws GitAPIException thrown if an error occurrs while initializing the local Git repository
     * @throws IOException thrown if an error occurrs while copying the files
     */
    protected void copyFilesToExport(List<RepositoryItem> itemList)
        throws RepositoryException, GitAPIException, IOException {
        for (RepositoryItem item : itemList) {
            File sourceFile =
                fileManager.getLibraryContentLocation( item.getBaseNamespace(), item.getFilename(), item.getVersion() );
            File destFile = new File( exportFolder, String.format( "/%s", item.getFilename() ) );

            FileUtils.copyFile( sourceFile, destFile );
        }
    }

    /**
     * Creates the remote GitHub repository and returns the URL. If a repository of the same name already exists, an
     * exception will be thrown.
     * 
     * @return String
     * @throws IOException thrown if the remote repository already exists or an error occurrs while accessing GitHub
     */
    protected String createGitHubRepo() throws IOException {
        OkHttpClient client = new OkHttpClient();

        // Determine the API base URL for repo creation/check
        String baseRepoUrl = ownerIsOrganization ? "https://api.github.com/orgs/" + ghOwnerName + "/repos"
            : "https://api.github.com/user/repos";

        // Step 1: Check if the repository already exists
        String repoCheckUrl =
            ownerIsOrganization ? "https://api.github.com/repos/" + ghOwnerName + "/" + ghRepositoryName
                : "https://api.github.com/repos/" + ghOwnerName + "/" + ghRepositoryName;

        Request checkRequest =
            new Request.Builder().url( repoCheckUrl ).header( "Authorization", "Bearer " + ghAccessToken )
                .header( "Accept", "application/vnd.github+json" ).get().build();

        try (Response checkResponse = client.newCall( checkRequest ).execute()) {
            if (checkResponse.isSuccessful()) {
                throw new IOException( "Repository already exists: " + ghRepositoryName );
            } else if (checkResponse.code() != 404) {
                throw new IOException(
                    "Error checking repository existence: " + checkResponse.code() + " - " + checkResponse.message() );
            }
        }

        // Step 2: Create the repository if it doesn't exist
        String requestBody = "{\"name\":\"" + ghRepositoryName + "\",\"private\":false}";

        Request createRequest = new Request.Builder().url( baseRepoUrl )
            .header( "Authorization", "Bearer " + ghAccessToken ).header( "Accept", "application/vnd.github+json" )
            .post( RequestBody.create( requestBody, MediaType.parse( "application/json" ) ) ).build();

        try (Response createResponse = client.newCall( createRequest ).execute()) {
            if (createResponse.isSuccessful()) {
                return "https://github.com/" + ghOwnerName + "/" + ghRepositoryName + ".git";
            } else if (createResponse.code() == 422) { // Unprocessable Entity, repository name conflict
                throw new IOException( "Repository conflicts with an existing repository: " + ghRepositoryName );
            } else {
                throw new IOException(
                    "Error creating repository: " + createResponse.code() + " - " + createResponse.message() );
            }
        }
    }

    /**
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        FileUtils.deleteDirectory( exportFolder );
    }

    /**
     * Returns true if the given namespace should be included in the export.
     * 
     * @param ns the namespace to check
     * @return boolean
     */
    private boolean isValidNamespace(String ns) {
        return (ns != null) && ns.startsWith( OTM_ROOT_NAMESPACE ) && !isExcluded( ns );
    }

    /**
     * Returns true if the item with the given name should be excluded from the export.
     * 
     * @param itemName the name of the item to check
     * @return boolean
     */
    private boolean isExcluded(String itemName) {
        boolean excluded = false;

        for (String exclude : EXCLUDED_NAMES) {
            excluded |= itemName.contains( exclude );
        }
        return excluded;
    }

}
