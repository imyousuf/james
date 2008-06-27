OVERVIEW

This is the alpha IMAP server implementation that was in the main branch.  I've (SK) moved it
to the proposal directory, but am not 100% sure if it will run (since it was alpha quality,
I'm not sure what should be working).  Hopefully someone can come and make this run great! 
We miss you Charles!


BUILD:
- build -buildfile proposals\imap\build-imap.xml

CONFIGURATION:
- see description in config.xml -> block <imapserver>

TEST:
1) You need to make sure both junit.jar and ant's optional.jar are available. You can add them to the tools/lib directory to ensure this.


2) Generate a clean distribution. The easiest way to get this is to run the "build-removeDist" target, which compiles the server files, and regenerates the dist directory.

3) Start James. ( dist/bin/run )

4) Now run the tests
    build -buildfile proposals\imap\build-test.xml

    a) For complete tests run target: "testimap" 

    b) To run tests individually run targets:
        testimap-init 
            - initialises the server for other tests.
	    - adds a user, "imapuser"
            - sends 4 test messages, using JavaMail
            - can only run this once on a clean distribution (otherwise username clashes)

        testimap-nonauthenticated
            - tests commands in NONAUTHENTICATED state (no initial login)

	testimap-authenticated
            - tests commands in AUTHENTICATED state (initial login)

        testimap-selected
            - tests commands in SELECTED state (mailbox selected)
            - these tests currently fail (I think)


