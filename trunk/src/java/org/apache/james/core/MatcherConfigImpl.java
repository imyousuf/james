/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.core;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.mailet.*;

/**
 * The implementation of the configuration object for a Matcher.
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class MatcherConfigImpl implements MatcherConfig {
    private String condition;
    private String name;
    private MailetContext context;

    public String getCondition() {
        return condition;
    }

    public void setCondition(String newCondition) {
        condition = newCondition;
    }

    public String getMatcherName() {
        return name;
    }

    public void setMatcherName(String newName) {
        name = newName;
    }

    public MailetContext getMailetContext() {
        return context;
    }

    public void setMailetContext(MailetContext newContext) {
        context = newContext;
    }
}
