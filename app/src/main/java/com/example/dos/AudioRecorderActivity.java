package com.example.dos;

import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.dos.Receiver.MessagingInterface;
import com.example.dos.Receiver.RecordTask;
import com.example.dos.Sender.BitFrequencyConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class AudioRecorderActivity extends AppCompatActivity implements MessagingInterface {

    private static final String TAG = AudioRecorderActivity.class.getSimpleName();

    //    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord audioRecord = null;
    private boolean isRecording = false;
    private boolean isListening = true;

    EditText editText;
    Button startBtn, playBtn, stopPlayBtn, sendBtn;
    //    Button stopBtn;
    int min = 0, sec = 0;
    String mFileName = null;
    AudioTrack at = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_recorder);

        setButtonHandlers();
        if (CheckPermissions()) {
            int intSize = AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS_OUT, RECORDER_AUDIO_ENCODING);
            at = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS_OUT, RECORDER_AUDIO_ENCODING, intSize, AudioTrack.MODE_STREAM);
            runTask(true);
        } else {
            RequestPermissions();
        }

    }

    private void setButtonHandlers() {

        editText = findViewById(R.id.text_ip);
        startBtn = findViewById(R.id.start_button);
        playBtn = findViewById(R.id.play_button);
        stopPlayBtn = findViewById(R.id.stopPlay_button);
        sendBtn = findViewById(R.id.send_button);
//        stopBtn = findViewById(R.id.stop_button);
        startBtn.setEnabled(true);
//        stopBtn.setEnabled(false);

        mFileName = Environment.getExternalStorageDirectory() + File.separator + getString(R.string.app_name);
        mFileName += "/voice8K16BitMono.pcm";

        TimePicker timePicker = findViewById(R.id.time_picker);
        timePicker.setIs24HourView(true);
        timePicker.setCurrentHour(0); //By default it sets device time in timePicker so, it sets value manually
        timePicker.setCurrentMinute(0);
        timePicker.setOnTimeChangedListener((timePicker1, i, i1) -> {
            min = timePicker1.getCurrentHour();
            sec = timePicker1.getCurrentMinute();
        });

        sendBtn.setOnClickListener(v -> {
            String message = String.valueOf(editText.getText());
            if (!message.isEmpty()) {
                sendValue(message);
            } else {
                Toast.makeText(AudioRecorderActivity.this, "Message is empty", Toast.LENGTH_SHORT).show();
            }
        });

        playBtn.setOnClickListener(view -> AsyncTask.execute(() -> {
            try {
                PlayShortAudioFileViaAudioTrack(mFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        startBtn.setOnClickListener(view -> {
            Log.i(TAG, "Button clicked");
            startRecording();
        });

        stopPlayBtn.setOnClickListener(view -> at.stop());

        /*stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                stopRecording();
            }
        });*/
    }

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private void startRecording() {

        if (CheckPermissions()) {

            if (min == 0 && sec == 0) {
                Toast.makeText(AudioRecorderActivity.this, "Timer is not set properly", Toast.LENGTH_SHORT).show();
            } else {
                startBtn.setEnabled(false);
//                stopBtn.setEnabled(true);

                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                        RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

                audioRecord.startRecording();
                isRecording = true;

                /*AsyncTask.execute(() -> {
                    while (isRecording) {
                        if (isListening) {
                            RecordTask recordTask = new RecordTask(this);
                            recordTask.execute();
                        }
                    }
                });*/

                Log.i(TAG, "Min: " + min + " Sec: " + sec);
                stopTimer(min, sec);
            }
        } else {
            RequestPermissions();
        }
    }

    //convert short to byte
    /*private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }*/

    /*private void writeAudioDataToFile() {
        // Write the output audio in byte

        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + getString(R.string.app_name));
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }

        if (success) {
            short[] sData = new short[BufferElements2Rec];

            FileOutputStream os = null;
            try {
                os = new FileOutputStream(mFileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            while (isRecording) {
                // gets the voice output from microphone to byte format

                audioRecord.read(sData, 0, BufferElements2Rec);
                System.out.println("Short writing to file" + Arrays.toString(sData));
                try {
                    // // writes the data to file from buffer
                    // // stores the voice buffer
                    byte[] bData = short2byte(sData);
                    assert os != null;
                    os.write(bData, 0, BufferElements2Rec * BytesPerElement);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                assert os != null;
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(AudioRecorderActivity.this, "Folder does not exist", Toast.LENGTH_SHORT).show();
        }

    }*/

    private void stopTimer(int minute, int second) {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    startBtn.setEnabled(true);
                    stopRecording();
                });
            }
        };
        int totalMillisecond = 1000 * second + minute * 60 * 1000;
        timer.schedule(timerTask, totalMillisecond);
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != audioRecord) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            Toast.makeText(getApplicationContext(), "Recording Stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private void PlayShortAudioFileViaAudioTrack(String filePath) throws IOException {
        // We keep temporarily filePath globally as we have only two sample sounds now..
        if (filePath == null)
            return;

        //Reading the file..
        File file = new File(filePath);
//        byte[] byteData = new byte[(int) 600000];
        byte[] byteData = new byte[(int) file.length()];
        Log.d(TAG, "ByteData length: " + (int) file.length() + "");

        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            in.read(byteData);
            in.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Set and push to audio track..
//        int intSize = AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, RECORDER_AUDIO_ENCODING);
//        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, RECORDER_AUDIO_ENCODING, intSize, AudioTrack.MODE_STREAM);
        at.play();
        // Write the byte array to the track
        at.write(byteData, 0, byteData.length);
        at.stop();
        at.release();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void sendValue(String msg) {

        runTask(false);
        int startFreq = 17500; //17500
        int endFreq = 20000; //20000
        int bitsPerTone = 4; //4bits

        //Create bit to frequency converter
        BitFrequencyConverter bitConverter = new BitFrequencyConverter(startFreq, endFreq, bitsPerTone);
        byte[] encodedMessage = msg.getBytes();
        ArrayList<Integer> freqs = bitConverter.calculateFrequency(encodedMessage);

        int bufferSize = AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        at = new AudioTrack(AudioManager.STREAM_MUSIC,
                RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize,
                AudioTrack.MODE_STREAM);
        at.play();

        //Start communication with start handshake
        double durationSec = 0.270;
        playTone(bitConverter.getHandshakeStartFreq(), durationSec);
        playTone(bitConverter.getHandshakeStartFreq(), durationSec);
        //Transfer message if chat and file extension if data
        for (int freq : freqs) {
            //playTone((double)freq,durationSec);
            playTone(freq, durationSec / 2);
            playTone(bitConverter.getHandshakeStartFreq(), durationSec);
        }
        //End communication with end handshake
        playTone(bitConverter.getHandshakeEndFreq(), durationSec);
        playTone(bitConverter.getHandshakeEndFreq(), durationSec);
        //If file is being send, send file data too
        at.release();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runTask(true);
            }
        }, 2000);
    }

    //Called to play tone of specific frequency for specific duration
    public void playTone(double freqOfTone, double duration) {
        //Calculate number of samples in given duration
        double dnumSamples = duration * RECORDER_SAMPLERATE;
        dnumSamples = Math.ceil(dnumSamples);
        int numSamples = (int) dnumSamples;
        double sample[] = new double[numSamples];
        //Every sample 16bit
        byte generatedSnd[] = new byte[2 * numSamples];
        //Fill the sample array with sin of given frequency
        double anglePadding = (freqOfTone * 2 * Math.PI) / (RECORDER_SAMPLERATE);
        double angleCurrent = 0;
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(angleCurrent);
            angleCurrent += anglePadding;
        }
        //Convert to 16 bit pcm (pulse code modulation) sound array
        //assumes the sample buffer is normalized.
        int idx = 0;
        int i = 0;
        //Amplitude ramp as a percent of sample count
        int ramp = numSamples / 20;
        //Ramp amplitude up (to avoid clicks)
        for (i = 0; i < ramp; ++i) {
            double dVal = sample[i];
            //Ramp up to maximum
            final short val = (short) ((dVal * 32767 * i / ramp));
            //In 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        // Max amplitude for most of the samples
        for (i = i; i < numSamples - ramp; ++i) {
            double dVal = sample[i];
            //Scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            //In 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        //Ramp amplitude down
        for (i = i; i < numSamples; ++i) {
            double dVal = sample[i];
            //Ramp down to zero
            final short val = (short) ((dVal * 32767 * (numSamples - i) / ramp));
            //In 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        try {
            // Play the track
            at.write(generatedSnd, 0, generatedSnd.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean CheckPermissions() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void RequestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, 1);
    }

    private void runTask(boolean run) {
        RecordTask recordTask = new RecordTask(AudioRecorderActivity.this);
        if (run) {
            isListening = true;
            AsyncTask.execute(recordTask::execute);
        } else {
            isListening = false;
            recordTask.setWorkFalse();
        }
    }

    /*@Override
    protected void onPause() {
        super.onPause();
        stopTimer(0, 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer(0,1);
    }*/

    @Override
    public void myMessage(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (isListening) {
                    Log.i(TAG, "Message: " + message);
                    Toast.makeText(AudioRecorderActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
        runTask(false);
        runTask(true);
    }

}