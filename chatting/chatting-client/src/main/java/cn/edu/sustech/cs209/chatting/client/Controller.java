package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {
    @FXML
    ListView<String> chatList;
    @FXML
    ListView<Message> chatContentList;
    Map<String, ListView<Message>> chatContent = new HashMap<>();
    @FXML
    TextArea inputArea;
    @FXML
    Label currentUsername;
    @FXML
    Label currentOnlineCnt;

    String username;
    String another;
    TCPClient tc = new TCPClient();
    Thread thread = new Thread(new online());
    Thread t = new Thread(new RecToServer());
    boolean aBoolean = false;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        Dialog<String> dialog = new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> input = dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */
            username = input.get();
            tc.connectToServer(username);
            t.start();
            while (!aBoolean) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            aBoolean = false;
            if (!tc.aBoolean) {
                System.out.println("Username has been used");
                Platform.exit();
            } else {
                thread.start();
                currentUsername.setText("Current User: " + username);
            }
        } else {
            System.out.println("Invalid username " + input + ", exiting");
            Platform.exit();
        }

        chatContentList.setCellFactory(new MessageCellFactory());
    }

    @FXML
    public void createPrivateChat() {
        AtomicReference<String> user = new AtomicReference<>();
        Stage stage = new Stage();
        ComboBox<String> userSel = new ComboBox<>();
        Map<String, String> map = new HashMap<>();
        map.put("userList", username);
        Gson gson = new Gson();
        try {
            tc.dos.writeUTF(gson.toJson(map));
            tc.dos.flush();
            map.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // FIXME: get the user list from server, the current user's name should be filtered out
        while (!aBoolean) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        aBoolean = false;
        for (String s : tc.list) {
            userSel.getItems().addAll(s);
        }
        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            another = userSel.getSelectionModel().getSelectedItem();
            stage.close();
        });
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();
        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
        if (chatList.getItems().contains(another)) {
            chatContentList = chatContent.get(another);
        } else {
            chatList.getItems().add(another);
            chatContentList.getItems().clear();
        }
    }

    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        // TODO
        String data = inputArea.getText();
        inputArea.clear();
        chatContentList.getItems().add(new Message(refFormatNowDate(), username, another, data));
        ListView<Message> listView = new ListView<>();
        if (chatContent.containsKey(another)) {
            for (Message message : chatContent.get(another).getItems()) {
                listView.getItems().add(message);
            }
        }
        chatContent.remove(another);
        listView.getItems().add(new Message(refFormatNowDate(), username, another, data));
        chatContent.put(another, listView);
        Map<String, String> map = new HashMap<>();
        map.put("timestamp", refFormatNowDate().toString());
        map.put("sentBy", username);
        map.put("sendTo", another);
        map.put("data", data);
        Gson gson = new Gson();
        try {
            tc.dos.writeUTF(gson.toJson(map));
            tc.dos.flush();
            map.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void open(String string) {
        chatContentList.getItems().clear();
        for (Message message : chatContent.get(string).getItems()) {
            chatContentList.getItems().add(message);
        }
    }

    public void chatChange() {
        another = chatList.getSelectionModel().getSelectedItem();
        open(another);
    }

    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {
                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        return;
                    }
                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());
                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");
                    if (username.equals(msg.getSendTo())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }

    public Long refFormatNowDate() {
        Date nowTime = new Date(System.currentTimeMillis());
        return nowTime.getTime();
    }

    public class online implements Runnable {
        public void run() {
            Map<String, String> map = new HashMap<>();
            map.put("online", "online");
            Gson g = new Gson();
            try {
                tc.dos.writeUTF(g.toJson(map));
                tc.dos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void Chat(String s) {
        Gson g = new Gson();
        JsonObject obj = g.fromJson(s, JsonObject.class);
        String name = obj.get("sentBy").getAsString();
        if (chatList.getItems().contains(name)) {
            ListView<Message> listView = new ListView<>();
            if (chatContent.containsKey(name)) {
                for (Message message : chatContent.get(name).getItems()) {
                    listView.getItems().add(message);
                }
            }
            listView.getItems().add(new Message(obj.get("timestamp").getAsLong(), obj.get("sentBy").getAsString(),
                    obj.get("sendTo").getAsString(), obj.get("data").getAsString()));
            chatContent.remove(name);
            chatContent.put(name, listView);
        } else {
            Message message = new Message(obj.get("timestamp").getAsLong(), obj.get("sentBy").getAsString(),
                    obj.get("sendTo").getAsString(), obj.get("data").getAsString());
            ListView<Message> ContentList = new ListView<>();
            ContentList.getItems().add(message);
            chatContent.remove(obj.get("sentBy").getAsString());
            chatContent.put(obj.get("sentBy").getAsString(), ContentList);
            chatList.getItems().add(obj.get("sentBy").getAsString());
        }
        another = obj.get("sentBy").getAsString();
        open(obj.get("sentBy").getAsString());
    }

    private class RecToServer implements Runnable {
        public void run() {
            try {
                while (tc.bConnected) {
                    String str = tc.dis.readUTF();
                    Gson g = new Gson();
                    JsonObject obj = g.fromJson(str, JsonObject.class);
                    if (obj.has("usernameCheck")) {
                        tc.aBoolean = obj.get("usernameCheck").getAsBoolean();
                        aBoolean = true;
                    } else if (obj.has("userList")) {
                        String s = obj.get("userList").getAsString();
                        tc.list = s.split(" ");
                        aBoolean = true;
                    } else if (obj.has("data")) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                Chat(str);
                            }
                        });
                    } else if (obj.has("online")) {
                        int i = obj.get("online").getAsInt();
                        currentOnlineCnt.setText("Online: " + i);
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("服务器已关闭");
            }
        }
    }
}
