package server;
/* code complet pour Server ici */
import common.Protocol;
import common.MD5Util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Server class: Handles client requests to list files, send file blocks, and validate downloads.
 * Synchronization is used on shared resources (trusted clients list, active connections counter).
 */
public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private static final Map<String, File> fileStorage = new HashMap<>();
    private static final Map<String, Set<ClientInfo>> trustedClients = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Object connectionLock = new Object();

    private static int CS = 5; // Max concurrent requests
    private static double P = 0.1; // Probability of failure
    private static int T = 10; // Time interval for simulating failures
    private static int activeConnections = 0;
    private static List<Socket> currentConnections = new ArrayList<>();

    private static int blockSize = 1024; // 1KB blocks by default

    public static void main(String[] args) throws Exception {
        int port = 1234; // default

        for (String arg : args) {
            if (arg.startsWith("--port=")) port = Integer.parseInt(arg.split("=")[1]);
            if (arg.startsWith("--CS=")) CS = Integer.parseInt(arg.split("=")[1]);
            if (arg.startsWith("--P=")) P = Double.parseDouble(arg.split("=")[1]);
            if (arg.startsWith("--T=")) T = Integer.parseInt(arg.split("=")[1]);
            if (arg.startsWith("--B=")) blockSize = Integer.parseInt(arg.split("=")[1]);
        }

        // Load files into server (for simplicity, load from a folder "server_files/")
        File folder = new File("server_files");
        if (!folder.exists()) {
            System.err.println("Folder server_files/ does not exist!");
            System.exit(1);
        }
        for (File file : folder.listFiles()) {
            fileStorage.put(file.getName(), file);
        }

        ServerSocket serverSocket = new ServerSocket(port);
        logger.info("Server started on port " + port);

        Timer failureTimer = new Timer();
        failureTimer.schedule(new TimerTask() {
            public void run() {
                simulateFailures();
            }
        }, T * 1000L, T * 1000L);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            synchronized (connectionLock) {
                if (activeConnections < CS) {
                    activeConnections++;
                    currentConnections.add(clientSocket);
                    executor.submit(new ClientHandler(clientSocket));
                } else {
                    delegateOrQueue(clientSocket);
                }
            }
        }
    }

    private static void simulateFailures() {
        Random random = new Random();
        synchronized (connectionLock) {
            if (!currentConnections.isEmpty() && random.nextDouble() < P) {
                int idx = random.nextInt(currentConnections.size());
                Socket s = currentConnections.get(idx);
                try {
                    s.close();
                    currentConnections.remove(idx);
                    activeConnections--;
                    logger.warning("Simulated failure: closed a client connection");
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void delegateOrQueue(Socket clientSocket) {
        logger.info("Queueing client request (no delegation implemented)");
        executor.submit(new ClientHandler(clientSocket));
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

                String command = in.readUTF();
                if (command.equals(Protocol.LIST)) {
                    // Send the number of available blocks for a given file
                    String requestedFile = in.readUTF();
                    File file = fileStorage.get(requestedFile);
                    if (file == null) {
                        out.writeInt(0);
                        return;
                    }
                    BlockManager bm = new BlockManager(file, blockSize);
                    out.writeInt(bm.getTotalBlocks());
                }
                else if (command.equals(Protocol.DOWNLOAD)) {
                    String requestedFile = in.readUTF();
                    int blockId = in.readInt();
                    File file = fileStorage.get(requestedFile);
                    if (file == null) return;
                    BlockManager bm = new BlockManager(file, blockSize);
                    byte[] block = bm.readBlock(blockId);

                    out.writeInt(block.length);
                    out.write(block);
                }
                else if (command.equals(Protocol.VALIDATE)) {
                    String requestedFile = in.readUTF();
                    String clientMd5 = in.readUTF();
                    File file = fileStorage.get(requestedFile);
                    String realMd5 = MD5Util.computeMD5(file);
                    if (clientMd5.equals(realMd5)) {
                        logger.info("Client validated successfully for file " + requestedFile);
                        // (add to trusted clients if needed)
                    }
                }

            } catch (IOException | RuntimeException e) {
                logger.severe("Error with client: " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                synchronized (connectionLock) {
                    activeConnections--;
                    currentConnections.remove(clientSocket);
                    try {
                        clientSocket.close();
                    } catch (IOException ignored) {}
                }
            }
        }
    }
}
