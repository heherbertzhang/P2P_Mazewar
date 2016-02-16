import java.io.Serializable;
import java.util.Map;

/**
 * Created by herbert on 2016-02-04.
 */
public class IpPacket implements Serializable{
    public String hostName;
    public IpLocation Ip;
    public int statusCode;

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

    public IpBroadCastPacket(Map cs){
        this.mClientTable = cs;
    }
}