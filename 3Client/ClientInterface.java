/*
 * ~~~ Aaron Oziel ~~~
 * Date Due: December 13, 2012
 * Class: CSS 434 (Fukuda)
 *
 *  ClientInterface.java
 *      Provided file. No adjustments or alterations made.
 */

import java.rmi.*;

public interface ClientInterface extends Remote {
    public boolean invalidate() throws RemoteException;

    public boolean writeback() throws RemoteException;
}
