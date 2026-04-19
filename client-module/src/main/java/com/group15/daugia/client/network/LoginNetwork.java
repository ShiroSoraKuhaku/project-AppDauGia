package com.group15.daugia.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginNetwork {
  // Req login từ server
  public static String loginReq(String request) {
    try (Socket socket = new Socket("localhost", 8080);
        PrintWriter out = new PrintWriter(socket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); ) {
      out.println(request);

      return in.readLine();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
