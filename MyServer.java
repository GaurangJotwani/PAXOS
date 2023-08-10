/**
 * MyServer.java
 * This is the main file for the Key-Value Store server application using Java RMI.
 * It starts the server and binds the KeyStoreRemote object to the RMI registry.
 * It also creates 9 replicas of the server and starts the coordinator.
 * Usage: java MyServer <port>
 *   - <port>: The port number on which the server will listen for incoming RMI requests.
 * Author: Gaurang Jotwani
 * Course: NEU Summer 23 CS 6650
 * Date: 08/07/2023
 */

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public class MyServer {
  /**
   * The main method that starts the server.
   * @param args Command line arguments (expects a single argument: the port number).
   */
  public static void main(String args[]) {
    try {
      if (args.length != 1) {
        System.err.println("Provide Correct number of Arguments (Port)");
        System.exit(-1);
      }
      // Get the port from command line arguments
      int first_port = Integer.parseInt(args[0]);
      System.out.println("Server Starting...");
      // Set RMI system properties- Timeout value set for 20 second
      System.setProperty("sun.rmi.transport.tcp.responseTimeout", "20000");
      System.setProperty("sun.rmi.transport.tcp.readTimeout", "20000");

      UniqueIDGenerator tokenGenerator = new UniqueIDGeneratorImpl();
      // Create an RMI registry for the Coordinator and bind it to the registry on port 8767
      Registry coordinatorRegistry = LocateRegistry.createRegistry(8767);
      coordinatorRegistry.bind("tokenGenerator", tokenGenerator);
      for (int i = 0; i < 9; i++) {
        KeyStore stub = new KeyStoreRemote(i, first_port + i, first_port);
        Registry participantRegistry = LocateRegistry.createRegistry(first_port + i);
        participantRegistry.bind("keyStore", stub);
      }

    } catch (AccessException e) {
      System.err.println("Cannot access host. Try again!");
      System.exit(-1);
    } catch (UnknownHostException e) {
      System.err.println("Unknown Host. Try again!");
      System.exit(-1);
    } catch (RemoteException e) {
      System.err.println(e.getMessage());
      System.err.println("Unknown Remote Exception. Try again!");
      System.exit(-1);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(-1);
    }
  }
}
