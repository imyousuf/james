/*--- formatted by Jindent 2.0b, (www.c-lab.de/~jindent) ---*/

package org.apache.james.smtpserver;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.avalon.blocks.*;
import org.apache.java.util.*;
import org.apache.java.lang.*;
import org.apache.java.recycle.*;
import org.apache.james.*;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */
public class JamesSpoolManager implements Stoppable, Contextualizable {

    private Configuration conf;
    private MessageSpool spool;
    private Logger logger;
    private Vector dnsServers;
    private org.apache.avalon.blocks.Store.ObjectRepository repository;
    private org.apache.avalon.blocks.Store store;

    private MailServletContext servletcontext;
    private Vector preprocessors, processors, postprocessors, failureprocessors;

    /**
     * SpoolManager constructor comment.
     */
    public JamesSpoolManager() {
    }

    public void init(Context context)
    throws Exception {

        this.conf = context.getConfiguration();
        this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);

        logger.log("JamesSpoolManager init...", "SMTPServer", logger.INFO);
  
        this.store = (org.apache.avalon.blocks.Store) context.getImplementation(Interfaces.STORE);
        this.spool = (MessageSpool) context.getImplementation("spool");
        this.repository = (org.apache.avalon.blocks.Store.ObjectRepository) store.getPublicRepository(org.apache.avalon.blocks.Store.OBJECT, org.apache.avalon.blocks.Store.SYNCHRONOUS, "LocalInbox");
        this.repository.setDestination(conf.getChild("repository").getValueAsString());
        logger.log("LocalInbox opened in " + conf.getChild("repository").getValueAsString(), "SMTPServer", logger.INFO);
        this.servletcontext = new JamesMailContext(context);

        this.dnsServers = null;

        try {
            Enumeration dnsE;
            for (dnsE = conf.getChildren("dns-server"); dnsE.hasMoreElements();) {
                if (this.dnsServers == null) {
                    this.dnsServers = new Vector();
                }
                Configuration node = (Configuration) dnsE.nextElement();
                this.dnsServers.addElement(node.getValueAsString());
            }
        } catch (ConfigurationException ce) {
            throw new Exception("No DNS servers specified");
        }
    }


    /**
     * This routinely checks the message spool for messages, and processes them as necessary
     */
    public void run() {

        logger.log("run JamesSpoolManager", "SMTPServer", logger.INFO);

        String key;
        while(true) {

            try {
                logger.log("running JamesSpoolManager", "SMTPServer", logger.INFO);

                // Get the next message to process
                key = spool.accept();

                // Get MessageContainer for this message
                MessageContainer mc = spool.retrive(key);

                // Get MimeMessage for this message
                javax.mail.Session session = javax.mail.Session.getDefaultInstance(new Properties(), null);
                session.setDebug(false);
                MimeMessage message = new ServerMimeMessage(session, mc.getBodyInputStream());

                // Get State for this message
                DeliveryState state = mc.getState();
    
            switch (state.getState()) {
    
                case DeliveryState.NOT_PROCESSED: 
                    logger.log("preprocessors " + state.getStateText(), "SMTPServer", logger.INFO);
                    process(preprocessors, message, state);
                    if (state.getState() == DeliveryState.NOT_PROCESSED) {
                        state.setState(DeliveryState.PRE_PROCESSED);
                    } 
                    break;
    
                case DeliveryState.PRE_PROCESSED: 
                    System.out.println("processors " + state.getStateText());
                    process(processors, message, state);
                    if (state.getState() == DeliveryState.PRE_PROCESSED) {
                        state.setState(DeliveryState.PROCESSED);
                    } 
                    break;
    
                case DeliveryState.PROCESSED: 
                    System.out.println("postprocessors " + state.getStateText());
                    process(postprocessors, message, state);
                    if (state.getState() == DeliveryState.PROCESSED) {
                        state.setState(DeliveryState.POST_PROCESSED);
                    } 
                    break;
    
                case DeliveryState.POST_PROCESSED: 
    
                    System.out.println("deliver " + state.getStateText());
                    // attempt to send the message
                    deliverMessage(message, state);
                    break;
    
                case DeliveryState.FAILED_DELIVERY: 
                    System.out.println("failureprocessors " + state.getStateText());
                    process(failureprocessors, message, state);
                    if (state.getState() == DeliveryState.FAILED_DELIVERY) {
                        state.setState(DeliveryState.FAILURE_PROCESSED);
                    } 
                    break;
            }
            logger.log("done w processing", "SMTPServer", logger.INFO);

            // if the message has completed it's duties
            if (state.getState() == DeliveryState.ABORT || state.getState() == DeliveryState.FAILURE_PROCESSED || state.getState() == DeliveryState.DELIVERED) {
                spool.remove(key);
            } else {

                mc.setState(state);

                OutputStream stream = spool.store(key, mc);

                
                // Write our modified mail message to the output stream
                try {
                    message.writeTo(stream);
                } catch (IOException ioe) {
                    logger.log("Error writing MimeMessage to OutputStream", "DATA-COMMAND", Logger.ERROR);
                }

                spool.free(key);
            }

            } catch (Exception e) {
                logger.log("exception in run JamesSpoolManager " + e.getMessage(), "SMTPServer", logger.ERROR);
            }
        }
        //logger.log("done run JamesSpoolManager");
    }
    
    public void stop() {
    }
    
    public void destroy()
    throws Exception {
    }

    /**
     * This method was created in VisualAge.
     * @param message javax.mail.internet.MimeMessage
     * @param state org.apache.james.DeliveryState
     */
    private void deliverMessage(MimeMessage message, DeliveryState state) {

        logger.log("Sending message", "SMTPServer", logger.INFO);

        InetAddress localAddr = null;

        // In case we need to record a failure for this message...

        Date msgTime = new Date();
        InternetAddress[] rcpt = state.getRecipients();
        Hashtable hosts = new Hashtable();

        // Categorize all the recipients into various mail servers that need to be delivered to and how

        for (int i = 0; i < rcpt.length; i++) {
            String address = rcpt[i].getAddress();
            logger.log("Sending message to " + address, "SMTPServer", logger.INFO);
            int index = address.indexOf('@');

            if (index == -1) {
                continue;
            } 

            String host = address.substring(index + 1);

            if (hosts.get(host) == null) {
                Vector addresses = new Vector();

                addresses.addElement(rcpt[i]);
                hosts.put(host, addresses);
            } else {
                Vector addresses = (Vector) hosts.get(host);

                addresses.addElement(rcpt[i]);
            }
        }

        // We've now split up the recipients into all the servers that need to be targeted.
        // Go through each one by one, resolve the IP address using the MX record, and send the message

        Session session = Session.getDefaultInstance(System.getProperties(), null);

        session.setDebug(false);

        // We need to put the message's sender here

        String sender = null;

        try {
            if (message.getHeader("Return-Path", ",") != null) {
                sender = message.getHeader("Return-Path", ",");
            } else {
                sender = message.getHeader("From", ",");
            }
            if (sender == null) {
                throw new MessagingException("Could not find sender in bad message.");
            } 

            InternetAddress addr[] = InternetAddress.parse(sender);

            sender = addr[0].getAddress();

            if (sender.indexOf('@') > -1) {
                sender = sender.substring(0, sender.indexOf("@"));
            } 
        } catch (Exception e) {
            System.out.println("Exception @ " + new Date());
            e.printStackTrace(System.out);
            state.put("failure.count", "1");
            state.put("failure.1.reason", "Could not determine sender.");

            String msg = e.getMessage().replace('\r', ' ').replace('\n', ' ').trim();

            state.put("failure.1.desc", msg);
            state.setState(DeliveryState.FAILED_DELIVERY);

            return;
        }

        Vector targetServers = null;
        int count = 0;

        for (Enumeration h = hosts.keys(); h.hasMoreElements(); ) {
            targetServers = new Vector();

            String host = (String) h.nextElement();

            // We want to track if we were successful sending messages to this server
            boolean success = false;
            boolean foundHost = false;

            // We actually can have a series of mail servers support receiving this message.... let's go
            // through them and sort them by priority

            try {

                // Look up the MX servers specified for this given host

                Enumeration s = lookupMXServers(host);

                if (s != null) {
                    for (; s.hasMoreElements(); ) {
                        foundHost = true;

                        Object obj = s.nextElement();

                        if (!(obj instanceof DNS.MXRecord)) {
                            continue;
                        } 

                        DNS.MXRecord rec = (DNS.MXRecord) obj;
                        int i = 0;

                        // Sort it

                        for (; i < targetServers.size(); i++) {
                            if (rec.getPriority() < ((DNS.MXRecord) targetServers.elementAt(i)).getPriority()) {
                                targetServers.insertElementAt(rec, i);
                                break;
                            }
                        }

                        if (i == targetServers.size()) {
                            targetServers.addElement(rec);
                        } 
                    }
                }
            } catch (IOException ioe) {
                System.out.println("Exception @ " + new Date());
                ioe.printStackTrace(System.out);
            }


            // We need to convert the vector addresses to an array
            Vector addresses = (Vector) hosts.get(host);
            InternetAddress addr[] = new InternetAddress[addresses.size()];

            for (int j = 0; j < addresses.size(); j++) {
                addr[j] = (InternetAddress) addresses.elementAt(j);
            }

            Exception me = null;

            // We should now have a list of target servers, sorted by priority... let's try them

            for (int i = 0; i < targetServers.size(); i++) {
                DNS.MXRecord rec = (DNS.MXRecord) targetServers.elementAt(i);

                // **************
                // Check whether rec.getTarget () resolves to localhost
                // **************

                URLName urlname = new URLName("smtp://" + sender + "@" + rec.getTarget());

                try {
                    InetAddress targetAddr = InetAddress.getByName(rec.getTarget().toString());

                    if (localAddr == null) {
                        localAddr = InetAddress.getLocalHost();
                    } 

                    if (targetAddr.equals(localAddr)) {
                        throw new LocalLoopbackException(rec.getTarget().toString());
                    } 

                    logger.log("Ready to connect for " + targetAddr.toString(), "SMTPServer", logger.INFO);

                    Transport t = session.getTransport(urlname);

                    t.connect();
                    t.sendMessage(message, addr);
                    t.close();

                    success = true;

                    break;
                } catch (SendFailedException sfe) {
                    me = sfe;
                } catch (Exception e) {
                    System.out.println("Exception @ " + new Date());
                    e.printStackTrace(System.out);

                    me = e;

                    System.out.println("continuing after exception");
                }
            }       // End of looping through the multiple mail servers that are possible for the given host name

            logger.log("Success:" + success, "SMTPServer", logger.INFO);

            if (!success) {
                state.setState(DeliveryState.FAILED_DELIVERY);

                // Let's now add reasons why it failed

                String msg = null;

                if (me != null) {
                    msg = me.getMessage().replace('\r', ' ').replace('\n', ' ').trim();
                } 
                if (!foundHost) {
                    state.put("failure." + count + ".reason", DeliveryState.HOST_NOT_FOUND + "");
                    state.put("failure." + count + ".desc", "Host not found");
                } else if (me != null && me instanceof javax.mail.SendFailedException && msg.indexOf("Invalid recipient") > -1) {
                    state.put("failure." + count + ".reason", DeliveryState.INVALID_RECIPIENT + "");
                    state.put("failure." + count + ".desc", msg);
                } else if (me != null && me instanceof java.net.UnknownHostException) {
                    state.put("failure." + count + ".reason", DeliveryState.UNKNOWN_HOST + "");
                    state.put("failure." + count + ".desc", msg);
                } else if (me != null && me instanceof LocalLoopbackException) {
                    state.put("failure." + count + ".reason", DeliveryState.ACCOUNT_NOT_FOUND + "");
                    state.put("failure." + count + ".desc", msg);
                } else if (me != null) {
                    state.put("failure." + count + ".reason", DeliveryState.UNKNOWN_FAILURE + "");
                    state.put("failure." + count + ".desc", me.getMessage());
                }

                // Add the addresses to this failure comment

                String addrString = "";

                for (int j = 0; j < addresses.size(); j++) {
                    addrString += addresses.elementAt(j).toString();

                    if (j < addresses.size() - 1) {
                        addrString += ',';
                    } 
                }

                state.put("failure." + count + ".addr", addrString);
                state.put("failure." + count + ".host", host);

                count++;
            }       // End of if not successful delivering to this server
        }       // Enumeration of the various hosts we're sending the message to

        // We've delivered this message, if we haven't received a failure

        if (state.getState() != DeliveryState.FAILED_DELIVERY) {
            state.setState(DeliveryState.DELIVERED);
        } else {
            state.put("failure.count", count + "");
        }
        try {
        state.writeTo(System.out);
        message.writeTo(System.out);
        } catch (Exception e) {}
    }

    /**
     * This method was created in VisualAge.
     * @return org.apache.james.MailServletContext
     */
    private MailServletContext getContext() {
        return servletcontext;
    }

    /**
     * This method was created in VisualAge.
     */
    protected void loadServlets() {

        // it should be dynamically loading...
        // this'll need lots of work
        // We preload all servlets since there is no optional servlets to get called (at least)
        // as currently envisioned... this will like change is we implement an invoke/forward
        // feature

        preprocessors = loadServlets("preprocessor");
        processors = loadServlets("processor");
        postprocessors = loadServlets("postprocessor");
        failureprocessors = loadServlets("failureprocessor");
    }

    /**
     * This method was created in VisualAge.
     */
    protected Vector loadServlets(String type) {

        // it should be dynamically loading...
        // this'll need lots of work

        Vector servlets = new Vector();
        Enumeration servletsE;

        try {

            for (servletsE = conf.getChildren("class"); servletsE.hasMoreElements();) {

                Configuration node = (Configuration) servletsE.nextElement();
                String servletName = node.getAttribute("name");

                if (servletName == null || servletName.trim().length() == 0 ) {
                    continue;
                } 

                try { // Check for enabled
                    if ( ! node.getChild("enabled").getValueAsBoolean() ) {
                        continue;
                    }
                } catch (ConfigurationException ce) {}

                System.out.println("Initializing command processor for " + servletName);

                try {
                    MailServlet servlet = (MailServlet) Class.forName(servletName).newInstance();
                
                    String servletArgs = null;
                    String servletConfFile = null;
                
                    try {
                        servletArgs = node.getChild("initargs").getValueAsString();
                    } catch (ConfigurationException ce) {}

                    try {
                        servletConfFile = node.getChild("conffile").getValueAsString();
                    } catch (ConfigurationException ce) {}

                    Properties props = parseArgs(servletArgs);
                    MailServletConfig config = new JamesServletConfig(props, servletConfFile, getContext());
                    servlet.init(config);
                    servlets.addElement(servlet);

                } catch (ClassCastException cce) {
                    logger.log("Mail Servlet ClassCastException : " + servletName, "SMTPServer", logger.ERROR);
                } catch (ClassNotFoundException cnfe) {
                    logger.log("Mail Servlet ClassNotFoundException : " + servletName, "SMTPServer", logger.ERROR);
                } catch (InstantiationException iie) {
                    logger.log("Mail Servlet InstantiationException : " + servletName, "SMTPServer", logger.ERROR);
                } catch (IllegalAccessException iie) {
                    logger.log("Mail Servlet IllegalAccessException : " + servletName, "SMTPServer", logger.ERROR);
                } catch (MessagingException me) {
                    logger.log("Mail Servlet MessagingException : " + servletName, "SMTPServer", logger.ERROR);
                }
            }

        } catch (ConfigurationException ce) {
            logger.log("Error initializing mail servlets : " + ce.getMessage(), "SMTPServer", logger.ERROR);
        }


        return servlets;
    }

    /**
     * This method was created in VisualAge.
     * @return java.util.Enumeration
     * @param server java.lang.String
     */
    private Enumeration lookupMXServers(String host) throws IOException {

        // This is the query going to the server

        DNS.Message query = new DNS.Message();

        // We need to build a round-robin style DNS resolver

        Enumeration dnsE = dnsServers.elements();
        while (dnsE.hasMoreElements()) {
            try {

                String thisServer = (String)dnsE.nextElement();

                logger.log("quering dns server " + thisServer, "SMTPServer", logger.INFO);

                // This is the DNS server we resolve with

                DNS.Resolver res = new DNS.Resolver(thisServer);

                // Not quite sure....

                query.getHeader().setFlag(DNS.dns.RD);
                query.getHeader().setOpcode(DNS.dns.QUERY);

                // This is the record that defines the query

                DNS.Record rec = DNS.Record.newRecord(new DNS.Name(host), DNS.dns.MX, DNS.dns.ANY);

                query.addRecord(DNS.dns.QUESTION, rec);

                // Do the query!

                DNS.Message response = res.send(query);

                /*
                 * //Section 0 is the query, so section 1 should be the responses, section 2 is the ANY portion
                 * java.util.Enumeration e = response.getSection (1);
                 * for (;e.hasMoreElements ();)
                 * {
                 * DNS.MXRecord r = (DNS.MXRecord)e.nextElement ();
                 * System.out.println (r.getName ());
                 * System.out.println (r.getTarget ());
                 * System.out.println (r.getPriority ());
                 * //System.out.println (e.nextElement ());
                 * }
                 */
                if (response.getSection(1).hasMoreElements()) {
                    return response.getSection(1);
                } else if (response.getSection(2) != null && response.getSection(2).hasMoreElements()) {
                    return response.getSection(2);
                } else if (response.getSection(3) != null && response.getSection(3).hasMoreElements()) {
                    return response.getSection(3);
                } 

                Vector orig = new Vector();

                rec = new DNS.MXRecord(new DNS.Name(host), DNS.dns.ANY, 0, 0, new DNS.Name(host));

                orig.addElement(rec);

                return orig.elements();
            } catch (Exception e) {

                // We failed to resolve the DNS entry (null pointer exception most likely)
                // We'll fail through, try a different DNS server...
                logger.log("Caught exception looking up DNS : " + e.getMessage(), "SMTPServer", logger.ERROR);

            }
        }

        return null;
    }

    /**
     * This converts a comma-delimited set for name=value pairs into a Properties object
     * @return java.util.Properties
     * @param config java.lang.String
     */
    private static Properties parseArgs(String config) {
        if (config == null) {
            return new Properties();
        } 

        StringTokenizer st = new StringTokenizer(config, ",", false);
        Properties props = new Properties();

        while (st.hasMoreTokens()) {
            String arg = st.nextToken().trim();
            int index = arg.indexOf('=');

            if (index == -1) {
                continue;
            } 

            String name = arg.substring(0, index).trim();
            String value = arg.substring(index + 1).trim();

            props.put(name, value);
        }

        return props;
    }

    /**
     * Process a message against the following Vector of servlets
     * @param servlets java.util.Vector
     * @param message javax.mail.internet.MimeMessage
     * @param state org.apache.james.DeliveryState
     */
    private void process(Vector servlets, MimeMessage message, DeliveryState state) {
        logger.log("Enter process servelts..", "SMTPServer", logger.INFO);

        if ( servlets == null ) {
            logger.log("No servlets for this processor", "SMTPServer", logger.INFO);
            return;
        }

        for (int i = 0; i < servlets.size(); i++) {
            try {
                logger.log("running servlet ", "SMTPServer", logger.INFO);
                MailServlet servlet = (MailServlet) servlets.elementAt(i);

                servlet.service(message, state);

                if (state.getState() == DeliveryState.ABORT) {
                    return;
                } 
            } catch (MessagingException me) {
                System.out.println("Exception @ " + new Date());
                me.printStackTrace(System.out);
            } catch (IOException ioe) {
                System.out.println("Exception @ " + new Date());
                ioe.printStackTrace(System.out);
            }
        }
    }


}



/*--- formatting done in "Sun Java Convention" style on 07-11-1999 ---*/

