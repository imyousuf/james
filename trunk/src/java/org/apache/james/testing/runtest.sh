echo 'runs james test suite and outputs result in csv file format'
echo 'test, method, time taken, successful invocations, invocation attempt'
java org.apache.james.testing.Main testconf.xml | grep "stat: " | sed "s/.*stat: //g"
