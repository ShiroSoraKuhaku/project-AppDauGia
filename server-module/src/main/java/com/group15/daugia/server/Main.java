package com.group15.daugia.server;

import javax.imageio.IIOException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/** Hello world! */
public class Main {

  public static void main(String[] args) {
    Socket socket;
    try (ServerSocket serverSocket = new ServerSocket(8080)) {
      while (true) {
        socket = serverSocket.accept();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
