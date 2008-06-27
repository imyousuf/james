/*
 * Copyright (c) 1998 Stefano Mazzocchi, Pierpaolo Fumagalli.  All rights reserved.
 */

import javax.servlet.*;
import org.apache.servlet.mail.*;
import java.io.*;

/**
 * This servlet implements a general mail filtering framework.
 * 
 * @author Stefano Mazzocchi
 * @author Pierpaolo Fumagalli
 */
public class MailFilterServlet extends MailServlet {
    
    public void service(MailServletRequest req, MailServletResponse res) 
    throws ServletException, IOException {
        this.filter(req.getInputStream(), res.getOutputStream());
    }
    
    /**
     * This is the method that performs the mail filtering.
     * Examples of mail filtering could be:
     * <UL>
     *  <LI>HTML conversion: mail HTML to plain text conversion.
     *  <LI>Attachment stripper: removes the MIME attachment from the mail.
     *  <LI>Language translation: translates the message into another language.
     *  <LI>...
     * </UL>
     */
    private void filter(InputStream in, OutputStream out) {
        // filter methods go here.
    }
    
    public String getServletInfo() {
        return "Mail Filter Servlet";
    }
}
