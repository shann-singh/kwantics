package com.example.kwantics;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.Manifest;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {


    private Button startButton, stopButton;
    private TextView textView;

    public AudioRecord recorder;
    public DataOutputStream dataOutputStream = null;
    public DataInputStream dataInputStream = null;
    public Socket socket = null;

    private int sampleRate = 8000;
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = (int) (2.25 * AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat));

    private boolean status = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                1);

        startButton = (Button) findViewById(R.id.start_button);
        stopButton = (Button) findViewById(R.id.stop_button);
        textView = (TextView) findViewById(R.id.textView);

        startButton.setOnClickListener(startListener);
        stopButton.setOnClickListener(stopListener);

    }

    private final OnClickListener startListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            if (status == false) {
                status = true;
                AudioStreamer audioStreamer = new AudioStreamer();
                audioStreamer.execute();
            }

        }
    };


    private final OnClickListener stopListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            stopRecording();
        }
    };

    public void stopRecording() {
        status = false;
        recorder.release();
        Log.d("VS", "Recorder released");
    }


    public class AudioStreamer extends AsyncTask<Void, String, Void> {

        @Override
        protected Void doInBackground(Void... voi) {
            try {
                byte[] buffer = new byte[minBufSize];

                final InetAddress destination = InetAddress.getByName("dev.kwantics.ai");
                int port = 7080;

                socket = new Socket(destination, port);
                socket.setSoTimeout(10000);
                Log.d("VS", "Socket connection status: " + socket.isConnected());

                dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                Log.d("VS", "Address retrieved");

                if (minBufSize != AudioRecord.ERROR_BAD_VALUE) {
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, minBufSize);
                    Log.d("VS", "Recorder initialized");
                }

                if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                    Log.d("VS", "Recorder working");
                    recorder.startRecording();
                }

                while (status == true) {

                    //reading data from MIC into buffer
                    int bufferReadResult = recorder.read(buffer, 0, buffer.length);

                    Log.d("VS", "DataOutputStream " + bufferReadResult);

                    dataOutputStream.write(buffer, 0, bufferReadResult);

                    dataOutputStream.flush();

                    Log.d("VS", "message : " + dataInputStream.available());

                    if (dataInputStream.available() > 0) {
                        String str = dataInputStream.readLine();
                        publishProgress(str);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }

                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(String... str) {
//            Log.d("VS", "from progress" + str);
            int intIndex = str[0].indexOf("finish");
            if (intIndex != -1) {
                textView.append(str[0] + "FINISH");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                stopRecording();
            } else {
                textView.append(str[0]);
            }
        }
    }
}