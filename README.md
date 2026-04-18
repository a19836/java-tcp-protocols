# Java TCP Protocols

> Original Repo: https://github.com/a19836/java-tcp-protocols/ 

## Overview

**Java TCP Protocols** is a Java library that simulates some behaviors of different TCP communication patterns between client and server.
This library contains some examples and code basis, so developers can understand how protocols work and start creating their own protocols.

The idea is to explain how to connect 2 end-points with TCP sockets - through the ServerSocket and Socket (Client) classes - and then transfer data between these points - through the BufferedReader, OutputWriter, InputStream, OutputStream, DataInputStream, DataOutputStream, ObjectInputStream, ObjectOutputStream...

To do this, we will create a client and server class to connect to each other, this is: 
- Client that reads user input and sends message to server. Then reads and prints html from server.
- Server that reads client messages and sends them back in a HTML format.

## Created Scenarios:

### Simple Get Server/Client: 
The idea is the same as described above, but the client creates a server socket for each message sent.
This is similar to the HTTP protocol, but without using the real HTTP 1.1 protocol - using only the TCP protocol.

### Simple Post Server/Client: 
The idea is the same as described above, but the client creates a server socket for each message sent and also sends some content in the message body, just like in an HTTP 1.1 POST request, but without using the real HTTP protocol.

### HTTP 2.0 Server/Client: 
The idea is the same as described above, but the client reuses the server's previous reconnection, if possible. In addition, it automatically reconnects when the connection times out and is automatically terminated.
This is a very simple simulation of the HTTP 2.0 protocol in browsers.

### String Protocol
The client sends an ACTION and a MESSAGE to be executed on the server. The ACTION must be predefined on the server, and the client simply requests that the server executes it with different payloads.
Basically, it uses logic similar to gRPC and REST, but instead of using binary transfer and URL, it uses simple string transfers with ACTION and MESSAGE headers.

### Binary Serialized Object gRPC Protocol
Simulate the gRPC protocol in a very simple way. Just the basic concepts of the gRPC protocol, where the client sends an object containing a method with parameters to be called on the server.

### Binary Serialized Object Protocol
The client serializes the EchoTask object into binary and sends it over the network. The server deserializes the binary payload, converts it into a RemoteExecutable object, and calls the execute method. This uses Java object serialization for the complete object transfer.
This has some similarities to the gRPC protocol, without using the HTTP protocol and without using the ProtoBuf library and the corresponding .proto files. Here, we transfer objects by serializing them.
The client and server share the RemoteExecutable and EchoTask classes.

### Binary Manual Properties Object Protocol
Since it's not possible to pass objects over the network without serializing them, we only transfer the object's properties and then create a new object on the server. That is, the client creates an object but sends only its properties to the server in binary format. This is faster than sending the entire serialized object.
Then, the server receives these properties and creates a new object on the server, executing it.
This has some similarities to the gRPC protocol, without using the HTTP protocol and without using the ProtoBuf library and the corresponding .proto files. Here, we manually transfer only the relevant properties of the object, in binary format, over the network.
The client and server share the BinaryTaskWire and EchoTask classes.

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
