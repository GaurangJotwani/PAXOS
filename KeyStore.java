/*
 * KeyStore.java
 *
 * This file defines the KeyStore interface, which extends the Acceptor, Learner, and Proposer
 * interfaces, providing a comprehensive set of methods for interacting with the Paxos-based
 * distributed key-value store. It also includes a method to receive pings from remote servers
 * for availability testing.
 *
 * Author: Gaurang Jotwani
 * Course: NEU Summer 23 CS 6650
 * Date: 08/07/2023
 */
import java.rmi.Remote;
import java.rmi.RemoteException;
public interface KeyStore extends Remote, Acceptor, Learner, Proposer {
  /**
   * Receives a ping from a remote server
   * Used for testing if the server is available.
   * @return The response to the ping.
   * @throws RemoteException If a remote communication error occurs.
   */
  String recievePing() throws RemoteException;

}