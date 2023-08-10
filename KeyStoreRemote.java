/*
 * KeyStoreRemote.java
 *
 * This file implements the KeyStore interface, providing the functionality to store, retrieve,
 * and delete key-value pairs in the Key Store. It uses a HashMap as the underlying data structure
 * and ensures thread safety by utilizing a ReentrantLock for mutual exclusion.
 *
 * Author: Gaurang Jotwani
 * Course: NEU Summer 23 CS 6650
 * Date: 08/07/2023
 */

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.rmi.server.RemoteServer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class KeyStoreRemote extends UnicastRemoteObject implements KeyStore {

  // Shared key-value store to store the key-value pairs
  private HashMap<String, String> keyValStore = new HashMap<>();
// Locks for ensuring thread safety during concurrent operations
  private Lock executorLock = new ReentrantLock(); // Lock for when the node is acting as a learner
  private Lock logLock = new ReentrantLock(); // Lock for log-related operations
  private Lock proposerLock = new ReentrantLock(); // Lock for proposer and acceptor specific
  // operations

  // Server number identifying the replica in the distributed system
  private int serverNumber;
  // Port number for the replica
  private int portNum;
  // List of available server instances for distributed coordination
  private ArrayList<KeyStore> availableServers;

  // Maps for tracking Paxos-related information and responses
  private ConcurrentHashMap<Integer ,PAXOSResponse> logRequestMap; // Stores Paxos request-response mappings
  private ConcurrentHashMap<Integer , Long> PromiseRequestMap; // Tracks promise request IDs for
  // particular LOG IDs
  private ConcurrentHashMap<Integer , PAXOSResponse> AcceptedRequestMap; // Tracks accepted Paxos
  // requests for particular LOG IDs

  // Flag indicating if this instance is acting as a proposer
  private boolean isProposer;
  // Counter for the current log entry count
  int currentLogCount;
  // Port number of the first server instance
  int firstPort;
  // Unique ID generator for creating unique IDs for transactions
  private static UniqueIDGenerator tokenGenerator;

  // Constructor
  KeyStoreRemote(int serverNum, int port_num, int first_port) throws RemoteException {
    super();
    // Initialize properties and data structures
    serverNumber = serverNum;
    portNum = port_num;
    firstPort = first_port;
    currentLogCount = 0;
    availableServers = new ArrayList<>();
    logRequestMap = new ConcurrentHashMap<>();
    PromiseRequestMap = new ConcurrentHashMap<>();
    AcceptedRequestMap = new ConcurrentHashMap<>();
    Registry registry = LocateRegistry.getRegistry("127.0.0.1", 8767);
    isProposer = false;
    try {
      // Making RMI connection to the token generator object
      tokenGenerator = (UniqueIDGenerator) registry.lookup("tokenGenerator");
    } catch (NotBoundException e) {
      throw new RuntimeException(e);
    }
    // Set RMI system properties - Timeout value set for 20 seconds.
    // Long time is set because the cache may be outdated and need time ot sync
    System.setProperty("sun.rmi.transport.tcp.responseTimeout", "20000");
    System.setProperty("sun.rmi.transport.tcp.readTimeout", "20000");
  }

  public void initiatePut(String key, String val) throws RemoteException {
    proposerLock.lock(); // Acquire the proposer lock to ensure mutual exclusion
    isProposer = true; // Mark this server as the proposer for this operation
    updateServers(); // Update the list of available servers for distributed coordination

    print("Received PUT " +
            "Request to PUT key \"" + key +"\" with val \"" + val);
    while (true) {
      int clog = currentLogCount + 1; // Increment the log ID for this operation
      // Create a Paxos request for the PUT operation
      PAXOSResponse paxosRequest = new PAXOSResponse(key, -1, "PUT", val, clog,
              System.nanoTime());
      // Start the Paxos consensus process
      PAXOSResponse reply = startPaxosRun(paxosRequest);
      // Execute the Paxos request
      executePaxosRequest(reply, this.serverNumber);
      // Check if the consensus was achieved for this request
      if (paxosRequest.uniqueId == reply.uniqueId) {
        // Inform learners to execute the request if the consensus was reached
        informLearners(reply); // Reset the proposer flag
        isProposer = false; // Release the proposer lock
        proposerLock.unlock();
        return;
      }
      /*
       If consensus is not achieved, the loop continues, and the process repeats. This can happen
        if the local cache is behind other caches and consensus for the requested logID was
        completed already.
       */
    }
  }

  public void initiateDelete(String key) throws RemoteException {
    proposerLock.lock(); // Acquire the proposer lock to ensure mutual exclusion
    isProposer = true; // Mark this server as the proposer for this operation
    updateServers(); // Update the list of available servers for distributed coordination

    print("Received DELETE Request to DELETE key \"" + key);
    while (true) {
      int clog = currentLogCount + 1;
      // Create a Paxos request for the DELETE operation
      PAXOSResponse paxosRequest = new PAXOSResponse(key, -1, "DELETE", null, clog
              , System.nanoTime());
      PAXOSResponse reply = startPaxosRun(paxosRequest);
      String status = executePaxosRequest(reply, this.serverNumber);
      // Check if the consensus was achieved for this request
      if (paxosRequest.uniqueId == reply.uniqueId) {
        informLearners(reply);
        if (status == "FAIL") {
          isProposer = false;
          proposerLock.unlock();
          throw new IllegalArgumentException("Key not found in cache! Illegal Operation!");
        }
        isProposer = false;
        proposerLock.unlock();
        return;
      }
        /*
       If consensus is not achieved, the loop continues, and the process repeats. This can happen
        if the local cache is behind other caches and consensus for the requested logID was
        completed already.
       */
    }
  }

  public String initiateGet(String key) throws RemoteException {
    proposerLock.lock(); // Acquire the proposer lock to ensure mutual exclusion
    isProposer = true; // Mark this server as the proposer for this operation
    updateServers(); // Update the list of available servers for distributed coordination
    print("Received GET Request to GET key \"" + key);
    while (true) {
      int clog = currentLogCount + 1;
      // Create a Paxos request for the GET operation
      PAXOSResponse paxosRequest = new PAXOSResponse(key, -1, "GET", null, clog,
              System.nanoTime());
      PAXOSResponse reply = startPaxosRun(paxosRequest);
      String status = executePaxosRequest(reply, this.serverNumber);
      // Check if the consensus was achieved for this request
      if (paxosRequest.uniqueId == reply.uniqueId) {
        informLearners(reply);
        if (status == "FAIL") {
          isProposer = false;
          proposerLock.unlock();
          throw new IllegalArgumentException("Key not found in cache! Illegal" +
                  " Operation!");
        }
        isProposer = false;
        proposerLock.unlock();
        return status;
      }
      /*
       If consensus is not achieved, the loop continues, and the process repeats. This can happen
        if the local cache is behind other caches and consensus for the requested logID was
        completed already.
       */
    }
  }

  /**
   * Initiates the Paxos algorithm for a given request to achieve consensus.
   *
   * @param request The Paxos request to be processed.
   * @return The response indicating consensus decision.
   * @throws RemoteException If a remote communication error occurs.
   */
  private PAXOSResponse startPaxosRun(PAXOSResponse request) throws RemoteException {
    print("Initiating Prepare Paxos Request for Log ID: " + request.logId);
    PAXOSResponse returnResponse;
    // Continuously loop until a consensus is reached or timeout
    int time = 50;
    while (true) {
      try {
        // Introduce a delay between retries using an exponential backoff strategy
        Thread.sleep(time);
        time = time * 2;
        // If the delay exceeds a certain limit, return null (timeout)
        if (time > 5000) return null;
      } catch (InterruptedException e) {
        // Handle thread interruption by setting proposer state and releasing lock
        isProposer = false;
        proposerLock.unlock();
        throw new RuntimeException(e);
      }
      // Clone the original request for modification
      returnResponse = request;
      Integer nullCounter = 0;
      // Generate a unique identifier for the current Paxos round
      returnResponse.id = tokenGenerator.getToken();
      List<PAXOSResponse> promises = processConcurrentMessages(returnResponse, "PROMISE");
      // Continue if the number of received 'PROMISE' messages is insufficient. Majority not
      // reached. For 9 servers, at least 5 need to respond.
      if (promises.size() < 5) continue;
      // Determine the minimum number of responses required for consensus
      Integer minResponses = (int) Math.ceil(promises.size()/2);
      // Initialize variables for tracking the largest promise and corresponding indices
      PAXOSResponse largest = new PAXOSResponse();
      largest.id = -1;
      ArrayList<Integer> indices = new ArrayList<>();
      int idx = -1;
      // Loop through received promises to find the largest promise
      for(PAXOSResponse each : promises) {
        idx += 1;
        if (each == null) {
          nullCounter += 1;
          continue;
        } else if (each.id > largest.id) {
          largest = each;
        }
        // Store the index of the promise for later use
        indices.add(idx);
      }
      if(nullCounter >= minResponses) {
        print("----------Couldnt get majority of promises. Try with higher ID--------");
        continue;
      };
      // We have a majority of promises
      print("Got majority of Promises!");
      // If the largest promise has a non-zero ID, adjust the return response ID
      // This means the requested proposal was not accepted and a consensus has already
      // been made for the given logID
      if (largest.id != 0) {
        long id = returnResponse.id;
        returnResponse = largest;
        returnResponse.id = id;
      }
      // Send accept messages to all the acceptors who took part in vote.
      List<PAXOSResponse> accepted = new ArrayList<>();
      for (int i : indices) {
        accepted.add(availableServers.get(i).accept(returnResponse));
      }
      // Determine the majority accepted response using a helper function
      PAXOSResponse majorityAccepted = calculateMostFrequentResponse(accepted);
      // If consensus cannot be reached, retry with a higher ID
      if (majorityAccepted == null) {
        print("----------Couldnt get majority of accepts. Try again with a higher ID--------");
        continue;
      }
      // Update the log with the accepted response and freeze the log
      logRequestMap.put(request.logId, majorityAccepted);
      print("Got majority of Accepts! Consensus Made!");
      return majorityAccepted;
    }
  }

  public PAXOSResponse promise(PAXOSResponse request) throws RemoteException {
    // Randomly simulate a failure with 5% probability
    if (Math.random() < .05) {
      print("------------------------------\n\n\n\nrandom failure " +
              "occured in acceptor while promising\n\n\n\n------------------------------");
      return null;
    }
    // Check if the proposer is currently the acceptor (prevent self-promise)
    // Node cannot be acceptor and proposer at the same time.
    if (isProposer) return null;
    // Acquire the proposer lock for mutual exclusion
    proposerLock.lock();
    isProposer = false;
    // If the request's log ID is not in the AcceptedRequestMap, add a dummy entry with ID 0
    if (!AcceptedRequestMap.containsKey(request.logId)) {
      PAXOSResponse dummy = new PAXOSResponse();
      dummy.id = 0;
      AcceptedRequestMap.put(request.logId, dummy);
    }
    // If the request's log ID is not in the PromiseRequestMap, add an entry with 0 as ID
    if(!PromiseRequestMap.containsKey(request.logId)) {
      PromiseRequestMap.put(request.logId, 0L);
    }
    // If the request's ID is greater than the stored promise ID, update and respond with 'ACCEPTED'
    if (request.id > PromiseRequestMap.get(request.logId)) {
      PromiseRequestMap.put(request.logId, request.id);
      proposerLock.unlock();
      print("Promise Given for logID:" + request.logId + " and ID: " + request.id);
      return AcceptedRequestMap.get(request.logId);
    }
    print("Ignoring Promise Request for log: " + request.logId + " and ID: " + request.id);
    proposerLock.unlock();
    return null;
  }

  public PAXOSResponse accept(PAXOSResponse request) throws RemoteException, IllegalArgumentException {
    // Randomly simulate a failure with 5% probability
    if (Math.random() < .05){
      print("------------------------------\n\n\n\nrandom failure " +
              "occured in acceptor while accepting\n\n\n\n------------------------------");
      return null;
    }
    // Check if the proposer is currently the acceptor (prevent self-promise)
    // Node cannot be acceptor and proposer at the same time.
    if (isProposer) return null;
    // Acquire the proposer lock for mutual exclusion
    proposerLock.lock();
    isProposer = false;
    // If the request's ID matches the promise ID, update AcceptedRequestMap and respond with 'ACCEPTED'
    // This is the highest ID that is seen.
    if (request.id == PromiseRequestMap.get(request.logId)) {
      AcceptedRequestMap.put(request.logId, request);
      proposerLock.unlock();
      print("Sending Accept Request for LogID: " + request.logId  + " and ID: " + request.id);
      return request;
    } else {
      // Request's ID doesn't match promise ID, reject the request
      proposerLock.unlock();
      print("Ignoring Accept Request for log: " + request.logId  + " and ID: " + request.id);
      return null;
    }
  }

  /**
   * Informs all learners to execute the Paxos request after consensus is reached.
   *
   * @param request The Paxos request to be executed.
   * @throws RemoteException If a remote communication error occurs.
   */
  private void informLearners(PAXOSResponse request) throws RemoteException {
    print("Informing all learners to Execute the request...");
    processConcurrentMessages(request, "EXECUTE");
  }
  public String executePaxosRequest(PAXOSResponse request, int serverNumber) throws RemoteException {
    // Simulate a random failure with 5% probability, unless the failure is on the same node
    if(Math.random() < .05 && this.serverNumber != serverNumber){
      print("------------------------------\n\n\n\nrandom failure " +
              "occured in Learner node.\n\n\n\n------------------------------");
      return null;
    }
    // Acquire the executor and log locks for mutual exclusion
    executorLock.lock();
    logLock.lock();
    print("Executing Paxos Request on key " + request.logId);
    // If consensus already reached for this log ID, skip execution. This is a duplicate request
    if (request.logId <= currentLogCount) {
      print("Already made consensus on this log ID. Not executing");
      logLock.unlock();
      executorLock.unlock();
      return null;
    };
    // Checking if the cache is currently stale (behind in LOG ID).
    // If it is, it and may ignore the request.
    // However, the cache will become in sync when it acts as a proposer
    // during client engagement, ensuring it receives the missed request later.
    // This is how the Learner errors will be handled by the nodes.
    if (request.logId != currentLogCount + 1) {
      print("Node is not execute to accept at this point as it is waiting for previous updates.");
      logLock.unlock();
      executorLock.unlock();
      return null;
    };
    print("Consensus made on Log ID: " + request.logId);
    currentLogCount = request.logId;
    logRequestMap.put(request.logId, request);
    logLock.unlock();
    // If the request is from a different server and it's a GET request, skip execution
    if (this.serverNumber != serverNumber & request.type == "GET") {
      executorLock.unlock();
      return null;
    }
    // Execute the operation based on the request type
    switch (request.type) {
      case "PUT":
        commitPut(request.key, request.value);
        break;
      case "DELETE":
        executorLock.unlock();
        return commitDelete(request.key);
      case "GET":
        executorLock.unlock();
        return get(request.key);
    }
    executorLock.unlock();
    return null;
  }

  /**
   * Commits a PUT operation by storing the key-value pair in the Key Store.
   *
   * @param key The key to be associated with the value.
   * @param val The value to be stored.
   */
  private void commitPut(String key, String val) {
    // Commits the PUT operation for the given key-value pair in the Key Store.
    // Store the key-value pair in the key-value store
    keyValStore.put(key, val);
    print(key + " with value \"" + val + "\" saved successfully");
  }

  /**
   * Commits a DELETE operation by removing a key-value pair from the Key Store.
   *
   * @param key The key of the entry to be deleted.
   * @return "PASS" if successful, "FAIL" if the key is not found.
   */
  private String commitDelete(String key) {
    // Commits the DELETE operation for the given key in the Key Store
    if (!keyValStore.containsKey(key)) {
      // Key not found in the key-value store. Do not proceed with DELETE
      print("Invalid Key Provided. DELETE Request FAILED");
      return "FAIL";
    }
    // Remove the key-value pair from the key-value store
    String val = keyValStore.remove(key);
    print("Successfully deleted key \"" + key +"\" with val \"" + val + "\"");
    return "PASS";
  }

  /**
   * Retrieves the value associated with a key from the Key Store.
   *
   * @param key The key of the entry to be retrieved.
   * @return The value associated with the given key, or "FAIL" if key is not found.
   */
  private String get(String key) {
    // Log the GET request
    print("Received GET Request to read key \"" + key +"\"");
    if (keyValStore.containsKey(key)) {
      // Get the value associated with the key from the key-value store
      String val = keyValStore.get(key);
      print("Successfully read key \"" + key +"\" with val \"" + val + "\"");
      return val;
    }
    print("Invalid Key Provided. GET Request FAILED");
    return "FAIL";
  }

  /**
   * Generates a log header with timestamp and client IP address.
   * @param ip The client IP address.
   * @return The log header string.
   */
  private String getLogHeader(String ip) {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    return "[" + timestamp.toString() + " ,Sever Port: " + portNum + " Server #: " + this.serverNumber +
            " Client IP: " + ip + "]  ";
  }

  /**
   * Retrieves the client host IP address.
   * @return The client host IP address.
   */
  public static String getClientHost() {
    String clientHost = null;
    try {
      clientHost = RemoteServer.getClientHost();
    } catch (ServerNotActiveException e) {
      throw new RuntimeException(e);
    }
    return clientHost;
  }

  /**
   * Prints a log message with timestamp, server port, server number, client IP, and message.
   *
   * @param message The message to be logged.
   */
  private void print(String message) {
    String clientHost = getClientHost();
    System.out.println(getLogHeader(clientHost) + message);
  }

  /**
   * Receives a ping from a remote server to check availability.
   *
   * @return "PASS" if the server is responsive.
   * @throws RemoteException If a remote communication error occurs.
   */
  public String recievePing() throws RemoteException {
    return "PASS";
  }

  /**
   * Sends a ping to another server to check its availability.
   *
   * @param toServer The server to ping.
   * @return true if the server responds, false otherwise.
   * @throws RemoteException If a remote communication error occurs.
   */
  private boolean sendPing(KeyStore toServer) throws RemoteException {
    try {
      if (toServer.recievePing().equals("PASS")) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Updates the list of available servers by pinging and identifying responsive ones.
   *
   * @return The number of available servers.
   * @throws RemoteException If a remote communication error occurs.
   */
  private int updateServers() throws RemoteException {
    availableServers.clear();
    int size;
    // Assuming 9 Servers
    for (int i = 0; i < 9; i++) {
      if (i == this.serverNumber) continue;
      Integer port = firstPort + i;
      try {
        Registry registry = LocateRegistry.getRegistry("127.0.0.1", port);
        KeyStore participant = (KeyStore) registry.lookup("keyStore");
        if (sendPing(participant)) {
          availableServers.add(participant);
        } else {
          print("A server failed on port: " + port);
        }
      } catch (Exception e) {
        // Go to next server is this is not connecting.
        continue;
      }
    }
    size = availableServers.size();
    return size;
  }

  /**
   * Processes Paxos messages concurrently to multiple servers and returns the responses.
   *
   * @param input     The Paxos request to send.
   * @param operation The type of Paxos operation: "PROMISE", "ACCEPT", or "EXECUTE".
   * @return List of responses from the servers.
   * @throws RemoteException If a remote communication error occurs.
   */
  private List<PAXOSResponse> processConcurrentMessages(PAXOSResponse input, String operation) throws RemoteException {
    ExecutorService executorService = Executors.newFixedThreadPool(5);
    List<Future<PAXOSResponse>> responses = new ArrayList<>();
    for (KeyStore cache : availableServers) {
      PAXOSResponse finalReturnResponse = input;
      Callable<PAXOSResponse> task = () -> {
        // Send the message to the cache and get the response
        if (operation == "PROMISE") {
          try {
            return cache.promise(finalReturnResponse);
          } catch (Exception e) {
            return null;
          }
        } else if (operation == "ACCEPT") {
          try {
            return cache.accept(finalReturnResponse);
          } catch (Exception e) {
            return null;
          }
        } else {
          cache.executePaxosRequest(finalReturnResponse, this.serverNumber);
          return null;
        }
      };
      responses.add(executorService.submit(task));
    }
    executorService.shutdown();
    List<PAXOSResponse> responses2 = new ArrayList<>();
    for (Future<PAXOSResponse> future : responses) {
      try {
        PAXOSResponse response = future.get();
        responses2.add(response);
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return responses2;
  }

  /**
   * Calculates the most frequent response from a list of Paxos responses.
   *
   * @param responses List of Paxos responses.
   * @return The most frequent response or null.
   */
  public PAXOSResponse calculateMostFrequentResponse(List<PAXOSResponse> responses) {
    // Create a HashMap to count the occurrences of each non-null log ID
    Map<Integer, Integer> logIdOccurrences = new HashMap<>();
    int mostFrequency = -1;
    // Count the occurrences of each non-null log ID in the responses
    for (PAXOSResponse response : responses) {
      if (response != null) {
        if (!logIdOccurrences.containsKey((int) response.id)) {
          logIdOccurrences.put((int) response.id, 1);
        } else {
          int temp = logIdOccurrences.get((int) response.id) + 1;
          logIdOccurrences.put((int) response.id, temp);
        }
        mostFrequency = Math.max(mostFrequency, logIdOccurrences.get((int) response.id));
      }
    }
    // Find the Response object with the most frequent log ID (ignoring null values)
    PAXOSResponse mostFrequentResponse = null;
    for (PAXOSResponse response : responses) {
      if (response != null && logIdOccurrences.get((int) response.id) == mostFrequency) {
        mostFrequentResponse = response;
        // Check if enough responses are there. Majority needs to accept for a consensus.
        if (logIdOccurrences.get((int) response.id) < 5) {
          return null;
        }
        break;
      }
    }
    return mostFrequentResponse;
  }
}


