/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.util;

/**
 * 
 * @author <a href="mailto:scoobie@pop.systemy.it">Federico Barbieri</a>
 */
public class Logger implements LoggerInterface {

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
    public void log(String msg, String channel, int level) {
        System.out.println(channel + "-> " + msg + " (" + level + ")");
    }

    /**
     * Method Declaration.
     * 
     * 
     * @param msg
     * 
     * @see
     */
    public void log(String msg) {
        this.log(msg, "Default", 0);
    }

    /**
     * Method Declaration.
     * 
     * 
     * @param msg
     * @param level
     * 
     * @see
     */
    public void log(String msg, int level) {
        this.log(msg, "Default", level);
    }

    /**
     * Method Declaration.
     * 
     * 
     * @return
     * 
     * @see
     */
    private String getTimeStamp() {
        return "";
    }

}



/*--- formatting done in "Sun Java Convention" style on 07-10-1999 ---*/

