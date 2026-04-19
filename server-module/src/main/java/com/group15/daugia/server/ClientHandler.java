package com.group15.daugia.server;

import java.net.Socket;

public class ClientHandler {
  Socket curClient;

  public ClientHandler(Socket socket) {
    curClient = socket;
  }
}
