/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import java.io.*;
import javax.mail.internet.InternetHeaders;
import org.apache.james.nntpserver.NNTPException;

public interface NNTPArticle {
    NNTPGroup getGroup();
    int getArticleNumber();
    String getUniqueID();
    void writeArticle(PrintWriter wrt);
    void writeHead(PrintWriter wrt);
    void writeBody(PrintWriter wrt);
    void writeOverview(PrintWriter wrt);
    String getHeader(String header);
}
