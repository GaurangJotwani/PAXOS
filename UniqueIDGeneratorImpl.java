/**
 * UniqueIDGeneratorImpl.java
 *
 * This file implements the UniqueIDGenerator interface, providing the functionality
 * to generate unique transaction IDs required for the Paxos algorithm. It uses a
 * lock to ensure thread safety when generating tokens.
 *
 * Author: Gaurang Jotwani
 * Course: NEU Summer 23 CS 6650
 * Date: 08/07/2023
 */

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UniqueIDGeneratorImpl implements UniqueIDGenerator, Serializable {

  // Initializing lock that is used to maintain uniqueness in the case of multiple
  // Nodes asking for tokens.
  private Lock tokenLock = new ReentrantLock();
  private int token;
  /**
   * Constructor to initialize the token generator.
   *
   * @throws RemoteException If a remote communication error occurs.
   */
  UniqueIDGeneratorImpl() throws RemoteException {
    super();
    token = 0;
  }

  /**
   * Generates a unique token that can be used as a transaction identifier.
   *
   * @return The generated unique token.
   * @throws RemoteException If a remote communication error occurs.
   */
  @Override
  public int getToken() throws RemoteException {
    tokenLock.lock();
    token = token + 1;
    tokenLock.unlock();
    return token;
  }
}
