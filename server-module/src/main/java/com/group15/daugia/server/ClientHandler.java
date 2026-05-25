package com.group15.daugia.server;

import com.google.gson.Gson;
import com.group15.daugia.shared.JSON.JSONUserTemp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

/**
 * Xử lý một kết nối TCP từ client.
 *
 * <p>Luồng xử lý: - Đọc dòng 1: command key (ví dụ "LOGIN", "WATCH-AUCTION") - Đọc dòng 2: JSON
 * payload - Nếu worker implement PersistentWorkable (WATCH-AUCTION): + Gọi startWatch() -> gửi ACK
 * ngay + Giữ socket mở, đọc thêm lệnh UNWATCH-AUCTION để kết thúc session - Ngược lại: gọi work()
 * -> gửi response -> đóng socket
 */
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

      if (req == null) return;

      Workable job = works.get(req);
      System.out.println("Aquire work: " + req);

      if (job == null) {
        out.println("INVALID");
        return;
      }

      // --- Persistent watch session ---
      if (job instanceof PersistentWorkable) {
        PersistentWorkable pw = (PersistentWorkable) job;

        // Gửi ACK ngay
        String ack = pw.startWatch(data, out);
        out.println(ack);

        // Giữ socket mở cho đến khi client gửi UNWATCH-AUCTION
        // hoặc socket bị đóng
        try {
          String line;
          while ((line = in.readLine()) != null) {
            if ("UNWATCH-AUCTION".equals(line)) {
              String unwatchData = in.readLine();
              pw.stopWatch(unwatchData != null ? unwatchData : data);
              break;
            }
            // Bỏ qua các dòng không hợp lệ trong session watch
          }
        } catch (IOException ignored) {
          // Socket đóng đột ngột – dọn dẹp
        } finally {
          pw.stopWatch(data);
        }
        return;
      }

      // --- Short-lived request/response ---
      try {
        out.println(job.work(data));
      } catch (RuntimeException e) {
        e.printStackTrace();
        JSONUserTemp error = new JSONUserTemp();
        error.setResponse("500 Server Error");
        out.println(new Gson().toJson(error));
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
