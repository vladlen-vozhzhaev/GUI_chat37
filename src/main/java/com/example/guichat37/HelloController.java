package com.example.guichat37;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class HelloController {
    Socket socket;
    DataInputStream in;
    DataOutputStream out;
    boolean publicMessage = true;
    int toUser = 0;
    @FXML
    private Label welcomeText;
    @FXML
    private TextField messageTextField;
    @FXML
    private TextArea messageTextArea;
    @FXML
    private Button sendBtn;
    @FXML
    private Button connectBtn;
    @FXML
    private VBox usersList;
    @FXML
    protected void send() {
        try {
            JSONObject jsonObject = new JSONObject();
            String message = messageTextField.getText();
            System.out.println(message);
            messageTextArea.appendText("Вы: "+message+"\n");
            messageTextField.clear();
            jsonObject.put("public", publicMessage);
            jsonObject.put("msg", message);
            jsonObject.put("id", toUser);
            out.writeUTF(jsonObject.toJSONString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @FXML
    public void connect(){
        connectBtn.setDisable(true);
        sendBtn.setDisable(false);
        try {
            this.socket = new Socket("127.0.0.1", 9123);
            this.out =new DataOutputStream(socket.getOutputStream());
            this.in = new DataInputStream(socket.getInputStream());
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONParser jsonParser = new JSONParser();
                        JSONObject jsonObject;
                        while (true){
                            String response = in.readUTF();
                            jsonObject = (JSONObject) jsonParser.parse(response);
                            System.out.println("Сообщение: "+jsonObject.get("msg"));
                            System.out.println("Пользователи: "+jsonObject.get("onlineUsers"));
                            if(jsonObject.get("msg") != null){
                                messageTextArea.appendText(jsonObject.get("msg")+"\n");
                            }else if(jsonObject.get("onlineUsers") != null){
                                JSONArray onlineUsers = (JSONArray) jsonObject.get("onlineUsers");
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        usersList.getChildren().clear();
                                        onlineUsers.forEach(user->{
                                            Button userBtn = new Button();
                                            JSONObject jsonUserObject = (JSONObject) user;
                                            userBtn.setText(jsonUserObject.get("name").toString());
                                            usersList.getChildren().add(userBtn);
                                            userBtn.setOnAction(e->{
                                                messageTextArea.clear();
                                                // Тут получаем приватные сообщения из БД
                                                publicMessage = false;
                                                toUser = Integer.parseInt(jsonUserObject.get("id").toString());
                                            });
                                        });
                                    }
                                });
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        System.out.println("Потеряно соединение с сервером");
                    }
                }
            });
            thread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}