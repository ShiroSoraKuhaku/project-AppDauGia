package com.group15.daugia.client.network;

import com.google.gson.Gson;
import com.group15.daugia.shared.JSONTemp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ShortConnectNetwork {
  public static String shortReq(String request, String jsonData) {
    try (Socket socket = new Socket("localhost", 8080);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); ) {
      Gson gson = new Gson();
      out.println(request);
      out.println(jsonData);
      String inData = in.readLine();
      // JSONTemp data = gson.fromJson(inData, JSONTemp.class);
      // return data;
      // TOD
      return inData;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
