/**
 * PAXOSResponse Class
 *
 * This class represents a response object used in the Paxos consensus algorithm.
 * It contains information about a proposed operation, such as a PUT, DELETE, or GET,
 * along with relevant details like key, value, operation type, log ID, and a unique ID.
 *
 * Author: Gaurang Jotwani
 * Course: NEU Summer 23 CS 6650
 * Date: 08/07/2023
 */

import java.io.Serializable;
public class PAXOSResponse implements Serializable {
  // Fields
  public String key = null;
  public long id;
  public String type = null;
  public String value = null;
  public int logId;
  public long uniqueId;

  /**
   * PAXOSResponse Constructor
   *
   * Initializes a PAXOSResponse object with the provided parameters.
   *
   * @param key       The key associated with the operation.
   * @param id        The identifier for the operation.
   * @param type      The type of operation (PUT, DELETE, GET).
   * @param value     The value associated with the operation (for PUT).
   * @param clog     The log ID for tracking Paxos rounds.
   * @param uniqueId  The unique ID for distinguishing requests.
   */
  public PAXOSResponse(String key, long id, String type, String value, Integer clog,
                       long uniqueId){
    this.key = key; // Key associated with the operation
    this.id = id; // Identifier for the operation
    this.type = type; // Type of operation (PUT, DELETE, GET)
    this.value = value; // Value associated with the operation (for PUT)
    this.logId = clog; // Log ID for tracking Paxos rounds
    this.uniqueId = uniqueId; // Unique ID for distinguishing requests
  }
  /**
   * Empty PAXOSResponse Constructor
   *
   * Initializes an empty PAXOSResponse object. Used for creating instances without parameters.
   */
  public PAXOSResponse() {}
}
