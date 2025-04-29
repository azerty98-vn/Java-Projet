package client;
/* code complet pour TrustedClient ici */
import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.logging.Logger;

/**
 * TrustedClient can serve file blocks to other clients if they present a valid token.
 */
public class TrustedClient {

    private static final Logger logger = Logger.getLogger(TrustedClient.class.getName());

    private final int listenPort;
    private final File file;
    private final int blockSize;
    private final String expectedToken;

    public TrustedClient(int listenPort, File file, int blockSize, String expectedToken) {
        this.listenPort = listenPort;
        this.file = file;
        this.blockSize = blockSize;
        this.expectedToken = expectedToken;
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(listenPort);
        logger.info("Trusted client started on port " + listenPort);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> handleRequest(socket)).start();
        }
    }

    private void handleRequest(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            String receivedToken = in.readUTF();
            if (!receivedToken.equals(expectedToken)) {
                logger.warning("Invalid token received");
                socket.close();
                return;
            }

            int blockId = in.readInt();
            byte[] block = readBlock(blockId);
            out.writeInt(block.length);
            out.write(block);

        } catch (IOException e) {
            logger.severe("TrustedClient connection error: " + e.getMessage());
        }
    }

    private byte[] readBlock(int blockId) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        long start = (long) blockId * blockSize;
        raf.seek(start);
        int size = (int) Math.min(blockSize, file.length() - start);
        byte[] buffer = new byte[size];
        raf.readFully(buffer);
        raf.close();
        return buffer;
    }
}
