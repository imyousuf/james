
package org.apache.james;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.avalon.*;
import org.apache.avalon.blocks.*;
import org.apache.avalon.configuration.*;

/**
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class MessageContainer implements Serializable {
    
    private void writeObject(java.io.ObjectOutputStream out)
    throws IOException {

        out.writeObject(sender);
        out.writeObject(recipients);
    }

    private void readObject(java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {

        sender = (String) in.readObject();
        recipients = (Vector) in.readObject();
    }

    public MessageContainer() {
    }

    public MessageContainer(String sender, Vector recipients) {
        this.sender = sender;
        this.recipients = recipients;
        this.body = null;
    }
    
    public InputStream body;
    
    public String sender;
    
    public Vector recipients;
    
    public void close() {
        
        try {
            body.close();
        } catch (IOException e) {
        }
    }
    
    //public Hashtable properties;
    
}
