

Build Procedure Description
---------------------------


This directory contains a set of resources used to build the james server platform. The initial
entry point for all build function is via project/build.xml. To initiate one-time setup (installation 
of required antlibs and application specific dependencies) please execute the following setap:

  $ cd james-server
  $ cd project
  $ ant setup

You are now ready to build james.  From the project subdirectory, please issue the following command:

  $ ant

The results of the build are placed in the james-server/target/deleiverables directory.


Resource Summary.
-----------------


  /james-server/project

     build.xml ..............................main ant build file

     build.properties ...................... project specific properties
 
     index.xml ............................. central catalog of artifact names and versions 
                                             used in the build of the james server

     standard.xml .......................... main build target template containing src preparation, 
                                             compilation, jar creation, test, and repository 
                                             installation targets (imported by build.xml)

     verify.xml ............................ includes a target to verify that the prerequsite
                                             ant library is installed (imported by build.xml)

     setup.xml ............................. one-time setup of the build environment - used to 
                                             install the dependent ant libraries and build 
                                             artifacts

  /james-server/project/merlin ............. resources specific to James in Merlin




