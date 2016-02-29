import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.BlockingQueue;


/**
 * Created by jiayuhe on 2016-02-27.
 */
public class ConfirmationBroadcast  extends Thread {
    private Map<String, MSocket> neighbours_socket;
    private BlockingQueue<MPacket> confirmation;
    private AtomicInteger sequenceNumber;
    private Hashtable<Integer, SenderPacketInfo> waitingToResend;

    public ConfirmationBroadcast(AtomicInteger sequenceNumber,BlockingQueue<MPacket> confirmation,Map<String, MSocket> neighbours_socket,Hashtable<Integer, SenderPacketInfo> waitingToResend ){
        this.confirmation = confirmation;
        this.sequenceNumber = sequenceNumber;
        this.neighbours_socket = neighbours_socket;
        this.waitingToResend = waitingToResend;
    }

    @Override
    public void run() {
        MPacket toClient = null;
        try{
            while (true){
                toClient = (MPacket) confirmation.take();
                toClient.sequenceNumber = sequenceNumber.incrementAndGet();
                //Initlize the List for ack
                Hashtable<String, Boolean> All_neighbour = new Hashtable<String, Boolean>();
                for (Map.Entry<String, MSocket> e : this.neighbours_socket.entrySet()) {
                    All_neighbour.put(e.getKey(), false);
                }
                // Initlize time
                long physicalTime = System.currentTimeMillis();
                SenderPacketInfo info = new SenderPacketInfo(All_neighbour, physicalTime, toClient);
                waitingToResend.put(toClient.sequenceNumber, info);//may need resend if drop the package

                System.out.println("sending confirmation: " + toClient.toString());
                for (Map.Entry<String, MSocket> e : neighbours_socket.entrySet()) {
                    MSocket each_client_socket = e.getValue();
                    each_client_socket.writeObject(toClient);
                }

            }

        }catch (InterruptedException e){
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
