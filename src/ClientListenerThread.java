import java.io.IOException;
import java.util.*;

public class ClientListenerThread implements Runnable {

    private MSocket mSocket = null;
    private Queue<MPacket> incomingQueue = null;

    public ClientListenerThread(Queue<MPacket> incomingQueue, MSocket mSocket) {

        this.incomingQueue = incomingQueue;
        this.mSocket = mSocket;

        if (Debug.debug) System.out.println("Instatiating ClientListenerThread");
    }

    public void run() {


        //run dequeue thread
        //new IncomingMessageHandleThread(incomingQueue, receivedQueue, displayQueue, actionHoldingCount, neighbours_socket, clientTable).start();

        if (Debug.debug) System.out.println("Starting ClientListenerThread");


        try {
            while (true) {

                MPacket received = (MPacket) mSocket.readObject();

                //System.out.println("listening: " + received.toString());
                if(!incomingQueue.offer(received)){
                    assert false;
                }
                /*
                for(MPacket packet: incomingQueue){
                    System.out.println("incomingqueue:" + packet.toString());
                }*/
                //System.out.println("added to incoming queue");
            }
        } catch (IOException e) {
            Thread.currentThread().interrupt();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
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
