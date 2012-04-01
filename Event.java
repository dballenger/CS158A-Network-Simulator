public class Event {
  /**
   The source and destination variables are probably redundant since we can just getFrame().getSource() or something instead
   
   Where are we sending from
  */
  private Node source;
  /**
   Where are we sending to
  */
  private Node destination;
  private Frame frame;
  /**
   When should this packet get sent?  During which simulation cycle
  */
  private long time_slot;
  /**
   How many times have we retried sending the frame?
  */
  private short retries = 0;
  /**
   The speed of light in copper, in meters per second
  */
  private final int SPEED_OF_LIGHT_IN_COPPER = 210000000;
  
  public Event(Node source, Node destination, Frame frame, long time_slot) {
    this.source       = source;
    this.destination  = destination;
    this.frame        = frame;
    this.time_slot    = time_slot;
    this.retries      = 0;
  }
  
  public long setTimeSlot(long time_slot) {
    this.time_slot = time_slot;
    
    return this.time_slot;
  }
  
  public long getTimeSlot() {
    return this.time_slot;
  }
  
  private long propogationDelay() {
    return (long)(((double)(Math.abs(Math.abs(this.source.getDistance()) - Math.abs(this.destination.getDistance())) / (double)SPEED_OF_LIGHT_IN_COPPER)) / 0.000000095);
  }
  
  public long getFinishedSlot() {
    return (this.time_slot + (int)this.frame.timeToTransmit() + this.propogationDelay());
  }
  
  public Node getSource() {
    return this.source;
  }
  
  public Node getDestination() {
    return this.destination;
  }
  
  public Frame getFrame() {
    return this.frame;
  }
  
  public short getRetries() {
    return this.retries;
  }
  
  public short incrementRetries() {
    return ++this.retries;
  }
  
  public String toString() {
    return "Event - From: " + this.source + " to: " + this.destination + " start time: " + this.getTimeSlot() + " end time: " + this.getFinishedSlot();
  }
}