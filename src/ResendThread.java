import java.util.Hashtable;
import java.util.Map;

/**
 * Created by jiayuhe on 2016-02-27.
 */

public class ResendThread extends Thread {
    private long timeout;
    public Map<String, MSocket> neighbours_socket;
    public Hashtable<Integer,SenderPacketInfo> waitToResendQueue;
    public ResendThread(long timeout, Map timeoutlist, Hashtable<Integer,SenderPacketInfo> waitToResendQueue, Map<String, MSocket> neighbours_socket){
        this.timeout = timeout;
        this.waitToResendQueue = waitToResendQueue;
        this.neighbours_socket = neighbours_socket;
    }

    @Override
    public void run() {
        System.out.println("starting resend thread: " + Thread.currentThread().getId());
        while(true){
            synchronized (waitToResendQueue) {
                for (Map.Entry<Integer, SenderPacketInfo> e : waitToResendQueue.entrySet()) {
                    long currenttime = System.currentTimeMillis();
                    if (currenttime - e.getValue().physicalTime > (timeout*e.getValue().resendtime)) {
                        // means timeout
                        // resend the messages to all the clients who didn't give me respond
                        e.getValue().resendtime = e.getValue().resendtime + 1;
                        Hashtable<String, Boolean> lostClients = (Hashtable<String, Boolean>) e.getValue().ackFromAll;
                        //update time
                        e.getValue().physicalTime = currenttime;
                        for (Map.Entry<String, Boolean> k : lostClients.entrySet()) {
                            MSocket lostClientSocket = neighbours_socket.get(k.getKey());
                            lostClientSocket.writeObject(e.getValue().packet);
                            System.out.println("resending to client:" + k.getKey() + " and packet:" + e.getValue().packet.toString());
                        }
                    }


                }
            }
        }

    }
}
