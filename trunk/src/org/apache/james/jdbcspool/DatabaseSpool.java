/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.jdbcspool;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import com.workingdogs.town.*;
import org.apache.james.*;

/**
 * Class Declaration.
 * 
 * 
 * @see
 *
 * @author
 * @version   %I%, %G%
 */
public class DatabaseSpool implements MessageSpool {
    protected String jdbcdriver;
    protected String jdbcconnstring;
    protected String jdbcuser;
    protected String jdbcpassword;

    /**
     * Method Declaration.
     * 
     * 
     * @param message
     * @param addresses
     *
     * @exception MessagingException
     *
     * @see
     */
    public void addMessage(MimeMessage message, InternetAddress[] addresses) throws MessagingException {
        DeliveryState state = new DeliveryState();

        state.setRecipients(addresses);

        // We'll assume that the message-id is unique and just create two files appropriately

        saveMessage(message, state);
    }

    /**
     * Method Declaration.
     * 
     * 
     * @param message
     * @param state
     *
     * @exception MessagingException
     *
     * @see
     */
    public void addMessage(MimeMessage message, DeliveryState state) throws MessagingException {

        // We'll assume that the message-id is unique and just create the database records appropriately

        saveMessage(message, state);
    }

    /**
     * Method Declaration.
     * 
     * 
     * @param message
     *
     * @see
     */
    public void cancelChanges(MimeMessage message) {
        try {
            new ExecuteStatement(jdbcdriver, jdbcconnstring, jdbcuser, jdbcpassword, "UPDATE Message SET checked_out = 'N' WHERE message_id = '" + message.getMessageID() + "'");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method Declaration.
     * 
     * 
     * @param message
     * @param state
     *
     * @see
     */
    public void checkinMessage(MimeMessage message, DeliveryState state) {
        saveMessage(message, state);
    }

    /**
     * Method Declaration.
     * 
     * 
     * @return
     *
     * @see
     */
    public MimeMessage checkoutMessage() {
        return null;
    }

    /**
     * Method Declaration.
     * 
     * 
     * @param message
     *
     * @return
     *
     * @see
     */
    public DeliveryState getDeliveryState(MimeMessage message) {
        return null;
    }

    /**
     * Method Declaration.
     * 
     * 
     * @return
     *
     * @see
     */
    public boolean hasMessages() {
        TableDataSet tds = null;

        try {
            tds = new TableDataSet(jdbcdriver, jdbcconnstring, jdbcuser, jdbcpassword, "Message");

            tds.setWhere("checked_out = 'N'");

            return tds.size() > 0;
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        finally {
            try {
                tds.close();
            } catch (IOException ioe) {}
        }

        return false;
    }

    /**
     * Method Declaration.
     * 
     * 
     * @param server
     *
     * @see
     */
    public void init(JamesServ server) {

        // We should load any connection information from the properties file

        jdbcdriver = server.getProperty("spool.jdbcdriver");
        jdbcconnstring = server.getProperty("spool.jdbcconnstring");
        jdbcuser = server.getProperty("spool.jdbcuser");
        jdbcpassword = server.getProperty("spool.jdbcpassword");

        // We should convert all checked out messages to normal state messages

        try {
            new ExecuteStatement(jdbcdriver, jdbcconnstring, jdbcuser, jdbcpassword, "UPDATE Message SET checked_out = 'N' where checked_out <> 'N'");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Method Declaration.
     * 
     * 
     * @param message
     *
     * @see
     */
    public void removeMessage(MimeMessage message) {
        try {
            new ExecuteStatement(jdbcdriver, jdbcconnstring, jdbcuser, jdbcpassword, "sp_RemoveMessage '" + message.getMessageID() + "'");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method Declaration.
     * 
     * 
     * @param message
     * @param state
     *
     * @exception MessagingException
     *
     * @see
     */
    public void saveMessage(MimeMessage message, DeliveryState state) throws MessagingException {
        try {}
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}



/*--- formatting done in "Sun Java Convention" style on 07-11-1999 ---*/

