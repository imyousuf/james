/*
 * Copyright (c) 1998 Stefano Mazzocchi, Pierpaolo Fumagalli.  All rights reserved.
 */
 
import javax.servlet.*;
import org.apache.servlet.mail.*;
import java.io.*;

/**
 * This servlet implements a general mailing list engine that could
 * be used to handle many simultaneous mailing list backended by
 * a database of names.
 *
 * @author Stefano Mazzocchi
 * @author Pierpaolo Fumagalli
 */
public class MailingListServlet extends MailServlet {
    
    public void service(MailServletRequest req, MailServletResponse res) 
    throws ServletException, IOException {
        // Get headers from the incoming mail
        MailHeaders headers = req.getHeaders();
        // Get the address this servlet is currently serving
        // This is used by the servlet to know which mailing list
        // have been requested.
        MailAddress list = req.getServletAddress();
        // Get the mail sender
        MailAddress sender = req.getSender();
        
        String subject = headers.getSubject().toLowerCase();
        
        // The sender of this mail is always the list.
        res.setSender(list);

        if (subject.startsWith("subscribe")) {
            res.setRecipient(sender);
            if (addSubscriber(list, sender)) {
                headers.setSubject("Welcome");
                res.getOutputStream().println("Welcome to the list!");
            } else {
                headers.setSubject("You're already listed");
                res.getOutputStream().println("You are already subscribed to this list");
            }
        } else if (subject.startsWith("unsubscribe")) {
            res.setRecipient(sender);
            if (removeSubscriber(list, sender)) {
                headers.setSubject("Good bye");
                res.getOutputStream().println("We'll miss you!");
            } else {
                headers.setSubject("You are not subscribed");
                res.getOutputStream().println("You are not subscribed to this list");
            }
        } else if (this.isPresent(sender, list)) {
            // Get the names of the subscribers
            MailAddress[] subs = this.getSubscribers(list);

            // Every part of the request that is not touched by the mail servlet
            // is copied onto the response stream. In this case, the subject.
                        
            // Set the recipients of this mail, hiding the list of the recipients
            res.setRecipients(subs, false);
            
            BufferedInputStream input = new BufferedInputStream(req.getInputStream());
            ServletOutputStream output = res.getOutputStream();

            // Copy the mail message
            int ch;
            while ((ch = input.read()) != -1) {
                output.write(ch);
            }
            
            // Append a signature to the message.
            output.println("\n--\nManaged by " + this.getServletInfo()); 
        } else {
            res.setRecipient(sender);
            headers.setSubject("Message Rejected");
            res.getOutputStream().println("Sorry, but you have to subscribe to the list " + 
                "to be allowed to send messages");
        }
        
        res.setHeaders(headers);
    }

    private MailAddress[] getSubscribers(MailAddress list) {
        // return the array of subscribers to this list
        return null; // for now
    }
        
    private boolean isPresent(MailAddress sender, MailAddress list) {
        return true; // if sender is present on the list
    }

    private boolean addSubscriber(MailAddress list, MailAddress sender) {
        // add sender from the list associated to the list mail address
        // this could use JDBC or whatever to keep track of these changes.
        
        return true; // if name wasn't present and got included
    }
    
    private boolean removeSubscriber(MailAddress list, MailAddress sender) {
        // add sender from the list associated to the list mail address
        // this could use JDBC or whatever to keep track of these changes.
        
        return true; // if name was present and got removed
    }
    
    public String getServletInfo() {
        return "Mailing List Servlet ver. 0.1";
    }
}