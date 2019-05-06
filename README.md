gRPC test exercise
========================================

I built it on a JDK 8.
Usage:
```
mvn install
```
To run JUnit 5 test from console you should follow https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher

IMHO you'd better not to use console, just open project via your IDE from maven model, and run tests there

To run server (actually you shouldn't) use 
```
java -cp target/gprc-for-loopme-1.0-SNAPSHOT-jar-with-dependencies.jar org.fchuiko.grpc.WebResourceCheckerServer
```
To run client (you shouldn't do it as well) 
```
java -cp target/gprc-for-loopme-1.0-SNAPSHOT-jar-with-dependencies.jar org.fchuiko.grpc.WebResourceCheckerClient
```
Thats it.