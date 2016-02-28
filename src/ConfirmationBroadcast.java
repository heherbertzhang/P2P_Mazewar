import java.util.Hashtable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.BlockingQueue;
import java.util.Hashtable;


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
                toClient.sequenceNumber = sequenceNumber.get();
                //Initlize the List for ack
                Hashtable<String, Boolean> All_neighbour = new Hashtable<String, Boolean>();
                for (Map.Entry<String, MSocket> e : this.neighbours_socket.entrySet()) {
                    All_neighbour.put(e.getKey(), false);
                }


                // Initlize time
                long time = System.currentTimeMillis();
                SenderPacketInfo info = new SenderPacketInfo(All_neighbour, this.sequenceNumber.get(), time, toClient);
                for (Map.Entry e : neighbours_socket.entrySet()) {
                    MSocket each_client_socket = (MSocket) e.getValue();
                    each_client_socket.writeObject(toClient);
                }

            }

        }catch (InterruptedException e){
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
