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

package org.apache.james.smtpserver;

import java.util.List;

import org.apache.avalon.framework.logger.AbstractLogEnabled;

/**
 * Custom CommandHandlers must extend this class.
 */
public abstract class AbstractCommandHandler extends AbstractLogEnabled {

    /**
     * If set to true all handler processing is stopped (fastfail)
     */
    private boolean stopHandlerProcessing = false;
    
    
    /**
     * Method to set if a after the handler no other command handlers should processed
     * @param stopHandlerProcessing true or false
     */
    public void setStopHandlerProcessing(boolean stopHandlerProcessing) {
        this.stopHandlerProcessing = stopHandlerProcessing;
    }
    
    /**
     * Return if the processing of other commandHandlers should be done
     * @return true or false
     */
    public boolean stopHandlerProcessing() {
        return stopHandlerProcessing;
    }
    
    /**
     * Handle the command
    **/
    public abstract void onCommand(SMTPSession session);

    /**
     * Return a List of implemented commands
     * 
     * @return List which contains implemented commands
     */
    public abstract List getImplCommands();
    
}
