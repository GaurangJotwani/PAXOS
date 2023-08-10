/*
 * Learner.java
 *
 * This file defines the Learner interface, which outlines the method for executing
 * a Paxos request after consensus is reached. Classes implementing this interface
 * will provide the functionality to execute the agreed-upon operation in the
 * distributed system.
 *
 * Author: Gaurang Jotwani
 * Course: NEU Summer 23 CS 6650
 * Date: 08/07/2023
 */

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Learner extends Remote {

  /**
   * Executes a Paxos request after consensus is reached.
   *
   * @param request      The Paxos request to be executed.
   * @param serverNumber The server number where the request originated.
   * @return The result of the execution.
   * @throws RemoteException If a remote communication error occurs.
   */
  String executePaxosRequest(PAXOSResponse request, int serverNumber) throws RemoteException;
}
