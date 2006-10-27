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
public interface AliasedUser {

    /**
     * Indicate if mail received for this user should be delivered locally to
     * a different address.
     */
    public abstract void setAliasing(boolean alias);

    /**
     * Return true if emails should be delivered locally to an alias.
     */
    public abstract boolean getAliasing();

    /**
     * Set local address to which email should be delivered.
     *
     * @return true if successful
     */
    public abstract boolean setAlias(String address);

    /**
     * Get local address to which mail should be delivered.
     */
    public abstract String getAlias();
}
/* 
 *
 * PVCS Log History:
 * $Log$
 *
 */
