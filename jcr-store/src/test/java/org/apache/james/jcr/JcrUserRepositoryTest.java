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

package org.apache.james.jcr;

import java.io.File;
import java.io.StringReader;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.userrepository.MockUsersRepositoryTest;
import org.xml.sax.InputSource;

public class JcrUserRepositoryTest extends MockUsersRepositoryTest {

    private static final String JACKRABBIT_HOME = "target/jackrabbit";
    
    private static final String CONFIG = 
            "<Repository>" +
            "<FileSystem class='org.apache.jackrabbit.core.fs.mem.MemoryFileSystem'>" +
            "    <param name='path' value='${rep.home}/repository'/>" +
            "</FileSystem>" +
            "<Security appName='Jackrabbit'>" +
            " <AccessManager class='org.apache.jackrabbit.core.security.SimpleAccessManager'>" +
            " </AccessManager>" +
            " <LoginModule class='org.apache.jackrabbit.core.security.SimpleLoginModule'>" +
            " </LoginModule>" +
            "</Security>" +
            "<Workspaces rootPath='${rep.home}/workspaces' defaultWorkspace='default'/>" +
            "  <Workspace name='${wsp.name}'>" +
            "    <FileSystem class='org.apache.jackrabbit.core.fs.mem.MemoryFileSystem'>" +
            "       <param name='path' value='${wsp.home}'/>" +
            "    </FileSystem>" +
            "    <PersistenceManager" +
            "             class='org.apache.jackrabbit.core.persistence.db.SimpleDbPersistenceManager'>" +
            "            <param name='url' value='jdbc:h2:${wsp.home}/db'/>" +
            "            <param name='driver' value='org.h2.Driver'/>" +
            "            <param name='schema' value='h2'/>" +
            "            <param name='schemaObjectPrefix' value='${wsp.name}_'/>" +
            "    </PersistenceManager>" +
            "    <SearchIndex class='org.apache.jackrabbit.core.query.lucene.SearchIndex'>" +
            "     <param name='path' value='${wsp.home}/index'/>" +
            "     <param name='textFilterClasses' value='org.apache.jackrabbit.extractor.HTMLTextExtractor,org.apache.jackrabbit.extractor.XMLTextExtractor'/>" +
            "     <param name='extractorPoolSize' value='2'/>" +
            "     <param name='supportHighlighting' value='true'/>" +
            "   </SearchIndex>" +
            "</Workspace>" +
            "<Versioning rootPath='${rep.home}/version'>" +
            "  <FileSystem class='org.apache.jackrabbit.core.fs.mem.MemoryFileSystem'>" +
            "    <param name='path' value='${rep.home}/version' />" +
            "  </FileSystem>" +
            "    <PersistenceManager" +
            "             class='org.apache.jackrabbit.core.persistence.db.SimpleDbPersistenceManager'>" +
            "            <param name='url' value='jdbc:h2:${rep.home}/version/db'/>" +
            "            <param name='driver' value='org.h2.Driver'/>" +
            "            <param name='schema' value='h2'/>" +
            "            <param name='schemaObjectPrefix' value='version_'/>" +
            "    </PersistenceManager>" +
            " </Versioning>" +
            " <SearchIndex class='org.apache.jackrabbit.core.query.lucene.SearchIndex'>" +
            "   <param name='path' value='${rep.home}/repository/index'/>" +
            "   <param name='textFilterClasses' value='org.apache.jackrabbit.extractor.HTMLTextExtractor,org.apache.jackrabbit.extractor.XMLTextExtractor'/>" +
            "   <param name='extractorPoolSize' value='2'/>" +
            "   <param name='supportHighlighting' value='true'/>" +
            " </SearchIndex>" +
            "</Repository>";
    
    RepositoryImpl repository;
        
    protected UsersRepository getUsersRepository() throws Exception 
    {
        return new JCRUsersRepository(repository);
    }

    protected void setUp() throws Exception {
        File home = new File(JACKRABBIT_HOME);
        if (home.exists()) {
            delete(home);
        }
        RepositoryConfig config = RepositoryConfig.create(new InputSource(new StringReader(CONFIG)), JACKRABBIT_HOME);
        repository = RepositoryImpl.create(config);
        super.setUp();
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            for (int i = 0; i < contents.length; i++) {
                delete(contents[i]);
            }
        } 
        file.delete();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        repository.shutdown();
    }
}
