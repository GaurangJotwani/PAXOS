/*
 * MyClient.java
 *
 * This file contains the implementation of the client-side program that interacts with the KeyStore server
 * using Java RMI (Remote Method Invocation). The client allows users to send various commands to the server
 * such as GET, PUT, DELETE, and QUIT, and handles the server responses accordingly.
 *
 * Author: Gaurang Jotwani
 * Course: NEU Summer 23 CS 6650
 * Date: 08/07/2023
 */

import java.rmi.*;
import java.sql.Timestamp;
import java.util.Scanner;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.net.MalformedURLException;

/**
 * The MyClient class represents a client application that interacts with the KeyStore server using RMI.
 * It allows the user to send various commands to the server, such as GET, PUT, and DELETE, to read, write,
 * and delete key-value pairs in the KeyStore.
 */
public class MyClient {
  private static String get = new String("GET");
  private static String delete = new String("DELETE");
  private static String put = new String("PUT");
  private static String quit = new String("QUIT");
  private static KeyStore keyStore;

  /**
   * The main method of the client program. It handles the user input and sends the appropriate commands
   * to the KeyStore server.
   *
   * @param args The command-line arguments. Expects the IP address and port of the KeyStore server.
   */
  public static void main(String args[]) {
    // Check if the correct number of command-line arguments is provided
    if (args.length != 2) {
      System.err.println("Provide Correct number of Arguments (IP and Port of server)");
      System.exit(-1);
    }
    // Extract ip and port from command line arguments
    String ip = args[0];
    int port = Integer.parseInt(args[1]);

    try {
      // Set RMI system properties- Timeout value set for 20 second
      System.setProperty("sun.rmi.transport.tcp.responseTimeout", "20000");
      System.setProperty("sun.rmi.transport.tcp.readTimeout", "20000");
      // Lookup the KeyStore remote object from the server
      keyStore = (KeyStore) Naming.lookup("rmi://" + ip + ":" + port + "/keyStore");
      System.out.println("Starting PrePopulation of keystore...");

      for (int i = 0; i < 5; i++) {
        handlePutRequest("x" + i + port, "y" + i);
      }
      for (int i = 0; i < 5; i++) {
        handleGetRequest("x" + i + port);
      }
      for (int i = 0; i < 5; i++) {
        handleDelRequest("x" + i + port);
      }
    } catch(MalformedURLException e) {
      System.err.println(getCurrentTimeStamp() + "URL does not exists. Try again!");
      System.exit(-1);
    } catch (UnknownHostException e) {
      System.err.println(getCurrentTimeStamp() + "Unknown Host. Try again!");
      System.exit(-1);
    } catch (AccessException e) {
      System.err.println(getCurrentTimeStamp() + "Cannot Access Host. Try again!");
      System.exit(-1);
    } catch (NotBoundException e) {
      System.err.println(getCurrentTimeStamp() + "Not Bound Exception. Try again!");
      System.exit(-1);
    } catch (ConnectException e) {
      System.err.println(getCurrentTimeStamp() + "Server taking too long to respond. Try again!");
      System.exit(-1);
    } catch (RemoteException e) {
      System.err.println(getCurrentTimeStamp() + "Remote Exception. Try again!");
      System.exit(-1);
    }
    Scanner input = new Scanner(System.in);
    System.out.print("Please Input Command in either of the following forms:\n\tGET " +
            "<key>\n\tPUT <key> <val>\n\tDELETE <key>\n\tQUIT\n");
    while(true) {
      System.out.print("Enter Command: ");
      String cmd = input.nextLine();
      String[] splited = cmd.split(" ");
      cmd = splited[0];
      // Check if the key length is too big
      if (splited.length >= 2 && splited[1].length() > 1024) {
        System.err.println(getCurrentTimeStamp() + "Key length is too big.");
        System.out.print("Please Input Command in either of the following forms:\n\tGET " +
                "<key>\n\tPUT <key> <val>\n\tDELETE <key>\n\tQUIT\n");
        continue;
      }
      // Handle the different command types
      if (cmd.equals(get) && splited.length == 2) {
        while(true) {
          try {
            handleGetRequest(splited[1]);
            break;
          } catch (RemoteException e) {
            continue;
          }
        }
      } else if (cmd.equals(put) && splited.length == 3) {
        // Check if the value length is too bigmoki1234
        if (splited[2].length() > 1024) {
          System.err.println(getCurrentTimeStamp() + "Val length is too big.");
          System.out.print("Please Input Command in either of the following forms:\n\tGET " +
                  "<key>\n\tPUT <key> <val>\n\tDELETE <key>\n\tQUIT\n");
          continue;
        }
        while(true) {
          try {
            handlePutRequest(splited[1], splited[2]);
            break;
          } catch (RemoteException e) {
            continue;
          }
        }
      } else if (cmd.equals(delete) && splited.length == 2) {
        while(true) {
          try {
            handleDelRequest(splited[1]);
            break;
          } catch (RemoteException e) {
            continue;
          }
        }
      } else if (cmd.equals(quit) && splited.length == 1) break; // Exit the loop and end the program
      else {
        System.err.println(getCurrentTimeStamp() + "Wrong format of command.");
        System.out.print("Please Input Command in either of the following forms:\n\tGET " +
                "<key>\n\tPUT <key> <val>\n\tDELETE <key>\n\tQUIT\n");
      }
    }
  }

  /**
   * Handles the GET request to retrieve the value associated with the given key from the KeyStore server.
   *
   * @param key The key to retrieve the value for.
   */
  private static void handleGetRequest(String key) throws RemoteException {
    try {
      // No two phase protocol required for GET request.
      String val = keyStore.initiateGet(key);
      System.out.println(getCurrentTimeStamp() + "Value Read: " + val);
      String serverMessage = "Successfully read key \"" + key +"\" with val \"" + val + "\"";
      System.out.println(getCurrentTimeStamp() + serverMessage);
    } catch (ConnectException e) {
      System.err.println(getCurrentTimeStamp() + "GET Command Not Successful. Server taking too " +
              "long to respond. Try again!");
    } catch (UnknownHostException e) {
      System.err.println(getCurrentTimeStamp() + "Unknown Host. Try again!");
    } catch (AccessException e) {
      System.err.println(getCurrentTimeStamp() + "Cannot Access Host. Try again!");
    } catch (IllegalArgumentException e) {
      System.err.println(getCurrentTimeStamp() + "Illegal Argument Exception! Key not found!");
    }
  }

  /**
   * Handles the DELETE request to delete the key-value pair associated with the given key from the KeyStore server.
   *
   * @param key The key to delete.
   */
  private static void handleDelRequest(String key) throws RemoteException {
    try {
      keyStore.initiateDelete(key);
      String serverMessage = "Successfully deleted key \"" + key +"\"";
      System.out.println(getCurrentTimeStamp() + serverMessage);
    } catch (ConnectException e) {
      System.err.println(getCurrentTimeStamp() + "DEL Command Not Successful. Server taking too " +
              "long to respond. Try again!");
    } catch (UnknownHostException e) {
      System.err.println(getCurrentTimeStamp() + "Unknown Host. Try again!");
    } catch (AccessException e) {
      System.err.println(getCurrentTimeStamp() + "Cannot Access Host. Try again!");
    } catch (IllegalArgumentException e) {
      System.err.println(getCurrentTimeStamp() + "Illegal Argument Exception! Key not found!");
    }
  }

  /**
   * Handles the PUT request to store the key-value pair in the KeyStore server.
   *
   * @param key The key to store.
   * @param val The value to associate with the key.
   */
  private static void handlePutRequest(String key, String val) throws RemoteException{
    try {
      keyStore.initiatePut(key, val);
      String serverMessage = key + " with value \"" + val + "\" saved successfully";
      System.out.println(getCurrentTimeStamp() + serverMessage);
    } catch (ConnectException e) {
      System.err.println(getCurrentTimeStamp() + "PUT Command Not Successful. Server taking too " +
              "long to respond. Try again!");
    } catch (UnknownHostException e) {
      System.err.println(getCurrentTimeStamp() + "Unknown Host. Try again!");
    } catch (AccessException e) {
      System.err.println(getCurrentTimeStamp() + "Cannot Access Host. Try again!");
    }
  }

  /**
   * Gets the current timestamp as a formatted string.
   *
   * @return The current timestamp as a string.
   */
  private static String getCurrentTimeStamp() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    return "[" + timestamp.toString() + "]  ";
  }
}
