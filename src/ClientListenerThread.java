import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientListenerThread implements Runnable {

    private MSocket mSocket = null;
    private Queue incomingQueue = null;

    public ClientListenerThread(Queue incomingQueue, MSocket mSocket) {

        this.incomingQueue = incomingQueue;
        this.mSocket = mSocket;

        if (Debug.debug) System.out.println("Instatiating ClientListenerThread");
    }

    public void run() {
        MPacket received = null;

        //run dequeue thread
        //new IncomingMessageHandleThread(incomingQueue, receivedQueue, displayQueue, actionHoldingCount, neighbours_socket, clientTable).start();

        if (Debug.debug) System.out.println("Starting ClientListenerThread");


        try {
            while (true) {
                received = (MPacket) mSocket.readObject();
                incomingQueue.add(received);
            }
        } catch (IOException e) {
            Thread.currentThread().interrupt();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
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
