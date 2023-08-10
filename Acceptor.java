/*
 * Acceptor.java
 *
 * This file defines the Acceptor interface, which outlines the methods for processing
 * Promise and Accept messages as part of the Paxos consensus process. Classes implementing
 * this interface will provide the necessary functionality to handle communication between
 * proposers and acceptors during the consensus reaching.
 *
 * Author: Gaurang Jotwani
 * Course: NEU Summer 23 CS 6650
 * Date: 08/07/2023
 */

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Acceptor extends Remote {

  /**
   * Processes a Promise message as part of the Paxos consensus process.
   *
   * @param request The Paxos request containing the Promise message.
   * @return The Promise response.
   * @throws RemoteException If a remote communication error occurs.
   */
  PAXOSResponse promise(PAXOSResponse request) throws RemoteException;

  /**
   * Processes an Accept message as part of the Paxos consensus process.
   *
   * @param request The Paxos request containing the Accept message.
   * @return The Accept response.
   * @throws RemoteException If a remote communication error occurs.
   */
  PAXOSResponse accept(PAXOSResponse request) throws RemoteException;

}
