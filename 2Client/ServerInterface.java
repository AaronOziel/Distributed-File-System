/*
 * ~~~ Aaron Oziel ~~~
 * Date Due: December 13, 2012
 * Class: CSS 434 (Fukuda)
 *
 *  ServerInterface.java
 *      Provided file. No adjustments or alterations made.
 */

import java.rmi.*;

public interface ServerInterface extends Remote {
    public FileContents download( String client, String filename, String mode )
            throws RemoteException;

    public boolean upload( String client, String filename,
            FileContents contents ) throws RemoteException;
}
