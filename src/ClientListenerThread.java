import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class ClientListenerThread implements Runnable {

    private MSocket mSocket  =  null;
    private Hashtable<String, Client> clientTable = null;
    private Queue mIncomingQueue = null;
	private Queue receivedQueue = null;
	private Queue displayQueue = null;
    public ClientListenerThread( MSocket mSocket,
                                Hashtable<String, Client> clientTable, Queue receivedQueue, Queue displayQueue){
        this.mSocket = mSocket;
        this.clientTable = clientTable;
        this.mIncomingQueue = new PriorityBlockingQueue<MPacket>(10, new PacketComparator());
		this.receivedQueue = receivedQueue;
		this.displayQueue = displayQueue;
        
        if(Debug.debug) System.out.println("Instatiating ClientListenerThread");
    }

    public void run() {
        MPacket received = null;
        
        //run dequeue thread
        new ReceivedQueueHandleThread(receivedQueue, displayQueue, neighbours_socket, clientTable).start();
        
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


/*
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
}*/
