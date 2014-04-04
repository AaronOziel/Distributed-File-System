/*
 * ~~~ Aaron Oziel ~~~
 * Date Due: December 13, 2012
 * Class: CSS 434 (Fukuda)
 *
 *  ClientShell.java
 */

import java.rmi.Naming;
import java.rmi.RemoteException;

public class ClientShell implements ClientInterface {

    String name;
    ClientInterface client;

    public ClientShell( String clientIPName, String port ) {
        name = clientIPName;
        try {
            String rmiClientAddress = String.format( "rmi://%s:%s/dfsclient", clientIPName, port );
            client = ( ClientInterface ) Naming.lookup( rmiClientAddress );
            if ( client == null )
                throw new NullPointerException();
        } catch ( NullPointerException ex ) {
            System.out.println( "       [ClientShell] Error in creating client object (Null)" );
        } catch ( Exception ex ) {
            System.out.println( "       [ClientShell] Error in creating client object" );
            ex.printStackTrace();
        }
    }

    @Override
    public boolean invalidate() throws RemoteException {
        return client.invalidate();
    }

    @Override
    public boolean writeback() throws RemoteException {
        return client.writeback();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals( Object o ) {
        if ( ( o == null ) || ( o.getClass() != ClientShell.class ) ) {
            return false;
        }
        ClientShell other = ( ClientShell ) o;
        return this.name.equals( other.getName() );
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return client.toString();
    }
}
