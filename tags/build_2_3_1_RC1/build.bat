@echo off

echo.
echo James Build System
echo -------------------

set OLD_ANT_HOME=%ANT_HOME%
set ANT_HOME=tools

set OLD_CLASSPATH=%CLASSPATH%
set CLASSPATH=phoenix-bin\lib\xercesImpl-2.0.2.jar;phoenix-bin\lib\xml-apis.jar;tools\lib\velocity-1.3-dev.jar;tools\lib\jdom-b7.jar

%ANT_HOME%\bin\ant.bat -emacs %1 %2 %3 %4 %5 %6 %7 %8
goto cleanup

:cleanup
set ANT_HOME=%OLD_ANT_HOME%
set CLASSPATH=%OLD_CLASSPATH%
