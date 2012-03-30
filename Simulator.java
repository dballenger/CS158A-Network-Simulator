import java.util.*;

/**
 Time slices in nanoseconds what about just time slice = 1 bit across the entire lan (500 meters)
 1 bit / 10Mbps = 95.3674316 nano seconds (time "width" of a bit)
 speed of light in copper: c(copper) = 2.1 x 10^8 m/s
 Time to travel across the whole lan: 500 meters / c(copper) = 2.38095 x 10^-5 seconds = 23809.5 nanoseconds

 
 We might not notice a frame on the wire for up to 512 bits (48.828125 microseconds) and cause a collison
*/

/**
 Distance wise we could probably just consider the length of the entire segment and use that to determine the time to spend data to another host.
 
 We probably don't need to worry about the distances between each node
*/

public class Simulator {
  /**
   Network events that have yet to be processed
  */
  private ArrayList<Event> events = new ArrayList<Event>();
  /**
   Events currently on the wire (frames still in transit)
  */
  private ArrayList<Event> onWireEvents = new ArrayList<Event>();
  private ArrayList<Node> nodes = new ArrayList<Node>();
  
  public static void main(String[] args) {
    System.out.println("802.3 Ethernet Network Simulator");
    
    Simulator sim = new Simulator();
    sim.setup();
    sim.run();
  }
  
  private void setup() {
    Random generator = new Random();
    long time_offset = 0;
    int packet_size = 0;
    
    /**
     Setup nodes for the simulation
    */
    for (int i = 0; i < 2; i++) {
      this.nodes.add(new Node());
    }
    
    /**
     Setup each node to randomly pick another node to send 10 seconds of traffic to at 1.5Mbps (one-way, think UDP stream)
    */
    for (Node source : this.nodes) {
      Node destination = null;
      
      while (destination == null || source == destination) {
        destination = this.nodes.get(generator.nextInt(this.nodes.size()));
      }
      
      System.out.println("Configuring node: " + source.getMacAddress() + " to transmit to: " + destination.getMacAddress());
      
      /**
       Create the frame events
      */
      ArrayList<Frame> frames = new ArrayList<Frame>();
      
      // the max may need to be offset in the event we determine that we aren't getting enough collisions and need to minimize the time in which machines can send packets
      time_offset = generator.nextInt(2 << 16); // for each machine we initial the offset to a random offset to begin with, this makes it so things won't all send at the same time
      
      for (int i = 0; i < 1280; i++) {
        packet_size = generator.nextInt(1420);
        this.events.add(new Event(source, destination, new Frame(source, destination, (64 + packet_size)), time_offset));
        
        time_offset += packet_size * 8; // the next packet should come immediately after this packet, not one bit later
      }
    }
    
    /**
     Sort the array list so events that happen first are at the beginning of the list
    */
    Collections.sort(this.events, new Comparator<Event>() {
      public int compare(Event e1, Event e2) {
        return (e1.getTimeSlot() <= e2.getTimeSlot() ? 0 : 1);
      }
    });
    
    /**
     debugging code
    int select = 1;
    System.out.println("Will take: " + events.get(select).getFrame().timeToTransmit() + " nano seconds to transmit " + events.get(select).getFrame().getPayloadSize() + " and run from " + events.get(select).getTimeSlot() + " until " + events.get(select).getFinishedSlot());
    System.out.println(events.get(select).getFrame());
    
    select = 2;
    System.out.println("Will take: " + events.get(select).getFrame().timeToTransmit() + " nano seconds to transmit " + events.get(select).getFrame().getPayloadSize() + " and run from " + events.get(select).getTimeSlot() + " until " + events.get(select).getFinishedSlot());
    System.out.println(events.get(select).getFrame());
    */
  }
  
  /**
   Run the simulation process
   
   When events get executed, they should get popped off the events array list and pushed into onWireEvents
   
   After the time to transmit has been reached, the event should be popped off the onWireEvents array list (and can be discarded)
  */
  private void run() {
    int timer = 0;
    Event current = null, next = null;
    this.onWireEvents = new ArrayList<Event>();
    
    /**
     Loop through the available events until we've got no more to process.
    */
    while (this.events.size() > 0 || this.onWireEvents.size() > 0) {
      /**
       Check for expired on wire events
       
       This could get removed and put into it's own method
      */
      for (int i = 0; i < this.onWireEvents.size(); i++) {
        if (this.onWireEvents.get(i).getFinishedSlot() < timer) {
//          System.out.println("Removing event " + this.onWireEvents.get(i) + " as it finished at " + this.onWireEvents.get(i).getFinishedSlot() + " and we are now at time " + timer);
          this.onWireEvents.remove(i);
          
          i--; // have to reset the i back one to account for the fact we just deleted one
        }
      }
      
      /**
        Pop the current event off the events queue and push it into the on wire queue
      */
      if (this.events.size() > 0) {
        /**
         We want to check if the medium is clear first, check that nothing has been in the onWireEvent queue more than 512 cycles (bits) - this will allow for collisions to occur
         We also want to check the time for this event has come
        */
        next = this.events.get(0);
        if (this.mediumClear(timer) && next.getTimeSlot() <= timer) {
          
          this.events.remove(0);
          this.onWireEvents.add(next);
        }
      }
      /**
       If there's more than 1 events in the onWireEvents array list, we have a collision
      */
      if (this.onWireEvents.size() > 1) {
        //in this case, we push all events back onto this.events at random intervals...Event will need to track the retries
        System.out.println("We have a collision on the network!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("Current time: " + timer);
        
        for (Event event : this.onWireEvents) {
          System.out.println(event);
        }
        
        System.out.println("We have a collision on the network!!!!!!!!!!!!!!!!!!!!!!!!!!");
        
        // resort the list so things are in order
        // new elements inserted at timer + generator.nextInt()
      }
      
      
      timer++;
    }
  }
  
  /**
   Helper function to check if the medium is clear to send
  
   @param Timer the current time
   @return True if the medium is clear (or appears clear), false if data transferring currently
  */
  private boolean mediumClear(int timer) {
    for (Event event : this.onWireEvents) {
      if ((event.getTimeSlot() - timer) <= 512) {
        return true;
      } else {
        return false;
      }
    }
    
    return true;
  }
}