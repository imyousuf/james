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
package org.apache.james.experimental.imapserver.commands;

public interface ImapCommandFactory {

    public abstract ImapCommand getAppend();

    public abstract ImapCommand getAuthenticate();

    public abstract ImapCommand getCapability();

    public abstract ImapCommand getCheck();

    public abstract ImapCommand getClose();

    public abstract ImapCommand getCopy();

    public abstract ImapCommand getCreate();

    public abstract ImapCommand getDelete();

    public abstract ImapCommand getExamine();

    public abstract ImapCommand getExpunge();

    public abstract ImapCommand getFetch();

    public abstract ImapCommand getList();

    public abstract ImapCommand getLogin();

    public abstract ImapCommand getLogout();

    public abstract ImapCommand getLsub();

    public abstract ImapCommand getNoop();

    public abstract ImapCommand getRename();

    public abstract ImapCommand getSearch();

    public abstract ImapCommand getSelect();

    public abstract ImapCommand getStatus();

    public abstract ImapCommand getStore();

    public abstract ImapCommand getSubscribe();

    public abstract ImapCommand getUid();

    public abstract ImapCommand getUnsubscribe();

}
