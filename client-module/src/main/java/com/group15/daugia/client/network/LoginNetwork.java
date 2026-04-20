package com.group15.daugia.client.network;

import com.group15.daugia.shared.JSONUserTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginNetwork {
  // Req login từ server
  public static String loginReq(String request, String jsonData) {
    try (Socket socket = new Socket("localhost", 8080);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); ) {
      out.println(request);
      out.println(jsonData);

      return in.readLine();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
