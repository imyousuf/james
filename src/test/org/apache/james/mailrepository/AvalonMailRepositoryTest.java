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

package org.apache.james.mailrepository;

import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.core.MailImpl;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.mailrepository.filepair.File_Persistent_Object_Repository;
import org.apache.james.mailrepository.filepair.File_Persistent_Stream_Repository;
import org.apache.james.test.mock.avalon.MockContext;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

public class AvalonMailRepositoryTest extends TestCase {

    String content = "Subject: test\r\nAAAContent-Transfer-Encoding: text/plain";
    String sep = "\r\n\r\n";
    String body = "original body\r\n.\r\n";

    /**
     * This test has been written as a proof to:
     * http://issues.apache.org/jira/browse/JAMES-559
     */
    public void testJames559() throws Exception {
        AvalonMailRepository mr = new AvalonMailRepository();
        MockStore mockStore = new MockStore();
        File_Persistent_Stream_Repository file_Persistent_Stream_Repository = new File_Persistent_Stream_Repository();
        MockContext mockContext = new MockContext(new File("."));
        file_Persistent_Stream_Repository.contextualize(mockContext);
        file_Persistent_Stream_Repository.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration2 = new DefaultConfiguration("conf");
        defaultConfiguration2.setAttribute("destinationURL", "file://var/mr");
        file_Persistent_Stream_Repository.configure(defaultConfiguration2);
        file_Persistent_Stream_Repository.initialize();
        mockStore.add("STREAM.mr", file_Persistent_Stream_Repository);
        File_Persistent_Object_Repository file_Persistent_Object_Repository = new File_Persistent_Object_Repository();
        file_Persistent_Object_Repository.contextualize(mockContext);
        file_Persistent_Object_Repository.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration22 = new DefaultConfiguration("conf");
        defaultConfiguration22.setAttribute("destinationURL", "file://var/mr");
        file_Persistent_Object_Repository.configure(defaultConfiguration22);
        file_Persistent_Object_Repository.initialize();
        mockStore.add("OBJECT.mr", file_Persistent_Object_Repository);
        mr.setStore(mockStore);

        mr.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration = new DefaultConfiguration("ReposConf");
        defaultConfiguration.setAttribute("destinationURL","file://var/mr");
        defaultConfiguration.setAttribute("type","MAIL");
        mr.configure(defaultConfiguration);
        mr.initialize();

        MimeMessageInputStreamSource mmis = null;
        try {
            mmis = new MimeMessageInputStreamSource("test", new SharedByteArrayInputStream((content+sep+body).getBytes()));
        } catch (MessagingException e) {
        }
        MimeMessage mimeMessage = new MimeMessageCopyOnWriteProxy(mmis);
        Collection recipients = new ArrayList();
        recipients.add(new MailAddress("rec1","domain.com"));
        recipients.add(new MailAddress("rec2","domain.com"));
        MailImpl m = new MailImpl("mail1",new MailAddress("sender","domain.com"),recipients,mimeMessage);
        mr.store(m);
        
        Mail m2 = mr.retrieve("mail1");
        m2.getMessage().setHeader("X-Header", "foobar");
        m2.getMessage().saveChanges();
        
        mr.store(m2);
        // ALWAYS remember to dispose mails!
        ContainerUtil.dispose(m2);
        
        m2 = mr.retrieve("mail1");
        assertEquals(m.getMessage().getContent().toString(),m2.getMessage().getContent().toString());
        
        m.dispose();
        ContainerUtil.dispose(m2);
        
        mr.remove("mail1");

    }


    /**
     * This test has been written as a proof to:
     * http://issues.apache.org/jira/browse/JAMES-559
     */
    public void testJames559WithoutSaveChanges() throws Exception {
        AvalonMailRepository mr = new AvalonMailRepository();
        MockStore mockStore = new MockStore();
        File_Persistent_Stream_Repository file_Persistent_Stream_Repository = new File_Persistent_Stream_Repository();
        MockContext mockContext = new MockContext(new File("."));
        file_Persistent_Stream_Repository.contextualize(mockContext);
        file_Persistent_Stream_Repository.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration2 = new DefaultConfiguration("conf");
        defaultConfiguration2.setAttribute("destinationURL", "file://var/mr");
        file_Persistent_Stream_Repository.configure(defaultConfiguration2);
        file_Persistent_Stream_Repository.initialize();
        mockStore.add("STREAM.mr", file_Persistent_Stream_Repository);
        File_Persistent_Object_Repository file_Persistent_Object_Repository = new File_Persistent_Object_Repository();
        file_Persistent_Object_Repository.contextualize(mockContext);
        file_Persistent_Object_Repository.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration22 = new DefaultConfiguration("conf");
        defaultConfiguration22.setAttribute("destinationURL", "file://var/mr");
        file_Persistent_Object_Repository.configure(defaultConfiguration22);
        file_Persistent_Object_Repository.initialize();
        mockStore.add("OBJECT.mr", file_Persistent_Object_Repository);
        mr.setStore(mockStore);

        mr.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration = new DefaultConfiguration("ReposConf");
        defaultConfiguration.setAttribute("destinationURL","file://var/mr");
        defaultConfiguration.setAttribute("type","MAIL");
        mr.configure(defaultConfiguration);
        mr.initialize();

        MimeMessageInputStreamSource mmis = null;
        try {
            mmis = new MimeMessageInputStreamSource("test", new SharedByteArrayInputStream((content+sep+body).getBytes()));
        } catch (MessagingException e) {
        }
        MimeMessage mimeMessage = new MimeMessageCopyOnWriteProxy(mmis);
        Collection recipients = new ArrayList();
        recipients.add(new MailAddress("rec1","domain.com"));
        recipients.add(new MailAddress("rec2","domain.com"));
        MailImpl m = new MailImpl("mail1",new MailAddress("sender","domain.com"),recipients,mimeMessage);
        mr.store(m);
        
        Mail m2 = mr.retrieve("mail1");
        m2.getMessage().setHeader("X-Header", "foobar");
        
        mr.store(m2);
        // ALWAYS remember to dispose mails!
        ContainerUtil.dispose(m2);
        
        m2 = mr.retrieve("mail1");
        assertEquals(m.getMessage().getContent().toString(),m2.getMessage().getContent().toString());
        
        m.dispose();
        ContainerUtil.dispose(m2);
        
        mr.remove("mail1");

    }

}

