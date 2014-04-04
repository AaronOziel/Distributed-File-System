/*
 * ~~~ Aaron Oziel ~~~
 * Date Due: December 13, 2012
 * Class: CSS 434 (Fukuda)
 *
 *  CacheEntry.java
 *      TODO Description:
 */

import java.rmi.RemoteException;
import java.util.Vector;

public class CacheEntry {

    public enum State {

        Not_Shared, Read_Shared, Write_Shared, Ownership_Change
    };
    public final String name;
    ClientShell owner;
    State state;
    Vector<ClientShell> readers = new Vector<ClientShell>( DFSServer.USER_MINIMUM );
    FileContents data;

    public CacheEntry( String name ) {
        this.name = name;
        owner = null;
        state = State.Not_Shared;
    }

    public boolean isOwned() {
        return owner != null;
    }

    public Vector<ClientShell> getReaders() {
        return readers;
    }

    public FileContents getData() {
        return data;
    }

    public void removeOwner() {
        owner = null;
        System.out.println( "     [CacheEntry] Owner Removed" );
        synchronized ( this ) {
            System.out.println( "     [CacheEntry] Notifying All" );
            notifyAll();
        }
        updateState();
    }

    public void removeReader( ClientShell reader ) {
        readers.remove( reader );
        updateState();
    }

    private void updateState() {
        if ( state == State.Ownership_Change )
            return;
        if ( owner == null ) {
            if ( readers.isEmpty() )
                state = State.Not_Shared;
            else
                state = State.Read_Shared;
        } else {
            state = State.Write_Shared;
        }
    }

    @Override
    public String toString() {
        String message = new String();
        message += "    ~~~~~~~~~~ " + name + " ~~~~~~~~~~ ";
        message += "\n    Owned By: " + ( ( owner == null ) ? "null" : owner.getName() );
        message += "\n    Readers: ";
        for ( ClientShell c : readers ) {
            message += c.getName() + ", ";
        }
        message += "\n    State:";
        switch ( state ) {
            case Not_Shared:
                message += " Not Shared\n";
                break;
            case Read_Shared:
                message += " Read Shared\n";
                break;
            case Write_Shared:
                message += " Write Shared\n";
                break;
            case Ownership_Change:
                message += " Changing Owners\n";
                break;
        }
        message += "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ \n";
        return message;
    }

    // These should all probably have syncronization?
    public synchronized void addReader( ClientShell reader ) {
        removeReader( reader );
        readers.add( reader );
        updateState();
    }

    public void setData( FileContents data ) {
        this.data = data;
    }

    public synchronized void addOwner( ClientShell newOwner ) throws RemoteException, InterruptedException {
        removeReader( newOwner );
        if ( state == State.Ownership_Change )
            wait();
        else
            state = State.Ownership_Change;

        while ( owner != null ) {
//            System.out.println( "     [CacheEntry] Cannot add owner, " + newOwner.getName() + " is waiting for writeback" );
            owner.writeback();
//            System.out.println( "     [CacheEntry] Waiting . . . " );
            //wait();
            wait( 100 );
            //            System.out.println( "     [CacheEntry] Owner added: " + newOwner.getName() );
        }
        state = null;
        owner = newOwner;
        updateState();
    }
}
