package org.apache.james.samples.mailets;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

import javax.mail.MessagingException;

import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MailetContext;

/**
 * Simply logs a message.
 */
public class HelloWorldMailet implements Mailet {

    private MailetConfig config;
    
    public void destroy() {

    }

    public String getMailetInfo() {
        return "Example mailet";
    }

    public MailetConfig getMailetConfig() {
        return config;
    }

    public void init(MailetConfig config) throws MessagingException {
        this.config = config;
    }

    public void service(Mail mail) throws MessagingException {
        MailetContext context = config.getMailetContext();
        context.log("Hello, World!");
        context.log("You have mail from " + mail.getSender().getUser());
    }
}
