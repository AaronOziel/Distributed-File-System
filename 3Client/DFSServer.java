/*
 * ~~~ Aaron Oziel ~~~
 * Date Due: December 13, 2012
 * Class: CSS 434 (Fukuda)
 *
 *  DFSServer.java
 *  Assumptions:
 *      -- Only basic user I/O validation is in place, all users are expected
 *          to know how to use this program. (ie: not entering words for a port #)
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DFSServer extends UnicastRemoteObject implements ServerInterface {

    public static final int USER_MINIMUM = 10; // Minimum expected users
    public static final char READ = 'r',
            WRITE = 'w';
    private final String PATH;
    Vector<ClientShell> clients = new Vector<ClientShell>( USER_MINIMUM );
    Vector<CacheEntry> cache = new Vector<CacheEntry>( USER_MINIMUM );
    File tempFile;
    String port;

    /**
     * Default server constructor. Initializes default working directory (PATH)
     * and establishes a temporary file used for manipulating the cache.
     *
     * @throws IOException
     */
    public DFSServer( String port ) throws IOException {
        PATH = "";
        tempFile = new File( PATH + "default.txt" );
        this.port = port;
    }

    /**
     * Search the cache to find if it contains the given file.
     *
     * @param filename Name of file sought after
     * @return -1 = Could not be found, Otherwise returns the index of the found
     * file
     */
    private int cacheContains( String filename ) {
        int contains = -1;
        for ( int i = 0; i < cache.size(); i++ ) {
            if ( cache.get( i ).name.equals( filename ) )
                contains = i;
        }
        return contains;
    }

    /**
     * RMI Function for downloading a file for local caching by a client object.
     *
     * First checks server cache to see if the file already exists, if it does
     * not it will retrieve or create it and add it to the cache.
     *
     * @param myIpName Address of calling client (IE: UW1-320-##)
     * @param filename Name of file to be downloaded
     * @param sMode "r" = Read, "w" = Write
     * @return Contents of found file for use by client
     */
    @Override
    public FileContents download( String myIpName, String filename, String sMode ) {
        // Format file name & mode correctly
        char mode = sMode.toLowerCase().charAt( 0 );
        if ( !filename.endsWith( ".txt" ) )
            filename += ".txt";
        // Create a temporary ClientShell
        ClientShell tempClient = new ClientShell( myIpName, port );
        // Add it if it is new
        if ( !clients.contains( tempClient ) ) {
            clients.add( tempClient );
        } else {
            flushReader( tempClient );
        }
        System.out.print( "     [Download] " + myIpName + " wants to download "
                + filename + " in mode " + mode + "\n--> " );
        // File Exists?
        int index = cacheContains( filename );
        // Create New File
        if ( index == -1 ) {
            System.out.print( "     [Download] " + filename + " does not exist,"
                    + " loading into cache . . ." + "\n--> " );
            try {
                CacheEntry newEntry = new CacheEntry( filename );
                tempFile = new File( filename );
                byte[] data = new byte[( int ) tempFile.length()];
                FileInputStream fis = new FileInputStream( tempFile );
                fis.read( data );
                newEntry.setData( new FileContents( data ) );
                cache.add( newEntry );
                index = cacheContains( filename );
                fis.close();
                System.out.print( "     [Download] Successfully loaded into "
                        + "cache \n--> " );
            } catch ( FileNotFoundException ex ) {
                System.out.println( "Error[DFSServer#005]   "
                        + "Could not download new file" );
                return null;
            } catch ( IOException ex ) {
                System.out.println( "Error[DFSServer#006]   "
                        + "Could not download new file" );
                return null;
            }
        }
        try {
            switch ( mode ) {
                case READ:
                    cache.get( index ).addReader( tempClient );
                    System.out.print( "     [Download] " + myIpName
                            + " was added as a reader to " + filename + "\n--> " );
                    break;
                case WRITE:
                    cache.get( index ).addOwner( tempClient );
                    System.out.print( "     [Download] " + myIpName
                            + " was added as the owner of " + filename + "\n--> " );
                    break;
            }
        } catch ( InterruptedException ex ) {
            System.out.println( "Error[DFSServer#003]   Download Interrupted" );
            ex.printStackTrace();
        } catch ( RemoteException ex ) {
            System.out.println( "Error[DFSServer#004]   Download Remote Exception" );
            ex.printStackTrace();
        }
        return cache.get( index ).getData();
    }

    /**
     * RMI Function for uploading a file back to the server
     *
     * The file must be opened in write mode or awaiting an ownership change or
     * else it will return false.
     *
     * @param myIpName Address of calling client (IE: UW1-320-##)
     * @param filename Name of file to be downloaded
     * @param contents Contents of file being uploaded
     * @return true = Successful Upload, false = Failed to Upload
     */
    @Override
    public boolean upload( String myIpName, String filename,
            FileContents contents ) throws RemoteException {
        int index = cacheContains( filename );
        if ( index == -1 ) // Does it exist?
            return false;
        if ( !cache.get( index ).isOwned() ) // Is it owned?
            return false;

        System.out.print( "     [Upload] Reciving '" + filename + "' and "
                + "loading into cache \n--> " );
        cache.get( index ).setData( contents );
        System.out.print( "     [Upload] Invalidating readers \n--> " );
        for ( ClientShell c : cache.get( index ).getReaders() )
            c.invalidate();
        System.out.print( "     [Upload] Removing owner \n--> " );
        cache.get( index ).removeOwner();
        System.out.print( "     [Upload] Upload Complete! \n--> " );
        return true;
    }

    public static void main( String[] args ) {
        if ( args.length != 1 ) {
            System.out.println( "Usage Error: ONLY ONE ARGUMENT <port #>" );
            System.exit( -1 );
        }

        try {
            String port = args[0];
            System.out.println( "Server initiation on port " + port );
            DFSServer server = new DFSServer( port );
            Naming.rebind( "rmi://localhost:" + port + "/dfsserver", server );
            Scanner in = new Scanner( System.in );
            System.out.println( "Server running . . ." );
            String input;
            while ( true ) {
                System.out.print( "--> " );
                input = in.nextLine();
                if ( input.equals( "exit" ) )
                    System.exit( 0 );
                else
                    if ( input.equals( "more" ) )
                        System.out.println( server.toString() );
            }
        } catch ( IOException ex ) {
            System.out.println( "Error[DFSServer#002]   Could not create "
                    + "server object" );
            ex.printStackTrace();
        }

    }

    @Override
    public String toString() {
        String message = new String();
        message += " ========== " + port + " ========== \n";
        message += " Clients:";
        for ( ClientShell c : clients ) {
            message += c.getName() + ", ";
        }
        message += "\n" + " File Summary: \n";
        for ( CacheEntry c : cache )
            message += c.toString();
        message += " ============================ \n";
        return message;
    }

    private void flushReader( ClientShell tempClient ) {
        for ( CacheEntry c : cache )
            c.removeReader( tempClient );
    }
}
