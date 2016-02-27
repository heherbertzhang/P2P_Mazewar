import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

public class ClientSenderThread implements Runnable {

    private BlockingQueue<MPacket> eventQueue = null;
    private Map<String, MSocket> neighbours_socket;
	private Queue receivedQueue = null;
    private string localname ;
    public ClientSenderThread(BlockingQueue eventQueue, Map<String, MSocket> neighbours_socket, Queue receivedQueue, string name){
        this.eventQueue = eventQueue;
        this.neighbours_socket = neighbours_socket;
		this.receivedQueue = receivedQueue;
        this.localname = name;
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

				// first broadcast
				toClient.type = MPacket.ACTION;
                toClient.name = localname;
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


