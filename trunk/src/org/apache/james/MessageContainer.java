/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.james.util.*;

/**
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class MessageContainer implements Serializable {

    private void writeObject(java.io.ObjectOutputStream out)
    throws IOException {

        try {
            message.writeTo(out);
        } catch (MessagingException e) {
        }

        state.writeTo(out);
    }

    private void readObject(java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException {

        state = new DeliveryState(in);
        Session session = Session.getDefaultInstance(new Properties(), null);
        session.setDebug(false);        
        try {
            message = new ServerMimeMessage(session, in);
        } catch (MessagingException e) {
        }
    }


    private MimeMessage message;

    private DeliveryState state;

    public MessageContainer() {

    }

    public MessageContainer(MimeMessage message, DeliveryState state) {

        this.message = message;
        this.state = state;
    }

    public DeliveryState getDeliveryState() {

        return state;
    }

    public void setDeliveryState(DeliveryState state) {

        this.state = state;
    }

    public MimeMessage getMimeMessage() {

        return message;
    }

    public void setMimeMessage(MimeMessage message) {

        this.message = message;
    }
}



/*--- formatting done in "Sun Java Convention" style on 07-11-1999 ---*/

