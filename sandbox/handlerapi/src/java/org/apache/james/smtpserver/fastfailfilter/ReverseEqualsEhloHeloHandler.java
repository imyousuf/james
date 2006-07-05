/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.smtpserver.fastfailfilter;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.smtpserver.AbstractCommandHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.dsn.DSNStatus;

public class ReverseEqualsEhloHeloHandler extends AbstractCommandHandler {

    private boolean checkAuthNetworks = false;


    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration handlerConfiguration)
            throws ConfigurationException {
        Configuration configRelay = handlerConfiguration.getChild(
                "checkAuthNetworks", false);
        if (configRelay != null) {
            setCheckAuthNetworks(configRelay.getValueAsBoolean(false));
        }
    }

    /**
     * Set to true if AuthNetworks should be included in the EHLO check
     * 
     * @param checkAuthNetworks
     *            Set to true to enable
     */
    public void setCheckAuthNetworks(boolean checkAuthNetworks) {
        this.checkAuthNetworks = checkAuthNetworks;
    }


    /**
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public void onCommand(SMTPSession session) {
        String argument = session.getCommandArgument();
        String responseString = null;

        /**
         * don't check if the ip address is allowed to relay. Only check if it
         * is set in the config. ed.
         */
        if (!session.isRelayingAllowed() || checkAuthNetworks) {
            try {
                // get reverse entry
                String reverse = getDnsServer().getByName(
                        session.getRemoteIPAddress()).getHostName();

                if (!argument.equals(reverse)) {
                    responseString = "501 "
                            + DSNStatus.getStatus(DSNStatus.PERMANENT,
                                    DSNStatus.DELIVERY_INVALID_ARG)
                            + " Provided EHLO " + argument
                            + " not equal reverse of "
                            + session.getRemoteIPAddress();

                    session.writeResponse(responseString);
                    getLogger().info(responseString);

                    // After this filter match we should not call any other handler!
                    setStopHandlerProcessing(true);
                }
            } catch (UnknownHostException e) {
                responseString = "501 "
                        + DSNStatus.getStatus(DSNStatus.PERMANENT,
                                DSNStatus.DELIVERY_INVALID_ARG) + " Ipaddress "
                        + session.getRemoteIPAddress() + " can not resolved";

                session.writeResponse(responseString);
                getLogger().info(responseString);

                // After this filter match we should not call any other handler!
                setStopHandlerProcessing(true);
            }
        }
    }
    
    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public List getImplCommands() {
        ArrayList implCommands = new ArrayList();
        implCommands.add("EHLO");
        implCommands.add("HELO");
        
        return implCommands;
    }

}

