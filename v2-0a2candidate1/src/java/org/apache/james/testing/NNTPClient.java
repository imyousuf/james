/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.testing;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import sun.net.nntp.*;
import org.apache.avalon.excalibur.io.IOUtil;
import java.io.*;

/**
 * @author Harmeet <hbedi@apache.org>
 */
public class NNTPClient {
    public static void main(String[] args) throws Exception {
        String server = args[0];
        String group = args[1];
        NntpClient nntp = new NntpClient(server);
        NewsgroupInfo info = nntp.getGroup(group);
        System.out.println("newsgroup: "+group+", "+info.firstArticle+", "+
                           info.lastArticle);
        File f = new File(group);
        f.mkdirs();
        for ( int i = 0 ; i < 10 ; i++ ) {
            int articleId = i+info.firstArticle;
            if ( articleId > info.lastArticle )
                break;
            IOUtil.copy(nntp.getArticle(articleId),
                        new FileOutputStream(new File(f,articleId+"")));
        }
    }
}
