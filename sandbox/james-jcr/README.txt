============================
JCR support for Apache James
============================

This is an experimental component for using a JCR content repository
as the email store of the Apache James project.

See the Apache James web site (http://james.apache.org/) for more
information. Apache James is a project of the Apache Software Foundation
(http://www.apache.org/).


License (see also LICENSE.txt)
==============================

Collective work: Copyright 2007 The Apache Software Foundation.

Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


Getting Started
===============

1) Build instructions

   This component currently uses Maven 2 as the main build environment
   and depends on the latest 3.0-SNAPSHOT versions of the Mailet API and
   the James Server core library. Other essential dependencies are the
   JCR API and Jackrabbit 1.3. Note that even though the build depends on
   snapshot versions of James, the resulting code should work also with
   at least James 2.3.x.

   Once you have all the required dependencies in your local Maven repository
   (not all of them are available in the central repositories) you can build
   this component with the following command:

       mvn package

   The resulting james-jcr-1.0-SNAPSHOT.jar file is placed in the "target"
   subdirectory.

   There is also a draft Ant build.xml file, but it's not really tested.

2) Install instructions

   To install this component in an existing James Server installation,
   you need to copy the following jar files to apps/james/SAR-INF/lib:

       james-jcr-1.0-SNAPSHOT.jar  - this component
       jcr-1.0.jar                 - JCR API
       jackrabbit-jcr-rmi.jar      - Jackrabbit RMI layer
       jackrabbit-api.jar          - JCR API extensions from Jackrabbit
       jackrabbit-jcr-commons.jar  - JCR utility classes from Jackrabbit

   You also need to have Jackrabbit or any other compliant JCR content
   repository available as a JCR-RMI server.

3) Configuration instructions

   To configure this component you need to add the following entries to
   the apps/james/SAR-INF/config.xml file in your James Server installation:

       <inboxRepository>
         <repository destinationURL="jcr://james:inbox/" type="MAIL"/>
       </inboxRepository>

       <repository class="org.apache.james.jcr.AvalonJCRMailRepository">
         <protocols>
           <protocol>jcr</protocol>
         </protocols>
         <types>
           <type>MAIL</type>
         </types>
         <config>
           <repository>//localhost/jackrabbit.repository</repository>
           <username>admin</username>
           <password>admin</password>
           <workspace>default</workspace>
         </config>
       </repository>

   You should replace the repository <config> entries with appropriate values
   for your content repository. The path in the destinationURL parameter is
   used as the path of the node under which all mail messages are stored.

   This component attempts to automatically register the custom namespaces
   and node types it uses, but this automatic registration only works for
   the latest Jackrabbit snapshots. For other content repositories and earlier
   Jackrabbit releases (including Jackrabbit 1.3) you need to manually register
   the namespaces and node types declared in the james.cnd file located in
   the src/main/resources/org/apache/james/jcr directory.

4) Example content

   The following is an example content listing of a content repository
   that contains a single message sent to the "jukka" account:

      /james:inbox
         jcr:primaryType: nt:folder
         jcr:created: 2007-05-17T14:20:51.125+03:00
      /james:inbox/jukka
         jcr:primaryType: nt:folder
         jcr:created: 2007-05-17T14:21:22.500+03:00
       /james:inbox/jukka/Mail1179400980953-1
         jcr:primaryType: james:mail
         jcr:created: 2007-05-17T14:23:01.000+03:00
         james:state: root
         james:sender: jukka@apache.org
         james:recipients: jukka@localhost
         james:remotehost: localhost
         james:remoteaddr: 127.0.0.1
       /james:inbox/jukka/Mail1179400980953-1/jcr:content
         jcr:primaryType: nt:resource
         jcr:uuid: d6e1b6c7-db28-4489-b3c7-ca9ca5722edd
         jcr:lastModified: 2007-05-17T14:23:00.953+03:00
         jcr:mimeType: message/rfc822
         jcr:data: [binary, 630 bytes]
