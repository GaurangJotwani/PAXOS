/*
 * UniqueIDGenerator.java
 *
 * This file defines the UniqueIDGenerator interface, which outlines the method for generating
 * unique transaction IDs required for the Paxos algorithm. Classes implementing this interface
 * will provide the functionality to generate unique tokens that can be used as identifiers.
 *
 * Author: Gaurang Jotwani
 * Course: NEU Summer 23 CS 6650
 * Date: 08/07/2023
 */

import java.rmi.Remote;
import java.rmi.RemoteException;
public interface UniqueIDGenerator extends Remote {
  /**
   * Generates a unique token that can be used as a transaction identifier.
   *
   * @return The generated unique token.
   * @throws RemoteException If a remote communication error occurs.
   */
  int getToken() throws RemoteException;
}