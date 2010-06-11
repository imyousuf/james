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

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.event.ConnectionListener;
import javax.mail.event.FolderListener;
import javax.mail.event.MessageChangedListener;
import javax.mail.event.MessageCountListener;
import javax.mail.search.SearchTerm;

/**
 * Simple 1:1 wrapper that holds a javax.mail.Folder internally to be accessed
 * through the FolderInterface
 */

public class FolderAdapter implements FolderInterface {
    
    protected Folder folder;

    /**
     * Constructor
     * 
     * @param folder the Folder 
     */
    public FolderAdapter(Folder folder) {
        this.folder=folder;
    }

    /**
     * @see javax.mail.Folder#addConnectionListener(ConnectionListener)
     */
    public void addConnectionListener(ConnectionListener arg0) {
        folder.addConnectionListener(arg0);
    }

    /**
     * @see javax.mail.Folder#addFolderListener(FolderListener)
     */
    public void addFolderListener(FolderListener arg0) {
        folder.addFolderListener(arg0);
    }

    /**
     * @see javax.mail.Folder#addMessageChangedListener(MessageChangedListener)
     */
    public void addMessageChangedListener(MessageChangedListener arg0) {
        folder.addMessageChangedListener(arg0);
    }

    /**
     * @see javax.mail.Folder#addMessageCountListener(MessageCountListener)
     */
    public void addMessageCountListener(MessageCountListener arg0) {
        folder.addMessageCountListener(arg0);
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#appendMessages(javax.mail.Message[])
     */
    public void appendMessages(Message[] arg0) throws MessagingException {
        folder.appendMessages(arg0);
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#close(boolean)
     */
    public void close(boolean arg0) throws MessagingException {
        folder.close(arg0);
    }

    /**
     * @see javax.mail.Folder#copyMessages(Message[], Folder)
     */
    public void copyMessages(Message[] arg0, Folder arg1) throws MessagingException {
        folder.copyMessages(arg0, arg1);
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#create(int)
     */
    public boolean create(int arg0) throws MessagingException {
        return folder.create(arg0);
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#delete(boolean)
     */
    public boolean delete(boolean arg0) throws MessagingException {
        return folder.delete(arg0);
    }

    /**
     * @see javax.mail.Folder#equals(Object)
     */
    public boolean equals(Object arg0) {
        return folder.equals(arg0);
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#exists()
     */
    public boolean exists() throws MessagingException {
        return folder.exists();
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#expunge()
     */
    public Message[] expunge() throws MessagingException {
        return folder.expunge();
    }

    /**
     * @see javax.mail.Folder#fetch(Message[], FetchProfile)
     */
    public void fetch(Message[] arg0, FetchProfile arg1) throws MessagingException {
        folder.fetch(arg0, arg1);
    }

    /**
     * @see javax.mail.Folder#getDeletedMessageCount()
     */
    public int getDeletedMessageCount() throws MessagingException {
        return folder.getDeletedMessageCount();
    }

    /**
     * @see javax.mail.Folder#getFolder(String)
     */
    public Folder getFolder(String arg0) throws MessagingException {
        return folder.getFolder(arg0);
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#getFullName()
     */
    public String getFullName() {
        // FIXME ugly workaroung for buggy getFullName in javamaildir
        String fn=folder.getFullName();
        if (fn.length()>1) {
            if (fn.charAt(0)=='.') {
                fn=fn.substring(1);
            }
        }
        return fn;
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#getMessage(int)
     */
    public Message getMessage(int arg0) throws MessagingException {
        return folder.getMessage(arg0);
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#getMessageCount()
     */
    public int getMessageCount() throws MessagingException {
        return folder.getMessageCount();
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#getMessages()
     */
    public Message[] getMessages() throws MessagingException {
        return folder.getMessages();
    }

    /**
     * @see javax.mail.Folder#getMessages(int, int)
     */
    public Message[] getMessages(int arg0, int arg1) throws MessagingException {
        return folder.getMessages(arg0, arg1);
    }

    /**
     * @see javax.mail.Folder#getMessages(int[])
     */
    public Message[] getMessages(int[] arg0) throws MessagingException {
        return folder.getMessages(arg0);
    }

    /**
     * @see javax.mail.Folder#getMode()
     */
    public int getMode() {
        return folder.getMode();
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#getName()
     */
    public String getName() {
        return folder.getName();
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#getNewMessageCount()
     */
    public int getNewMessageCount() throws MessagingException {
        return folder.getNewMessageCount();
    }

    
    /**
     * @see javax.mail.Folder#getParent()
     */
    public Folder getParent() throws MessagingException {
        return folder.getParent();
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#getPermanentFlags()
     */
    public Flags getPermanentFlags() {
        return folder.getPermanentFlags();
    }

    /**
     * @see javax.mail.Folder#getSeparator()
     */
    public char getSeparator() throws MessagingException {
        return folder.getSeparator();
    }

    /**
     * @see javax.mail.Folder#getStore()
     */
    public Store getStore() {
        return folder.getStore();
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#getType()
     */
    public int getType() throws MessagingException {
        return folder.getType();
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#getUnreadMessageCount()
     */
    public int getUnreadMessageCount() throws MessagingException {
        return folder.getUnreadMessageCount();
    }

    /**
     * @see javax.mail.Folder#getURLName()
     */
    public URLName getURLName() throws MessagingException {
        return folder.getURLName();
    }

    /**
     * @see javax.mail.Folder#hashCode()
     */
    public int hashCode() {
        return folder.hashCode();
    }

    /**
     * @see javax.mail.Folder#hasNewMessages()
     */
    public boolean hasNewMessages() throws MessagingException {
        return folder.hasNewMessages();
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#isOpen()
     */
    public boolean isOpen() {
        return folder.isOpen();
    }

    /**
     * @see javax.mail.Folder#isSubscribed()
     */
    public boolean isSubscribed() {
        return folder.isSubscribed();
    }

    /**
     * @see javax.mail.Folder#list()
     */
    public Folder[] list() throws MessagingException {
        return folder.list();
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#list(java.lang.String)
     */
    public FolderInterface[] list(String arg0) throws MessagingException {
        return wrapFolders(folder.list(arg0));
    }

    /**
     * @see javax.mail.Folder#listSubscribed()
     */
    public Folder[] listSubscribed() throws MessagingException {
        return folder.listSubscribed();
    }

    /**
     * @see javax.mail.Folder#listSubscribed(String)
     */
    public Folder[] listSubscribed(String arg0) throws MessagingException {
        return folder.listSubscribed(arg0);
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#open(int)
     */
    public void open(int arg0) throws MessagingException {
        folder.open(arg0);
    }

    /**
     * @see javax.mail.Folder#removeConnectionListener(ConnectionListener)
     */
    public void removeConnectionListener(ConnectionListener arg0) {
        folder.removeConnectionListener(arg0);
    }
    
    /**
     * @see javax.mail.Folder#removeFolderListener(FolderListener)
     */
    public void removeFolderListener(FolderListener arg0) {
        folder.removeFolderListener(arg0);
    }

    /**
     * @see javax.mail.Folder#removeMessageChangedListener(MessageChangedListener)
     */
    public void removeMessageChangedListener(MessageChangedListener arg0) {
        folder.removeMessageChangedListener(arg0);
    }

    /**
     * @see javax.mail.Folder#removeMessageCountListener(MessageCountListener)
     */
    public void removeMessageCountListener(MessageCountListener arg0) {
        folder.removeMessageCountListener(arg0);
    }

    /**
     * @see org.apache.james.mailrepository.javamail.FolderInterface#renameTo(javax.mail.Folder)
     */
    public boolean renameTo(Folder arg0) throws MessagingException {
        return folder.renameTo(arg0);
    }

    /**
     * @see javax.mail.Folder#search(SearchTerm, Message[])
     */
    public Message[] search(SearchTerm arg0, Message[] arg1) throws MessagingException {
        return folder.search(arg0, arg1);
    }

    /**
     * @see javax.mail.Folder#search(SearchTerm)
     */
    public Message[] search(SearchTerm arg0) throws MessagingException {
        return folder.search(arg0);
    }

    /**
     * @see javax.mail.Folder#setFlags(int, int, Flags, boolean)
     */
    public void setFlags(int arg0, int arg1, Flags arg2, boolean arg3) throws MessagingException {
        folder.setFlags(arg0, arg1, arg2, arg3);
    }

    /**
     * @see javax.mail.Folder#setFlags(int[], Flags, boolean)
     */
    public void setFlags(int[] arg0, Flags arg1, boolean arg2) throws MessagingException {
        folder.setFlags(arg0, arg1, arg2);
    }

    /**
     * @see javax.mail.Folder#setFlags(Message[], Flags, boolean)
     */
    public void setFlags(Message[] arg0, Flags arg1, boolean arg2) throws MessagingException {
        folder.setFlags(arg0, arg1, arg2);
    }

    /**
     * @see javax.mail.Folder#setSubscribed(boolean)
     */
    public void setSubscribed(boolean arg0) throws MessagingException {
        folder.setSubscribed(arg0);
    }

    /**
     * @see javax.mail.Folder#toString()
     */
    public String toString() {
        return folder.toString();
    }

    /**
     * Wrap Folder in this class 
     * 
     * @param folder the folder to wrap
     * @return new instance of this class wrapped 
     */
    protected FolderInterface wrapFolder(Folder folder) {
        return new FolderAdapter(folder);
    }
    
    /**
     * @see #wrapFolder(Folder)
     */
    protected FolderInterface[] wrapFolders(Folder[] folders) {
        FolderInterface[] fis=new FolderInterface[folders.length];
        for (int i = 0; i < folders.length; i++) {
            fis[i]=wrapFolder(folders[i]);
            
        }
        return fis;
    }
}
