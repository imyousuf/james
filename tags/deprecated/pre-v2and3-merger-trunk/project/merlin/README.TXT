

Running James SNAPSHOT in Merlin.
---------------------------------

The resources contained in this directory are based on the current snapshot build of the merlin system.  You 
can download and install the latest development build from the following url:

  http://www.dpml.net/avalon/merlin/dist

Instructions for installing merlin are available at this url:

  http://avalon.apache.org/central/about/installation/binary.html



Customizing the james configuration for your environment.
---------------------------------------------------------

All files necessary to run merlin are referenced from the local merlin.properties
file.  These include the kernel definition, logging configuration, and your local 
site configuration.  This last file needs to be updated to include details such 
as the domain name your serving, remote access account names and passwords, etc.

See:

  config.xml


Running James.
--------------


After updating config.xml you can start James by doing the following:


  $ cd james-server/project/merlin
  $ merlin


Will will normally see the following messages logged to your console:

  James Mail Server SNAPSHOT
  Remote Manager Service started plain:4555
  POP3 Service started plain:110
  SMTP Service started plain:25


Creating a new user account.
----------------------------

To test you configuration you will need to create a new user account in james.  To do this
you need to telnet to localhost 4555 and adduser. 


  $ telnet localhost 4555
  JAMES Remote Administration Tool SNAPSHOT
  Please enter your login and password
  Login id:
  demo
  Password:
  demo
  Welcome demo. HELP for a list of commands

  adduser postmaster somepassword
  User postmaster added

  quit

  Bye


From here you should be able to send and receive mail via james using the account name you created.


Steve.
