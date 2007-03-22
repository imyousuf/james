
package org.apache.james.mailboxmanager.torque.om;


import java.util.ArrayList;
import java.util.List;

import org.apache.torque.TorqueException;
import org.apache.torque.om.Persistent;

/**
 * The skeleton for this class was autogenerated by Torque on:
 *
 * [Wed Sep 06 11:15:19 CEST 2006]
 *
 * You should add additional methods to this class to meet the
 * application requirements.  This class will only be generated as
 * long as it does not already exist in the output directory.
 */
public  class MessageRow
    extends org.apache.james.mailboxmanager.torque.om.BaseMessageRow
    implements Persistent
{
    /**
     * 
     */
    private static final long serialVersionUID = -75081490028686786L;

    public MessageFlags getMessageFlags() throws TorqueException {
        MessageFlags mf =null;
        List l = getMessageFlagss();
        if (l.size()==1) {
            mf=(MessageFlags)l.get(0);
        }
        return mf;
    }

    public void setMessageFlags(MessageFlags messageFlags) {
        this.collMessageFlagss=new ArrayList(1);
        collMessageFlagss.add(messageFlags);
    }

}