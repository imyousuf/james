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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Message.RecipientType;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMessage;

import org.apache.jackrabbit.util.Text;

/**
 * JavaBean that stores messages to a JCR content repository.
 */
public class JCRStoreBean {

    /**
     * Parent node where the messages are stored.
     */
    private Node parent;

    public void setParentNode(Node parent) {
        this.parent = parent;
    }

    /**
     * Stores the given mail message to the content repository.
     *
     * @param message mail message
     * @throws MessagingException if the message could not be read
     * @throws RepositoryException if the message could not be saved
     */
    public void storeMessage(Message message)
            throws MessagingException, RepositoryException {
        try {
            Node node = createNode(parent, getMessageName(message), "nt:file");
            importEntity(message, node);
            parent.save();
        } catch (IOException e) {
            throw new MessagingException("Could not read message", e);
        }
    }

    /**
     * Import the given entity to the given JCR node.
     *
     * @param entity the source entity
     * @param parent the target node
     * @throws MessagingException if the message could not be read
     * @throws RepositoryException if the message could not be written
     * @throws IOException if the message could not be read
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
