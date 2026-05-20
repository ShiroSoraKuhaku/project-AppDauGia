package com.group15.daugia.server.service;

import com.google.gson.Gson;
import com.group15.daugia.shared.JSON.JSONAuctionEventTemp;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Quản lý danh sách watcher in-memory và broadcast event đến họ.
 * Singleton thread-safe.
 */
public class AuctionWatcherService {

    private static AuctionWatcherService instance;

    /**
     * Map: auctionId -> Set<PrintWriter> (các socket đang watch auction đó)
     */
    private final ConcurrentMap<Integer, Set<PrintWriter>> watchers = new ConcurrentHashMap<>();

    private final Gson gson = new Gson();

    private AuctionWatcherService() {}

    public static AuctionWatcherService getInstance() {
        if (instance == null) {
            synchronized (AuctionWatcherService.class) {
                if (instance == null) {
                    instance = new AuctionWatcherService();
                }
            }
        }
        return instance;
    }

    /**
     * Đăng ký một PrintWriter vào danh sách watcher của auction.
     */
    public void register(int auctionId, PrintWriter out) {
        watchers.computeIfAbsent(auctionId,
                id -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(out);
    }

    /**
     * Hủy đăng ký PrintWriter khỏi danh sách watcher của auction.
     */
    public void unregister(int auctionId, PrintWriter out) {
        Set<PrintWriter> set = watchers.get(auctionId);
        if (set != null) {
            set.remove(out);
        }
    }

    /**
     * Broadcast một event đến tất cả watcher của auction.
     * Tự động dọn các writer đã đóng.
     */
    public void broadcast(int auctionId, JSONAuctionEventTemp event) {
        Set<PrintWriter> set = watchers.get(auctionId);
        if (set == null || set.isEmpty()) return;

        String json = gson.toJson(event);
        set.removeIf(out -> {
            try {
                out.println(json);
                return out.checkError(); // true = lỗi -> xóa khỏi set
            } catch (Exception e) {
                return true;
            }
        });
    }

    /**
     * Số lượng watcher hiện tại của một auction.
     */
    public int watcherCount(int auctionId) {
        Set<PrintWriter> set = watchers.get(auctionId);
        return set == null ? 0 : set.size();
    }
}
