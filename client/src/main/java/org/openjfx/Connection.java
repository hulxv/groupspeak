package org.openjfx;

import java.io.*;
import java.net.Socket;

public class Connection {

    private Socket socket = null;
    private InputStreamReader inputStreamReader = null;
    private OutputStreamWriter outputStreamWriter = null;
    private BufferedReader bufferedReader = null;
    private BufferedWriter bufferedWriter = null;


    public Connection(String IP, int Port) throws IOException{
        this.socket = new Socket(IP,Port);

        this.inputStreamReader = new InputStreamReader(socket.getInputStream());

        this.outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());

        this.bufferedReader = new BufferedReader(inputStreamReader);

        this.bufferedWriter = new BufferedWriter(outputStreamWriter);

    }

}
