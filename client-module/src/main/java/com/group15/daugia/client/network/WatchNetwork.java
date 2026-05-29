package com.group15.daugia.client.network;

import com.google.gson.Gson;
import com.group15.daugia.shared.JSON.JSONAuctionEventTemp;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Kết nối socket persistent cho WATCH-AUCTION / UNWATCH-AUCTION.
 * Sau khi gửi WATCH-AUCTION, server trả ACK snapshot rồi tiếp tục push event.
 */
public class WatchNetwork {

  private static final String HOST = "localhost";
  private static final int PORT = 8080;

  private final Gson gson = new Gson();
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private Thread listenerThread;
  private volatile boolean running = false;

  /**
   * Bắt đầu watch auction.
   *
   * @param auctionId  ID phiên đấu giá
   * @param token      Token xác thực
   * @param onSnapshot Callback nhận snapshot đầu tiên (ACK từ server)
   * @param onEvent    Callback nhận các push event tiếp theo
   */
  public void startWatch(
      int auctionId,
      String token,
      Consumer<JSONAuctionTemp> onSnapshot,
      Consumer<JSONAuctionEventTemp> onEvent) {

    try {
      socket = new Socket(HOST, PORT);
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      // Gửi request WATCH-AUCTION
      String requestJson = "{\"auctionId\":" + auctionId + ",\"token\":\"" + token + "\"}";
      out.println("WATCH-AUCTION");
      out.println(requestJson);

      // Đọc dòng đầu tiên — ACK snapshot
      String firstLine = in.readLine();
      if (firstLine != null && onSnapshot != null) {
        JSONAuctionTemp snapshot = gson.fromJson(firstLine, JSONAuctionTemp.class);
        onSnapshot.accept(snapshot);
      }

      // Bắt đầu lắng nghe push event trên thread riêng
      running = true;
      listenerThread = new Thread(() -> {
        try {
          String line;
          while (running && (line = in.readLine()) != null) {
            if (onEvent != null) {
              JSONAuctionEventTemp event = gson.fromJson(line, JSONAuctionEventTemp.class);
              onEvent.accept(event);
            }
          }
        } catch (IOException e) {
          if (running) {
            System.err.println("[WatchNetwork] Listener bị ngắt: " + e.getMessage());
          }
        }
      });
      listenerThread.setDaemon(true);
      listenerThread.start();

    } catch (IOException e) {
      System.err.println("[WatchNetwork] Không thể kết nối: " + e.getMessage());
    }
  }

  /**
   * Dừng watch auction (gửi UNWATCH-AUCTION và đóng socket).
   *
   * @param auctionId ID phiên đấu giá
   * @param token     Token xác thực
   */
  public void stopWatch(int auctionId, String token) {
    running = false;
    try {
      if (out != null) {
        // Gửi UNWATCH trên cùng socket hoặc kết nối mới
        out.println("UNWATCH-AUCTION");
        out.println("{\"auctionId\":" + auctionId + ",\"token\":\"" + token + "\"}");
      }
    } catch (Exception ignored) {
    }
    closeQuietly();
  }

  private void closeQuietly() {
    try {
      if (listenerThread != null) listenerThread.interrupt();
      if (in != null) in.close();
      if (out != null) out.close();
      if (socket != null) socket.close();
    } catch (IOException ignored) {
    }
  }
}
