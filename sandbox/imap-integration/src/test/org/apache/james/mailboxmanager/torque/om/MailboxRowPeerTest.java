package org.apache.james.mailboxmanager.torque.om;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.james.mailboxmanager.torque.AbstractMailboxRowTestCase;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Transaction;

public class MailboxRowPeerTest extends AbstractMailboxRowTestCase { 

    public MailboxRowPeerTest() throws TorqueException {
        super();
    }

    private Thread runThread(final long id, final int no) {
        Thread t = new Thread() {
            public void run() {
                System.out.println("Thread " + no + " started.");
                Connection c = null;
                boolean retry;
                do {
                    retry = false;
                    try {
                        c = Transaction.begin("mailboxmanager");
                        c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                        c.setAutoCommit(false);
                        MailboxRow m = MailboxRowPeer.retrieveByPK(id, c);
                        System.out.println("Thread " + no + " sleeps.");
                        long uid = m.getLastUid() + 1;
                        m.setLastUid(uid);
                        m.save(c);
                        Transaction.commit(c);
                        System.out.println("Thread " + no + " has set to "
                                + uid);
                    } catch (TorqueException e) {

                        int errorCode = -1;
                        String state ="";
                        Transaction.safeRollback(c);
                        Throwable t = e.getCause();
                        if (t instanceof SQLException) {
                            errorCode=((SQLException) t).getErrorCode();
                            if (errorCode==1213) retry=true;
                            state = ((SQLException) t).getSQLState();
                        }
                        System.out.println("Thread " + no + " State: "+state+ " SQLERROR"+errorCode + " " + e);
                    } catch (SQLException e) {
                        System.out.println("SQLException!");
                        e.printStackTrace();
                    } 
//                        catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                } while (retry);

            }

        };
        t.start();
        return t;
    }

    public void testConcurrentSerializableMailboxRowUidUpdate() throws Exception {
         MailboxRow m=new MailboxRow();
         m.setName("#users.joachim.INBOX");
         m.save();
         m=MailboxRowPeer.retrieveByName("#users.joachim.INBOX");
         long id=m.getMailboxId();

        Thread t[] = new Thread[16];
        for (int i = 0; i < t.length; i++) {
            t[i] = runThread(id, i);
        }
        for (int i = 0; i < t.length; i++) {
            System.out.println("Warte auf Thread " + i);
            t[i].join();
        }

    }

    public void runBare() throws Throwable {
        // This avoid unittest to be ran because it doesn't work with derby, don't know why.
        // TODO fix and enable (removing this empty method)
    }

}
