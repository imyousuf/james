   _                    _             __                           
  /_\  _ __   __ _  ___| |__   ___    \ \  __ _ _ __ ___   ___ ___ 
 //_\\| '_ \ / _` |/ __| '_ \ / _ \    \ \/ _` | '_ ` _ \ / _ | __|
/  _  \ |_) | (_| | (__| | | |  __/ /\_/ / (_| | | | | | |  __|__ \
\_/ \_/ .__/ \__,_|\___|_| |_|\___| \___/ \__,_|_| |_| |_|\___|___/
      |_|                                                          

 __                            _____    ___               _____ 
/ _\ ___ _ ____   _____ _ __  |___ /   / _ \        /\/\ |___ / 
\ \ / _ \ '__\ \ / / _ \ '__|   |_ \  | | | |_____ /    \  |_ \ 
_\ \  __/ |   \ V /  __/ |     ___) |_| |_| |_____/ /\/\ \___) |
\__/\___|_|    \_/ \___|_|    |____/(_)\___/      \/    \/____/ 
                                                                
--------------------------------------------------------------------                                                            
### http://james.apache.org/server/3/ ###

Thank you for testing Apache James Server 3.0-M3!

  * What's new in 3.0-M3 for end users
    - Numerous IMAP bug fixes (client support outlook,..., memory leaks, NIO and IO)
    - Telnet Management has been removed in favor of JMX with client shell
    - More metrics counters available via JMX
    - JPA validated against more databases (among others Oracle)
    - Multiple port configurations per protocol
    - POP3 is now operational (was buggy in 3.0-M2)
    - Mailbox Copier
    - Upgrade tool for James 2.3 are available
    - Better logging
    - More documentation on web site for configuration,...
    - ... and much more, see details on https://issues.apache.org/jira/secure/ReleaseNote.jspa?projectId=10411&version=12315512
  * What's new in 3.0-M3 for developers
    - Less maven modules
    - Maven 3.0.2 required to build
    - Upgrade to latest frameworks versions (netty, activemq, jackrabbit...)
    - Code reports generation via 'mvn site -P site-reports' maven profile
    - Corrections further to findbugs,... reports
    - Code formatting
    - ... and much more, see details on https://issues.apache.org/jira/secure/ReleaseNote.jspa?projectId=10411&version=12315512

  * Quick Start  http://james.apache.org/server/3/quick-start.html

  * Install      http://james.apache.org/server/3/install.html
  * Configure    http://james.apache.org/server/3/config.html
  * Manage       http://james.apache.org/server/3/manage.html
  * Monitor      http://james.apache.org/server/3/monitor.html

  * Upgrade from James 3.0-M2
      For JPA Mailbox, database table names have changes.
        You have to manually migrate the database...
          or use the mailbox-copier to backup to maildir, recreate database from scratch, and recopy from maildir

  * Develop      http://james.apache.org/server/3/dev.html

  * Feedbacks and Questions 
      Mailing lists    http://james.apache.org/mail.html
      Twitter          http://twitter.com/ApacheJames

  * Upgrade from James 2.3 http://james.apache.org/server/3/upgrade-2.3.html

(asciiart on http://patorjk.com/software/taag/ with Ogre font)
