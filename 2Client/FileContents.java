/*
 * ~~~ Aaron Oziel ~~~
 * Date Due: December 13, 2012
 * Class: CSS 434 (Fukuda)
 *
 *  FileContents.java
 *      Provided file. No adjustments or alterations made.
 */

import java.io.*;
import java.util.*;

public class FileContents implements Serializable {
    private byte[] contents;

    public FileContents( byte[] contents ) {
        this.contents = contents;
    }

    public void print() throws IOException {
        System.out.println( "FileContents = " + contents );
    }

    public byte[] get() {
        return contents;
    }
}
