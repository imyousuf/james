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
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Message.RecipientType;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMessage;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.jackrabbit.util.Text;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetConfig;

/**
 * Mailet that stores messages to a JCR content repository.
 */
public class JCRStoreMailet implements Mailet {

    /**
     * Mailet configuration.
     */
    private MailetConfig config;

    /**
     * JCR content repository.
     */
    private Repository repository;

    /**
     * Returns information about this mailet.
     *
     * @return mailet information
     */
    public String getMailetInfo() {
        return "JCR Store Mailet";
    }

    /**
     * Returns the mailet configuration.
     *
     * @return mailet configuration
     */
    public MailetConfig getMailetConfig() {
        return config;
    }

    /**
     * Initializes this mailet by connecting to the configured JCR repository.
     *
     * @param config mailet configuration
     * @throws MessagingException if the JCR repository can not be accessed
     */
    public void init(MailetConfig config) throws MessagingException {
        this.config = config;

        String url = config.getInitParameter("url");
        try {
            ClientRepositoryFactory factory = new ClientRepositoryFactory();
            this.repository = factory.getRepository(url);
        } catch (Exception e) {
            throw new MessagingException(
                    "Error accessing the content repository: " + url, e);
        }
    }

    /**
     * Closes this mailet by releasing the JCR connection.
     */
    public void destroy() {
        this.repository = null;
        this.config = null;
    }

    /**
     * Stores the given mail message to the content repository.
     *
     * @param mail mail message
     * @throws MessagingException if the message could not be saved
     */
    public void service(Mail mail) throws MessagingException {
        try {
            String username = config.getInitParameter("username");
            String password = config.getInitParameter("password");
            String workspace = config.getInitParameter("workspace");
            String path = config.getInitParameter("path");

            Credentials credentials = null;
            if (username != null) {
                credentials =
                    new SimpleCredentials(username, password.toCharArray());
            }

            Session session = repository.login(credentials, workspace);
            try {
                Item item = session.getItem(path);
                if (item instanceof Node) {
                    importMessage(mail.getMessage(), (Node) item);
                } else {
                    throw new MessagingException("Invalid path: " + path);
                }
            } finally {
                session.logout();
            }
        } catch (IOException e) {
            throw new MessagingException("IO error", e);
        } catch (RepositoryException e) {
            throw new MessagingException("Repository access error", e);
        }
    }

    /**
     * Import the given message to the given JCR node.
     *
     * @param message the source message
     * @param parent the target node
     * @throws MessagingException if the message could not be read
     * @throws RepositoryException if the message could not be written
     * @throws IOException 
     */
    private void importMessage(Message message, Node parent)
            throws MessagingException, RepositoryException, IOException {
        Node node = createNode(parent, getMessageName(message), "nt:file");
        importEntity(message, node);
        parent.save();
    }

    /**
     * Import the given entity to the given JCR node.
     *
     * @param entity the source entity
     * @param parent the target node
     * @throws MessagingException if the message could not be read
     * @throws RepositoryException if the message could not be written
     * @throws IOException 
     */
    private void importEntity(Part entity, Node parent)
            throws MessagingException, RepositoryException, IOException {
        Node node = parent.addNode("jcr:content", "nt:unstructured");

        setProperty(node, "description", entity.getDescription());
        setProperty(node, "disposition", entity.getDisposition());
        setProperty(node, "filename", entity.getFileName());

        if (entity instanceof MimeMessage) {
            MimeMessage mime = (MimeMessage) entity;
            setProperty(node, "subject", mime.getSubject());
            setProperty(node, "message-id", mime.getMessageID());
            setProperty(node, "content-id", mime.getContentID());
            setProperty(node, "content-md5", mime.getContentMD5());
            setProperty(node, "language", mime.getContentLanguage());
            setProperty(node, "sent", mime.getSentDate());
            setProperty(node, "received", mime.getReceivedDate());
            setProperty(node, "from", mime.getFrom());
            setProperty(node, "to", mime.getRecipients(RecipientType.TO));
            setProperty(node, "cc", mime.getRecipients(RecipientType.CC));
            setProperty(node, "bcc", mime.getRecipients(RecipientType.BCC));
            setProperty(node, "reply-to", mime.getReplyTo());
            setProperty(node, "sender", mime.getSender());
        }

        Object content = entity.getContent();
        ContentType type = getContentType(entity);
        node.setProperty("jcr:mimeType", type.getBaseType());
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                Node child;
                if (part.getFileName() != null) {
                    child = createNode(node, part.getFileName(), "nt:file");
                } else {
                    child = createNode(node, "part", "nt:unstructured");
                }
                importEntity(part, child);
            }
        } else if (content instanceof String) {
            byte[] bytes = ((String) content).getBytes("UTF-8");
            node.setProperty("jcr:encoding", "UTF-8");
            node.setProperty("jcr:data", new ByteArrayInputStream(bytes));
        } else if (content instanceof InputStream) {
            setProperty(
                    node, "jcr:encoding", type.getParameter("encoding"));
            node.setProperty("jcr:data", (InputStream) content);
        } else {
            node.setProperty("jcr:data", entity.getInputStream());
        }
    }

    /**
     * Suggests a name for the node where the given message will be stored.
     *
     * @param message mail message
     * @return suggested name
     * @throws MessagingException if an error occurs
     */
    private String getMessageName(Message message)
            throws MessagingException {
        DateFormat format = new SimpleDateFormat("yyyy-mm-dd");
        String subject = message.getSubject().replaceAll("[\\[\\]:/]", "");
        return format.format(message.getSentDate()) + " " + subject; 
    }

    /**
     * Creates a new node with a name that resembles the given suggestion.
     * The created node is not saved by this method.
     *
     * @param parent parent node
     * @param name suggested name
     * @param type node type
     * @return created node
     * @throws RepositoryException if an error occurs
     */
    private Node createNode(Node parent, String name, String type)
            throws RepositoryException {
        String original = name;
        name = Text.escapeIllegalJcrChars(name);
        for (int i = 2; parent.hasNode(name); i++) {
            name = Text.escapeIllegalJcrChars(original + i);
        }
        return parent.addNode(name, type);
    }

    /**
     * Returns the content type of the given message entity. Returns
     * the default "text/plain" content type if a content type is not
     * available. Returns "applicatin/octet-stream" if an error occurs.
     *
     * @param entity the message entity
     * @return content type, or <code>text/plain</code> if not available
     */
    private static ContentType getContentType(Part entity) {
        try {
            String type = entity.getContentType();
            if (type != null) {
                return new ContentType(type);
            } else {
                return new ContentType("text/plain");
            }
        } catch (MessagingException e) {
            ContentType type = new ContentType();
            type.setPrimaryType("application");
            type.setSubType("octet-stream");
            return type;
        }
    }

    /**
     * Sets the named property if the given value is not null.
     *
     * @param node target node
     * @param name property name
     * @param value property value
     * @throws RepositoryException if an error occurs
     */
    private void setProperty(Node node, String name, String value)
            throws RepositoryException {
        if (value != null) {
            node.setProperty(name, value);
        }
    }

    /**
     * Sets the named property if the given array of values is
     * not null or empty.
     *
     * @param node target node
     * @param name property name
     * @param values property values
     * @throws RepositoryException if an error occurs
     */
    private void setProperty(Node node, String name, String[] values)
            throws RepositoryException {
        if (values != null && values.length > 0) {
            if (values.length == 1) {
                node.setProperty(name, values[0]);
            } else {
                node.setProperty(name, values);
            }
        }
    }

    /**
     * Sets the named property if the given value is not null.
     *
     * @param node target node
     * @param name property name
     * @param value property value
     * @throws RepositoryException if an error occurs
     */
    private void setProperty(Node node, String name, Date value)
            throws RepositoryException {
        if (value != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(value);
            node.setProperty(name, calendar);
        }
    }

    /**
     * Sets the named property if the given value is not null.
     *
     * @param node target node
     * @param name property name
     * @param value property value
     * @throws RepositoryException if an error occurs
     */
    private void setProperty(Node node, String name, Address value)
            throws RepositoryException {
        if (value != null) {
            node.setProperty(name, value.toString());
        }
    }

    /**
     * Sets the named property if the given array of values is
     * not null or empty.
     *
     * @param node target node
     * @param name property name
     * @param values property values
     * @throws RepositoryException if an error occurs
     */
    private void setProperty(Node node, String name, Address[] values)
            throws RepositoryException {
        if (values != null && values.length > 0) {
            if (values.length == 1) {
                node.setProperty(name, values[0].toString());
            } else {
                String[] strings = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    strings[i] = values[i].toString();
                }
                node.setProperty(name, strings);
            }
        }
    }

}
