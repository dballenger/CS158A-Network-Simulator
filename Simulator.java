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
  /**
   The nodes between which we'll be sending our traffic
  */
  private ArrayList<Node> nodes = new ArrayList<Node>();
  /**
   Variables for metrics
  */
  private int initial_frames = 0, dropped_frames = 0, retried_frames = 0;
  
  public static void main(String[] args) {
    System.out.println("802.3 Ethernet Network Simulator");
    
    Simulator sim = new Simulator();
    /**
     Configure the simulator, should eventually prompt for:
     * How many nodes we have
     * How much traffic to send between nodes
     * How fast the traffic should transmit
    */
    sim.setup();
    /**
      Actually run the simulator, this is where we should collect statisitics
    */
    sim.run();
    /**
      Here we should analyze the statistics (collisions, transmit speed, etc)
      sim.metrics();
    */
  }
  
  private void setup() {
    Random generator = new Random();
    long time_offset = 0;
    int packet_size = 0;
    final int INTER_FRAME_DELAY = 22135;
    final int NODES = 128;
    final int PACKETS_EACH = 1280;
    
    /**
     Setup nodes for the simulation
    */
    for (int i = 0; i < NODES; i++) {
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
      
      /**
       The max may need to be offset in the event we determine that we aren't getting enough collisions and need to minimize the time in which machines can send packets
       for each machine we initial the offset to a random offset to begin with, this makes it so things won't all send at the same time
      */
      time_offset = generator.nextInt(2 << 16);
      
      for (int i = 0; i < PACKETS_EACH; i++) {
        packet_size = 512; // generator.nextInt(1420);
        this.events.add(new Event(source, destination, new Frame(source, destination, (64 + packet_size)), time_offset));
        
        // this isn't really correct, we want it to only be 1.5Mbps, not 10MB all right after the other.
        time_offset += packet_size * 8 + INTER_FRAME_DELAY; // the next packet should come immediately after this packet, not one bit later
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
     For metrics, store how many frames we're queued to send
    */
    this.initial_frames = this.events.size();
  }
  
  /**
   Run the simulation process
   
   When events get executed, they should get popped off the events array list and pushed into onWireEvents
   
   After the time to transmit has been reached, the event should be popped off the onWireEvents array list (and can be discarded)
  */
  private void run() {
    /**
     Magic number for how long to delay as part of the backoff algorithm
    */
    final int RETRY_DELAY = 537;
    /**
     Variable to hold the retry count of the currently processing frame
    */
    short retries = 0;
    /**
     Track where we are in the simulation
    */
    int timer = 0, delay = 0;
    /**
     What event we're currently working with and just about to process if the carrier sense is clear
    */
    Event current = null, next = null;
    /**
     The random number generator for the backoff algorithm
    */
    Random generator = new Random();
    /**
     The events currently on the wire as part of the simulation
    */
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
        if (this.onWireEvents.get(i).getFinishedSlot() <= timer) {
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
       
       Check if the events are inside or outside of the 512 bit detection window, in reality we wouldn't know immediately about the collision (but in a simulator we obviously can)
      */
      if (this.mediumClear(timer) && this.onWireEvents.size() > 1) { // this isn't quite what it should be
        // in this case, we push all events back onto this.events at random intervals...Event will need to track the retries (the exponential backoff algorithm)
        System.out.println("We have a collision on the network!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("Current time: " + timer);
        
        /**
         We need to loop through each of the events on the wire, and if they're part of this collision, we need to increment
         their retry counter and schedule them to run again in the future according to the backoff algorithm
        */
        for (Event event : this.onWireEvents) {
          retries = event.incrementRetries();
          
          /**
           1) send a jamming signal
           Then one of the below options depending on how many retries we're attempting:
           
           2) resend the frame immediately (on the next timer cycle) or after 51.2 microseconds (random selection)
           3) If that fails: k * 51.2 microseconds where k = 0,1,2,3
           4) If it's still failing, k * 51.2 microseconds where k is [0, 2^3 - 1]
           5) If it's still failing, try up to [0, 2^10 - 1]
           Note: 51.2 microseconds = 536.870912 clock ticks in our simulation
          */
          
          /**
           Send the jamming signal
           
           Just not right now since it will cause an endless loop creating jamming frames for the jamming frames
          */
//          this.events.add(new Event(event.getSource(), event.getDestination(), new Frame(event.getSource(), event.getDestination(), 4, timer)));
          
          /**
           Determine if we should "immediately" resend the frame or send it again later
          */
          if (retries < 17) {
            if (retries <= 10) {
              delay = generator.nextInt(1 << retries);
            } else {
              delay = generator.nextInt(11);
            }
          } else {
            /**
             The specification says that if we we resending 16 times, just drop the frame.
            */
            dropped_frames++;
            System.out.println("Dropping frame, 16 retries already attempted");
          }
          
          /**
           We haven't gotten to the point where we should drop the frame,
           so we update the timeslot for it to send again in and queue it.
          */
          if (retries < 17) {
            this.retried_frames++;
            
            event.setTimeSlot(timer + (delay * RETRY_DELAY));
            this.events.add(event);
            
            System.out.println("Triggering resend of frame with delay factor " + delay + ":" + event);
          }
        }
        /**
         Reset the array list to be empty now, since all the colliding packets have been requeued
        */
        this.onWireEvents = new ArrayList<Event>();
        
        /**
         Resort the list for the new frames being resent
        */
        Collections.sort(this.events, new Comparator<Event>() {
          public int compare(Event e1, Event e2) {
            return (e1.getTimeSlot() <= e2.getTimeSlot() ? 0 : 1);
          }
        });
      }
      
      /**
       We're done with this step, so increment the timer for the next run around
      */
      timer++;
    }
    
    System.out.println("Simulation complete.  Out of " + this.initial_frames + " initial frames queued, we had " + this.dropped_frames + " dropped frames and " + this.retried_frames + " retried frames");
  }
  
  /**
   Helper function to check if the medium is clear to send
  
   @param Timer the current time
   @return True if the medium is clear (or appears clear), false if data transferring currently
  */
  private boolean mediumClear(int timer) {
    Random generator = new Random();
    
    for (Event event : this.onWireEvents) {
      /**
       To "account" for the fact not all devices are going to be 512 bits away from each other, we should probably randomly generate a number between 1 and 512 to check against
      */
      if ((timer - event.getTimeSlot()) <= generator.nextInt(512)) {
        /**
         No frames on the wire within 512 bits of distance
        */
        return true;
      } else {
        /**
         There exists a frame on the wire within 512 bits of the sender
        */
        return false;
      }
    }
    
    /**
     By default the medium is clear (we know this because this will only happen when nothing is on the wire)
    */
    return true;
  }
}