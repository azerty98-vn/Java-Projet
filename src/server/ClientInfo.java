package server;
/* code complet pour ClientInfo ici */
import java.net.InetAddress;
import java.util.Objects;

/**
 * Represents a trusted client with its IP address and port.
 * Used for delegation in the Server.
 */
public class ClientInfo {
    private final InetAddress address;
    private final int port;

    public ClientInfo(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientInfo)) return false;
        ClientInfo that = (ClientInfo) o;
        return port == that.port && address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }
}
