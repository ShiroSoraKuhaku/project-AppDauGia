package com.group15.daugia.server;

import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.service.AuctionWatcherService;
import com.group15.daugia.shared.JSON.JSONAuctionEventTemp;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.*;

/**
 * Scheduler quản lý vòng đời auction: SCHEDULED -> ACTIVE -> ENDED. Hỗ trợ gia hạn anti-sniping
 * (eBay-style).
 *
 * <p>Singleton – khởi tạo một lần trong Main.java khi server start.
 */
public class AuctionClock {

  /** Ngưỡng thời gian còn lại (giây) để kích hoạt gia hạn khi có bid mới */
  public static final long EXTEND_THRESHOLD_SECS = 30;

  /** Số giây gia hạn thêm khi bid cận giờ */
  public static final long EXTEND_AMOUNT_SECS = 30;

  private static AuctionClock instance;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

  /** auctionId -> ScheduledFuture của end job */
  private final ConcurrentMap<Integer, ScheduledFuture<?>> endJobs = new ConcurrentHashMap<>();

  private final AuctionDao dao = AuctionDao.getInstance();
  private final AuctionWatcherService svc = AuctionWatcherService.getInstance();

  private AuctionClock() {}

  public static AuctionClock getInstance() {
    if (instance == null) {
      synchronized (AuctionClock.class) {
        if (instance == null) {
          instance = new AuctionClock();
        }
      }
    }
    return instance;
  }

  // -----------------------------------------------------------------------
  // Bootstrap: load tất cả auction SCHEDULED/ACTIVE từ DB và lên lịch
  // -----------------------------------------------------------------------
  public void bootstrap() {
    LocalDateTime now = LocalDateTime.now();
    List<JSONAuctionTemp> auctions = dao.loadUpcomingAndActiveAuctions(now);
    for (JSONAuctionTemp a : auctions) {
      scheduleAuction(a, now);
    }
    System.out.println("[AuctionClock] Bootstrapped " + auctions.size() + " auction(s).");
  }

  /** Lên lịch start + end cho một auction. Gọi khi tạo auction mới hoặc khi bootstrap. */
  public void scheduleAuction(JSONAuctionTemp auction, LocalDateTime now) {
    int id = auction.getAuctionId();

    if ("SCHEDULED".equals(auction.getStatus())) {
      LocalDateTime startTime =
          LocalDateTime.parse(
              auction.getStartTime(),
              java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      long startDelay = ChronoUnit.SECONDS.between(now, startTime);
      if (startDelay < 0) startDelay = 0;

      scheduler.schedule(() -> onAuctionStart(id), startDelay, TimeUnit.SECONDS);
    }

    if ("ACTIVE".equals(auction.getStatus()) || "SCHEDULED".equals(auction.getStatus())) {
      scheduleEnd(id, auction.getEndTime(), now);
    }
  }

  // -----------------------------------------------------------------------
  // Callback: auction bắt đầu
  // -----------------------------------------------------------------------
  private void onAuctionStart(int auctionId) {
    boolean updated = dao.markAuctionStarted(auctionId, LocalDateTime.now());
    if (!updated) return; // đã bị cancel hoặc trạng thái không hợp lệ

    JSONAuctionTemp snap = dao.getAuctionSnapshot(auctionId);
    if (snap == null) return;

    JSONAuctionEventTemp event = new JSONAuctionEventTemp();
    event.setEventType("AUCTION_STARTED");
    event.setAuctionId(auctionId);
    event.setStatus("ACTIVE");
    event.setCurPrice(snap.getCurPrice());
    event.setCurLeader(snap.getCurLeader());
    event.setEndTime(snap.getEndTime());
    event.setSecondsRemaining(snap.getSecondsRemaining());
    event.setVersion(snap.getVersion());

    svc.broadcast(auctionId, event);
    System.out.println("[AuctionClock] Auction " + auctionId + " STARTED.");
  }

  // -----------------------------------------------------------------------
  // Callback: auction kết thúc
  // -----------------------------------------------------------------------
  private void onAuctionEnd(int auctionId) {
    boolean updated = dao.markAuctionEnded(auctionId, LocalDateTime.now());
    if (!updated) return;

    JSONAuctionTemp snap = dao.getAuctionSnapshot(auctionId);
    if (snap == null) return;

    JSONAuctionEventTemp event = new JSONAuctionEventTemp();
    event.setEventType("AUCTION_ENDED");
    event.setAuctionId(auctionId);
    event.setStatus("ENDED");
    event.setCurPrice(snap.getCurPrice());
    event.setCurLeader(snap.getCurLeader());
    event.setEndTime(snap.getEndTime());
    event.setSecondsRemaining(0);
    event.setVersion(snap.getVersion());

    svc.broadcast(auctionId, event);
    endJobs.remove(auctionId);
    System.out.println(
        "[AuctionClock] Auction " + auctionId + " ENDED. Winner: " + snap.getCurLeader());
  }

  // -----------------------------------------------------------------------
  // Gia hạn anti-sniping: gọi từ PlaceBidWorker sau khi bid thành công
  // -----------------------------------------------------------------------
  public void tryExtend(int auctionId) {
    JSONAuctionTemp snap = dao.getAuctionSnapshot(auctionId);
    if (snap == null || !"ACTIVE".equals(snap.getStatus())) return;

    long secsLeft = snap.getSecondsRemaining();
    if (secsLeft > EXTEND_THRESHOLD_SECS) return; // không cần gia hạn

    LocalDateTime newEnd = LocalDateTime.now().plusSeconds(EXTEND_AMOUNT_SECS);
    boolean extended = dao.extendAuctionEndTime(auctionId, newEnd, snap.getVersion());
    if (!extended) return; // race condition – bỏ qua

    // Reschedule end job
    scheduleEnd(
        auctionId,
        newEnd.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        LocalDateTime.now());

    // Lấy snapshot mới sau khi extend
    JSONAuctionTemp newSnap = dao.getAuctionSnapshot(auctionId);
    if (newSnap == null) return;

    JSONAuctionEventTemp event = new JSONAuctionEventTemp();
    event.setEventType("AUCTION_EXTENDED");
    event.setAuctionId(auctionId);
    event.setStatus("ACTIVE");
    event.setCurPrice(newSnap.getCurPrice());
    event.setCurLeader(newSnap.getCurLeader());
    event.setEndTime(newSnap.getEndTime());
    event.setSecondsRemaining(newSnap.getSecondsRemaining());
    event.setVersion(newSnap.getVersion());

    svc.broadcast(auctionId, event);
    System.out.println("[AuctionClock] Auction " + auctionId + " EXTENDED to " + newEnd);
  }

  // -----------------------------------------------------------------------
  // Helper: lên lịch (hoặc reschedule) end job
  // -----------------------------------------------------------------------
  private void scheduleEnd(int auctionId, String endTimeStr, LocalDateTime now) {
    // Cancel job cũ nếu có
    ScheduledFuture<?> old = endJobs.remove(auctionId);
    if (old != null) {
      old.cancel(false);
    }

    LocalDateTime endTime =
        LocalDateTime.parse(
            endTimeStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    long delay = ChronoUnit.SECONDS.between(now, endTime);
    if (delay < 0) delay = 0;

    ScheduledFuture<?> future =
        scheduler.schedule(() -> onAuctionEnd(auctionId), delay, TimeUnit.SECONDS);
    endJobs.put(auctionId, future);
  }

  // -----------------------------------------------------------------------
  // Test-only: reset scheduler để tránh state leakage giữa các test
  // -----------------------------------------------------------------------
  public void resetForTests() {
    endJobs.forEach((id, future) -> future.cancel(false));
    endJobs.clear();
    scheduler.shutdownNow();
    // Khởi tạo lại scheduler thông qua reflection để dùng được cho test tiếp theo
    try {
      java.lang.reflect.Field f = AuctionClock.class.getDeclaredField("scheduler");
      f.setAccessible(true);
      f.set(this, Executors.newScheduledThreadPool(10));
    } catch (Exception e) {
      throw new RuntimeException("resetForTests failed", e);
    }
  }
}
