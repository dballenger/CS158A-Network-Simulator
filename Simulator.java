import java.util.*;
import java.io.*;

/**
  Time slice = 1 bit
 1 bit / 10Mbps = 95.3674316 nano seconds (time "width" of a bit)
 speed of light in copper: c(copper) = 2.1 x 10^8 m/s
 Time to travel across the whole lan: 500 meters / c(copper) = 2.38095 x 10^-5 seconds = 23809.5 nanoseconds
 
 We might not notice a frame on the wire for up to 512 bits (48.828125 microseconds) and cause a collison
 
 Need to take into account the distance for determing how long it takes to transmit
*/

/**
 Distance wise we could probably just consider the length of the entire segment and use that to determine the time to spend data to another host.
 
 We probably don't need to worry about the distances between each node.
 If we do, define the disance from the "center" of the segment.  Negative distances being to the left of the center, and positive distances being to the right of the center
 
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
   The list of completed (transmitted) events
  */
  private ArrayList<Event> completedEvents = new ArrayList<Event>();
  /**
   The nodes between which we'll be sending our traffic
  */
  private ArrayList<Node> nodes = new ArrayList<Node>();
  /**
   Variables for metrics
  */
  private int initial_frames = 0, dropped_frames = 0, retried_frames = 0;
  
  /**
   The speed of the medium in bits per second
  */
  final int MEDIUM_SPEED = 10000000;
  /**
   Make bits into Megabits
  */
  final int BIT_FACTOR = 1000000;
  /**
   This represents the delay we need to be sending data at 1.5Mbps
  */
  final int DEFAULT_INTER_FRAME_DELAY = 22135;
  int INTER_FRAME_DELAY = 0;
  /**
   How many nodes we want to simulate for
  */
  final int DEFAULT_NODES = 8;
  int NODES = 0;
  /**
   How many packets do we want each host to send?
  */
  final int DEFAULT_PACKETS_EACH = 1280;
  int PACKETS_EACH = 0;
  
  /**
   How big should the payload in the frames be? In bytes
  */
  final int DEFAULT_PACKET_SIZE = 512;
  int PACKET_SIZE = DEFAULT_PACKET_SIZE;
  
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
    sim.statistics();
  }
  
  private void setup() {
    Random generator = new Random();
    long time_offset = 0;
    
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      
      try {
        System.out.print("How many nodes? ");
        NODES = Integer.parseInt(in.readLine());
        System.out.println("Configuring for " + NODES + " nodes");
      } catch (NumberFormatException e) {
        System.out.println("You didn't enter anything, defaulting to " + DEFAULT_NODES + " nodes.");
        NODES = DEFAULT_NODES;
      }
      
      try {
        System.out.print("How many packets per node? ");
        PACKETS_EACH = Integer.parseInt(in.readLine());
        System.out.println("Configuring for " + PACKETS_EACH + " packets per node");
      } catch (NumberFormatException e) {
        System.out.println("You didn't enter anything, defaulting to " + DEFAULT_PACKETS_EACH + " packets per node");
        PACKETS_EACH = DEFAULT_PACKETS_EACH;
      }
      
      try {
        System.out.print("How fast should nodes attempt to send their packets in Mbps? ");
        double read = Double.parseDouble(in.readLine());
        
        INTER_FRAME_DELAY = (int)(((
                                ((double)MEDIUM_SPEED / (PACKET_SIZE * 8)) - ((read * (double)BIT_FACTOR) / (PACKET_SIZE * 8))) 
                                /
                                ((double)(read * (double)BIT_FACTOR) / (PACKET_SIZE * 8))) * (double)(PACKET_SIZE * 8));
        System.out.println("Configured throttling to induce a inter frame delay of " + INTER_FRAME_DELAY + " bits of idle time");
      } catch (NumberFormatException e) {
        System.out.println("You didn't enter a speed, defaulting to 1.5Mbps for an inter frame delay of 16,602 bits");
        INTER_FRAME_DELAY = 16602;
      }
      
    } catch (Exception e) {
      System.err.println("We encountered a problem, quitting..." + e);
      System.exit(-1);
    }
    
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
      
      source.setDestinationNode(destination);
      
      System.out.println("Configuring node: " + source.getMacAddress() + " to transmit to: " + destination.getMacAddress());
      
      /**
       The max may need to be offset in the event we determine that we aren't getting enough collisions and need to minimize the time in which machines can send packets
       for each machine we initial the offset to a random offset to begin with, this makes it so things won't all send at the same time
      */
      time_offset = generator.nextInt(2 << 12);
      
      for (int i = 0; i < PACKETS_EACH; i++) {
        this.events.add(new Event(source, destination, new Frame(source, destination, (64 + PACKET_SIZE)), time_offset));
        
        time_offset += (PACKET_SIZE * 8) + INTER_FRAME_DELAY + (generator.nextInt(2 << 8) * (generator.nextInt(10) == 0 ? -1 : 1)); // the next packet should come immediately after this frame + the inter frame delay, not one bit later
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
     Magic number for how many ticks to delay as part of the backoff algorithm
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
/*      if (timer % MEDIUM_SPEED == 0) {
        System.out.println(timer / MEDIUM_SPEED + " seconds into the simulation, and we have " + this.events.size() + " left to send");
      }/*
      /**
       Check for expired on wire events
       
       This could get removed and put into it's own method
      */
      for (int i = 0; i < this.onWireEvents.size(); i++) {
        if (this.onWireEvents.get(i).getFinishedSlot() <= timer) {
          /**
           Mark the event as completed and push it into the completed array list
          */
          this.onWireEvents.get(i).setFinished(timer);
          this.completedEvents.add(this.onWireEvents.get(i));
          
          /**
           Remove it from the wire
          */
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
        if (this.mediumClear(next.getSource(), timer) && next.getTimeSlot() <= timer) {
          this.events.remove(0);
          this.onWireEvents.add(next);
          
          if (next.getSource().getFirstFrameSeen() == 0) {
            next.getSource().setFirstFrameSent(timer);
          }
          
          next.getSource().setLastFrameSent(timer);
        }
      }
      
      /**
       If there's more than 1 events in the onWireEvents array list, we have a collision
       
       Check if the events are inside or outside of the 512 bit detection window
       
       Loop through each of the nodes, and see if one with a bit on the wire is within the time/distance to notice the other
      */
      if (this.onWireEvents.size() > 1) { // this isn't quite what it should be
        // in this case, we push all events back onto this.events at random intervals...Event will need to track the retries (the exponential backoff algorithm)

        /**
         We need to loop through each of the events on the wire, and if they're part of this collision, we need to increment
         their retry counter and schedule them to run again in the future according to the backoff algorithm
        */
        for (Event event : this.onWireEvents) {
          retries = event.incrementRetries();
          
          /**
           2) resend the frame immediately (on the next timer cycle) or after 51.2 microseconds (random selection)
           3) If that fails: k * 51.2 microseconds where k = 0,1,2,3
           4) If it's still failing, k * 51.2 microseconds where k is [0, 2^3 - 1]
           5) If it's still failing, try up to [0, 2^10 - 1]
           Note: 51.2 microseconds = 536.870912 clock ticks in our simulation
          */
          
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
            
            System.out.println("Triggering resend of frame with delay factor " + delay + " on retry " + retries + ": " + event);
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
  
  private void statistics() {
    // 1280 frames * 512 bytes -> bits, this number is per node
    int total_data_transmitted = PACKETS_EACH * (PACKET_SIZE * 8);
    long frames_sent = 0;
    double speed = 0.0;
    double delay_in_seconds = 0.0;
    int time_taken_to_transmit = 0;
    
    /**
     Human readable statistics output
    */
    for (Node node : this.nodes) {
      speed = 0.0;
      frames_sent = 0;
      
      time_taken_to_transmit = node.getLastFrameSeen() - node.getFirstFrameSeen(); // time in bits
      frames_sent = this.calculateFramesDelivered(node, node.getDestinationNode());
      delay_in_seconds = this.calculateDelay(node, node.getDestinationNode());
      
      speed = (double)(frames_sent * PACKET_SIZE * 8) / ((double)time_taken_to_transmit / MEDIUM_SPEED / BIT_FACTOR);
      System.out.println("Speed to transmit from " + node.getMacAddress() + " -> " + node.getDestinationNode() + ": " + (Math.floor(speed * 10000) / 10000) + " Mbps with an average delay of " + delay_in_seconds + " seconds and " + frames_sent + " successfully transmitted frames");
    }
    
    /**
     CSV output
    */
    for (Node node : this.nodes) {
      speed = 0.0;
      frames_sent = 0;
      
      time_taken_to_transmit = node.getLastFrameSeen() - node.getFirstFrameSeen(); // time in bits
      frames_sent = this.calculateFramesDelivered(node, node.getDestinationNode());
      delay_in_seconds = this.calculateDelay(node, node.getDestinationNode());
      
      speed = (double)(frames_sent * PACKET_SIZE * 8) / ((double)time_taken_to_transmit / MEDIUM_SPEED / BIT_FACTOR);
      System.out.println(node.getMacAddress() + "," + node.getDestinationNode() + "," + (Math.floor(speed * 10000) / 10000) + " Mbps," + delay_in_seconds);
    }
  }
  
  /**
   Helper function to check if the medium is clear to send
   
   @param source The source node, the dstination is pulled from the various events (if any)
   @param Timer the current time
   @return True if the medium is clear (or appears clear), false if data transferring currently
  */
  private boolean mediumClear(Node source, int timer) {
    Random generator = new Random();
    int distance_propogated_from_source = 0;
    
    for (Event event : this.onWireEvents) {
      /**
        We need to check if bits from the sender would have reached our node at this time
      */
      if ((timer - event.getTimeSlot()) <= this.distanceBetweenNodes(source, event.getDestination())) {
        /**
         No frames on the wire within 512 bits of distance
         
         Any possible bits of a frame haven't reached us yet
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
  
  /**
   Determine the distance between two nodes.  Distance is given as a position (to the right) or negative (to the left)
   value which indicates distance from the center of the segment.
   
   @param source The first node
   @param destination The second node
   @return The distance (always positive) in bits
  */
  private int distanceBetweenNodes(Node source, Node destination) {
    final int SPEED_OF_LIGHT_IN_COPPER = 210000000; // m/s
    int distance_in_meters = 0;
    
    /**
     Both nodes are to the left of the center of the segment or both to the right of the segment
    */
    if ((source.getDistance() < 0 && destination.getDistance() < 0) || (source.getDistance() > 0 && destination.getDistance() > 0)) {
      distance_in_meters = Math.abs(source.getDistance() - destination.getDistance());
    } else {
     /**
      Both nodes are on opposite sides of the segment
     */ 
     distance_in_meters = (Math.abs(source.getDistance()) + Math.abs(destination.getDistance()));
    }
    
    return (int)Math.ceil((((double)distance_in_meters / SPEED_OF_LIGHT_IN_COPPER) / ((double)1 / MEDIUM_SPEED)));
  }
  
  /**
   Determine if we've detected any collisions
   
   @param events The list of events on the wire
   @param timer The current simulation time ticket
   @return true if collision detected, otherwise false
  */
  private boolean detectCollision(ArrayList<Event> events, int timer) {
    /**
     For each event on the wire, we want to see if it's reached another node which is also currently sending traffic
     (also in the events list)
    */
    for (Event event : events) {
      Node source = event.getSource();
      
      for (Event check_event : events) {
        if ((timer - check_event.getTimeSlot()) < this.distanceBetweenNodes(source, check_event.getDestination()) && event != check_event) {
          return false;
        }
      }
    }
    
    /**
     If we've gotten this far in the method, we don't have a collision
    */
    return true;
  }
  
  /**
   Calculate the delay across all frames between the given source and destination
   
   @param source The source node
   @param destination The destination node
   
   @return The average delay in seconds
  */
  private double calculateDelay(Node source, Node destination) {
    double total_delay = 0.0;
    
    for (Event event : this.completedEvents) {
      /**
       Check if the event was between our given source and destination nodes
      */
      if (event.getSource() == source && event.getDestination() == destination) {
        total_delay += event.getFinished() - event.getStarted();
      }
    }
    
    return ((total_delay / this.calculateFramesDelivered(source, destination)) / MEDIUM_SPEED);
  }
  
  /**
   Count how many frames were sent between two nodes.
   
   @param source The source node
   @param destination The destination node
   
   @return Number of frames sent between the two nodes
  */
  private long calculateFramesDelivered(Node source, Node destination) {
    long frames = 0;
    
    for (Event event : this.completedEvents) {
      if (event.getSource() == source && event.getDestination() == destination) {
        frames++;
      }
    }
    
    return frames;
  }
}