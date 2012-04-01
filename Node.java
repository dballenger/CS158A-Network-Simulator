import java.util.*;

public class Node {
  private String mac_address = "";
  private int bandwidth = 0;
  private int distance = 0;
  private int first_frame_sent = 0;
  private int last_frame_sent = 0;
  
  private Node destinationNode;
  
  public Node() {
    Random generator = new Random();
    
    this.mac_address = "" + Integer.toHexString(generator.nextInt(16)) + Integer.toHexString(generator.nextInt(16)) + ":" +
                     Integer.toHexString(generator.nextInt(16)) + Integer.toHexString(generator.nextInt(16)) + ":" +
                     Integer.toHexString(generator.nextInt(16)) + Integer.toHexString(generator.nextInt(16)) + ":" +
                     Integer.toHexString(generator.nextInt(16)) + Integer.toHexString(generator.nextInt(16)) + ":" +
                     Integer.toHexString(generator.nextInt(16)) + Integer.toHexString(generator.nextInt(16)) + ":" +
                     Integer.toHexString(generator.nextInt(16)) + Integer.toHexString(generator.nextInt(16));
    this.bandwidth = 10000000; // 10Mbps
    this.distance = generator.nextInt(250) * (generator.nextBoolean() ? -1 : 1);
  }
  
  public Node getDestinationNode() {
    return this.destinationNode;
  }
  
  public void setDestinationNode(Node node) {
    this.destinationNode = node;
  }
  
  public int getFirstFrameSeen() {
    return this.first_frame_sent;
  }
  
  public int getLastFrameSeen() {
    return this.last_frame_sent;
  }
  
  public void setFirstFrameSent(int time) {
    this.first_frame_sent = time;
  }
  
  public void setLastFrameSent(int time) {
    this.last_frame_sent = time;
  }
  
  public String getMacAddress() {
    return this.mac_address;
  }
  
  public int getDistance() {
    return this.distance;
  }
  
  public int getBandwidth() {
    return this.bandwidth;
  }
  
  public String toString() {
    return this.mac_address;
  }
}