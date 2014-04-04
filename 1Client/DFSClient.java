/*
 * ~~~ Aaron Oziel ~~~
 * Date Due: December 13, 2012
 * Class: CSS 434 (Fukuda)
 *
 *  DFSClient.java
 *  Assumptions:
 *      -- Only basic user I/O validation is in place, all users are expected
 *          to know how to use this program. (ie: not entering words for a port #)
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
import java.rmi.server.UnicastRemoteObject;

public class DFSClient extends UnicastRemoteObject implements ClientInterface {

    public enum State {

        Invalid, Read_Shared, Write_Owned, Release_Ownership
    };
    public static final char READ = 'r',
            WRITE = 'w';
    final String FILE_NAME, // File NAME to be read in
            ADDRESS, // ie: uw1-320-07
            PATH; // ie: /tmp/username.txt
    final String[] READ_ONLY, // Command to set file permissions Read Only
            READ_WRITE, // "         "       "       "     Read & Write
            EMACS;              // Command to open an emacs session
    // Local Cache Vars
    File tempFile;
    String localFileName;
    FileContents localCache;
    State state;    // State of cached file
    ServerInterface server;

    /**
     * Default Constructor
     *
     * @throws RemoteException
     */
    public DFSClient( ServerInterface servint ) throws IOException {
        // Initialize file related vars
        FILE_NAME = "aaronoz.txt";
        PATH = "tmp/" + FILE_NAME;
        ADDRESS = InetAddress.getLocalHost().getHostName();
        // Init. Commands
        READ_ONLY = new String[]{ "chmod", "400", PATH };
        READ_WRITE = new String[]{ "chmod", "600", PATH };
        EMACS = new String[]{ "sh", "-c", "vim " + PATH + " </dev/tty >/dev/tty" };
        //EMACS = new String[]{ "emacs", "tmp/aaronoz.txt" };
        // Init. class vars
        tempFile = new File( PATH );
        localFileName = "aaronoz";
        localCache = null;
        state = State.Invalid;
        server = servint;
    }

    /**
     * @param args [0] Server Address (IE: UW1-320-##), [1] Port (IE: 64589),
     */
    public static void main( String[] args ) {
        try {
            String address = InetAddress.getLocalHost().getHostName();
            if ( args.length < 2 ) {
                System.out.println( "Too Few Arguments: <server address> <port>" );
                return;
            }
            // Setup server and IO variables
            ServerInterface server = ( ServerInterface ) Naming.lookup(
                    "rmi://" + args[0] + ":" + args[1] + "/dfsserver" );
            BufferedReader br = new BufferedReader( new InputStreamReader( System.in ) );
            String fileName, fileMode;
            DFSClient client = new DFSClient( server );
            Naming.rebind( "rmi://localhost:" + args[1] + "/dfsclient", client );
            
            // Bulk of the program here
            while ( true ) {
                // Ask for input
                System.out.println( "File Client: " + address );
                System.out.print( "File Name: " );
                fileName = br.readLine().trim();
                //if ( !fileName.endsWith( ".txt" ) )
                //    fileName += ".txt";
                // Exit?
                if ( fileName.startsWith( "exit" ) ) {
                    System.exit( 0 );
                } else
                    if ( fileName.startsWith( "more" ) ) {
                        System.out.println( client.toString() );
                        continue;
                    }
                System.out.print( "How? (r/w): " );
                fileMode = br.readLine().trim();
                // Use input
                if ( client.openFile( fileName, fileMode ) )
                    client.emacs();
            }
        } catch ( NotBoundException ex ) {
            System.out.println( "Error[DFSClient#006]   Not able to bind server" );
            ex.printStackTrace();
        } catch ( MalformedURLException ex ) {
            System.out.println( "Error[DFSClient#007]   Malformed URL" );
            ex.printStackTrace();
        } catch ( UnknownHostException ex ) {
            System.out.println( "Error[DFSClient#008]   Unknown Host" );
            ex.printStackTrace();
        } catch ( RemoteException ex ) {
            System.out.println( "Error[DFSClient#009]   Remote Exception" );
            ex.printStackTrace();
        } catch ( IOException ex ) {
            System.out.println( "Error[DFSClient#010]   IOException" );
            ex.printStackTrace();
        }
    }

    /**
     * Prepares the local file to be opened by first checking the local cache,
     * then taking all steps necessary to reset permissions and download any new
     * file.
     *
     * @param filename Name of file to be downloaded
     * @param mode File mode (Read or Write)
     * @throws IOException
     */
    private boolean openFile( String filename, String sMode ) {
        // Format mode variables correctly.
        sMode = sMode.substring( 0 ).toLowerCase();
        char mode = sMode.charAt( 0 );
        boolean success = false;
        if ( !( mode == READ || mode == WRITE ) ) {
            System.out.println( "Error[DFSClient#014]   Improper mode '"
                    + mode + "'" );
            return false;
        }
        // File Replacement
        if ( !localFileName.equals( filename ) ) {
            if ( state == State.Write_Owned )
                uploadFile();
            state = State.Invalid;
        }

        switch ( state ) {
            case Invalid: // A) 1, 2
                success = downloadFile( filename, sMode );
                if ( success )
	                state = ( mode == READ ) ? State.Read_Shared : State.Write_Owned;
                break;
            case Read_Shared: // B) 2
                if ( mode == WRITE ) {
                    success = downloadFile( filename, sMode );
                    state = State.Write_Owned;
                } else
                    success = true;
                break;
            case Write_Owned:
                // Already Done Above
                success = true;
                break;
            case Release_Ownership:
                System.out.println( "Proabably shouldn't be here. . . " );
                success = false;
                break;
        }		changePermission( sMode );
        return success;
    }

    /**
     * Opens the currently cached file in Emacs if possible.
     */
    private void emacs() {
        try {
            Process p = Runtime.getRuntime().exec( EMACS );
            p.waitFor();
        } catch ( FileNotFoundException ex ) {
            System.out.println( "Error[DFSClient#003]   Could not open local cache, invalid" );
        } catch ( IOException ex ) {
            System.out.println( "Error[DFSClient#004]   Could not open local cache with Emacs" );
        } catch ( InterruptedException ex ) {
            System.out.println( "Error[DFSClient#005]   Emacs session inertupted " );
        }
        // Complete Session
        if ( state == State.Release_Ownership ) {
            uploadFile();
            state = State.Invalid;
        }
    }

    /**
     * Could not properly implement due to execution issues.
     *
     * @param mode READ or WRITE
     */
    private void changePermission( String mode ) {
        try {
            String[] command;
            command = ( mode.equals( READ ) ? READ_WRITE : READ_ONLY );
            Process p = Runtime.getRuntime().exec( command );
        } catch ( IOException ex ) {
            System.out.println( "Error[DFSClient#015]   Could not set permissions" );
        }
    }

    /**
     * Downloads a file from the DFS Sever
     *
     * @param filename Name of file to be downloaded
     * @param mode File mode (Read or Write)
     * @throws IOException
     */
    private boolean downloadFile( String filename, String mode ) {
        try {
            FileContents newFile = server.download( ADDRESS, filename, mode );
            if ( newFile == null ) {
                System.out.println( "Error[DFSClient#016]   No such file exisits" );
                return false;
            }
            localCache = newFile;
            localFileName = filename;
            tempFile = new File( PATH );
            tempFile.setWritable( true );
            FileOutputStream out = new FileOutputStream( tempFile );
            out.write( localCache.get() );
            out.close();
            tempFile.setWritable( mode.equals( WRITE ) );
            return true;
        } catch ( RemoteException ex ) {
            System.out.println( "Error[DFSClient#011]   "
                    + "Error in Download() RemoteException" );
            return false;
        } catch ( FileNotFoundException ex ) {
            System.out.println( "Error[DFSClient#012]   "
                    + "Error in Download() FileNotFoundException" );
            return false;
        } catch ( IOException ex ) {
            System.out.println( "Error[DFSClient#013]   "
                    + "Error in Download() IOException" );
            return false;
        }
    }

    /**
     * Uploads a file to the DFS Sever
     *
     * @param filename Name of file to be downloaded
     * @param mode File mode (Read or Write)
     * @throws IOException
     */
    private void uploadFile() {
        try {
            if ( state == State.Write_Owned || state == State.Release_Ownership ) {
                // Read out latest version for upload
                tempFile = new File( PATH );
                byte[] data = new byte[( int ) tempFile.length()];
                FileInputStream fis = new FileInputStream( tempFile );
                fis.read( data );
                fis.close();
                localCache = new FileContents( data );

                // No upload it
                if ( !server.upload( ADDRESS, localFileName, localCache ) ) {
                    System.out.println( "Error[DFSClient#001]    Could not upload file to server!" );
                } else {
                    state = State.Write_Owned;
                }
            }
        } catch ( IOException ex ) {
            System.out.println( "Error[DFSClient#002]    Could not upload file to server!" );
        }
    }

    @Override
    public String toString() {
        String message = new String();
        message += " ========== " + ADDRESS + " ========== \n";
        message += " Writing to: " + PATH + "\n";
        message += " Current File: " + localFileName + "\n";
        message += " State:";
        switch ( state ) {
            case Invalid:
                message += " Invalid\n";
                break;
            case Read_Shared:
                message += " Read Shared\n";
                break;
            case Release_Ownership:
                message += " Released Ownership\n";
                break;
            case Write_Owned:
                message += " Write Owned\n";
                break;
        }
        message += " =================================== ";
        return message;
    }

    /**
     * Set the DFS client’s file state to “Invalid”
     */
    @Override
    public boolean invalidate() {
        if ( state == State.Read_Shared ) {
            state = State.Invalid;
            return true;
        } else
            if ( state == State.Invalid )
                return true;
        return false;
    }

    /**
     * Request the DFS client to upload its current cache.
     *
     * Optimization Notes: -- Could save time by having a "modified" state so
     * that write back is not used 100% of the time. (Only if an actual edit has
     * been preformed)
     */
    @Override
    public boolean writeback() {
        if ( state == State.Write_Owned )/*|| state == State.Release_Ownership )*/ {
            state = state.Release_Ownership;
            return true;
        }
        return false;
    }
}
