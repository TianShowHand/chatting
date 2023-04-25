package cn.edu.sustech.cs209.chatting.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class TCPServer extends JFrame {
    private static final long serialVersionUID = 1L;
    private ServerSocket ss = null;
    private boolean bStart = false;
    private JTextArea taContent = new JTextArea();
    private int index = 0;
    List<Client> clients = new ArrayList<>();
    List<String> userlist;

    public void launchFrame() {
        taContent.setEditable(false);
        taContent.setBackground(Color.DARK_GRAY);
        taContent.setForeground(Color.YELLOW);
        this.add(taContent);
        this.setSize(300, 350);
        this.setLocation(400, 200);
        this.setTitle("TCP Server");
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        tcpMonitor();
    }

    public void tcpMonitor() {
        try {
            ss = new ServerSocket(8888);
            bStart = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            while (bStart) {
                index++;
                Socket s = ss.accept();
                Client c = new Client(s);
                clients.add(c);
                taContent.append(s.getInetAddress().getHostAddress()
                        + " connected " + index + " clients\n");
                new Thread(c).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                ss.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Client implements Runnable {
        DataInputStream dis = null;
        DataOutputStream dos = null;
        Socket s;
        boolean bStart;
        String name;

        Client(Socket s) {
            this.s = s;
            try {
                dis = new DataInputStream(s.getInputStream());
                dos = new DataOutputStream(s.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            bStart = true;
        }

        private boolean Check(String s1) {
            if (userlist == null) {
                userlist = new ArrayList<>();
                userlist.add(s1);
                this.name = s1;
                return true;
            }
            if (!userlist.contains(s1)) {
                userlist.add(s1);
                this.name = s1;
                return true;
            }
            return false;
        }

        public void sendToClient(String s1) {
            try {
                dos.writeUTF(s1);
                dos.flush();
            } catch (IOException e) {
                index--;
                clients.remove(this);
                userlist.remove(this.name);
                taContent.append(s.getInetAddress().getHostAddress()
                        + " exited " + index + " clients\n");
                System.out.println("对方退出了！我从List里面去掉了！");
            }
        }

        public void run() {
            try {
                while (bStart) {
                    String str = dis.readUTF();
                    Gson g = new Gson();
                    JsonObject obj = g.fromJson(str, JsonObject.class);
                    if (obj.has("usernameCheck")) {
                        Map<String, Boolean> map = new HashMap<>();
                        map.put("usernameCheck", Check(obj.get("usernameCheck").getAsString()));
                        sendToClient(g.toJson(map));
                    } else if (obj.has("userList")) {
                        Map<String, String> map = new HashMap<>();
                        StringBuilder strings = new StringBuilder();
                        List<String> list = userlist;
                        list.remove(this.name);
                        for (String s1 : list) {
                            strings.append(s1).append(" ");
                        }
                        map.put("userList", strings.toString());
                        sendToClient(g.toJson(map));
                    } else if (obj.has("data")) {
                        Map<String, String> map = new HashMap<>();
                        map.put("timestamp", obj.get("timestamp").getAsString());
                        map.put("sentBy", obj.get("sentBy").getAsString());
                        map.put("sendTo", obj.get("sendTo").getAsString());
                        map.put("data", obj.get("data").getAsString());
                        for (Client c : clients) {
                            if (c.name.equals(obj.get("sendTo").getAsString().trim())) {
                                c.sendToClient(g.toJson(map));
                            }
                        }
                    } else if (obj.has("online")) {
                        Map<String, Integer> map = new HashMap<>();
                        if (userlist == null) {
                            map.put("online", 0);
                        } else {
                            map.put("online", userlist.size());
                        }
                        sendToClient(g.toJson(map));
                    }
                }
            } catch (EOFException e) {
                clients.remove(this);
                userlist.remove(this.name);
                taContent.append(s.getInetAddress().getHostAddress()
                        + " exited " + clients.size() + " clients\n");
                System.out.println("client closed");
            } catch (SocketException e) {
                System.out.println("client closed");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (s != null)
                        s.close();
                    if (dis != null)
                        dis.close();
                    if (dos != null)
                        dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
