
To run the James test - do the following:

    $ cd <james-directory>
    $ ant -buildfile james.xml clean
    $ and -buildfile james.xml

You now have a version of james built above the Avalon release candidates:

    build/lib/james-3.0.jar
    build/lib/mailet.jar

The sar generation is not complete yet. If you have Merlin installed you can run James with the following command:

    $ merlin -home tests -profile merlin\kernel.xml -system %MERLIN_HOME% 

To invoke the testcase:

    $ cd <james-dir>/tests
    $ ant -buildfile test.xml

Cheers, Steve.
