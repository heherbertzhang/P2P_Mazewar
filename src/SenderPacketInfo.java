import java.util.Hashtable;
import java.util.Map;

/**
 * Created by jiayuhe on 2016-02-27.
 */
public class SenderPacketInfo {
    public MPacket packet;
    public Hashtable<String, Boolean> ackFromAll;
    public int sequence_Number;
    public float time;
    public int getReleasedCount;

    public SenderPacketInfo(Hashtable<String, Boolean> All_neighbour, int sequence_Number, long time, MPacket packet){
        this.sequence_Number = sequence_Number;
        this.ackFromAll = All_neighbour;
        this.time = time;
        this.packet = packet;
        this.getReleasedCount = 0;
    }
    public void acknowledgeReceivedFrom(String name){
        ackFromAll.remove(name);
    }
    public void getReleasedFrom(String name){
        ackFromAll.remove(name);
        getReleasedCount += 1;
    }

    public Map whoToResend(){
        return ackFromAll;
    }

    public MPacket whatToResend(){
        return packet;
    }
}
