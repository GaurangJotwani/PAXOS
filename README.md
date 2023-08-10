# Distributed Key-Value Store with Paxos Consensus Algorithm

## Overview
This project implements a distributed key-value store using the Paxos consensus algorithm for achieving fault-tolerant distributed transactions. The system consists of multiple server instances that coordinate to maintain a consistent key-value store across the distributed environment. The Paxos algorithm ensures consensus on operations such as PUT, GET, and DELETE, even in the presence of node failures. Learner and Acceptor nodes are simulated to fail at a rate of 5%

## Assumptions

The following assumptions were made in the implementation of this project:

- The maximum message size for a command or value is limited to 1024 bytes.
- The server does not persist the key-value store in a database or file. If the server is restarted, all stored data will be lost.
- The key and val stored in the cache will be string type
- The node will not fail when it is acting as a proposer
- The node will fail 5 percent of the times when it is acting as an acceptor or a learner
- There will be 9 replicas created
- No more than 3 proposers can concurrently pass messages. It may lead to a deadlock otherwise.
- It is assumed that the Key-Value Store server application is running on a local host

## Compilation and Running

To compile the project, follow these steps:

1. Ensure you have Java Development Kit (JDK) installed on your system.
2. Open a terminal or command prompt.
3. Navigate to the project directory.
4. Compile the source files using the following command:


    javac *.java

5. To run the server, use the following command


    java MyServer <port>

Replace `<port>` with the desired port number to start the server on. This will start 5 servers on ports: <port>, <port + 1>, <port + 2>, <port + 3>, <port + 4> 

7. To run the client, use the following command


    java MyClient <ip> <port>


Replace `<ip>` with the IP address or hostname of the server, and `<port>` with the port number the server is running on. The client can connect to any of the 9 servers on ports: <port>, <port + 1>, <port + 2>,.....,<port + 9>.

## Port Number Assignment for Replicas
The port numbers for the nine replica servers are determined based on the provided first_port argument when starting the Key-Value Store server. The first replica server will use the first_port, the second replica will use first_port + 1, and so on until the ninth replica, which will use first_port + 8. The ID generator is bound to port 8767 to manage unique increasing IDs required for PAXOS algorithm.


## Sending Commands to the Server

Once the server and client are running, you can send commands to the server using the client. The following commands are supported:

- **GET**: Retrieve the value associated with a specific key.

`GET <key>`

The server will respond with the value associated with the key, or an error message if the key does not exist. Server will start the message with "1" if request is successful otherwise it's an error.

- **PUT**: Store a key-value pair in the server's key-value store.

`PUT <key> <val>`

The server will respond with a success message if the key-value pair is stored successfully. Server will start the message with "1" if request is successful otherwise it's an error.

- **DELETE**: Remove a key-value pair from the server's key-value store.

`DELETE <key>`

The server will respond with a success message if the key is found and removed, or an error message if the key does not exist. Server will start the message with "1" if request is successful otherwise it's an error.

- **QUIT**: Close the connection to the server and terminate the client.

`QUIT`

## PAXOS Algorithm

### Paxos Implementation for Consensus
In a distributed environment, achieving consensus among multiple nodes on a series of instructions is a fundamental challenge. The Paxos consensus algorithm is used to solve this problem by ensuring that nodes agree on a common value even if some nodes are faulty or messages are lost. In the context of your project, Paxos is employed to achieve consensus on the operations (PUT, GET, DELETE) that need to be executed at each log ID in the distributed key-value store.

1. **Prepare Phase**: The Paxos process begins with the prepare phase, initiated by the Proposer. In the startPaxosRun method, the system tries to achieve consensus by proposing a value for the current log ID. The Proposer generates a unique ID (returnResponse.id) and sends a prepare request to the Acceptors (other replicas) to gather promises.

2. **Promise Phase** - Each Acceptor checks if it has already promised to another proposal with a higher unique ID. If not, it sends a promise to the Proposer. The Proposer collects these promises and evaluates if a majority (quorum) of Acceptors have agreed to the proposal.

3. **Accept Phase**:  If a quorum is achieved, the Proposer proceeds to the accept phase. It sends an accept request to the Acceptors, including the proposed value for the current log ID. Acceptors check if they have already accepted another proposal with a higher unique ID. If not, they accept the proposal and send an acknowledgment.
4. **Learning Phase**: Once a quorum of Acceptors accepts the proposal, the consensus is considered achieved. The Learner component learns the accepted value and notifies other components that the operation for the current log ID can be executed.
---

## Simulating Failures and Recovery Process - Acceptor Nodes

### Failure Simulation
The project incorporates a simulated failure scenario to mimic real-world distributed system behavior. In the Paxos consensus algorithm, the acceptor nodes may fail with a 5% probability during both the promising and accepting phases. This introduces a degree of uncertainty and unpredictability into the system, enabling us to test the robustness and fault tolerance of the distributed key-value store.

### Recovery Process
Despite the simulated failures, the Paxos algorithm is designed to tolerate and recover from such incidents. The project is equipped with a recovery process that ensures the system can still reach consensus even in the presence of failing acceptor nodes. Paxos continues as long as a majority of nodes (i.e., acceptors) agree on a proposal, even if a minority of nodes experience failures.

### Replica Count and Deadlocks
To mitigate the impact of failures and reduce the probability of deadlocks, the system employs nine replicas. This redundancy enhances the system's fault tolerance, as the majority of nodes needed for consensus can still be achieved even if some replicas fail.

### Proposers and System Load
The system's scalability and stability are maintained by limiting the number of proposers. It is recommended to have no more than three proposers at any given time. This limitation prevents an excessive number of concurrent operations and proposals, reducing the risk of contention and ensuring a smooth flow of the Paxos algorithm.

By simulating failures and designing a recovery process, the project offers insights into the resiliency and reliability of the distributed key-value store, even in the face of adverse conditions.

## Learner Nodes: Failures and Recovery

### Simulating Learner Node Failures
The project incorporates a simulation of failures in the Learner nodes. Similar to the acceptor nodes, the Learner nodes may fail with a 5% probability. When a Learner node fails, it implies that the cache and log ID for that node will not be updated. Consequently, the cache on the failed Learner node may become less updated compared to other nodes in the system.

### Impact of Learner Node Failures
Due to the failure of a Learner node, the system might experience a temporary inconsistency between nodes. The Learner node, after encountering a failure, may not process incoming requests and, as a result, ignore requests, leading to a potential data disparity.

### Recovery Process for Learner Nodes
When a new request comes from another server with a higher log ID, the stale cache might ignore the request. This is because the node is not ready to accept operations for the new log ID until it acts as a proposer itself. When the node eventually acts as a proposer for a client engagement, it synchronizes its state and cache, becoming ready to accept the new request at the higher log ID.

The process of becoming a proposer for a client request helps refresh the cache and ensure that the node is up-to-date with the latest operations.
### Paxos Algorithm and Consistency
Overall, the Paxos algorithm guarantees that a consensus decision is reached among the distributed nodes, ensuring that the correct operations are executed at each log ID in the key-value store, even in the presence of failures and stale caches.
By simulating Learner node failures and leveraging the inherent capabilities of the Paxos algorithm, the project demonstrates how the system can recover from temporary inconsistencies and maintain its integrity in the face of node failures.

## Other Error Handling

The KeyStore application incorporates error handling to provide robustness and graceful handling of various exceptional scenarios. The server implements error handling for various scenarios, such as invalid keys provided by clients, server unavailability during two-phase commit, and communication failures. Error messages are printed to the console to indicate the cause of the error, and appropriate actions are taken to recover from errors when possible. The following types of errors are taken care of:

- **RemoteException**: This exception can occur during remote method invocations and signifies a failure in the RMI communication. The application handles RemoteExceptions appropriately and provides meaningful error messages to the user.

- **MalformedURLException**: This exception can occur when the URL for the RMI registry is malformed or invalid. The application catches this exception and displays an informative error message, allowing the user to retry with a correct URL.

- **UnknownHostException**: This exception can occur when the server's IP address or hostname is not recognized. The application handles this exception and notifies the user about the unknown host error.

- **AccessException**: This exception can occur when there is a problem accessing the remote host. The application catches AccessExceptions and displays an appropriate error message to guide the user.

- **NotBoundException**: This exception can occur when the requested remote object is not found in the RMI registry. The application handles this exception and informs the user about the unavailability of the desired remote object.

- **ConnectException**: This exception can occur when the client fails to establish a connection with the server. The application catches ConnectExceptions and provides a user-friendly error message, indicating a timeout or unresponsive server.
- **KeyLength Error**: If length of key or value is more than 1024 bytes, it will ask the user to input it again.
- **User Errors**: If there are user errors like invalid keys or invalid commands, the server will respond with a RemoteException.



By handling these exceptions, the KeyStore application enhances its robustness and provides users with clear feedback in case of errors. The error handling mechanism ensures that the application gracefully handles exceptional situations and guides users in resolving any connection or communication issues.




