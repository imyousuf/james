
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

    private InputStream in;
    private OutputStream out;
    private String sender;
    private Vector recipients;
    private DeliveryState state;

    public void setSender(String s) {
        sender = s;
    }

    public String getSender() {
        return sender;
    }

    public void setBodyInputStream(InputStream i) {
        in = i;
    }

    public InputStream getBodyInputStream() {
        if ( in == null ) { return null; }
        return in;
    }	

    public void setBodyOutputStream(OutputStream o) {
        out = o;
    }

    public OutputStream getBodyOutputStream() {
        if ( out == null ) { return null; }
        return out;
    }

    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException
    {
        out.writeObject(sender);
        out.writeObject(recipients);
        out.writeObject(state);		
    }

    private void readObject(java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        sender = (String) in.readObject();
        recipients = (Vector) in.readObject();
        state = (DeliveryState) in.readObject();
    }

    public MessageContainer() {
        state = new DeliveryState();
    }

    public MessageContainer(String sender, Vector recipients) {
        state = new DeliveryState();
        this.setRecipients(recipients);
        this.sender = sender;
        this.in = null;
        this.out = null;
    }
    
    public void close() {
        if (in != null) { 
            try { in.close(); } catch (Exception e) {}
        }
        if (out != null ) {
            try { out.close(); } catch (Exception e) {}
        }
    }

    public void setRecipients(Vector recipients) {
        InternetAddress[] addr = new InternetAddress[recipients.size()];
        for ( int i=0; i<recipients.size(); i++) {
            try {
                addr[i] = new InternetAddress((String)recipients.elementAt(i));
            } catch (AddressException ae) {}
        }
        state.setRecipients(addr);
    }

    public Vector getRecipients() {
        Vector v = new Vector();
        InternetAddress[] addr = state.getRecipients();
        for ( int i=0; i<addr.length; i++) {
            v.addElement(addr[i]);
        }
        return v;
    }

    public void setState(DeliveryState ds) {
        state = ds;
    }

    public DeliveryState getState() {
        if ( state == null ) { return new DeliveryState(); }
        return state;
    }

}
