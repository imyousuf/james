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



package org.apache.james.transport.mailets;



import javax.sql.DataSource;
import org.apache.mailet.DataSourceInject;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailRepository;
import org.apache.mailet.MailetException;
import org.apache.mailet.ServiceInject;

/**
 * Stores incoming Mail in the specified Repository.
 * If the "passThrough" in confs is true the mail will be returned untouched in
 * the pipe. If false will be destroyed.
 * @version 1.0.0, 24/04/1999
 *
 * @version This is $Revision$
 */
public class ToRepository extends GenericMailet {

    /**
     * The repository where this mailet stores mail.
     */
    private MailRepository repository;

    /**
     * Whether this mailet should allow mails to be processed by additional mailets
     * or mark it as finished.
     */
    private boolean passThrough = false;

    /**
     * The path to the repository
     */
    private String repositoryPath;

    
    @DataSourceInject(dsName="maildb")public void setDatasource(DataSource d) {
        System.out.println(" just for kicks ... mailet got datasource ... "+d);
    }
    
    /**
     * Initialize the mailet, loading configuration information.
     */
    
    
    public void init() {
        repositoryPath = getInitParameter("repositoryPath");
        try {
            passThrough = new Boolean(getInitParameter("passThrough")).booleanValue();
        } catch (Exception e) {
            // Ignore exception, default to false
        }

      
            try{
                repository = getMailetContext().getMailRepository(repositoryPath);
            }catch (MailetException e){
                log("Failed to retrieve repository:" + e.getMessage());
            
        } catch (Exception e) {
            log("Failed to retrieve Store component:" + e.getMessage());
        }

    }

    

    /**
     * Store a mail in a particular repository.
     *
     * @param mail the mail to process
     */
    public void service(Mail mail) throws javax.mail.MessagingException {
        StringBuffer logBuffer =
            new StringBuffer(160)
                    .append("Storing mail ")
                    .append(mail.getName())
                    .append(" in ")
                    .append(repositoryPath);
        log(logBuffer.toString());
    
        repository.store(mail);
        if (!passThrough) {
            mail.setState(Mail.GHOST);
        }
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "ToRepository Mailet";
    }
}
