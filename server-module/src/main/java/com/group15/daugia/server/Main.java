package com.group15.daugia.server;

import javax.imageio.IIOException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/** Hello world! */
public class Main {

  public static void main(String[] args) {
    Socket socket;
    ArrayList<ClientHandler> clients = new ArrayList<>();
    try (ServerSocket serverSocket = new ServerSocket(8080)) {
      while (true) {
        socket = serverSocket.accept();
        ClientHandler client = new ClientHandler(socket);
        clients.add(client);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
