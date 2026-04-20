package com.group15.daugia.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class ClientHandler implements Runnable {
  Socket curClient;
  Map<String, Workable> works = Work.getWorks();

  public ClientHandler(Socket socket) {
    curClient = socket;
  }

  @Override
  public void run() {
    try (PrintWriter out = new PrintWriter(curClient.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(curClient.getInputStream()))) {
      String req = in.readLine();
      String data = in.readLine();

      Workable job = works.get(req);
      System.out.println("Aquire work");
      if (job != null) {
        out.println(job.work(data));
      } else {
        out.println("INVALID");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
