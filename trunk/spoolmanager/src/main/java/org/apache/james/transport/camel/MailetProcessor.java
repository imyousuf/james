package org.apache.james.transport.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;

/**
 * Mailet wrapper which execute a Mailet in a Processor
 *
 */
public class MailetProcessor implements Processor{

    private Mailet mailet;
   
    public MailetProcessor(Mailet mailet) {
        this.mailet = mailet;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.camel.Processor#process(org.apache.camel.Exchange)
     */
    public void process(Exchange exchange) throws Exception {
        try {
            mailet.service((Mail)exchange.getIn().getBody());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
                
    }

}
