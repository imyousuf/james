/*
 * Copyright (c) 1998 Stefano Mazzocchi, Pierpaolo Fumagalli.  All rights reserved.
 */

import javax.servlet.*;
import org.apache.servlet.mail.*;
import java.io.*;

/**
 * This mail servlet may be used to kill transparently kill unwanted messages.
 * 
 * Its behavior may be highly complex and configurable to avoid behavior changes
 * between mail clients and same filtering rules for many mailboxes at once.
 *
 * @author Stefano Mazzocchi
 * @author Pierpaolo Fumagalli
 */
public class MailKillerServlet extends MailServlet {
    
    public void service(MailServletRequest req, MailServletResponse res) 
    throws ServletException, IOException {
        // complex mail killing techniques go here.    
    }
    
    public String getServletInfo() {
        return "Mail Killer Servlet";
    }
}
