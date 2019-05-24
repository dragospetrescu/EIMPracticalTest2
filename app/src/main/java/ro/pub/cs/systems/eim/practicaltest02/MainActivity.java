package ro.pub.cs.systems.eim.practicaltest02;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.protocol.HTTP;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DRAGOS_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button serverStartButton = findViewById(R.id.serverStartButton);
        Button clientStartButton = findViewById(R.id.clientStartButton);
        final EditText serverPortInput = findViewById(R.id.serverPortEditor);
        final EditText clientURLInput = findViewById(R.id.clientURLText);
        final TextView clientOutputText = findViewById(R.id.clientOutputText);
        serverStartButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {


                int port = Integer.parseInt(serverPortInput.getText().toString());
                ServerThread serverThread = new ServerThread(port);
                serverThread.start();

            }
        });

        clientStartButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                int port = Integer.parseInt(serverPortInput.getText().toString());
                String url = clientURLInput.getText().toString();
                ClientThread clientThread = new ClientThread(url, port, clientOutputText);
                clientThread.start();
            }
        });
    }


    class ServerThread extends Thread {

        private List<String> goodURLs;
        int port;
        ServerSocket serverSocket;

        public ServerThread(int port) {
            this.port = port;
            goodURLs = new ArrayList<>();
            try {
                Log.e(TAG, "" + port);
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void stopThread() {
            interrupt();
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ioException) {
                    Log.e(TAG, "[SERVER THREAD] An exception has occurred: " + ioException.getMessage());
                    ioException.printStackTrace();
                }
            }
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Log.e("DRAGOS", "[SERVER THREAD] Waiting for a client invocation...");
                    Socket socket = serverSocket.accept();
                    Log.e("DRAGOS", "[SERVER THREAD] A connection request was received from " + socket.getInetAddress() + ":" + socket.getLocalPort());
                    CommunicationThread communicationThread = new CommunicationThread(this, socket);
                    communicationThread.start();
                }
            } catch (ClientProtocolException clientProtocolException) {
                Log.e("DRAGOS", "[SERVER THREAD] An exception has occurred: " + clientProtocolException.getMessage());
                clientProtocolException.printStackTrace();
            } catch (IOException ioException) {
                Log.e("DRAGOS", "[SERVER THREAD] An exception has occurred: " + ioException.getMessage());
                ioException.printStackTrace();
            }
        }

    }

    class CommunicationThread extends Thread {

        private ServerThread serverThread;
        private Socket socket;

        public CommunicationThread(ServerThread serverThread, Socket socket) {

            this.serverThread = serverThread;
            this.socket = socket;
        }

        @Override
        public void run() {
            if (socket == null) {
                Log.e(TAG, "[COMMUNICATION THREAD] Socket is null!");
                return;
            }

            try {
                BufferedReader bufferedReader = Utilities.getReader(socket);
                PrintWriter printWriter = Utilities.getWriter(socket);
                Log.e(TAG, "SERVER " + socket.toString());
                if (bufferedReader == null || printWriter == null) {
                    Log.e(TAG, "[COMMUNICATION THREAD] Buffered Reader / Print Writer are null!");
                    return;
                }
                Log.e(TAG, "[COMMUNICATION THREAD] Waiting for url from client!");
//                String url = bufferedReader.readLine();

                String url = "https://ocw.cs.pub.ro/courses/eim/informatii/anunturi";
                if (url == null || url.isEmpty()) {
                    Log.e(TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (city / information type!");
                    return;
                }
                String result;
                if (serverThread.goodURLs.contains(url)) {
                    Log.e(TAG, "[COMMUNICATION THREAD] Getting the information from the webservice -> " + url);
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(url);
                    ResponseHandler<String> responseHandler = new BasicResponseHandler();
                    result = httpClient.execute(httpPost, responseHandler);
                } else {
                    result = "bad url";
                }
                Log.e(TAG, result);

                printWriter.println(result);
                printWriter.flush();

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ioException) {
                        Log.e(TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                        ioException.printStackTrace();
                    }
                }
            }
        }
    }

    class ClientThread extends Thread {

        private String url;
        private final TextView weatherForecastTextView;
        private int port;

        public ClientThread(String url, int port, TextView weatherForecastTextView) {

            this.port = port;
            this.url = url;
            this.weatherForecastTextView = weatherForecastTextView;
        }

        public void run() {
            Socket socket = null;
            try {
                Log.e(TAG, "[CLIENT THREAD] STARTED on port " + port);

                socket = new Socket("localhost", port);
                if (socket == null) {
                    Log.e(TAG, "[CLIENT THREAD] Could not create socket!");
                    return;
                }
                Thread.sleep(100);
                Log.e(TAG, "CLIENT" + socket.toString());
                Log.e(TAG, "[CLIENT THREAD] GETTING READER / WRITER");
                BufferedReader bufferedReader = Utilities.getReader(socket);
                PrintWriter printWriter = Utilities.getWriter(socket);
                if (bufferedReader == null || printWriter == null) {
                    Log.e(TAG, "[CLIENT THREAD] Buffered Reader / Print Writer are null!");
                    return;
                }
                Log.e(TAG, "[CLIENT THREAD] SENDING URL + url");

                printWriter.println(url);
                printWriter.flush();
                String total = "";
                int i = 0;
                String weatherInformation;
                do {
                    weatherInformation = bufferedReader.readLine();
                    if(weatherInformation == null) {
                        break;
                    }
                    total += weatherInformation;
                    i++;
                } while (i < 10);
                final String finalizedWeateherInformation = weatherInformation;
                weatherForecastTextView.post(new Runnable() {
                    @Override
                    public void run() {
                        weatherForecastTextView.setText(finalizedWeateherInformation);
                    }
                });
//
//                while ((weatherInformation = bufferedReader.readLine()) != null) {
//                    final String finalizedWeateherInformation = weatherInformation;
//                    weatherForecastTextView.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            weatherForecastTextView.setText(finalizedWeateherInformation);
//                        }
//                    });
//                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ioException) {
                        Log.e(TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                        ioException.printStackTrace();
                    }
                }
            }
        }
    }
}
