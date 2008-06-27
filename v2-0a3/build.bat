@echo off

echo.
echo James Build System
echo -------------------

set ANT_HOME=tools
set CLASSPATH=lib\xerces-1.4.3.jar;tools\lib\velocity-1.3-dev.jar;tools\lib\jdom-b7.jar

%ANT_HOME%\bin\ant.bat -emacs %1 %2 %3 %4 %5 %6 %7 %8
goto cleanup

:cleanup
set ANT_HOME=
