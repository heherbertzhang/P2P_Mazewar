import java.util.Hashtable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientSenderThread implements Runnable {

    private BlockingQueue<MPacket> eventQueue = null;
    private Map<String, MSocket> neighbours_socket;
    private AtomicInteger squenceNumber;
    private AtomicInteger lamportClock;
    private Hashtable<Integer, SenderPacketInfo> waitingToResend;
    private Queue incomingQueue;

    public ClientSenderThread(AtomicInteger sequencenumber, BlockingQueue<MPacket> eventQueue, Map<String, MSocket> neighbours_socket, Queue<MPacket> incomingQueue, AtomicInteger lamportClock, Hashtable<Integer, SenderPacketInfo> waitingToResend) {
        this.eventQueue = eventQueue;
        this.neighbours_socket = neighbours_socket;
        this.lamportClock = lamportClock;
        this.waitingToResend = waitingToResend;
        this.squenceNumber = sequencenumber;
        this.incomingQueue = incomingQueue;
    }

    public void run() {

        if (Debug.debug) System.out.println("Starting ClientSenderThread: "  + Thread.currentThread().getId());


        while (true) {
            try {
                //Take packet from queue
                /*
                System.out.println("eventqueue!!!!!!!");
                for(MPacket p : eventQueue){
                    System.out.println(p.toString());
                }
                System.out.println("eventqueue end!!!!!!!!");
                */

                MPacket toClient = (MPacket) eventQueue.take();//must declare here as temp variable!!!!!!!!!!!!!!!!!!!
                //!!!!!!!!!!!!!!!!cannot declare out side since it will be reused that previous value!!!!!!!!!!!!!!!!!
                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                //mSocket.writeObject(toClient);


                // first broadcast
                toClient.timestamp = lamportClock.incrementAndGet();
                toClient.sequenceNumber = this.squenceNumber.incrementAndGet();
                ;
                System.out.println("Sending " + toClient.toString());

                //Initlize the List for ack
                Hashtable<String, Boolean> All_neighbour = new Hashtable<String, Boolean>();
                for (Map.Entry<String, MSocket> e : this.neighbours_socket.entrySet()) {
                    All_neighbour.put(e.getKey(), false);
                }
                // Initlize time
                long physicalTime = System.currentTimeMillis();
                SenderPacketInfo info = new SenderPacketInfo(All_neighbour, physicalTime, toClient);
                synchronized (waitingToResend) {
                    waitingToResend.put(toClient.sequenceNumber, info);//put to wait to resend queue
                }


                for (Map.Entry<String, MSocket> e : neighbours_socket.entrySet()) {
                    MSocket each_client_socket = e.getValue();
                    each_client_socket.writeObject(toClient);
                }
                //send to itself

                if (!incomingQueue.offer(toClient)) {
                    assert false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }


}



