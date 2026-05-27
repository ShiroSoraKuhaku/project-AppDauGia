package com.group15.daugia.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {
  private final int port;
  private ServerSocket serverSocket;
  Socket socket;
  private ArrayList<ClientHandler> clients;
  ExecutorService threadPool;
  private boolean isRunnin;

  public AuctionServer(int port) throws IOException {
    this.serverSocket = new ServerSocket(port);
    this.port = this.serverSocket.getLocalPort();

    this.threadPool = Executors.newFixedThreadPool(10);
    this.clients = new ArrayList<>();
  }

  public void start() {
    this.isRunnin = true;
    System.out.println("Server on");
    TokenClock.getInstance().start();
    try {
      while (true) {
        socket = serverSocket.accept();
        ClientHandler client = new ClientHandler(socket);
        System.out.println("New Connection");
        threadPool.execute(client);
        clients.add(client);
      }
    } catch (IOException e) {
      if (isRunnin) {
        System.out.println("Unexpected Error");
      }
    }
  }

  public void stop() {
    this.isRunnin = false;

    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    threadPool.shutdown();
    TokenClock.getInstance().shutdownAndClearTokens();
  }

  public int gerPort() {
    return this.port;
  }
}
