package com.example.dos;

import android.content.pm.PackageManager;
import android.media.AsyncPlayer;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.telecom.Call;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.dos.FFT.Complex;
import com.example.dos.FFT.FFT;
import com.example.dos.Receiver.Callback;
import com.example.dos.Receiver.ChunkElement;
import com.example.dos.Receiver.RecordTask;
import com.example.dos.Receiver.Recorder;
import com.example.dos.Receiver.TestTask;
import com.example.dos.Sender.BitFrequencyConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class AudioRecorderActivity extends AppCompatActivity implements Callback {

    private static final String TAG = AudioRecorderActivity.class.getSimpleName();

    //    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord audioRecord = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    private double durationSec = 0.270;

    //List of samples that need to be calculated
    private ArrayList<ChunkElement> recordedArray;
    //Recorder task used for recording samples
    private Recorder recorder = null;
    //Received message (after recording)
    private String myString = "";
    //Working task flag
    private boolean work = true;

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
        int intSize = AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS_OUT, RECORDER_AUDIO_ENCODING);
        at = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS_OUT, RECORDER_AUDIO_ENCODING, intSize, AudioTrack.MODE_STREAM);
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
        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int i, int i1) {
                min = timePicker.getCurrentHour();
                sec = timePicker.getCurrentMinute();
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String message = String.valueOf(editText.getText());
                if (!message.isEmpty()) {
                    sendValue(message);
                } else {
                    Toast.makeText(AudioRecorderActivity.this, "Message is empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            PlayShortAudioFileViaAudioTrack(mFileName);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Button clicked");
                startRecording();
            }
        });

        stopPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                at.stop();
            }
        });

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

                recordingThread = new Thread(new Runnable() {
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                writeAudioDataToFile();
                                 /*Handler handler = new Handler();
                                Runnable runnableCode = new Runnable() {
                                    @Override
                                    public void run() {
                                        handler.postDelayed(this, 800);
                                    }
                                };
                                handler.post(runnableCode);*/
                            }
                        });
                    }
                }, "AudioRecorder Thread");
                recordingThread.start();

                AsyncTask.execute(() -> {
                    while (isRecording) {
                        short[] sData = new short[BufferElements2Rec];
                        audioRecord.read(sData, 0, BufferElements2Rec);
//                                    System.out.println("Short writing to file" + Arrays.toString(sData));
                        byte[] bData = short2byte(sData);
//                        Log.i(TAG, "Byte data: " + Arrays.toString(bData));

//                        RecordTask recordTask = new RecordTask();
//                        recordTask.execute();

//                        onBufferAvailable(bData);

                    }
                });

                Log.i(TAG, "Min: " + min + " Sec: " + sec);
                stopTimer(min, sec);
            }
        } else {
            RequestPermissions();
        }
    }

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void writeAudioDataToFile() {
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

    }

    private void stopTimer(int minute, int second) {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startBtn.setEnabled(true);
                        stopRecording();
                    }
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
            recordingThread = null;
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
//        at.write(byteData, 0, 600000);
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

        int startFreq = 17500; //17500
        int endFreq = 20000; //20000
        int bitsPerTone = 4; //4bits

        //Create bit to frequency converter
        BitFrequencyConverter bitConverter = new BitFrequencyConverter(startFreq, endFreq, bitsPerTone);
        byte[] encodedMessage = msg.getBytes();

        Log.i(TAG, "Encoded Message: " + Arrays.toString(encodedMessage));

        ArrayList<Integer> freqs = bitConverter.calculateFrequency(encodedMessage);

        int bufferSize = AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        at = new AudioTrack(AudioManager.STREAM_MUSIC,
                RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize,
                AudioTrack.MODE_STREAM);
        at.play();

        //Calculate number of tones to be played
        int currProgress = 0;
        int allLength = freqs.size() * 2 + 4;

        //Start communication with start handshake
        playTone((double) bitConverter.getHandshakeStartFreq(), durationSec);
        playTone((double) bitConverter.getHandshakeStartFreq(), durationSec);
        //Transfer message if chat and file extension if data
        for (int freq : freqs) {
            //playTone((double)freq,durationSec);
            playTone((double) freq, durationSec / 2);
            playTone((double) bitConverter.getHandshakeStartFreq(), durationSec);
        }
        //End communication with end handshake
        playTone((double) bitConverter.getHandshakeEndFreq(), durationSec);
        playTone((double) bitConverter.getHandshakeEndFreq(), durationSec);
        //If file is being send, send file data too
        at.release();
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

    private void receiveValue(byte[] value) {

        int StartFrequency = 17500;
        int EndFrequency = 20000;
        int BitPerTone = 4;

        //Create list for recorded samples
        recordedArray = new ArrayList<ChunkElement>();
        //Create frequency to bit converter with specific parameters
        BitFrequencyConverter bitConverter = new BitFrequencyConverter(StartFrequency, EndFrequency, BitPerTone);
        //Load channel synchronization parameters
        int HalfPadd = bitConverter.getPadding() / 2;
        int HandshakeStart = bitConverter.getHandshakeStartFreq();
        int HandshakeEnd = bitConverter.getHandshakeEndFreq();

        //Create recorder and start it
        recorder = new Recorder();
        recorder.setCallback(this);
        recorder.start();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                //Flag used for start of receiving
                int listeningStarted = 0;
                //Counter used to know when to start receiving
                int startCounter = 0;
                //Counter used to know when to end receiving
                int endCounter = 0;
                //Used if file is being received for name part of file
                byte[] namePartBArray = null;
                //Flag used to know if data has been received before last synchronization bit
                int lastInfo = 2;
                myString = "";

                while (work) {
                    //Wait and get recorded data

            /*ChunkElement tempElem;
            synchronized (recordedArraySem) {
                while (recordedArray.isEmpty()) {
                    try {
                        recordedArraySem.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                tempElem = recordedArray.remove(0);
                recordedArraySem.notifyAll();
            }*/

                    //Calculate frequency from recorded data
                    Log.i(TAG, "Output Value: " + Arrays.toString(value));
                    double currNum = calculate(value, StartFrequency, EndFrequency, HalfPadd);

                    //Check if listening started
                    if (listeningStarted == 0) {
                        //If listening didn't started and frequency is in range of StartHandshakeFrequency
                        if ((currNum > (HandshakeStart - HalfPadd)) && (currNum < (HandshakeStart + HalfPadd))) {
                            startCounter++;
                            //If there were two StartHandshakeFrequency one after another start recording
                            if (startCounter >= 2) {
                                listeningStarted = 1;
                            }
                        } else {
                            //If its not StartHandshakeFrequency reset counter
                            startCounter = 0;
                        }
                    }
                    //If listening started
                    else {
                        //Check if its StartHandshakeFrequency (used as synchronization bit) after receiving
                        //starts
                        if ((currNum > (HandshakeStart - HalfPadd)) && (currNum < (HandshakeStart + HalfPadd))) {
                            //Reset flag for received data
                            lastInfo = 2;
                            //Reset end counter
                            endCounter = 0;
                        } else {
                            //Check if its EndHandshakeFrequency
                            if (currNum > (HandshakeEnd - HalfPadd)) {
                                endCounter++;
                                //If there were two EndHandshakeFrequency one after another stop recording if
                                //chat message is expected fileName==null or if its data transfer and only name
                                //has been received, reset counters and flags and start receiving file data.

                                //TODO: this if condition is used for writing or creating file so it may not require
                        /*if (endCounter >= 2) {
                            if (fileName != null && namePartBArray == null) {
                                namePartBArray = bitConverter.getAndResetReadBytes();
                                listeningStarted = 0;
                                startCounter = 0;
                                endCounter = 0;
                            } else {
                                setWorkFalse();
                            }
                        }*/


                            } else {
                                //Reset end counter
                                endCounter = 0;
                                //Check if data has been received before last synchronization bit
                                if (lastInfo != 0) {
                                    //Set flag
                                    lastInfo = 0;
                                    //Add frequency to received frequencies
                                    bitConverter.calculateBits(currNum);
                                }
                            }
                        }
                    }
                }

                //Convert received frequencies to bytes
                byte[] readBytes = bitConverter.getAndResetReadBytes();
                try {
                    if (namePartBArray == null) {
                        //If its chat communication set message as return string
                        myString = new String(readBytes, StandardCharsets.UTF_8);
                        Log.i(TAG, "Output string: " + myString);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    //Called for calculating frequency with highest amplitude from sound sample
    private double calculate(byte[] buffer, int StartFrequency, int EndFrequency, int HalfPad) {
        int analyzedSize = 1024;
        Complex[] fftTempArray1 = new Complex[analyzedSize];
        int tempI = -1;
        //Convert sound sample from byte to Complex array
        for (int i = 0; i < analyzedSize * 2; i += 2) {
            short buff = buffer[i + 1];
            short buff2 = buffer[i];
            buff = (short) ((buff & 0xFF) << 8);
            buff2 = (short) (buff2 & 0xFF);
            short tempShort = (short) (buff | buff2);
            tempI++;
            fftTempArray1[tempI] = new Complex(tempShort, 0);
        }
        //Do fast fourier transform
        final Complex[] fftArray1 = FFT.fft(fftTempArray1);
        //Calculate position in array where analyzing should start and end

        int startIndex1 = ((StartFrequency - HalfPad) * (analyzedSize)) / 44100;
        int endIndex1 = ((EndFrequency + HalfPad) * (analyzedSize)) / 44100;

        int max_index1 = startIndex1;
        double max_magnitude1 = (int) fftArray1[max_index1].abs();
        double tempMagnitude;
        //Find position of frequency with highest amplitude


        for (int i = startIndex1; i < endIndex1; ++i) {
            tempMagnitude = fftArray1[i].abs();
            if (tempMagnitude > max_magnitude1) {
                max_magnitude1 = (int) tempMagnitude;
                max_index1 = i;
            }
        }
        return 44100.0 * max_index1 / (analyzedSize);

    }

    //Called to turn off task
    public void setWorkFalse() {
        if (recorder != null) {
            recorder.stop();
            recorder = null;
        }
        this.work = false;
    }

    public boolean CheckPermissions() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void RequestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, 1);
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
    public void onBufferAvailable(byte[] buffer) {

        Log.i(TAG, "Buffer from interface: " + Arrays.toString(buffer));
//        receiveValue(buffer);

        /*String recordedArraySem = "Semaphore";
        synchronized (recordedArraySem) {
            recordedArray.add(new ChunkElement(buffer));
            recordedArraySem.notifyAll();
            while (recordedArray.size() > 100) {
                try {
                    recordedArraySem.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }*/
    }

    @Override
    public void setBufferSize(int size) {}

}