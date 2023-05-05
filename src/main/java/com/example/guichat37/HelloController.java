package com.example.guichat37;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.Socket;

public class HelloController {
    Socket socket;
    DataInputStream in;
    DataOutputStream out;
    boolean publicMessage = true;
    int toUser = 0;
    int selfId;
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
    private Button inputBtn;
    private File file;
    @FXML
    protected void send() {
        try {
            JSONObject jsonObject = new JSONObject();
            if(file != null){
                jsonObject.put("action", "sendFile");
                jsonObject.put("fileName", file.getName());
                out.writeUTF(jsonObject.toJSONString());
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                byte[] buffer = new byte[1024];
                int i;
                while ((i = bis.read(buffer)) != -1){
                    out.write(buffer);
                }
                out.flush();
                file = null;
            }else{
                String message = messageTextField.getText();
                System.out.println(message);
                messageTextArea.appendText("Вы: "+message+"\n");
                messageTextField.clear();
                jsonObject.put("public", publicMessage);
                jsonObject.put("msg", message);
                jsonObject.put("id", toUser);
                out.writeUTF(jsonObject.toJSONString());
            }
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
                            if(jsonObject.get("self_id") != null){
                                selfId = Integer.parseInt(jsonObject.get("self_id").toString());
                            }else if(jsonObject.get("msg") != null){
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
                                            if(Integer.parseInt(jsonUserObject.get("id").toString()) == selfId)
                                                return;
                                            userBtn.setText(jsonUserObject.get("name").toString());
                                            userBtn.setPrefWidth(200);
                                            userBtn.setLineSpacing(5);
                                            usersList.getChildren().add(userBtn);
                                            userBtn.setOnAction(e->{
                                                JSONObject jsonObject1 = new JSONObject();
                                                messageTextArea.clear();
                                                // Тут получаем приватные сообщения из БД
                                                publicMessage = false;
                                                toUser = Integer.parseInt(jsonUserObject.get("id").toString());
                                                jsonObject1.put("action", "getMessages");
                                                jsonObject1.put("toUser", toUser);
                                                try {
                                                    out.writeUTF(jsonObject1.toJSONString());
                                                } catch (IOException ex) {
                                                    throw new RuntimeException(ex);
                                                }
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

    @FXML
    public void fileReceiver(){
        Stage stage = (Stage) inputBtn.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        file = fileChooser.showOpenDialog(stage);
        if(file != null){
            String fileName = file.getName();
            messageTextField.setText(fileName);
        }
    }

}