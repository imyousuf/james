  =======================================================================
  ===         The Apache Software Foundation JAMES Server             ===
  =======================================================================
    

  What is it?
  -----------

  JAMES Server is a 100% pure Java server application designed to be a 
  complete, portable and powerful enterprise mail engine solution based 
  on currently available open protocols  (SMTP, POP3, NNTP).

  JAMES Server was formerly known as the Java Apache Mail Enterprise Server.

  Development Status for JAMES 2.3.x
  ----------------------------------
  
  SMTP Server: Stable 
  POP3 Server: Stable
  IMAP Server: Under development
  NNTP Server: Stable
  
  Current Feature
  ---------------

  These are some JAMES features:
  
    o  complete portability:  James is a 100% pure Java(tm) application
       based on the Java 2 platform and the JavaMail 1.4 API.
       
    o  protocol abstraction:  unlike other mail engines, protocols are seen only
       like "communication languages" ruling communications between clients and
       the server. James is not be tied to any particular protocol but follow an 
       abstracted server design (like JavaMail did on the client side)
       
    o  complete solution:  the mail system is able to handle both mail
       transport and storage in a single server application. Apache James
       works alone without the need for any other server or solution.
       
    o  mailet support:  James supports the Apache Mailet API. A Mailet
       is a discrete piece of mail-processing logic which is incorporated into
       a Mailet-compliant mail-server's processing. This easy-to-write,
       easy-to-use pattern allows developers to build powerful customized mail
       systems. Examples of the services a Mailet might provide include: a
       mail-to-fax or mail-to-phone transformer, a filter, a language
       translator, a mailing list manager, etc.
       Several Mailets are included in the JAMES distribution.
    
    o  resource abstraction:  like protocols, resources are abstracted and,
       accessed through defined interfaces (JavaMail for transport, JDBC for
       storage in RDBMS's, Apache Mailet API), the server is highly modular
       and can reuse solutions from other projects or applications.  
       
    o  secure and multi-threaded design:  based on the technology developed for
       the Apache JServ servlet engine, James has a careful,  security-oriented, 
       full multi-threaded design, to allow performance,  scalability and 
       mission-critical use.
       
    o  Currently JAMES support SMTP, POP3, NNTP and a simple remote administration
       facility.
       

  Planned Features
  ----------------

    o  IMAP support.
    
    o  IMAP server side filtering.

    o  More powerful remote admin protocol and tool.
    
    o  Extended set of Mailet to easily support most mail system request.
    
    o  anything else you may want if you help us writing it :-)
       

  Requirements
  ------------

  - JAMES.sar (required)
  - Phoenix server (Avalon-Phoenix) (required)

  JAMES distribution includes both required items.
  
  To recompile James from sources you should use org.apache.tools.ant and its 
  needed packages (ant.jar, javac.jar, xml.jar).

  Up and working...
  -----------------

  Step 1: installation.

    Download distibution. Extract all files in your favorite folder. You've
    probably done that if you are reading this file!


  Step 2: start phoenix.

    M$ users should just run /bin/run.bat. Unix users will find run.sh
    under the same folder - you may need to chmod +x run.sh and chmod +x phoenix.sh.
    A JVM must be in the path.

    Phoenix will unpack the james.sar into apps/james. Note that port 1111
    must be available for Phoenix to run (used by RMI server).

    Most UNIX systems require superuser privileges to open sockets below 1024,
    which includes the IANA-standard SMTP (on port 25), POP3 (on port 110),
    IMAP (port ) and NNTP (port ).  You will get an error message with
      'org.apache.avalon.phoenix.containerkit.lifecycle.LifecycleException: Component 
      named "xxx" failed to pass through the Starting stage. (Reason: java.net.BindException: 
      Permission denied).'
    or something similar if you have not got the right privileges.

    If you have the right privileges,  you should see

      'Phoenix 4.2

       James Mail Server 2.3.0
       Remote Manager Service started plain: 4555
       POP3 Service started plain:110
       SMTP Service started plain:25
       NNTP Service started plain:119
       FetchMail Disabled'

    Congratulations! You have James up and running.

  Step 3: Adding users

    Once James is running, telnet to port 4555.  You will see somthing like:
      'JAMES RemoteAdministration Tool 2.3.0
       Please enter your login and password
       Login id:'

    The defaul id and password are both 'root'. These can be changed in
    the  configuration file (see Step 4).
    
    To get help for the RemoteAdmin tool, type help. To add a user, type
    adduser [username] [password] .
    Eg: adduser test test


  Step 4: Test James

    Once you have some users entered, you can test James by sending mail to
    them.  Note that for mail to get to your machine, you need MX records in
    the DNS system.  You will see files materialise in
    apps/james/var/mail/inboxes.

    Retrieve the mail by configuring your POP client to get mail (inbound mail
    server) from James.

    Test outbound mail handling by configuring your client to use James smtp.
    (By default, you can only send mail from the machine on which James is
    running. This is to stop spam relaying. This can be changed in config
    file.)

    Trace out JAMES actions in /logs/*info.log.
    Action that will be taken by JAMES on incoming mail are configurated in
    the mailet pipe line (/apps/james/SAR-INF/config.xml). Look at it if you want to
    understand what's happening.

  Step 5: configuration.

    The configuration files are in apps/james/conf and apps/james/SAR-INF/.  For new 
    users, the only elements you need to worry about are in config.xml. You probably 
    want to change the root password in the remote manager section and to add your
    local network's IP address to the anti-spam mailet.


  
  Good luck :)  


  Licensing and legal issues
  --------------------------

  For legal and licensing issues, please look in the legal section of
  the documentation or read the LICENSE.txt and NOTICE.txt files.
  

  Crypto Notice
  -------------

  This distribution includes cryptographic software.  The country in
  which you currently reside may have restrictions on the import,
  possession, use, and/or re-export to another country, of
  encryption software.  BEFORE using any encryption software, please
  check your country's laws, regulations and policies concerning the
  import, possession, or use, and re-export of encryption software, to
  see if this is permitted.  See http://www.wassenaar.org/ for more
  information.

  The U.S. Government Department of Commerce, Bureau of Industry and
  Security (BIS), has classified this software as Export Commodity
  Control Number (ECCN) 5D002.C.1, which includes information security
  software using or performing cryptographic functions with asymmetric
  algorithms.  The form and manner of this Apache Software Foundation
  distribution makes it eligible for export under the License Exception
  ENC Technology Software Unrestricted (TSU) exception (see the BIS
  Export Administration Regulations, Section 740.13) for both object
  code and source code.

  The following provides more details on the included cryptographic
  software:

  SSL support in James Server rely on javax.net.ssl.SSLSocket java class
  included in SUN JRE/JDK.

  BCmail, used by SMIME enabled mailets included in James Server, rely on
  the JCE (Java Cryptography Extension) included in the Java 2 JRE since
  release 1.4 
  
  =======================================================================

  Thanks.

                                           The James Project
                                        http://james.apache.org/
