package com.group15.daugia.server;

import javax.imageio.IIOException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Main {

  public static void main(String[] args) {
    Socket socket;
    ArrayList<ClientHandler> clients = new ArrayList<>();
    ExecutorService threadPool = Executors.newFixedThreadPool(10);

    try (ServerSocket serverSocket = new ServerSocket(8080)) {
      System.out.println("Server on");
      while (true) {
        socket = serverSocket.accept();
        ClientHandler client = new ClientHandler(socket);
        System.out.println("New Connection");
        threadPool.execute(client);
        clients.add(client);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
