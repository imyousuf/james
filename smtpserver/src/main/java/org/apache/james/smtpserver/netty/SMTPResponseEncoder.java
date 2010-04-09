package org.apache.james.smtpserver.netty;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.protocols.smtp.SMTPResponse;
import org.apache.james.socket.netty.AbstractResponseEncoder;

public class SMTPResponseEncoder extends AbstractResponseEncoder<SMTPResponse>{

    public SMTPResponseEncoder() {
        super(SMTPResponse.class, "US-ASCII");
    }

    @Override
    protected List<String> getResponse(SMTPResponse response) {
        List<String> responseList = new ArrayList<String>();
        
        for (int k = 0; k < response.getLines().size(); k++) {
            StringBuffer respBuff = new StringBuffer(256);
            respBuff.append(response.getRetCode());
            if (k == response.getLines().size() - 1) {
                respBuff.append(" ");
                respBuff.append(response.getLines().get(k));

            } else {
                respBuff.append("-");
                respBuff.append(response.getLines().get(k));

            }
            responseList.add(respBuff.toString());
        }
        
        return responseList;
    }

}
