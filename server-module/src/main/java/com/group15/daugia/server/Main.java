package com.group15.daugia.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  public static void main(String[] args) {
    // Bootstrap AuctionClock: load và lên lịch tất cả auction SCHEDULED/ACTIVE từ DB
    AuctionClock.getInstance().bootstrap();

    try {
      AuctionServer server = new AuctionServer(8080);
      server.start();
    } catch (IOException e) {
      System.out.println("Error in server");
      e.printStackTrace();
    }
  }
}
