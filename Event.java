public class Event {
  private Node source;
  private Node destination;
  private Frame frame;
  /**
   When should this packet get sent?  During which simulation cycle
  */
  private long time_slot;
  
  public Event(Node source, Node destination, Frame frame, long time_slot) {
    this.source = source;
    this.destination = destination;
    this.frame = frame;
    this.time_slot = time_slot;
  }
  
  public long getTimeSlot() {
    return this.time_slot;
  }
  
  public long getFinishedSlot() {
    return (this.time_slot + (int)this.frame.timeToTransmit());
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
  
  public String toString() {
    return "Event - From: " + this.source + " to: " + this.destination + " start time: " + this.getTimeSlot() + " end time: " + this.getFinishedSlot();
  }
}