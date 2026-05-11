package com.group15.daugia.server;

import com.group15.daugia.server.worker.LoginWorker;
import com.group15.daugia.server.worker.LogoutWorker;
import com.group15.daugia.server.worker.SignupWorker;

import java.util.HashMap;
import java.util.Map;

public class Work {
  private static Map<String, Workable> works = new HashMap<>();

  static {
    works.put("LOGIN", new LoginWorker());
    works.put("RM-TOKEN", new LogoutWorker());
    works.put("SIGNUP", new SignupWorker());
  }

  public static Map<String, Workable> getWorks() {
    return works;
  }
}
