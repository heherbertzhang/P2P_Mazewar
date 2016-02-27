import java.util.Hashtable;

/**
 * Created by jiayuhe on 2016-02-27.
 */
public class SenderPacketInfo {
    public MPacket Packet;
    public Map<String, boolean> Ack_From_All;
    public int sequence_Number;
    public float time;
    public SenderPacketInfo(Hashtable<String, Boolean> All_neighbour, int sequence_Number, float time){
        this.sequence_Number = sequence_Number;
        this.Ack_From_All = All_neighbour:
        this.time = time;
    }
}
