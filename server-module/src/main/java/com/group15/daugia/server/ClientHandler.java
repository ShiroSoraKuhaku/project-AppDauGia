package com.group15.daugia.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class ClientHandler {
  Socket curClient;

  public ClientHandler(Socket socket) {
    curClient = socket;
    Map<String, Workable> works = Work.getWorks();
    try (PrintWriter out = new PrintWriter(curClient.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(curClient.getInputStream()))) {
      String req = in.readLine();
      String data = in.readLine();

      Workable job = works.get(req);
      job.work(data);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
