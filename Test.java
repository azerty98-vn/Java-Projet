/* code complet pour Test ici */
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Test class: launches server and multiple clients automatically using ProcessBuilder.
 */
public class Test {

    public static void main(String[] args) throws Exception {
        int numberOfClients = 3; // default
        int DC = 4; // default concurrent downloads per client
        double P = 0.2; // probability of server closing connections
        int T = 10; // interval for simulating failures

        for (String arg : args) {
            if (arg.startsWith("--clients=")) {
                numberOfClients = Integer.parseInt(arg.split("=")[1]);
            }
            if (arg.startsWith("--DC=")) {
                DC = Integer.parseInt(arg.split("=")[1]);
            }
            if (arg.startsWith("--P=")) {
                P = Double.parseDouble(arg.split("=")[1]);
            }
            if (arg.startsWith("--T=")) {
                T = Integer.parseInt(arg.split("=")[1]);
            }
        }

        // Start the server
        ProcessBuilder serverBuilder = new ProcessBuilder(
                "java", "-cp", "src", "server.Server",
                "--port=1234", "--CS=5", "--P=" + P, "--T=" + T
        );
        serverBuilder.inheritIO();
        Process serverProcess = serverBuilder.start();
        Thread.sleep(2000); // Wait for the server to fully start

        // Start the clients
        List<Process> clients = new ArrayList<>();
        for (int i = 0; i < numberOfClients; i++) {
            ProcessBuilder clientBuilder = new ProcessBuilder(
                    "java", "-cp", "src", "client.Client",
                    "--serverIp=127.0.0.1", "--serverPort=1234",
                    "--file=testfile.txt", "--DC=" + DC, "--downloadPath=downloads/"
            );
            clientBuilder.inheritIO();
            Process clientProcess = clientBuilder.start();
            clients.add(clientProcess);
        }

        // Wait for all clients to finish
        for (Process client : clients) {
            client.waitFor();
        }

        System.out.println("All clients finished downloading!");

        // Stop server manually (optional)
        serverProcess.destroy();
    }
}
