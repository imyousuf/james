/*
 * Copyright (c) 1999 The Java Apache Project.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software must display the following acknowledgment:
 *    "This product includes software and design ideas developed by the Java
 *    Apache Project (http://java.apache.org/)."
 *
 * 4. Redistributions of any form whatsoever must retain the following
 *    acknowledgment:
 *    "This product includes software and design ideas developed by the Java
 *    Apache Project (http://java.apache.org/)."
 *
 * THIS SOFTWARE IS PROVIDED BY THE JAVA APACHE PROJECT "AS IS" AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE JAVA APACHE PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Java Apache Project. For more information
 * on the Java Apache Project please see <http://java.apache.org/>.
 */

package org.apache.avalon.blocks;

import org.apache.avalon.*;
/**
 * The <code>Logger</code> block interface.
 * <br>
 * This interface must be implemented by those blocks willing to log data
 * coming from other blocks. 
 * <br>
 * The six levels used for logging are (in order of gravity): EMERGENCY,
 * CRITICAL, ERROR, WARNING, INFO and DEBUG.
 *
 * @version 1.0 (CVS $Revision: 1.1 $ $Date: 1999/09/03 00:29:28 $)
 * @author <a href="mailto:scoobie@pop.systemy.it">Federico Barbieri</a>
 * @author <a href="mailto:pier@apache.org">Pierpaolo Fumagalli</a>
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @author <a href="http://java.apache.org/">Java Apache Project</a>
 */
public interface Logger extends Block {
    /**
     * The version of this interface (&quot;<b>1.0</b>&quot;).
     */
    public static final Version INTERFACE_VERSION = new Version(1, 0, 0, true);

    /**
     * The EMERGENCY logging level (anything logged under this level means
     * that a <b>critical permanent error</b> was detected).
     * <br>
     * When a <code>Block</code> logs something with this level means that
     * any other task will be impossible without administrator's intervention.
     * <br>
     * Example:
     * <ul>
     * <li><i>
     * AJP handler cannot find the secret key file. Cannot process servlet
     * requests.
     * </i></li>
     * <li><i>
     * Cannot access to mail server database URL jdbc:mysql://localhost/users
     * (SQLException caught in driver initialization)
     * </i></li>
     * </ul>
     */
    public static final int EMERGENCY=0;

    /**
     * The CRITICAL logging level (anything logged under this level means that
     * a <b>critical temporary error</b> was detected).
     * <br>
     * When a <code>Block</code> logs something with this level means that
     * tasks similar to the one generating the error will be impossible
     * without administrator's intervention, but other tasks (maybe using
     * different resources) will be possible.
     * <br>
     * Example:
     * <ul>
     * <li><i>
     * Cannot serve servlet in zone &quot;myServletZone&quot; (repository not
     * found)
     * </i></li>
     * <li><i>
     * Cannot alias address &quot;listserv@apache.org&quot; to ListServlet
     * (mail servlet not found)
     * </i></li>
     * </ul>
     */
    public static final int CRITICAL=1;
    
    /**
     * The ERROR logging level (anything logged under this level means that a
     * <b>non-critical error</b> was detected).
     * <br>
     * When a <code>Block</code> logs something with this level means that
     * the current tasks failed to execute, but the stability of the block is
     * not compromised.
     * <br>
     * Example:
     * <ul>
     * <li><i>
     * Cannot serve servlet &quot;SnoopServlet&quot; to client 192.168.1.1 due
     * to security restriction
     * </i></li>
     * <li><i>
     * User &quot;ianosh&quot; denied access via POP3 protocol due to invalid
     * password
     * </i></li>
     * </ul>
     */
    public static final int ERROR=2;

    /**
     * The WARNING logging level (anything logged under this level means that
     * a <b>generic error</b> was detected).
     * <br>
     * When a <code>Block</code> logs something with this level means that
     * the current tasks failed to execute due to inconsistencies in the
     * request handled by the block.
     * <br>
     * Example:
     * <ul>
     * <li><i>
     * Cannot find servlet &quot;SnoopServlet&quot; requested by 192.168.1.1
     * via AJP protocol
     * </i></li>
     * <li><i>
     * Cannot deliver mail from &quot;root@localhost&quot; to 
     * &quot;ianosh@iname.com&quot; via protocol SMTP (user not found)
     * </i></li>
     * </ul>
     */
    public static final int WARNING=4;

    /**
     * The INFO logging level (anything logged under this level means that
     * an <b>informative message</b> must be logged).
     * <br>
     * When a <code>Block</code> logs something with this level means that
     * something that may be interesting happened.
     * <br>
     * Example:
     * <ul>
     * <li><i>
     * Successfully handled request from 192.168.1.1 via AJP protocol for
     * &quot;SnoopServlet&quot; servlet
     * </i></li>
     * <li><i>
     * Mail from &quot;root@localhost&quot; delivered successfully to 
     * &quot;ianosh@iname.com&quot; via protocol SMTP
     * </i></li>
     * </ul>
     */
    public static final int INFO=6;

    /**
     * The DEBUG logging level (anything logged under this level means that
     * an <b>debug message</b> must be logged).
     * <br>
     * When a <code>Block</code> logs something with this level means that
     * something that may be interesting while developing, tracking bugs or
     * misconfigurations.
     * <br>
     * <br>
     * Example:
     * <ul>
     * <li><i>
     * Requested servlet &quot;snoop&quot; was aliased to 
     * &quot;org.apache.servlets.SnoopServlet&quot;
     * </i></li>
     * <li><i>
     * MX record for domain &quot;dom.tld&quot; resolved to host
     * &quot;mail.dom.tld&quot; (192.168.1.1) with preference level 10
     * </i></li>
     * </ul>
     */
    public static final int DEBUG=7;
    
    /**
     * Log an object thru the specified channel and with the specified level.
     *
     * @param data The object to log.
     * @param channel The channel name used for logging.
     * @param level The level used for logging.
     */
    public void log(Object data, String channel, int level);
    
    public void log(Object data, int level);
    
    public void log(Object data);
}