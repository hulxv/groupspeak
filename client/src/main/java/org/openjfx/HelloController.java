package org.openjfx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloController {

    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    private Label IP_label;
    @FXML
    private Label Port_label;
    @FXML
    private TextField IP_text;
    @FXML
    private TextField Port_text;
    @FXML
    private TextField User_text;
    @FXML
    private TextField Password_text;
    @FXML
    private TextField Email_text;
    @FXML
    private TextField Display_name_text;
    @FXML
    private TextField confirm_pass_text;
    @FXML
    private Label user_label;
    @FXML
    private Label password_label;
    @FXML
    private Label confirm_label;
    @FXML
    private Label email_label;


    String IP;
    String IP_Regex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    String PortText;
    String User;
    String Password;
    String email;
    String Display_name;
    String confirm_pass;


    public void SwitchtoLogin(ActionEvent e) {

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Login.fxml"));
        try {
            root = fxmlLoader.load();
        } catch (IOException ex) {
            System.err.println("Failed to load Login FXML.");
            ex.printStackTrace();
            return;
        }

        stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void Disconnect(ActionEvent e) {

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("connect.fxml"));
        try {
            root = fxmlLoader.load();
        } catch (IOException ex) {
            System.err.println("Failed to load Login FXML.");
            ex.printStackTrace();
            return;
        }

        stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private Connection connection;

    public void switchtohome(ActionEvent e) {

        try {
            boolean validationfailed = false;
            IP = IP_text.getText().trim();
            PortText = Port_text.getText();
            System.out.println("Your IP is " + IP + " your port is " + PortText);
            if (IP.isEmpty()) {
               IP_label.setText("Please Enter  ");
               validationfailed = true;
                }
           else if(!IP.matches(IP_Regex)){
                IP_label.setText("Enter a valid ");
                validationfailed = true;
            }
            if(PortText.isEmpty()){
               Port_label.setText("Please Enter   ");
                validationfailed = true;
            }
            int Port = Integer.parseInt(PortText);
            if (Port < 0 || Port > 65535){
                Port_label.setText("Enter a valid ");
                validationfailed = true;
            }
            if (validationfailed){
                return;
            }
            connection = new Connection(IP, Port);


        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));

        // This load() might throw IOException, which will be caught below
        Parent root = fxmlLoader.load();

        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }
    // Specific catch for non-numeric port input
    catch (NumberFormatException exception) {
       Port_label.setText("Enter number for ");
    }
    // Catch for FXML loading errors or other unexpected errors
    catch (Exception exception) {
        System.err.println("An error occurred during scene switch: " + exception);
        exception.printStackTrace();
    }
    }

    public void back(ActionEvent e) {

        try {

            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
            Parent root = fxmlLoader.load();
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        }
        catch (Exception exception) {
            System.err.println("An error occurred during scene switch: " + exception);
            exception.printStackTrace();
        }
    }

    public void SwitchtoSignup(ActionEvent e) {

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Signup.fxml"));
        try {
            root = fxmlLoader.load();
        } catch (IOException ex) {
            System.err.println("Failed to load Signup FXML.");
            ex.printStackTrace();
            return;
        }

        stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void login(ActionEvent e) {

        try {
            boolean validationfailed = false;
            User = User_text.getText();
            Password = Password_text.getText();

            if (User.isEmpty()) {
                user_label.setText("Please Enter ");
                validationfailed = true;
            }
            if (Password.isEmpty()) {
                password_label.setText("Please Enter ");
                validationfailed = true;
            }
            if (validationfailed){
                return;
            }
            System.out.println("Your user is " + User + " your pass is " + Password);
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("chat.fxml"));

            Parent root = fxmlLoader.load(); // IOException is now handled by catch block
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        }
        catch (Exception exception) {
            System.err.println("An error occurred during scene switch: " + exception);
            exception.printStackTrace();
        }
    }

    public void signup(ActionEvent e) {

        try {
            boolean validationFailed = false;
            User = User_text.getText();
            Password = Password_text.getText();
            email = Email_text.getText();
            confirm_pass = confirm_pass_text.getText();
            Display_name = Display_name_text.getText();

            if (email.isEmpty()) {
                email_label.setText("Please Enter ");
            }
            else if (!email.contains("@")|| !email.contains(".")){
                email_label.setText("Invalid ");
                validationFailed = true;
            }
            if (User.isEmpty()) {
                user_label.setText("Please Enter ");
                validationFailed = true;
            }
            if (Password.isEmpty()) {
                password_label.setText("Please Enter ");
                validationFailed = true;
            }
            if (confirm_pass.isEmpty()) {
                confirm_label.setText("Please Enter ");
                validationFailed = true;
            }
            else if (!confirm_pass.equals(Password)) {
                confirm_label.setText("Not matching ");
                validationFailed = true;
            }
            if (validationFailed) {
                return;
            }


            System.out.println("Your user is " + User + " your pass is " + Password);
            System.out.println("your user is "+ User + " email " +email + " password " + Password + " Displayname " + Display_name);
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("chat.fxml"));

            Parent root = fxmlLoader.load(); // IOException is now handled by catch block
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        }
        catch (Exception exception) {
            System.err.println("An error occurred during scene switch: " + exception);
            exception.printStackTrace();
        }
    }
}