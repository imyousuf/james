@echo off

set JAMES_CLASSPATH=avalon-loader.jar;../lib/xerces.jar;../lib/mail_1_2.jar;../lib/activation.jar;../lib/dnsjava.jar;../lib/town.jar
java -cp %CLASSPATH%;%JAMES_CLASSPATH% %AVALON_JVM_FLAGS% AvalonLoader %1 %2 %3 %4 %5 %6 %7 %8 %9
