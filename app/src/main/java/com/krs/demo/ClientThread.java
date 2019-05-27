package com.krs.demo;

import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import static com.krs.demo.Constants.SERVERPORT;

public class ClientThread implements Runnable {

    private Socket socket;
    private BufferedReader input;
    private DataInputStream dis;
    private String TAG=ClientThread.class.getSimpleName();
    private TextView txt_status;
    private TextView txt_gross_wt;

    ClientThread(TextView txt_status, TextView txt_gross_wt)
    {
        this.txt_status=txt_status;
        this.txt_gross_wt=txt_gross_wt;
    }

    @Override
    public void run() {

        try {
            InetAddress serverAddr = InetAddress.getByName(Constants.SERVER_IP);
            socket = new Socket(serverAddr, SERVERPORT);
            InputStream is = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int read;
            String message = null;

            while ((read = is.read(buffer)) != -1) {
                message = new String(buffer, 0, read);
                System.out.print(message);
                System.out.flush();
                txt_status.setText(message);
                Log.e(TAG, "message from server: " + message);
                if (!message.contains("raju") && !message.contains("Connect")) {
                    txt_gross_wt.setText(message);
                }
            }
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
            Log.e(TAG, "UnknownHostException: ");
        } catch (IOException e1) {
            e1.printStackTrace();
            Log.e(TAG, "IOException:");
        }
    }

    void sendMessage(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (null != socket) {
                        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                            /*PrintWriter out = new PrintWriter(socket.getOutputStream());
                            out.println();
                            out.flush();*/
                        Log.e(TAG, "sendMessage: " + message);
                        out.println(message);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
