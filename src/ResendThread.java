import com.sun.org.apache.regexp.internal.RE;

import java.util.Hashtable;
import java.util.Map;

/**
 * Created by jiayuhe on 2016-02-27.
 */

public class ResendThread extends Thread {
    private long timeout;
    public Map<String, MSocket> neighbours_socket;
    public Hashtable<Integer,SenderPacketInfo> waitToResendQueue;
    public ResendThread(long timeout, Hashtable<Integer,SenderPacketInfo> waitToResendQueue, Map<String, MSocket> neighbours_socket){
        this.timeout = timeout;
        this.waitToResendQueue = waitToResendQueue;
        this.neighbours_socket = neighbours_socket;
    }

    @Override
    public void run() {
        while(true){
            for (Map.Entry<Integer,SenderPacketInfo> e: waitToResendQueue.entrySet()){
                long currenttime = System.currentTimeMillis();
                if (currenttime - e.getValue().time > timeout){
                    // means timeout
                    // resend the messages to all the clients who didn't give me respond

                    Hashtable<String, Boolean> lostClients= e.getValue().Ack_From_All;
                    for (Map.Entry k : lostClients.entrySet()) {
                        MSocket lostClientSocket = neighbours_socket.get(k.getKey());
                        lostClientSocket.writeObject(e.getValue().Packet);
                    }
                }
            }
        }

    }
}
