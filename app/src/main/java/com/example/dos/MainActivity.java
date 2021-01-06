package com.example.dos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    private static String TAG = MainActivity.class.getSimpleName();

    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private static final String LOG_TAG = "AudioRecording";
    private static String mFileName = null;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;

    Button startBtn;
    //    Button stopBtn;
    Button playBtn;
    Button stopPlayBtn;

    int min = 0, sec = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startBtn = findViewById(R.id.start_button);
//        stopBtn = findViewById(R.id.stop_button);
        playBtn = findViewById(R.id.play_button);
        stopPlayBtn = findViewById(R.id.stopPlay_button);

        TimePicker timePicker = findViewById(R.id.time_picker);
        timePicker.setIs24HourView(true);
        timePicker.setCurrentHour(0); //By default it sets device time in timePicker so, it sets value manually
        timePicker.setCurrentMinute(0);
        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int i, int i1) {
                String time = "Current time: " + timePicker.getCurrentHour() + " : " + timePicker.getCurrentMinute();
                min = timePicker.getCurrentHour();
                sec = timePicker.getCurrentMinute();
            }
        });

//        stopBtn.setEnabled(false);
        playBtn.setEnabled(false);
        stopPlayBtn.setEnabled(false);

        mFileName = Environment.getExternalStorageDirectory() + File.separator + getString(R.string.app_name);
        mFileName += "/AudioRecording.3gp";

        startBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("LongLogTag")
            @Override
            public void onClick(View view) {

                if (CheckPermissions()) {

                    if (min == 0 && sec == 0) {
                        Toast.makeText(MainActivity.this, "Timer is not set properly", Toast.LENGTH_SHORT).show();
                    } else {

//                        stopBtn.setEnabled(true);
                        startBtn.setEnabled(false);
                        playBtn.setEnabled(false);
                        stopPlayBtn.setEnabled(false);

                        File folder = new File(Environment.getExternalStorageDirectory() +
                                File.separator + getString(R.string.app_name));
                        boolean success = true;
                        if (!folder.exists()) {
                            success = folder.mkdirs();
                        }

                        if (success) {
                            mRecorder = new MediaRecorder();
                            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

                            mRecorder.setAudioEncodingBitRate(AudioFormat.ENCODING_PCM_16BIT);
                            mRecorder.setAudioSamplingRate(44100);

                            mRecorder.setOutputFile(mFileName);

                            try {
                                mRecorder.prepare();
                            } catch (IOException e) {
                                Log.e(LOG_TAG, "prepare() failed");
                            }

                            mRecorder.start();
                            Toast.makeText(getApplicationContext(),
                                    "Recording Started, Timer is set for " + min + " minute and " + sec + " seconds", Toast.LENGTH_LONG).show();

                            getSamplingData();
                            stopTimer(min, sec);

                        } else {
                            Toast.makeText(MainActivity.this, "Folder does not exist", Toast.LENGTH_SHORT).show();
                        }

                    }


                } else {
                    RequestPermissions();
                }
            }
        });

        /*stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });*/

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                stopBtn.setEnabled(false);
                startBtn.setEnabled(true);
                playBtn.setEnabled(false);
                stopPlayBtn.setEnabled(true);

                mPlayer = new MediaPlayer();
                try {
                    mPlayer.setDataSource(mFileName);
                    mPlayer.prepare();
                    mPlayer.start();
                    Toast.makeText(getApplicationContext(), "Recording Started Playing", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "prepare() failed");
                }
            }
        });

        stopPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayer.release();
                mPlayer = null;
//                stopBtn.setEnabled(false);
                startBtn.setEnabled(true);
                playBtn.setEnabled(true);
                stopPlayBtn.setEnabled(false);
                Toast.makeText(getApplicationContext(), "Playing Audio Stopped", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void stopRecording() {
//        stopBtn.setEnabled(false);
        startBtn.setEnabled(true);
        playBtn.setEnabled(true);
        stopPlayBtn.setEnabled(true);

        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        Toast.makeText(getApplicationContext(), "Recording Stopped", Toast.LENGTH_LONG).show();
    }

    private void stopTimer(int minute, int second) {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopRecording();
                    }
                });
            }
        };
        int totalMillisecond = 1000 * second + minute * 60 * 1000;
        timer.schedule(timerTask, totalMillisecond);
    }

    private void getSamplingData() {
        final Handler handler = new Handler();
        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {

                if (mRecorder == null) {
                    Log.i(TAG, "Recorder is null");
                    handler.removeCallbacksAndMessages(null);
                } else {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Log.d(TAG, "Raw data: " + mRecorder.getMetrics());
                    }



                    Log.d(TAG, "Raw data: " + mRecorder.getMaxAmplitude());
                    Log.i(TAG, "Recorder is not null");
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.post(runnableCode);
    }

    public boolean CheckPermissions() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void RequestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);
    }
}