/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import java.util.Date;
import java.util.Iterator;

/** 
 * Contract exposed by a NewsGroup
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
public interface NNTPGroup {
    String getName();
    String getDescription();
    boolean isPostAllowed();

    /** the current article pointer. 
     * @return <0 indicates invalid/unknown value
     */
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
