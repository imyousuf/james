/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import java.util.Iterator;
import java.util.Date;

/**
 * Abstraction of entire NNTP Repository.
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
public interface NNTPRepository {
    NNTPGroup getGroup(String groupName);
    NNTPArticle getArticleFromID(String id);
    void createArticle(NNTPLineReader reader);
    Iterator getMatchedGroups(String wildmat);
    Iterator getGroupsSince(Date dt);
    Iterator getArticlesSince(Date dt);
    boolean isReadOnly();
}
