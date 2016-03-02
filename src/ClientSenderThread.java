import java.util.Hashtable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientSenderThread implements Runnable {

    private BlockingQueue<MPacket> eventQueue = null;
    private Map<String, MSocket> neighbours_socket;
    private AtomicInteger squenceNumber;
    private Queue<MPacket> receivedQueue = null;
    private AtomicInteger lamportClock;
    private Hashtable<Integer, SenderPacketInfo> waitingToResend;

    public ClientSenderThread(AtomicInteger sequencenumber, BlockingQueue<MPacket> eventQueue, Map<String, MSocket> neighbours_socket, Queue<MPacket> receivedQueue, AtomicInteger lamportClock, Hashtable<Integer, SenderPacketInfo> waitingToResend) {
        this.eventQueue = eventQueue;
        this.neighbours_socket = neighbours_socket;
        this.receivedQueue = receivedQueue;
        this.lamportClock = lamportClock;
        this.waitingToResend = waitingToResend;
        this.squenceNumber = sequencenumber;
    }

    public void run() {
        MPacket toClient = null;
        if (Debug.debug) System.out.println("Starting ClientSenderThread");

        try {
            while (true) {
                //Take packet from queue
                toClient = (MPacket) eventQueue.take();
                if (Debug.debug) System.out.println("Sending " + toClient);
                //mSocket.writeObject(toClient);


                // first broadcast
                toClient.timestamp = lamportClock.incrementAndGet();
                toClient.sequenceNumber = this.squenceNumber.incrementAndGet();;

                //Initlize the List for ack
                Hashtable<String, Boolean> All_neighbour = new Hashtable<String, Boolean>();
                for (Map.Entry<String, MSocket> e : this.neighbours_socket.entrySet()) {
                    All_neighbour.put(e.getKey(), false);
                }

                // Initlize time
                long time = System.currentTimeMillis();
                SenderPacketInfo info = new SenderPacketInfo(All_neighbour, this.squenceNumber.get(), time, toClient);

                for (Map.Entry<String, MSocket> e : neighbours_socket.entrySet()) {
                    MSocket each_client_socket = e.getValue();
                    each_client_socket.writeObject(toClient);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }


    }
}


