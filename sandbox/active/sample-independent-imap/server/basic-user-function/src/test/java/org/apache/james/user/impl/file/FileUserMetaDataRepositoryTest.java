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

package org.apache.james.user.impl.file;

import java.io.File;

import org.apache.commons.io.FileUtils;

import junit.framework.TestCase;

public class FileUserMetaDataRepositoryTest extends TestCase {

    private static final String SYMBOLIC_KEY = "\\/><'@~ #][}{()=+.,| !`%$3\" exit(0)";

    private static final String KEY = "key";

    private static final String USER = "user";
    private static final String ANOTHER_USER = "another user";

    private static final String TEST_DIRECTORY = "target/testusermetadata";
    
    FileUserMetaDataRepository repository;
    
    protected void setUp() throws Exception {
        super.setUp();
        final File directory = new File(TEST_DIRECTORY);
        if (directory.exists()) {
            FileUtils.deleteDirectory(directory);
        }
        repository = new FileUserMetaDataRepository(TEST_DIRECTORY);
    }

    public void testClear() throws Exception {
        final Long value = new Long(99);
        repository.setAttribute(USER, value, KEY);
        assertEquals(value, repository.getAttribute(USER, KEY));
        final Long anotherValue = new Long(199);
        repository.setAttribute(ANOTHER_USER, anotherValue, KEY);
        assertEquals(anotherValue, repository.getAttribute(ANOTHER_USER, KEY));
        repository.clear(USER);
        assertNull(repository.getAttribute(USER, KEY));
        assertEquals(anotherValue, repository.getAttribute(ANOTHER_USER, KEY));
    }

    public void testSetGetAttribute() throws Exception {
        final Long value = new Long(99);
        repository.setAttribute(USER, value, KEY);
        assertEquals(value, repository.getAttribute(USER, KEY));
    }

    public void testUnsafeUserNames() throws Exception {
        final Long valueOne = new Long(99);
        final String userOne = USER + '\u25B2';
        repository.setAttribute(userOne, valueOne, KEY);
        final Long valueTwo = new Long(90);
        final String userTwo = USER + '\u25B3';
        repository.setAttribute(userTwo, valueTwo, KEY);
        final Long valueThree = new Long(80);
        final String userThree = USER + '\u25B4';
        repository.setAttribute(userThree, valueThree, KEY);
        final Long valueFour = new Long(199);
        final String userFour = USER + '\uA48A';
        repository.setAttribute(userFour, valueFour, KEY);
        final Long valueFive = new Long(190);
        final String userFive = USER + '\uA485';
        repository.setAttribute(userFive, valueFive, KEY);
        final Long valueSix = new Long(180);
        final String userSix = USER + '\uA486';
        repository.setAttribute(userSix, valueSix, KEY);
        final Long valueSeven = new Long(290);
        final String userSeven = USER + '\uFFFE';
        repository.setAttribute(userSeven, valueSeven, KEY);
        final Long valueEight = new Long(280);
        final String userEight = USER + '\uFFFF';
        repository.setAttribute(userEight, valueEight, KEY);
        
        assertEquals(valueOne, repository.getAttribute(userOne, KEY));
        assertEquals(valueTwo, repository.getAttribute(userTwo, KEY));
        assertEquals(valueThree, repository.getAttribute(userThree, KEY));
        assertEquals(valueFour, repository.getAttribute(userFour, KEY));
        assertEquals(valueFive, repository.getAttribute(userFive, KEY));
        assertEquals(valueSix, repository.getAttribute(userSix, KEY));
        assertEquals(valueSeven, repository.getAttribute(userSeven, KEY));
        assertEquals(valueEight, repository.getAttribute(userEight, KEY));
    }
    
    public void testUnsafeKeyNames() throws Exception {
        final Long value = new Long(99);
        repository.setAttribute(USER, value, SYMBOLIC_KEY);
        assertEquals(value, repository.getAttribute(USER, SYMBOLIC_KEY));
    }
    
    public void testGetWithoutSet() throws Exception {
        assertNull(repository.getAttribute(USER, KEY));
    }
}
