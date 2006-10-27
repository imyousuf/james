/*
 * Created on Oct 27, 2006
 *
 * PVCS Workfile Details:
 * $Workfile$
 * $Revision$
 * $Author$
 * $Date$
 * $Modtime$
 */

package org.apache.mailet;


/**
 * @author angusd 
 * @author $Author$ 
 * @version $Revision$
 */
public interface ForwardingUser {

    /**
     * Indicate if mail for this user should be forwarded to some other mail
     * server.
     *
     * @param forward whether email for this user should be forwarded
     */
    public abstract void setForwarding(boolean forward);

    /** 
     * Return true if mail for this user should be forwarded
     */
    public abstract boolean getForwarding();

    /**
     * <p>Set destination for forwading mail</p>
     * <p>TODO: Should we use a MailAddress?</p>
     *
     * @param address the forwarding address for this user
     */
    public abstract boolean setForwardingDestination(MailAddress address);

    /**
     * Return the destination to which email should be forwarded
     */
    public abstract MailAddress getForwardingDestination();
}
/* 
 *
 * PVCS Log History:
 * $Log$
 *
 */
