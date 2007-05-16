/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.james.jcr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailRepository;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 * Mail repository that is backed by a JCR content repository.
 */
public class JCRMailRepository implements MailRepository {

    /**
     * JCR content repository used as the mail repository.
     * Must be set before the any mail operations are performed.
     */
    private Repository repository;

    /**
     * Login credentials for accessing the repository.
     * Set to <code>null</code> (the default) to use default credentials.
     */
    private Credentials credentials;

    /**
     * Name of the workspace used as the mail repository.
     * Set to <code>null</code> (the default) to use the default workspace.
     */
    private String workspace;

    /**
     * Retuns the JCR content repository used as the mail repository.
     *
     * @return JCR content repository
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * Sets the JCR content repository to be used as the mail repository.
     *
     * @param repository JCR content repository
     */
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    /**
     * Returns the login credentials for accessing the repository.
     *
     * @return login credentials,
     *         or <code>null</code> if using the default credentials
     */
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Sets the login credentials for accessing the repository.
     *
     * @param credentials login credentials,
     *                    or <code>null</code> to use the default credentials
     */
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    /**
     * Returns the name of the workspace used as the mail repository.
     *
     * @return workspace name,
     *         or <code>null</code> if using the default workspace
     */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * Sets the name of the workspace used as the mail repository.
     *
     * @param workspace workspace name,
     *                  or <code>null</code> to use the default workspace
     */
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    //------------------------------------------------------< MailRepository >

    public Iterator list() throws MessagingException {
        try {
            Session session = repository.login(credentials, workspace);
            try {
                Collection keys = new ArrayList();
                QueryManager manager = session.getWorkspace().getQueryManager();
                Query query = manager.createQuery(
                        "//element(*,james:mail)", Query.XPATH);
                NodeIterator iterator = query.execute().getNodes();
                while (iterator.hasNext()) {
                    String name = iterator.nextNode().getName();
                    keys.add(Text.unescapeIllegalJcrChars(name));
                }
                return keys.iterator();
            } finally {
                session.logout();
            }
        } catch (RepositoryException e) {
            throw new MessagingException("Unable to list messages", e);
        }
    }

    public Mail retrieve(String key) throws MessagingException {
        try {
            Session session = repository.login(credentials, workspace);
            try {
                String name = ISO9075.encode(Text.escapeIllegalJcrChars(key));
                QueryManager manager = session.getWorkspace().getQueryManager();
                Query query = manager.createQuery(
                        "//element(" + name + ",james:mail)", Query.XPATH);
                NodeIterator iterator = query.execute().getNodes();
                if (iterator.hasNext()) {
                    return getMail(iterator.nextNode());
                } else {
                    return null;
                }
            } finally {
                session.logout();
            }
        } catch (IOException e) {
            throw new MessagingException(
                    "Unable to retrieve message: " + key, e);
        } catch (RepositoryException e) {
            throw new MessagingException(
                    "Unable to retrieve message: " + key, e);
        }
    }

    public void store(Mail mail) throws MessagingException {
        try {
            Session session = repository.login(credentials, workspace);
            try {
                String name = Text.escapeIllegalJcrChars(mail.getName());
                QueryManager manager = session.getWorkspace().getQueryManager();
                Query query = manager.createQuery(
                        "//element(" + name + ",james:mail)", Query.XPATH);
                NodeIterator iterator = query.execute().getNodes();
                if (iterator.hasNext()) {
                    while (iterator.hasNext()) {
                        setMail(iterator.nextNode(), mail);
                    }
                } else {
                    Node root = session.getRootNode();
                    Node node = root.addNode(name, "james:mail");
                    Node resource = node.addNode("jcr:content", "nt:resource");
                    resource.setProperty("jcr:lastModified", Calendar.getInstance());
                    resource.setProperty("jcr:mimeType", "message/rfc822");
                    setMail(node, mail);
                }
                session.save();
            } finally {
                session.logout();
            }
        } catch (IOException e) {
            throw new MessagingException(
                    "Unable to store message: " + mail.getName(), e);
        } catch (RepositoryException e) {
            throw new MessagingException(
                    "Unable to store message: " + mail.getName(), e);
        }
    }

    public void remove(String key) throws MessagingException {
        try {
            Session session = repository.login(credentials, workspace);
            try {
                String name = ISO9075.encode(Text.escapeIllegalJcrChars(key));
                QueryManager manager = session.getWorkspace().getQueryManager();
                Query query = manager.createQuery(
                        "//element(" + name + ",james:mail)", Query.XPATH);
                NodeIterator nodes = query.execute().getNodes();
                while (nodes.hasNext()) {
                    nodes.nextNode().remove();
                }
                session.save();
            } finally {
                session.logout();
            }
        } catch (RepositoryException e) {
            throw new MessagingException("Unable to remove message: " + key, e);
        }
    }

    public void remove(Mail mail) throws MessagingException {
        remove(mail.getName());
    }

    public void remove(Collection mails) throws MessagingException {
        try {
            Session session = repository.login(credentials, workspace);
            try {
                QueryManager manager = session.getWorkspace().getQueryManager();
                Iterator iterator = mails.iterator();
                while (iterator.hasNext()) {
                    Mail mail = (Mail) iterator.next();
                    String name = ISO9075.encode(Text.escapeIllegalJcrChars(
                            mail.getName()));
                    Query query = manager.createQuery(
                            "//element(" + name + ",james:mail)", Query.XPATH);
                    NodeIterator nodes = query.execute().getNodes();
                    while (nodes.hasNext()) {
                        nodes.nextNode().remove();
                    }
                }
                session.save();
            } finally {
                session.logout();
            }
        } catch (RepositoryException e) {
            throw new MessagingException("Unable to remove messages", e);
        }
    }

    public boolean lock(String key) throws MessagingException {
        return false;
    }

    public boolean unlock(String key) throws MessagingException {
        return false;
    }

    //-------------------------------------------------------------< private >

    /**
     * Reads a mail message from the given mail node.
     *
     * @param node mail node
     * @return mail message
     * @throws MessagingException if a messaging error occurs
     * @throws RepositoryException if a repository error occurs
     * @throws IOException if an IO error occurs
     */
    private Mail getMail(Node node)
            throws MessagingException, RepositoryException, IOException {
        String name = Text.unescapeIllegalJcrChars(node.getName());
        MailImpl mail = new MailImpl(
                name, getSender(node), getRecipients(node),
                getMessage(node));
        mail.setState(getState(node));
        mail.setErrorMessage(getError(node));
        mail.setRemoteHost(getRemoteHost(node));
        mail.setRemoteAddr(getRemoteAddr(node));
        getAttributes(node, mail);
        return mail;
    }

    /**
     * Writes the mail message to the given mail node.
     *
     * @param node mail node
     * @param mail mail message
     * @throws MessagingException if a messaging error occurs
     * @throws RepositoryException if a repository error occurs
     * @throws IOException if an IO error occurs
     */
    private void setMail(Node node, Mail mail)
            throws MessagingException, RepositoryException, IOException {
        setState(node, mail.getState());
        setError(node, mail.getErrorMessage());
        setRemoteHost(node, mail.getRemoteHost());
        setRemoteAddr(node, mail.getRemoteAddr());
        setSender(node, mail.getSender());
        setRecipients(node, mail.getRecipients());
        setMessage(node, mail.getMessage());
        setAttributes(node, mail);
    }

    /**
     * Reads the message state from the james:state property.
     *
     * @param node mail node
     * @return message state, or {@link Mail#DEFAULT} if not set
     * @throws RepositoryException if a repository error occurs
     */
    private String getState(Node node) throws RepositoryException {
        try {
            return node.getProperty("james:state").getString();
        } catch (PathNotFoundException e) {
            return Mail.DEFAULT;
        }
    }

    /**
     * Writes the message state to the james:state property.
     *
     * @param node mail node
     * @param state message state
     * @throws RepositoryException if a repository error occurs
     */
    private void setState(Node node, String state) throws RepositoryException {
        node.setProperty("james:state", state);
    }

    /**
     * Reads the error message from the james:error property.
     *
     * @param node mail node
     * @return error message, or <code>null</code> if not set
     * @throws RepositoryException if a repository error occurs
     */
    private String getError(Node node) throws RepositoryException {
        try {
            return node.getProperty("james:error").getString();
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    /**
     * Writes the error message to the james:error property.
     *
     * @param node mail node
     * @param error error message
     * @throws RepositoryException if a repository error occurs
     */
    private void setError(Node node, String error) throws RepositoryException {
        node.setProperty("james:error", error);
    }

    /**
     * Reads the remote host name from the james:remotehost property.
     *
     * @param node mail node
     * @return remote host name, or <code>null</code> if not set
     * @throws RepositoryException if a repository error occurs
     */
    private String getRemoteHost(Node node) throws RepositoryException {
        try {
            return node.getProperty("james:remotehost").getString();
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    /**
     * Writes the remote host name to the james:remotehost property.
     *
     * @param node mail node
     * @param host remote host name
     * @throws RepositoryException if a repository error occurs
     */
    private void setRemoteHost(Node node, String host)
            throws RepositoryException {
        node.setProperty("james:remotehost", host);
    }

    /**
     * Reads the remote address from the james:remoteaddr property.
     *
     * @param node mail node
     * @return remote address, or <code>null</code> if not set
     * @throws RepositoryException if a repository error occurs
     */
    private String getRemoteAddr(Node node) throws RepositoryException {
        try {
            return node.getProperty("james:remoteaddr").getString();
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    /**
     * Writes the remote address to the james:remoteaddr property.
     *
     * @param node mail node
     * @param addr remote address
     * @throws RepositoryException if a repository error occurs
     */
    private void setRemoteAddr(Node node, String addr)
            throws RepositoryException {
        node.setProperty("james:remoteaddr", addr);
    }

    /**
     * Reads the envelope sender from the james:sender property.
     *
     * @param node mail node
     * @return envelope sender, or <code>null</code> if not set
     * @throws MessagingException if a messaging error occurs
     * @throws RepositoryException if a repository error occurs
     */
    private MailAddress getSender(Node node)
            throws MessagingException, RepositoryException {
        try {
            String sender = node.getProperty("james:sender").getString();
            return new MailAddress(sender);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    /**
     * Writes the envelope sender to the james:sender property.
     *
     * @param node mail node
     * @param sender envelope sender
     * @throws MessagingException if a messaging error occurs
     * @throws RepositoryException if a repository error occurs
     */
    private void setSender(Node node, MailAddress sender)
            throws MessagingException, RepositoryException {
        node.setProperty("james:sender", sender.toString());
    }

    /**
     * Reads the list of recipients from the james:recipients property.
     *
     * @param node mail node
     * @return list of recipient, or an empty list if not set
     * @throws MessagingException if a messaging error occurs
     * @throws RepositoryException if a repository error occurs
     */
    private Collection getRecipients(Node node)
            throws MessagingException, RepositoryException {
        try {
            Value[] values = node.getProperty("james:recipients").getValues();
            Collection recipients = new ArrayList(values.length);
            for (int i = 0; i < values.length; i++) {
                recipients.add(new MailAddress(values[i].getString()));
            }
            return recipients;
        } catch (PathNotFoundException e) {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Writes the list of recipients to the james:recipients property.
     *
     * @param node mail node
     * @param recipients list of recipient
     * @throws MessagingException if a messaging error occurs
     * @throws RepositoryException if a repository error occurs
     */
    private void setRecipients(Node node, Collection recipients)
            throws MessagingException, RepositoryException {
        String[] values = new String[recipients.size()];
        Iterator iterator = recipients.iterator();
        for (int i = 0; iterator.hasNext(); i++) {
            values[i] = iterator.next().toString();
        }
        node.setProperty("james:recipients", values);
    }

    /**
     * Reads the message content from the jcr:content/jcr:data binary property.
     *
     * @param node mail node
     * @return mail message
     * @throws MessagingException if a messaging error occurs
     * @throws RepositoryException if a repository error occurs
     * @throws IOException if an IO error occurs
     */
    private MimeMessage getMessage(Node node)
            throws MessagingException, RepositoryException, IOException {
        try {
            node = node.getNode("jcr:content");
        } catch (PathNotFoundException e) {
            node = node.getProperty("jcr:content").getNode();
        }

        InputStream stream = node.getProperty("jcr:data").getStream();
        try {
            Properties properties = System.getProperties();
            return new MimeMessage(
                    javax.mail.Session.getDefaultInstance(properties),
                    stream);
        } finally {
            stream.close();
        }
    }

    /**
     * Writes the message content to the jcr:content/jcr:data binary property.
     *
     * @param node mail node
     * @param message mail message
     * @throws MessagingException if a messaging error occurs
     * @throws RepositoryException if a repository error occurs
     * @throws IOException if an IO error occurs
     */
    private void setMessage(Node node, final MimeMessage message)
            throws MessagingException, RepositoryException, IOException {
        try {
            node = node.getNode("jcr:content");
        } catch (PathNotFoundException e) {
            node = node.getProperty("jcr:content").getNode();
        }

        PipedInputStream input = new PipedInputStream();
        final PipedOutputStream output = new PipedOutputStream(input);
        new Thread() {
            public void run() {
                try {
                    message.writeTo(output);
                } catch (Exception e) {
                } finally {
                    try {
                        output.close();
                    } catch (IOException e) {
                    }
                }
            }
        }.start();
        node.setProperty("jcr:data", input);
    }

    /**
     * Writes the mail attributes from the jamesattr:* property.
     *
     * @param node mail node
     * @param mail mail message
     * @throws RepositoryException if a repository error occurs
     * @throws IOException if an IO error occurs
     */
    private void getAttributes(Node node, Mail mail)
            throws RepositoryException, IOException {
        PropertyIterator iterator = node.getProperties("jamesattr:*");
        while (iterator.hasNext()) {
            Property property = iterator.nextProperty();
            String name = Text.unescapeIllegalJcrChars(
                    property.getName().substring("jamesattr:".length()));
            if (property.getType() == PropertyType.BINARY) {
                InputStream input = property.getStream();
                try {
                    ObjectInputStream stream = new ObjectInputStream(input);
                    mail.setAttribute(name, (Serializable) stream.readObject());
                } catch (ClassNotFoundException e) {
                    throw new IOException(e.getMessage());
                } finally {
                    input.close();
                }
            } else {
                mail.setAttribute(name, property.getString());
            }
        }
    }
    
    /**
     * Writes the mail attributes to the jamesattr:* property.
     *
     * @param node mail node
     * @param mail mail message
     * @throws RepositoryException if a repository error occurs
     * @throws IOException if an IO error occurs
     */
    private void setAttributes(Node node, Mail mail)
            throws RepositoryException, IOException {
        Iterator iterator = mail.getAttributeNames();
        while (iterator.hasNext()) {
            String name = (String) iterator.next();
            Object value = mail.getAttribute(name);
            name = "jamesattr:" + Text.escapeIllegalJcrChars(name);
            if (value instanceof String || value == null) {
                node.setProperty(name, (String) value);
            } else {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                ObjectOutputStream output = new ObjectOutputStream(buffer);
                output.writeObject(value);
                output.close();
                node.setProperty(
                        name,
                        new ByteArrayInputStream(buffer.toByteArray()));
            }
        }
    }

}
