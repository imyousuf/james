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
package org.apache.james.core;

import com.sun.mail.util.SharedByteArrayInputStream;

import javax.mail.MessagingException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import junit.framework.TestCase;

/**
 * Test the subject folding issue.
 */
public class MimeMessageWrapperTest extends TestCase {

    MimeMessageWrapper mw = null;
    String content = "Subject: foo\r\nContent-Transfer-Encoding2: plain";
    String sep = "\r\n\r\n";
    String body = "bar\r\n.\r\n";

    public MimeMessageWrapperTest(String arg0) {
        super(arg0);

        MimeMessageInputStreamSource mmis = null;
        try {
            mmis = new MimeMessageInputStreamSource("test", new SharedByteArrayInputStream((content+sep+body).getBytes()));
        } catch (MessagingException e) {
        }
        mw = new MimeMessageWrapper(mmis);
    }

    /*
     * Class under test for String getSubject()
     */
    public void testGetSubjectFolding() {
        try {
            StringBuffer res = new StringBuffer();
            BufferedReader r = new BufferedReader(new InputStreamReader(mw.getInputStream()));
            String line;
            while (r.ready()) {
                line = r.readLine();
                res.append(line+"\r\n");
            }
            r.close();
            assertEquals(body,res.toString());
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /*
     * Class under test for String getSubject()
     */
    public void testAddHeaderAndSave() {
        try {
            mw.addHeader("X-Test", "X-Value");
            mw.saveChanges();

            ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
            mw.writeTo(rawMessage);
            String res = rawMessage.toString();
            
            boolean found = res.indexOf("X-Test: X-Value") > 0;
            assertEquals(true,found);

        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
