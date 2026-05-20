package com.group15.daugia.server;

import java.io.PrintWriter;

/**
 * Mở rộng Workable cho các lệnh cần giữ session lâu dài (watch). ClientHandler sẽ kiểm tra nếu
 * worker implement interface này thì truyền thêm PrintWriter để worker có thể push nhiều event về
 * sau.
 */
public interface PersistentWorkable extends Workable {
  /**
   * Khởi tạo session watch.
   *
   * @param data JSON payload từ client
   * @param out PrintWriter của socket – worker giữ lại để push event
   * @return JSON ACK gửi ngay cho client
   */
  String startWatch(String data, PrintWriter out);

  /**
   * Dọn dẹp khi socket đóng hoặc client gửi UNWATCH.
   *
   * @param data JSON payload (chứa auctionId + token)
   */
  void stopWatch(String data);
}
