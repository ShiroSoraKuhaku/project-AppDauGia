package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.PersistentWorkable;
import com.group15.daugia.server.service.AuctionWatcherService;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;

import java.io.PrintWriter;

/**
 * WATCH-AUCTION: đăng ký client vào danh sách watcher của một auction. Trả ACK ngay, giữ socket mở
 * để nhận push event.
 *
 * <p>Request JSON: { "auctionId": 1, "token": "..." }
 * <p>ACK JSON: { "response": "200 OK", "auctionId": 1, ... snapshot ... }
 * <p>Event JSON: { "eventType": "AUCTION_STARTED" | "AUCTION_ENDED" | "AUCTION_EXTENDED" |
 * "BID_PLACED" | "AUCTION_CANCELLED" | "BID_CANCELLED", "auctionId": 1, "status": "...",
 * "curPrice": 0.0, "curLeader": "...", "endTime": "...", "secondsRemaining": 0, "version": 1,
 * "bidderUsername": "...", "bidAmount": 0.0 }
 */
public class WatchAuctionWorker implements PersistentWorkable {

  private final Gson gson = new Gson();
  private final AuctionDao dao = AuctionDao.getInstance();
  private final AuctionWatcherService watcherSvc = AuctionWatcherService.getInstance();

  // Lưu thông tin session để dọn dẹp khi stopWatch
  private final ThreadLocal<Integer> sessionAuctionId = new ThreadLocal<>();
  private final ThreadLocal<PrintWriter> sessionOut = new ThreadLocal<>();

  @Override
  public String work(String data) {
    // Không dùng trong luồng persistent – delegate sang startWatch
    return startWatch(data, null);
  }

  @Override
  public String startWatch(String data, PrintWriter out) {
    Gson gson = new Gson();
    JSONAuctionTemp req = gson.fromJson(data, JSONAuctionTemp.class);
    JSONAuctionTemp ans = new JSONAuctionTemp();

    if (req == null || req.getToken() == null || req.getToken().isBlank() || req.getAuctionId() <= 0) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    String username = UserDAO.getUserDao().getUsernameByToken(req.getToken());
    if (username == null) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    // Validate auction tồn tại
    JSONAuctionTemp snap = dao.getAuctionSnapshot(req.getAuctionId());
    if (snap == null) {
      ans.setResponse("404 Not Found");
      return gson.toJson(ans);
    }

    // Đăng ký watcher
    if (out != null) {
      watcherSvc.register(req.getAuctionId(), out);
      sessionAuctionId.set(req.getAuctionId());
      sessionOut.set(out);
    }

    // Trả snapshot hiện tại làm ACK
    snap.setResponse("200 OK");
    return gson.toJson(snap);
  }

  @Override
  public void stopWatch(String data) {
    Integer auctionId = sessionAuctionId.get();
    PrintWriter out = sessionOut.get();
    if (auctionId != null && out != null) {
      watcherSvc.unregister(auctionId, out);
    }
    sessionAuctionId.remove();
    sessionOut.remove();
  }
}
