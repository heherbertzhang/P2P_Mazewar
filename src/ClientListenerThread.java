import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class ClientListenerThread implements Runnable {

    private MSocket mSocket  =  null;
    private Hashtable<String, Client> clientTable = null;
    private Queue mIncomingQueue = null;
    public ClientListenerThread( MSocket mSocket,
                                Hashtable<String, Client> clientTable){
        this.mSocket = mSocket;
        this.clientTable = clientTable;
        this.mIncomingQueue = new PriorityBlockingQueue<MPacket>(10, new PacketComparator());
        
        if(Debug.debug) System.out.println("Instatiating ClientListenerThread");
    }

    public void run() {
        MPacket received = null;
        
        //run dequeue thread
        new Thread(new DequeueThread(mIncomingQueue, clientTable)).start();
        
        if(Debug.debug) System.out.println("Starting ClientListenerThread");
        
       
        
        while(true){
            try{
                received = (MPacket) mSocket.readObject();
                System.out.println("Received in clt " + received);
                
                //add to incoming queue
                mIncomingQueue.add(received);
                MPacket peek = (MPacket)mIncomingQueue.peek();
                if (Debug.debug) {
                    if (peek != null) {
                        System.out.println("adding, current peek is " + peek.sequenceNumber);
                    } else {
                        System.out.println("add fail");
                    }
                }
               
            }catch(IOException e){
                Thread.currentThread().interrupt();    
            }catch(ClassNotFoundException e){
                e.printStackTrace();
            }            
        }
    }
}
class DequeueThread implements Runnable{
    private Hashtable<String, Client> clientTable = null;
    private Queue mIncomingQueue = null;
    private int mNextSequenceNum = 0;
    public DequeueThread(Queue queue, Hashtable<String, Client> clientTable){
        this.mNextSequenceNum = 0;
        this.mIncomingQueue = queue;
        this.clientTable = clientTable;
    }
    public void run(){
        if(Debug.debug) System.out.println("Starting dequeue thread");
        Client client = null;
        while(true){
                MPacket peek = (MPacket)mIncomingQueue.peek();
                while(peek == null || peek.sequenceNumber != mNextSequenceNum){
                
                    if(Debug.debug && peek != null) System.out.println("waiting, current peek is " + peek.sequenceNumber);
                    peek = (MPacket)mIncomingQueue.peek();
                }
                mNextSequenceNum++;
                if(Debug.debug) System.out.println("sent");
                //now the sequence matched
                MPacket received = (MPacket)mIncomingQueue.remove();
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

class PacketComparator implements Comparator<MPacket>{
    @Override
    public int compare(MPacket x, MPacket y){
        if(x.sequenceNumber < y.sequenceNumber){
            return -1;
        }
        if(x.sequenceNumber > y.sequenceNumber){
            return 1;
        }
        return 0;
    }
}
