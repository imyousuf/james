/*
 * Copyright (c) 1998 Stefano Mazzocchi, Pierpaolo Fumagalli.  All rights reserved.
 */
 
package org.apache.servlet.mail;

import javax.servlet.*;
import java.io.*;

/**
 * This class is a generic servlet abstract extention that simplifies 
 * the process of writing a mail servlet. To implement a mail
 * servlet, a servlet writer must subclass it and override the 
 * service() method.
 *
 * <P>While an Http servlet is described as an action associated with the
 * called Http URL, a mail servlet may be considered as an active filter
 * associated with a given mail address.
 *
 * <P>The mail servlet recieves a MailServletRequest encapsulating the mail
 * message received and a MailServletResponse used by the mail servlet to
 * forward the message to the specified destinations and with the message
 * body processed by the mail servlet.
 *
 * @author Stefano Mazzocchi <stefano@apache.org>
 * @author Pierpaolo Fumagalli <p.fumagalli@fumagalli.org>
 * @version pre-draft 1.0 (submitted for review)
 */
public abstract class MailServlet extends GenericServlet implements Serializable {

    /**
     * Default constructor does nothing.
     */
    public MailServlet() {
    }

    /**
     * This method forwards the request and response objects to the other
     * service method casting them to the appropriate mail equivalents.
     */
    public void service(ServletRequest servletRequest, ServletResponse servletResponse)
        throws ServletException, IOException
    {
        MailServletRequest mailServletRequest;
        MailServletResponse mailServletResponse;
        
        try {
            mailServletRequest = (MailServletRequest) servletRequest;
            mailServletResponse = (MailServletResponse) servletResponse;
        } catch(ClassCastException ex) {
            throw new ServletException("non-Mail request or response");
        }
        
        this.service(mailServletRequest, mailServletResponse);
    }

    /**
     * This method must be implemented by the mail servlet writers.
     */
    public abstract void service(MailServletRequest mailServletRequest, 
            MailServletResponse mailServletResponse)
        throws ServletException, IOException;
}