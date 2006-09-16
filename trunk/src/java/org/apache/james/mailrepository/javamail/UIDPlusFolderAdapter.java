/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/



package org.apache.james.mailrepository.javamail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;

/**
 * Simple 1:1 wrapper for original JavaMail Folder and UIDFolder. uses
 * reflection to call UIDPlusFolder methods on a JavaMail Folder.
 */
public class UIDPlusFolderAdapter extends FolderAdapter implements UIDPlusFolder {
    
    private Method addUIDMessagesMethod;
    
    public UIDPlusFolderAdapter(Folder folder) throws NoSuchMethodException {
        super(folder);
        try {
            addUIDMessagesMethod=folder.getClass().getMethod("addUIDMessages",
                    new Class[] { Message[].class });
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } 
    }

    public long[] addUIDMessages(Message[] msgs) throws MessagingException {
        try {
            return (long[]) addUIDMessagesMethod.invoke(folder, new Object[] { msgs });
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause=e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            }
            throw new RuntimeException(e);
        }
    }

    public Message[] addMessages(Message[] msgs) throws MessagingException {
       throw new RuntimeException("Method addMessages(Message[] msgs) not implemented");
    }

    public long getUIDValidity() throws MessagingException {
        return ((UIDFolder)folder).getUIDValidity();
    }

    public Message getMessageByUID(long arg0) throws MessagingException {
        return ((UIDFolder)folder).getMessageByUID(arg0);
    }

    public Message[] getMessagesByUID(long arg0, long arg1) throws MessagingException {
        return ((UIDFolder)folder).getMessagesByUID(arg0, arg1);
    }

    public Message[] getMessagesByUID(long[] arg0) throws MessagingException {
        return ((UIDFolder)folder).getMessagesByUID(arg0);
    }

    public long getUID(Message arg0) throws MessagingException {
        return ((UIDFolder)folder).getUID(arg0);
    }
    
    
    

}
