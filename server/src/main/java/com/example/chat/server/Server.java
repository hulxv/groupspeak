package com.example.chat.server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    
    private ServerSocket serverSocket = null;
    private int port;
    private static List<ClientHandler> connectedClients;
    private ExecutorService threadPool;
    private boolean running = true;
    private static final int THREAD_POOL_SIZE = 10;

    public Server(int port) {
        this.port = port;
        connectedClients = Collections.synchronizedList(new ArrayList<>());
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server Started on port " + port);

            while (running) {
                System.out.println("Connected Clients: " + connectedClients.size());
                System.out.println("Waiting for a client...");
                Socket socket = serverSocket.accept();
                System.out.println("Client accepted: " + socket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(socket);
                connectedClients.add(clientHandler);

                threadPool.execute(clientHandler);
            }
        }
        catch(IOException e) {
            System.err.println("Server error: " + e.getMessage());
            // e.printStackTrace();
        }
    }

    public static synchronized void removeClient(ClientHandler client) {
        try {
            client.getSocket().close();

        } catch (Exception e) {

        }


        connectedClients.remove(client);
        System.out.println("Client removed. Total clients: " + connectedClients.size());
    }

    public List<ClientHandler> getConnectedClients() {
        return new ArrayList<>(connectedClients);
    }

    public void shutdown() {
        try {
            running = false;
            List<ClientHandler> clientsToRemove = new ArrayList<>(connectedClients);
            for(ClientHandler client : clientsToRemove) {
                ProtocolParser.sendRaw("{\"type\":\"server_closed\"}", client.getFraming());
                removeClient(client);
            }

            if(serverSocket != null && !serverSocket.isClosed() && connectedClients.size() == 0) {
                serverSocket.close();
            }

            threadPool.shutdown();

            if(!threadPool.awaitTermination(1, TimeUnit.SECONDS));
        }
        catch(IOException | InterruptedException e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }
}
