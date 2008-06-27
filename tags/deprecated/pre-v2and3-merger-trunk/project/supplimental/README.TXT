
Avalon Supplemental
===================

The Avalon Supplemental module prepares a bar file holding
supplimental containing required by the avalon runtime.

Build details
-------------

The etc/deliverables/jars directory is a placeholder for jar 
files that are required to bootstrap the core buildsystem.  
To setup the library you need to download the following 
jar files and place them in jars directory under the names 
show below:

  mailapi-1.3.1.jar
  jms-1.1.jar

The mailet and jms jar files can be downloaded from the 
following urls:

  http://java.sun.com/products/javamail/downloads/index.html
  http://java.sun.com/products/jms/docs.html

Following population of the deliverables/jars directory as 
detailed above - generate a bar file using the following
command:

  $ ant bar install
