/*
 * Proposer.java
 *
 * This file defines the Proposer interface, which outlines the methods for initiating
 * DELETE, PUT, and GET operations as part of the Paxos consensus process. Classes implementing
 * this interface will provide the functionality to propose operations to be executed in the
 * distributed system.
 *
 * Author: Gaurang Jotwani
 * Course: NEU Summer 23 CS 6650
 * Date: 08/07/2023
 */

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Proposer extends Remote {

  /**
   * Initiates a DELETE operation to remove a key-value pair from the cache.
   *
   * @param key The key of the entry to be deleted.
   * @throws RemoteException If a remote communication error occurs.
   */
  public void initiateDelete(String key) throws RemoteException;

  /**
   * Initiates a PUT operation for storing a key-value pair in the cache.
   *
   * @param key The key to be associated with the value.
   * @param val The value to be stored.
   * @throws RemoteException If a remote communication error occurs.
   */
  public void initiatePut(String key, String val) throws RemoteException;

  /**
   * Initiates a GET operation to retrieve the value associated with a key from the cache.
   *
   * @param key The key of the entry to be retrieved.
   * @return The value associated with the given key.
   * @throws RemoteException If a remote communication error occurs.
   */
  String initiateGet(String key) throws RemoteException;

}
