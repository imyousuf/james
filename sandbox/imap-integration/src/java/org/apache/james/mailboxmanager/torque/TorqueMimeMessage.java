package org.apache.james.mailboxmanager.torque;

import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.james.mailboxmanager.torque.om.MessageBody;
import org.apache.james.mailboxmanager.torque.om.MessageFlags;
import org.apache.james.mailboxmanager.torque.om.MessageHeader;
import org.apache.james.mailboxmanager.torque.om.MessageHeaderPeer;
import org.apache.james.mailboxmanager.torque.om.MessageRow;
import org.apache.torque.TorqueException;
import org.apache.torque.util.Criteria;

public class TorqueMimeMessage extends MimeMessage {

    private int size;

    private TorqueMimeMessage() {
        super((Session)null);

    }
    
    public int getSize() throws MessagingException {
        return size;
    }
    
    
    /**
     * will not throw an exception when the message gets deleted in the meantime so result maybe incomplete. 
     * 
     * @param messageRow
     * @param log
     * @return
     * @throws TorqueException
     * @throws MessagingException
     */
    
    public static TorqueMimeMessage createMessage(MessageRow messageRow, Log log) throws TorqueException, MessagingException {
        
        TorqueMimeMessage tmm=new TorqueMimeMessage();
        
        List headers=messageRow.getMessageHeaders(new Criteria().addAscendingOrderByColumn(MessageHeaderPeer.LINE_NUMBER));
        tmm.content=((MessageBody)messageRow.getMessageBodys().get(0)).getBody();
        tmm.size=messageRow.getSize();
        
        String messageIdName=null;
        String messageIdValue=null;
        for (Iterator iter = headers.iterator(); iter.hasNext();) {
            MessageHeader header = (MessageHeader) iter.next();
            if ("message-id".equals(header.getField().toLowerCase())) {
                messageIdName=header.getField();
                messageIdValue=header.getValue();
            } else {
                tmm.addHeader(header.getField(),header.getValue());    
            }
        }
        
        // null-safe flags setup
        
        MessageFlags messageFlags=messageRow.getMessageFlags();
        if (messageFlags != null) {
			tmm.setFlags(messageRow.getMessageFlags().getFlagsObject(), true);
		}

        
        // save and reset the message-id
        tmm.saveChanges();
        if (messageIdName!=null) {
            tmm.setHeader(messageIdName,messageIdValue);
        }

        return tmm;
    }
    
    
}
