@echo off
echo Avalon runner
set LOCAL_CLASSPATH=Loader.jar;../lib/ApacheJava.jar;../lib/AvalonInterfaces.jar;../lib/xerces_1_1_1.jar;../lib/mail_1_1_3.jar;../lib/activation.jar
java -cp "%CLASSPATH%;%LOCAL_CLASSPATH%" org.apache.avalon.loader.Main