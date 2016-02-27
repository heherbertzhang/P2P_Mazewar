import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientSenderThread implements Runnable {

    private BlockingQueue<MPacket> eventQueue = null;
    private Map<String, MSocket> neighbours_socket;
    private int squenceNumber;
	private Queue receivedQueue = null;
    private AtomicInteger lamportClock;
    private Hashtable<Integer,SenderPacketInfo> waitingToResend;
    public ClientSenderThread(BlockingQueue eventQueue, Map<String, MSocket> neighbours_socket, Queue receivedQueue, AtomicInteger lamportClock, Hashtable<Integer,SenderPacketInfo> waitingToResend){
        this.eventQueue = eventQueue;
        this.neighbours_socket = neighbours_socket;
		this.receivedQueue = receivedQueue;
        this.lamportClock = lamportClock;
        this.waitingToResend = waitingToResend;
        this.squenceNumber = 0;

    }
    
    public void run() {
        MPacket toClient = null;
        if(Debug.debug) System.out.println("Starting ClientSenderThread");
        while(true){
            try{                
                //Take packet from queue
                toClient = (MPacket)eventQueue.take();
                if(Debug.debug) System.out.println("Sending " + toClient);
                //mSocket.writeObject(toClient);
                this.squenceNumber = this.squenceNumber + 1;

				// first broadcast
                toClient.timestamp = lamportClock.incrementAndGet();
                toClient.sequenceNumber = this.squenceNumber;

                //Initlize packet
                Hashtable<String, Boolean> All_neighbour = new Hashtable <String, Boolean>();
                for (Map.Entry<String, MSocket> e: this.neighbours_socket.entrySet()){
                    All_neighbour.put(e.getKey(), false);
                }

                // Initlize time
                long time = System.currentTimeMillis();
                SenderPacketInfo info = new SenderPacketInfo(All_neighbour, this.squenceNumber, time);
                for (Map.Entry e : neighbours_socket.entrySet()){
                     MSocket each_client_socket = (MSocket) e.getValue();
                     each_client_socket.writeObject(toClient);
                }
            }catch(InterruptedException e){
                e.printStackTrace();
                Thread.currentThread().interrupt();    
            }
            
        }
    }
}


