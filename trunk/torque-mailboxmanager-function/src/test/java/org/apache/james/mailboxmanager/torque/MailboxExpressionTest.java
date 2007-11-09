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

package org.apache.james.mailboxmanager.torque;

import junit.framework.TestCase;

public class MailboxExpressionTest extends TestCase {

    private static final String PART = "mailbox";
    private static final String SECOND_PART = "sub";
    private static final String BASE = "BASE";

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private MailboxExpression create(String expression) {
        return new MailboxExpression(BASE, expression, '*', '%');
    }
    
    public void testIsWild() throws Exception {
        assertTrue(create("*").isWild());
        assertTrue(create("%").isWild());
        assertTrue(create("One*").isWild());
        assertTrue(create("*One").isWild());
        assertTrue(create("A*A").isWild());
        assertTrue(create("One%").isWild());
        assertTrue(create("%One").isWild());
        assertTrue(create("A%A").isWild());
        assertFalse(create("").isWild());
        assertFalse(create(null).isWild());
        assertFalse(create("ONE").isWild());
    }
    
    public void testCombinedNameEmptyPart() throws Exception {
        MailboxExpression expression = new MailboxExpression(BASE, "", '*', '%');
        assertEquals(BASE, expression.getCombinedName('.'));

    } 
    
    public void testNullCombinedName() throws Exception {
        MailboxExpression expression = new MailboxExpression(null, null, '*', '%');
        assertNotNull(expression.getCombinedName('.'));

    }
    
    public void testSimpleCombinedName() throws Exception {
        MailboxExpression expression = create(PART);
        assertEquals(BASE + "." + PART, expression.getCombinedName('.'));
        assertEquals(BASE + "/" + PART, expression.getCombinedName('/'));
    }
    
    public void testCombinedNamePartStartsWithDelimiter () throws Exception {
        MailboxExpression expression = create("." + PART);
        assertEquals(BASE + "." + PART, expression.getCombinedName('.'));
        assertEquals(BASE + "/." + PART, expression.getCombinedName('/'));
    }
    
    public void testCombinedNameBaseEndsWithDelimiter() throws Exception {
        MailboxExpression expression = new MailboxExpression(BASE + '.', PART, '*', '%');
        assertEquals(BASE + "." + PART, expression.getCombinedName('.'));
        assertEquals(BASE + "./" + PART, expression.getCombinedName('/'));
    }
    
    public void testCombinedNameBaseEndsWithDelimiterPartStarts() throws Exception {
        MailboxExpression expression = new MailboxExpression(BASE + '.', '.' + PART, '*', '%');
        assertEquals(BASE + "." + PART, expression.getCombinedName('.'));
        assertEquals(BASE + "./." + PART, expression.getCombinedName('/'));
    }
    
    public void testSimpleExpression() throws Exception {
        MailboxExpression expression = create(PART);
        assertTrue(expression.isExpressionMatch(PART, '.'));
        assertTrue(expression.isExpressionMatch(PART, '/'));
        assertFalse(expression.isExpressionMatch('.' + PART, '.'));
        assertFalse(expression.isExpressionMatch(PART + '.', '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART, '.'));
    }
    
    public void testEmptyExpression() throws Exception {
        MailboxExpression expression = create("");
        assertTrue(expression.isExpressionMatch("", '.'));
        assertTrue(expression.isExpressionMatch("", '/'));
        assertFalse(expression.isExpressionMatch("whatever", '.'));
        assertFalse(expression.isExpressionMatch(BASE + '.' + "whatever", '.'));
        assertFalse(expression.isExpressionMatch(BASE + "whatever", '.'));
    }
    
    public void testOnlyLocalWildcard() throws Exception {
        MailboxExpression expression = create("%");
        assertTrue(expression.isExpressionMatch("", '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(PART, '.'));
        assertTrue(expression.isExpressionMatch(PART + SECOND_PART, '.'));
        assertFalse(expression.isExpressionMatch(PART + '.' + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(PART + '/' + SECOND_PART, '.'));
    }
    
    
    public void testOnlyFreeWildcard() throws Exception {
        MailboxExpression expression = create("*");
        assertTrue(expression.isExpressionMatch("", '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(PART, '.'));
        assertTrue(expression.isExpressionMatch(PART + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(PART + '.' + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(PART + '/' + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(PART + '.' + SECOND_PART, '.'));
    }
    
    
    public void testEndsWithLocalWildcard() throws Exception {
        MailboxExpression expression = create(PART + '%');
        assertFalse(expression.isExpressionMatch("", '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(PART, '.'));
        assertTrue(expression.isExpressionMatch(PART + SECOND_PART, '.'));
        assertFalse(expression.isExpressionMatch(PART + '.' + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(PART + '/' + SECOND_PART, '.'));
        assertFalse(expression.isExpressionMatch(PART + '.' + SECOND_PART, '.'));
    }
    
    public void testStartsWithLocalWildcard() throws Exception {
        MailboxExpression expression = create('%' + PART);
        assertFalse(expression.isExpressionMatch("", '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + PART, '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART + '.' + PART, '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART + '.' + PART + '.' + SECOND_PART, '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART, '.'));
    }
    
    public void testContainsLocalWildcard() throws Exception {
        MailboxExpression expression = create(SECOND_PART + '%' + PART);
        assertFalse(expression.isExpressionMatch("", '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + PART, '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART + '.' + PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '/' + PART, '.'));
        assertFalse(expression.isExpressionMatch(PART, '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART + "w.hat.eve.r" + PART, '.'));
    }
    
    
    public void testEndsWithFreeWildcard() throws Exception {
        MailboxExpression expression = create(PART + '*');
        assertFalse(expression.isExpressionMatch("", '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(PART, '.'));
        assertTrue(expression.isExpressionMatch(PART + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(PART + '.' + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(PART + '/' + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(PART + '.' + SECOND_PART, '.'));
    }
    
    public void testStartsWithFreeWildcard() throws Exception {
        MailboxExpression expression = create('*' + PART);
        assertFalse(expression.isExpressionMatch("", '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '.' + PART, '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART, '.'));
    }
    
    public void testContainsFreeWildcard() throws Exception {
        MailboxExpression expression = create(SECOND_PART + '*' + PART);
        assertFalse(expression.isExpressionMatch("", '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '.' + PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '/' + PART, '.'));
        assertFalse(expression.isExpressionMatch(PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + "w.hat.eve.r" + PART, '.'));
    }
    
    public void testDoubleFreeWildcard() throws Exception {
        MailboxExpression expression = create(SECOND_PART + "**" + PART);
        assertFalse(expression.isExpressionMatch("", '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '.' + PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '/' + PART, '.'));
        assertFalse(expression.isExpressionMatch(PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + "w.hat.eve.r" + PART, '.'));
    }
    
    public void testFreeLocalWildcard() throws Exception {
        MailboxExpression expression = create(SECOND_PART + "*%" + PART);
        assertFalse(expression.isExpressionMatch("", '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '.' + PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '/' + PART, '.'));
        assertFalse(expression.isExpressionMatch(PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + "w.hat.eve.r" + PART, '.'));
    }
    
    public void testLocalFreeWildcard() throws Exception {
        MailboxExpression expression = create(SECOND_PART + "%*" + PART);
        assertFalse(expression.isExpressionMatch("", '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '.' + PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '/' + PART, '.'));
        assertFalse(expression.isExpressionMatch(PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + "w.hat.eve.r" + PART, '.'));
    }
    
    public void testMultipleFreeWildcards() throws Exception {
        MailboxExpression expression = create(SECOND_PART + '*' + PART + '*' + SECOND_PART + "**");
        assertTrue(expression.isExpressionMatch(SECOND_PART + PART + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '.' + PART + '.' + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + "tosh.bosh" + PART + "tosh.bosh" + SECOND_PART + "boshtosh", '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART + '.' + PART.substring(1) + '.' + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '.' + PART.substring(1) + '.' + SECOND_PART + PART + '.' + SECOND_PART  + "toshbosh", '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART + '.' + PART.substring(1) + '.' + SECOND_PART + PART + '.' + SECOND_PART.substring(1), '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + "tosh.bosh" + PART + "tosh.bosh" + PART + SECOND_PART + "boshtosh" + PART + SECOND_PART, '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART.substring(1) + "tosh.bosh" + PART + "tosh.bosh" + SECOND_PART + PART.substring(1) + SECOND_PART + "boshtosh" + PART + SECOND_PART.substring(1), '.'));
    }
    
    public void testSimpleMixedWildcards() throws Exception {
        MailboxExpression expression = create(SECOND_PART + '%' + PART + '*' + SECOND_PART);
        assertTrue(expression.isExpressionMatch(SECOND_PART + PART + SECOND_PART, '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART + '.' + PART + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + PART + '.' + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + PART + SECOND_PART + "Whatever", '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + PART + SECOND_PART + ".Whatever.", '.'));
    }
    
    public void testFreeLocalMixedWildcards() throws Exception {
        MailboxExpression expression = create(SECOND_PART + '*' + PART + '%' + SECOND_PART);
        assertTrue(expression.isExpressionMatch(SECOND_PART + PART + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '.' + PART + SECOND_PART, '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART + PART + '.' + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + PART  + "Whatever" + SECOND_PART, '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART + PART + SECOND_PART + ".Whatever.", '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '.' + PART + SECOND_PART, '.'));
        assertFalse(expression.isExpressionMatch(SECOND_PART + '.' + PART + SECOND_PART + '.' + SECOND_PART, '.'));
        assertTrue(expression.isExpressionMatch(SECOND_PART + '.' + PART + '.' + SECOND_PART + PART + SECOND_PART, '.'));
    }
}
