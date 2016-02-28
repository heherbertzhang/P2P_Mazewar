import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by herbert on 2016-02-04.
 */
public class IpPacket implements Serializable{
    public String hostName;
    public IpLocation Ip;
    public int statusCode;
    public Player player;

    public IpPacket(String hostName, String host, int port){
        this.hostName = hostName;
        this.Ip = new IpLocation(host, port);
    }

    public IpPacket(int statusCode){
        this.statusCode = statusCode;
    }
}

class IpLocation implements Serializable{
    public String hostAddress;
    public Integer port;
    public IpLocation(String host, int port){
        this.hostAddress = host;
        this.port = port;
    }
}

class IpBroadCastPacket implements Serializable{
    Map<String, IpLocation> mClientTable;
    List<Player> players;
    public IpBroadCastPacket(Map cs, List players){
        this.mClientTable = cs;
        this.players = players; //need copy constructor?
    }
}