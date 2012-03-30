import java.util.*;

public class Frame {
  private String preamble;
  private String start_delimiter;
  private String destination;
  private String source;
  private int vlan_tag;
  private short ethertype;
  private int payload_size;
  private int crc;
  
  public Frame(Node source, Node destination, int payload_size) {
    /**
     7 octects
    */
    this.preamble = "10101010101010101010101010101010101010101010101010101010";
    /**
     1 octet
    */
    this.start_delimiter = "10101011";
    /**
     6 octets
    */
    this.destination = destination.getMacAddress();
    /**
     6 octets
    */
    this.source = source.getMacAddress();
    /**
     4 octets
    */
    this.vlan_tag = 0;
    /**
     2 octets
    */
    this.ethertype = 0x800;
    
    /**
     Valid size range:
     42 - 1500 octets
    */
    this.payload_size = payload_size;
    
    /**
     4 octets
     We don't particularly care what it is for the simulation right now
    */
    Random generator = new Random();
    this.crc = generator.nextInt();
  }
  
  public int getPayloadSize() {
    return this.payload_size;
  }
  
  /**
   @return time to transmit the frame in (our time clock is 1 bit per iteration of the simulator)
  */
  public double timeToTransmit() {
    return (42 + (float)this.payload_size * 8);
  }
  
  public String toString() {
    return this.preamble + "|" + this.start_delimiter + "|" + this.destination + "|" + this.source + "|" + this.vlan_tag + "|0x" + Integer.toHexString(this.ethertype) + "|<" + this.payload_size + " bytes>|0x" + Integer.toHexString(this.crc);
  }
}