package client;
/* code complet pour Client ici */
import common.MD5Util;
import common.Protocol;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Client downloads a file from server in multiple blocks concurrently.
 */
public class Client {

    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private final String serverIp;
    private final int serverPort;
    private final String fileId;
    private final int dc;
    private final int blockSize;
    private final File downloadPath;

    public Client(String serverIp, int serverPort, String fileId, int dc, int blockSize, File downloadPath) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.fileId = fileId;
        this.dc = dc;
        this.blockSize = blockSize;
        this.downloadPath = downloadPath;
    }

    public void start() throws Exception {
        List<Integer> pendingBlocks = new ArrayList<>();

        // Get total blocks
        try (Socket socket = new Socket(serverIp, serverPort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF(Protocol.LIST);
            int totalBlocks = in.readInt();
            for (int i = 0; i < totalBlocks; i++) {
                pendingBlocks.add(i);
            }
        }

        ExecutorService pool = Executors.newFixedThreadPool(dc);
        for (int blockId : pendingBlocks) {
            pool.submit(() -> {
                try {
                    downloadBlock(blockId);
                } catch (Exception e) {
                    logger.severe("Block download failed: " + blockId);
                }
            });
        }

        pool.shutdown();
        while (!pool.isTerminated()) {
            Thread.sleep(100);
        }

        verifyMD5();
    }

    private void downloadBlock(int blockId) throws Exception {
        Socket socket = new Socket(serverIp, serverPort);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        DataInputStream in = new DataInputStream(socket.getInputStream());

        out.writeUTF(Protocol.DOWNLOAD);
        out.writeUTF(fileId);
        out.writeInt(blockId);

        int size = in.readInt();
        byte[] buffer = new byte[size];
        in.readFully(buffer);

        FileOutputStream fos = new FileOutputStream(new File(downloadPath, fileId + "_" + blockId));
        fos.write(buffer);
        fos.close();

        socket.close();
    }

    private void verifyMD5() throws Exception {
        // Concatenate all blocks
        File finalFile = new File(downloadPath, fileId + "_full");
        try (FileOutputStream fos = new FileOutputStream(finalFile)) {
            for (int i = 0; ; i++) {
                File block = new File(downloadPath, fileId + "_" + i);
                if (!block.exists()) break;
                FileInputStream fis = new FileInputStream(block);
                byte[] buffer = new byte[blockSize];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fis.close();
            }
        }

        String md5 = MD5Util.computeMD5(finalFile);

        try (Socket socket = new Socket(serverIp, serverPort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            out.writeUTF(Protocol.VALIDATE);
            out.writeUTF(fileId);
            out.writeUTF(md5);
        }

        logger.info("Client finished download and verification!");
    }
}
