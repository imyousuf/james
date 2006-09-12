package org.apache.james.transport.mailets;

import org.apache.james.util.mailet.FlowedMessageUtils;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;

import javax.mail.MessagingException;

import java.io.IOException;

/**
 * Convert a message to format=flowed
 */
public class WrapText extends GenericMailet {
    private static final String PARAMETER_NAME_FLOWED_DELSP = "delsp";
    private static final String PARAMETER_NAME_WIDTH = "width";
    
    private boolean optionFlowedDelsp = false;
    private int optionWidth = FlowedMessageUtils.RFC2646_WIDTH;
    
    /**
     * returns a String describing this mailet.
     * 
     * @return A desciption of this mailet
     */
    public String getMailetInfo() {
        return "WrapText";
    }

    private static boolean getBooleanParameter(String v, boolean def) {
        return def ? 
                !(v != null && (v.equalsIgnoreCase("false") || v.equalsIgnoreCase("no"))) : 
                    v != null && (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes"))  ;
    }
    
    public void init() throws MailetException {
        optionFlowedDelsp = getBooleanParameter(getInitParameter(PARAMETER_NAME_FLOWED_DELSP), optionFlowedDelsp);
        optionWidth = Integer.parseInt(getInitParameter(PARAMETER_NAME_WIDTH, "" + optionWidth));
    }

    public void service(Mail mail) throws MailetException {
        // TODO We could even manage the flow when the message is quoted-printable
        
        try {
            FlowedMessageUtils.flowMessage(mail.getMessage(), optionFlowedDelsp, optionWidth);
            
        } catch (MessagingException e) {
            throw new MailetException("Could not wrap message", e);
            
        } catch (IOException e) {
            throw new MailetException("Could not wrap message", e);
        }
        
    }
}
