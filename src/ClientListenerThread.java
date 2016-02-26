import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientListenerThread implements Runnable {

    private Map<String, MSocket> neighbours_socket  =  null;
    private Hashtable<String, Client> clientTable = null;
	private Queue receivedQueue = null;
	private Queue displayQueue = null;
    private Queue incomingQueue = null;
    private AtomicInteger actionHoldingCount = null;
    public ClientListenerThread( Map<String, MSocket> neighbours_socket,
                                Hashtable<String, Client> clientTable, Queue receivedQueue, Queue displayQueue, Queue incomingQueue, AtomicInteger actionHoldingCount){
        this.neighbours_socket = neighbours_socket;
        this.clientTable = clientTable;
        this.receivedQueue = receivedQueue;
		this.displayQueue = displayQueue;
        this.incomingQueue = incomingQueue;
        this.actionHoldingCount = actionHoldingCount;
        if(Debug.debug) System.out.println("Instatiating ClientListenerThread");
    }

    public void run() {
        MPacket received = null;
        
        //run dequeue thread
        //new IncomingMessageHandleThread(incomingQueue, receivedQueue, displayQueue, actionHoldingCount, neighbours_socket, clientTable).start();
        
        if(Debug.debug) System.out.println("Starting ClientListenerThread");
        
       
        
        while(true){
            try{

                for (Map.Entry e : neighbours_socket.entrySet()){
                    received = (MPacket) ((MSocket)(e.getValue())).readObject();
                    incomingQueue.add(received);

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
