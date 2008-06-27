@echo off
echo Avalon runner
set LOCAL_CLASSPATH=Avalon.jar;JAMES.jar;../lib/activation.jar;../lib/mail.jar;../lib/xerces_1_0_1.jar;../lib/dnsjava.jar
java -cp "%CLASSPATH%;%LOCAL_CLASSPATH%" org.apache.avalon.Main