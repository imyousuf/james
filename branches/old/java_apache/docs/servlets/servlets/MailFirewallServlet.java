/*
 * Copyright (c) 1998 Stefano Mazzocchi, Pierpaolo Fumagalli.  All rights reserved.
 */

import javax.servlet.*;
import org.apache.servlet.mail.*;
import java.io.*;

/**
 * This is an example class that implements a cypto envelope for 
 * mail messages. The mail is sent to a firewall mailbox that is aliased
 * to this servlet. If the mail arrives without the "Crypto" header
 * set, it is encrypted and sent. If the header is set, the mail is decripted
 * and sent.
 *
 * This could be used to implement a transparent crypto transport to avoid
 * the use of encryption on mail clients.
 *
 * @author Stefano Mazzocchi
 * @author Pierpaolo Fumagalli
 */
public class MailFirewallServlet extends MailServlet {
    
    public void service(MailServletRequest req, MailServletResponse res) 
    throws ServletException, IOException {
        MailHeaders headers = req.getHeaders();
        
        if (headers.getHeader("Crypted") == null) {
            headers.setHeader("Crypted", this.getServletInfo());
            res.setHeaders(headers);
            this.crypt(req.getInputStream(), res.getOutputStream());
        } else { 
            headers.removeHeader("Crypted");
            res.setHeaders(headers);
            this.decrypt(req.getInputStream(), res.getOutputStream());
        }
        
    }
    
    private void crypt(InputStream in, OutputStream out) {
        // crypt routine should go here
    }
    
    private void decrypt(InputStream in, OutputStream out) {
        // decrytp routine should go here
    }

    public String getServletInfo() {
        return "Mail Firewall Servlet";
    }
}

