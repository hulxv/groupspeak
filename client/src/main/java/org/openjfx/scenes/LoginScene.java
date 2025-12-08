package org.openjfx.scenes;

import java.io.IOException;
import org.openjfx.scenes.components.DisconnectButton;
import org.openjfx.ChatHandler;
import org.openjfx.ClientState;
import org.openjfx.ProtocolHandler.LoginResponse;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginScene extends Scene {
  private TextField userField;
  private PasswordField passField;
  private Label userLabel;
  private Label passLabel;

  public LoginScene(Stage stage) {
    super(new AnchorPane(), 600, 400);
    AnchorPane root = (AnchorPane)getRoot();
    root.getStyleClass().add("root");
    root.setStyle("-fx-background-color: #121212;");

    if (getClass().getResource("/styles.css") != null) {
      getStylesheets().add(
          getClass().getResource("/styles.css").toExternalForm());
    }

    VBox centerBox = new VBox(15);
    centerBox.setAlignment(Pos.CENTER);
    AnchorPane.setTopAnchor(centerBox, 80.0);
    AnchorPane.setBottomAnchor(centerBox, 80.0);
    AnchorPane.setLeftAnchor(centerBox, 0.0);
    AnchorPane.setRightAnchor(centerBox, 0.0);

    // User Field
    VBox userBox = new VBox(5);
    userBox.setMaxWidth(290);
    userLabel = new Label("Username / Email");
    userLabel.setStyle(
        "-fx-text-fill: #fff8e7; -fx-font-weight: bold; -fx-font-size: 18px;");
    userField = new TextField();
    userField.getStyleClass().add("search-field");
    userField.setStyle("-fx-background-color: #222225; -fx-text-fill: " +
                       "#fff8e7; -fx-background-radius: 8;");
    userField.setAlignment(Pos.CENTER);
    userField.setMaxWidth(240);
    userBox.getChildren().addAll(userLabel, userField);

    // Password Field
    VBox passBox = new VBox(5);
    passBox.setMaxWidth(290);
    passLabel = new Label("Password");
    passLabel.setStyle(
        "-fx-text-fill: #fff8e7; -fx-font-weight: bold; -fx-font-size: 18px;");
    passField = new PasswordField();
    passField.getStyleClass().add("search-field");
    passField.setStyle("-fx-background-color: #222225; -fx-text-fill: " +
                       "#fff8e7; -fx-background-radius: 8;");
    passField.setAlignment(Pos.CENTER);
    passField.setMaxWidth(240);
    passBox.getChildren().addAll(passLabel, passField);

    // Buttons
    HBox btnBox = new HBox(20);
    btnBox.setAlignment(Pos.CENTER);

    Button backBtn = new Button("Back");
    backBtn.getStyleClass().add("send-btn");
    backBtn.setStyle("-fx-background-color: #222225; -fx-background-radius: " +
                     "15; -fx-text-fill: #fff8e7; -fx-font-size: 15px;");
    backBtn.setPrefWidth(110);
    backBtn.setOnAction(e -> stage.setScene(new HomeScene(stage)));

    Button loginBtn = new Button("Login");
    loginBtn.getStyleClass().add("send-btn");
    loginBtn.setStyle("-fx-background-color: #222225; -fx-background-radius: " +
                      "15; -fx-text-fill: #fff8e7; -fx-font-size: 15px;");
    loginBtn.setPrefWidth(110);
    loginBtn.setOnAction(e -> handleLogin(stage));

    btnBox.getChildren().addAll(backBtn, loginBtn);

    centerBox.getChildren().addAll(userBox, passBox, btnBox);

    DisconnectButton disconnectBtn = new DisconnectButton();

    AnchorPane.setBottomAnchor(disconnectBtn, 27.0);
    AnchorPane.setLeftAnchor(disconnectBtn, 27.0);

    root.getChildren().addAll(centerBox, disconnectBtn);
  }

  private void handleLogin(Stage stage) {
    String user = userField.getText();
    String pass = passField.getText();
    boolean fail = false;

    if (user.isEmpty()) {
      userLabel.setText("Please Enter Username");
      fail = true;
    } else {
      userLabel.setText("Username / Email");
    }

    if (pass.isEmpty()) {
      passLabel.setText("Please Enter Password");
      fail = true;
    } else {
      passLabel.setText("Password");
    }

    if (fail)
      return;

    try {
        ChatHandler chatHandler = new ChatHandler();
        LoginResponse response = chatHandler.login(user, pass);
        
        if (response.success) {
            System.out.println("Login successful. Token: " + response.sessionToken);
            
            ClientState state = ClientState.getInstance();
            state.setSessionToken(response.sessionToken);
            state.setCurrentUserId(response.userId);
            state.setCurrentUsername(user);

            state.setCurrentDisplayName(response.displayName != null ? response.displayName : user);
            state.setCurrentEmail(response.email != null ? response.email : "unknown@example.com");
            
            stage.setScene(new ChatScene());
        } else {
            System.err.println("Login failed: " + response.message);
            userLabel.setText(response.message != null ? response.message : "Login Failed");
            userLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
    } catch (IOException e) {
        e.printStackTrace();
        userLabel.setText("Connection Error");
    }
  }
}
