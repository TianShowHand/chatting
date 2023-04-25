package cn.edu.sustech.cs209.chatting.client;

import com.google.gson.Gson;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class TCPClient {
    private static final long serialVersionUID = 1L;
    boolean live = false;
    Socket s = null;
    DataOutputStream dos = null;
    DataInputStream dis = null;
    boolean bConnected = false;
    String[] list;

    boolean aBoolean;

    public void connectToServer(String username) {
        try {
            s = new Socket("127.0.0.1", 8888);
            dos = new DataOutputStream(s.getOutputStream());
            dis = new DataInputStream(s.getInputStream());
            bConnected = true;
            Map<String, String> map = new HashMap<>();
            map.put("usernameCheck", username);
            Gson gson = new Gson();
            try {
                dos.writeUTF(gson.toJson(map));
                dos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
