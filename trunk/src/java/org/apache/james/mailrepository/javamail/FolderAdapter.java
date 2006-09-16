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

    public FolderAdapter(Folder folder) {
        this.folder=folder;
    }

    public void addConnectionListener(ConnectionListener arg0) {
        folder.addConnectionListener(arg0);
    }

    public void addFolderListener(FolderListener arg0) {
        folder.addFolderListener(arg0);
    }

    public void addMessageChangedListener(MessageChangedListener arg0) {
        folder.addMessageChangedListener(arg0);
    }

    public void addMessageCountListener(MessageCountListener arg0) {
        folder.addMessageCountListener(arg0);
    }

    public void appendMessages(Message[] arg0) throws MessagingException {
        folder.appendMessages(arg0);
    }

    public void close(boolean arg0) throws MessagingException {
        folder.close(arg0);
    }

    public void copyMessages(Message[] arg0, Folder arg1) throws MessagingException {
        folder.copyMessages(arg0, arg1);
    }

    public boolean create(int arg0) throws MessagingException {
        return folder.create(arg0);
    }

    public boolean delete(boolean arg0) throws MessagingException {
        return folder.delete(arg0);
    }

    public boolean equals(Object arg0) {
        return folder.equals(arg0);
    }

    public boolean exists() throws MessagingException {
        return folder.exists();
    }

    public Message[] expunge() throws MessagingException {
        return folder.expunge();
    }

    public void fetch(Message[] arg0, FetchProfile arg1) throws MessagingException {
        folder.fetch(arg0, arg1);
    }

    public int getDeletedMessageCount() throws MessagingException {
        return folder.getDeletedMessageCount();
    }

    public Folder getFolder(String arg0) throws MessagingException {
        return folder.getFolder(arg0);
    }

    public String getFullName() {
        return folder.getFullName();
    }

    public Message getMessage(int arg0) throws MessagingException {
        return folder.getMessage(arg0);
    }

    public int getMessageCount() throws MessagingException {
        return folder.getMessageCount();
    }

    public Message[] getMessages() throws MessagingException {
        return folder.getMessages();
    }

    public Message[] getMessages(int arg0, int arg1) throws MessagingException {
        return folder.getMessages(arg0, arg1);
    }

    public Message[] getMessages(int[] arg0) throws MessagingException {
        return folder.getMessages(arg0);
    }

    public int getMode() {
        return folder.getMode();
    }

    public String getName() {
        return folder.getName();
    }

    public int getNewMessageCount() throws MessagingException {
        return folder.getNewMessageCount();
    }

    public Folder getParent() throws MessagingException {
        return folder.getParent();
    }

    public Flags getPermanentFlags() {
        return folder.getPermanentFlags();
    }

    public char getSeparator() throws MessagingException {
        return folder.getSeparator();
    }

    public Store getStore() {
        return folder.getStore();
    }

    public int getType() throws MessagingException {
        return folder.getType();
    }

    public int getUnreadMessageCount() throws MessagingException {
        return folder.getUnreadMessageCount();
    }

    public URLName getURLName() throws MessagingException {
        return folder.getURLName();
    }

    public int hashCode() {
        return folder.hashCode();
    }

    public boolean hasNewMessages() throws MessagingException {
        return folder.hasNewMessages();
    }

    public boolean isOpen() {
        return folder.isOpen();
    }

    public boolean isSubscribed() {
        return folder.isSubscribed();
    }

    public Folder[] list() throws MessagingException {
        return folder.list();
    }

    public Folder[] list(String arg0) throws MessagingException {
        return folder.list(arg0);
    }

    public Folder[] listSubscribed() throws MessagingException {
        return folder.listSubscribed();
    }

    public Folder[] listSubscribed(String arg0) throws MessagingException {
        return folder.listSubscribed(arg0);
    }

    public void open(int arg0) throws MessagingException {
        folder.open(arg0);
    }

    public void removeConnectionListener(ConnectionListener arg0) {
        folder.removeConnectionListener(arg0);
    }

    public void removeFolderListener(FolderListener arg0) {
        folder.removeFolderListener(arg0);
    }

    public void removeMessageChangedListener(MessageChangedListener arg0) {
        folder.removeMessageChangedListener(arg0);
    }

    public void removeMessageCountListener(MessageCountListener arg0) {
        folder.removeMessageCountListener(arg0);
    }

    public boolean renameTo(Folder arg0) throws MessagingException {
        return folder.renameTo(arg0);
    }

    public Message[] search(SearchTerm arg0, Message[] arg1) throws MessagingException {
        return folder.search(arg0, arg1);
    }

    public Message[] search(SearchTerm arg0) throws MessagingException {
        return folder.search(arg0);
    }

    public void setFlags(int arg0, int arg1, Flags arg2, boolean arg3) throws MessagingException {
        folder.setFlags(arg0, arg1, arg2, arg3);
    }

    public void setFlags(int[] arg0, Flags arg1, boolean arg2) throws MessagingException {
        folder.setFlags(arg0, arg1, arg2);
    }

    public void setFlags(Message[] arg0, Flags arg1, boolean arg2) throws MessagingException {
        folder.setFlags(arg0, arg1, arg2);
    }

    public void setSubscribed(boolean arg0) throws MessagingException {
        folder.setSubscribed(arg0);
    }

    public String toString() {
        return folder.toString();
    }

}
