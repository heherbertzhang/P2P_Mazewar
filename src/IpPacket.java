import java.io.Serializable;

/**
 * Created by herbert on 2016-02-04.
 */
public class IpPacket implements Serializable{
    public String hostName;
    public IpLocation Ip;
    public int statusCode;

    public IpPacket(String host, String hostName, int port){
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
