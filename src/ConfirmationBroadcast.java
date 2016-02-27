import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.BlockingQueue;

/**
 * Created by jiayuhe on 2016-02-27.
 */
public class ConfirmationBroadcast  extends Thread {
    private Map<String, MSocket> neighbours_socket;
    private BlockingQueue<MPacket> confirmation;
    private AtomicInteger sequenceNumber;

    public ConfirmationBroadcast(AtomicInteger sequenceNumber,BlockingQueue<MPacket> confirmation,Map<String, MSocket> neighbours_socket ){
        this.confirmation = confirmation;
        this.sequenceNumber = sequenceNumber;
        this.neighbours_socket = neighbours_socket;
    }

    @Override
    public void run() {
        MPacket toClient = null;
        try{
            while (true){
                toClient = (MPacket) confirmation.take();
                toClient.sequenceNumber = sequenceNumber.get();
            }

        }catch (InterruptedException e){

        }
    }
}
