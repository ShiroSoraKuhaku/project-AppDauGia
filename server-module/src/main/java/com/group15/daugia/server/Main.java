package com.group15.daugia.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.TimeZone;

public class Main {

  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    SchemaMigration.ensureSchema();

    // Bootstrap AuctionClock: load và lên lịch tất cả auction SCHEDULED/ACTIVE từ DB
    AuctionClock.getInstance().bootstrap();

    try {
      AuctionServer server = new AuctionServer(8080);
      Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
      server.start();
    } catch (IOException e) {
      System.out.println("Error in server");
      e.printStackTrace();
    }
  }
}
