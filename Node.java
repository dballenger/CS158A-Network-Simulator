import java.util.*;

public class Node {
  private String mac_address;
  private int bandwidth;
  
  public Node() {
    Random generator = new Random();
    
    this.mac_address = "" + Integer.toHexString(generator.nextInt(16)) + Integer.toHexString(generator.nextInt(16)) + ":" +
                     Integer.toHexString(generator.nextInt(16)) + Integer.toHexString(generator.nextInt(16)) + ":" +
                     Integer.toHexString(generator.nextInt(16)) + Integer.toHexString(generator.nextInt(16)) + ":" +
                     Integer.toHexString(generator.nextInt(16)) + Integer.toHexString(generator.nextInt(16)) + ":" +
                     Integer.toHexString(generator.nextInt(16)) + Integer.toHexString(generator.nextInt(16)) + ":" +
                     Integer.toHexString(generator.nextInt(16)) + Integer.toHexString(generator.nextInt(16));
    this.bandwidth = 10000000; // 10Mbps
  }
  
  public String getMacAddress() {
    return this.mac_address;
  }
  
  public int getBandwidth() {
    return this.bandwidth;
  }
  
  public String toString() {
    return this.mac_address;
  }
}