/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.util;

/**
 * 
 * @author <a href="mailto:scoobie@pop.systemy.it">Federico Barbieri</a>
 */
public interface LoggerInterface {

    public static final int EMERGENCY_LEVEL = 0;
    public static final int CRITICAL_LEVEL = 1;
    public static final int ERROR_LEVEL = 2;
    public static final int WARNING_LEVEL = 4;
    public static final int INFO_LEVEL = 6;
    public static final int DEBUG_LEVEL = 7;

    /**
     * Method Declaration.
     * 
     * 
     * @param msg
     * @param channel
     * @param level
     * 
     * @see
     */
    public void log(String msg, String channel, int level);

    /**
     * Method Declaration.
     * 
     * 
     * @param msg
     * @param level
     * 
     * @see
     */
    public void log(String msg, int level);

    /**
     * Method Declaration.
     * 
     * 
     * @param msg
     * 
     * @see
     */
    public void log(String msg);
}



/*--- formatting done in "Sun Java Convention" style on 07-10-1999 ---*/

