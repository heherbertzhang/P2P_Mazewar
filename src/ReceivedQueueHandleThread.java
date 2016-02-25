import java.util.Hashtable;
import java.util.Map;
import java.util.Queue;

/**
 * Created by herbert on 2016-02-25.
 */
public class ReceivedQueueHandleThread extends Thread{

    private Hashtable<String, Client> clientTable = null;
    private Queue receivedQueue = null;
    private Queue displayQueue = null;
    private Queue eventQueue = null;
    private Map<String, MSocket> neighbousSockets = null;
    private Queue incomingQueue = null;
    private int mNextSequenceNum = 0;
    public ReceivedQueueHandleThread(Queue incoming, Queue receivedQueue, Queue displayQueue, Queue eventQueue, Map neighbours_socket, Hashtable<String, Client> clientTable){
        this.mNextSequenceNum = 0;
        this.receivedQueue = receivedQueue;
        this.displayQueue = displayQueue;
        this.neighbousSockets = neighbours_socket;
        this.eventQueue = eventQueue;
        this.clientTable = clientTable;
        this.incomingQueue = incoming;
    }
    public void run(){
        if(Debug.debug) System.out.println("Starting received queue handle thread");
        while(true){
            while ()
            boolean isActionInitiator = true;
            if(eventQueue.isEmpty()){
                isActionInitiator = false;
            }

        }
        Client client = null;
        while(true){
            MPacket peek = (MPacket) receivedQueue.peek();
            while(peek == null || peek.sequenceNumber != mNextSequenceNum){

                if(Debug.debug && peek != null) System.out.println("waiting, current peek is " + peek.sequenceNumber);
                peek = (MPacket) receivedQueue.peek();
            }
            mNextSequenceNum++;
            if(Debug.debug) System.out.println("sent");
            //now the sequence matched
            MPacket received = (MPacket) receivedQueue.remove();
            if(Debug.debug) System.out.println("ready to take action");
            client = clientTable.get(received.name);
            if(received.event == MPacket.UP){
                client.forward();
            }else if(received.event == MPacket.DOWN){
                client.backup();
            }else if(received.event == MPacket.LEFT){
                client.turnLeft();
            }else if(received.event == MPacket.RIGHT){
                client.turnRight();
            }else if(received.event == MPacket.FIRE){
                System.out.println(client.getName() + " about to call fire()");
                client.fire();
            } else if (received.event == MPacket.DIE) {
                Player newPosition = received.players[0];
                Client sourceClient = clientTable.get(received.players[1].name);
                //Client destClient = clientTable.get(newPosition.name);
                client.die(sourceClient, newPosition);
            } else if (received.event == MPacket.MOVE_BULLET) {
                //int prj = received.projectile;
                String prj = received.name;
                client.bullet_move(prj);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }
}

class DisplayThread extends Thread {
    public void run(){
        if(Debug.debug) System.out.println("Starting dequeue thread");
        Client client = null;
        while(true){
            MPacket peek = (MPacket) receivedQueue.peek();
            while(peek == null || peek.sequenceNumber != mNextSequenceNum){

                if(Debug.debug && peek != null) System.out.println("waiting, current peek is " + peek.sequenceNumber);
                peek = (MPacket) receivedQueue.peek();
            }
            mNextSequenceNum++;
            if(Debug.debug) System.out.println("sent");
            //now the sequence matched
            MPacket received = (MPacket) receivedQueue.remove();
            if(Debug.debug) System.out.println("ready to take action");
            client = clientTable.get(received.name);
            if(received.event == MPacket.UP){
                client.forward();
            }else if(received.event == MPacket.DOWN){
                client.backup();
            }else if(received.event == MPacket.LEFT){
                client.turnLeft();
            }else if(received.event == MPacket.RIGHT){
                client.turnRight();
            }else if(received.event == MPacket.FIRE){
                System.out.println(client.getName() + " about to call fire()");
                client.fire();
            } else if (received.event == MPacket.DIE) {
                Player newPosition = received.players[0];
                Client sourceClient = clientTable.get(received.players[1].name);
                //Client destClient = clientTable.get(newPosition.name);
                client.die(sourceClient, newPosition);
            } else if (received.event == MPacket.MOVE_BULLET) {
                //int prj = received.projectile;
                String prj = received.name;
                client.bullet_move(prj);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }
}