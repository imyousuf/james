@echo off
echo Avalon runner
set LOCAL_CLASSPATH=Loader.jar;../lib/AvalonAware.jar;../lib/xerces.jar;../lib/mail_1_2.jar;../lib/activation.jar
java -cp "%CLASSPATH%;%LOCAL_CLASSPATH%" org.apache.avalon.loader.Main