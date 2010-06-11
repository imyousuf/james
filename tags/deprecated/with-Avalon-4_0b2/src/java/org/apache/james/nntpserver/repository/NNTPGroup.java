/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import java.util.*;
import java.io.*;
import org.apache.avalon.excalibur.io.ExtensionFileFilter;
import org.apache.avalon.excalibur.io.InvertedFileFilter;
import org.apache.avalon.excalibur.io.AndFileFilter;
import org.apache.james.nntpserver.NNTPException;
import org.apache.james.nntpserver.DateSinceFileFilter;

public interface NNTPGroup {
    String getName();
    String getDescription();
    boolean isPostAllowed();

    // the current article pointer. <0 indicates invalid/unknown value
    int getCurrentArticleNumber();
    void setCurrentArticleNumber(int articleNumber);

    int getNumberOfArticles();
    int getFirstArticleNumber();
    int getLastArticleNumber();

    NNTPArticle getCurrentArticle();
    NNTPArticle getArticle(int number);
    //NNTPArticle getArticleFromID(String id);
    Iterator getArticlesSince(Date dt);
    Iterator getArticles();
    Object getPath();
}
