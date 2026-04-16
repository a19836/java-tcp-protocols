# Java TCP Protocols

> Original Repo: https://github.com/a19836/java-tcp-protocols/ 

## Overview

**Java TCP Protocols** is a Java library that simulates some behaviors of different TCP communication patterns between client and server.
This library contains some examples and code basis, so developers can understand how protocols work and start creating their own protocols.

Main Idea: 
- Client that reads user input and sends message to server. Then reads and prints html from server.
- Server that reads client messages and sends them back in a HTML format.

## Created Scenarios:

### Simple Get Server/Client: 
Same idea described above, but client creates a server socket for each message sent.
This is similar with the HTTP protocol, but without using the HTTP protocol - only using TCP protocol.

### Simple Post Server/Client: 
Same idea described above, but client creates a server socket for each message sent and sends also some content in the body, just like a HTTP POST request, but without using the HTTP protocol.

### HTTP 2.0 Server/Client: 
Same idea described above, but clients reuses the previous server reconnection, if possible. Also it reconnects automatically when connections reaches to timeout and is killed automatically. 
This is a very simple simulation of the HTTP 2.0 protocol in the browsers.

### String Protocol
Client sends an ACTION and a MESSAGE to be executed from the server side. The ACTION must be pre-defined in the server side and the client simple asks server to execute them with different payloads.
Basically some logic than gRPC but instead of using binary transfer, it uses simple string transfers with ACTION and MESSAGE strings.

### Binary Serialized Object gRPC Protocol
Simulate the gRPC protocol in a very simple way. Only the basics of the gRPC protocol, where client transfers an object containing a method with params to be called in the server side.

### Binary Serialized Object Protocol
Client serializes the EchoTask object in binary and sends it over the network. Server deserializes the binary payload, converts it to a RemoteExecutable object, and calls the execute method. This uses Java object serialization for the full object transfer.
This has some similarities with the gRPC protocol without using HTTP protocol and without using the ProtoBuf library and the correspondent .proto files. Here we transfer the objects by serializing them.
Client and Server share the RemoteExecutable and EchoTask classes.

### Binary Manual Properties Object Protocol
Because is not possible passing objects throught the network without being serialized, we only transfer the object properties and then create a new object in the server side, this is, client creates a object but only sends its properties to server, in binary form. This is faster than sending the entire object serialized.
Then server, receives these properties and creates a new object from the server side, executing it. 
This has some similarities with the gRPC protocol without using HTTP protocol and without using the ProtoBuf library and the correspondent .proto files. Here we transfer only the objects properties manually that matter - via binary form through the network.
Client and Server share the BinaryTaskWire and EchoTask classes.

## Run:
From the `java` folder (so `protocol/` is on the classpath for object protocol):
```java
javac */*.java
```

Server (pick one), from `java` folder:

```java
java httpprotocol.Server
# or: java simpleget.Server
# or: java simplepost.Server
# or: java stringprotocol.Server
# or: java binaryserializedobjectgrpcprotocol.Server
# or: java binaryserializedobjectprotocol.Server
# or: java binarymanualpropertiesobjectprotocol.Server
```

Client, from `java` folder:

```java
java httpprotocol.Client
# or: java simpleget.Client
# or: java simplepost.Client
# or: java stringprotocol.Client
# or: java binaryserializedobjectgrpcprotocol.Client
# or: java binaryserializedobjectprotocol.Client
# or: java binarymanualpropertiesobjectprotocol.Client
```

Note that the `binaryserializedobjectprotocol.Client`, `binaryserializedobjectprotocol.Server`, `binarymanualpropertiesobjectprotocol.Client` and `binarymanualpropertiesobjectprotocol.Server` classes need the parent directory on the classpath so `protocol.*` loads. If you prefer `cd binaryserializedobjectprotocol` or `cd binarymanualpropertiesobjectprotocol`, use:
```java
java -cp .:.. Server
java -cp .:.. Client
```
