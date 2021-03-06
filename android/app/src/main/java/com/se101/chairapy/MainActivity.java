package com.se101.chairapy;

import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;

import java.util.HashMap;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private static final int REQUEST_ENABLE_BT = 2000;
    private String TAG_BT="BT device";
    private Handler handler;
    BertEmotionModel model;
    JSONArray suggestionArr;
    private boolean emotionsReceived = false;

    //TextView mTextTv;
    ImageButton mVoiceBtn;

    // bluetooth stuff
    public static BluetoothSocket socket;
    ProgressDialog dialog;
    NumberPicker BuzzSetter;
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread btConnection;
    private ConnectedThread connectedThread;
    public static final int MESSAGE_READ = 0;
    public static final int MESSAGE_WRITE = 1;
    public static final int MESSAGE_TOAST = 2;
    private Handler BThandler = new Handler();
    private boolean BT_CONNECTED = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        mVoiceBtn = findViewById(R.id.voiceBtn);
        mVoiceBtn.setOnClickListener(v -> speak());


        BuzzSetter = (NumberPicker) findViewById(R.id.buzzSet);
        BuzzSetter.setMinValue(5);
        BuzzSetter.setMaxValue(120);
        BuzzSetter.setValue(30);

        //Gets whether the selector wheel wraps when reaching the min/max value.
        BuzzSetter.setWrapSelectorWheel(true);

        //Set a value change listener for NumberPicker
        BuzzSetter.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal){
                //Display the newly selected number from picker
                String val = String.format("<%3d>", newVal);
                Log.e("Selected Number", val);
                // Send command to Arduino board
                connectedThread.write(val.getBytes());
            }
        });
        toggleNumPicker();

        Button notificationButton = findViewById(R.id.notificationButton);
        AtomicBoolean toggle = new AtomicBoolean(false);

        notificationButton.setOnClickListener(v -> {
            if (!toggle.get()) {
                Toast.makeText(this, "Set reminder", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
                @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

                // period for reminders to 'get up'
                int period = BuzzSetter.getValue() * 60 * 1000;
                Timer t = new Timer();
                t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // get time - repeats every user-selected number of minutes
                        long currentTime = System.currentTimeMillis();
                        long reminderTime = currentTime + (long) period;

                        alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                    }
                }, 0, period);

                /* change based off of
                int dinnerHour = 18;
                int dinnerMinute = 0;
                int sleepHour = 3;
                int sleepMinute = 0;
                ZoneId zone = ZoneId.of("America/Toronto");
                LocalTime time = LocalTime.now(zone);
                System.out.println(time);
                if (time.getHour() == dinnerHour && time.getMinute() == dinnerMinute) {
                    // is it time to consume?
                    System.out.println("dinnertime!");
                } else if (time.getHour() == sleepHour && time.getMinute() == sleepMinute) {
                    // is it time to pass out?
                    System.out.println("bedtime!");
                }
                */

                toggle.set(true);
            } else {
                Toast.makeText(this, "Turned off reminder toggle", Toast.LENGTH_SHORT).show();
                toggle.set(false);
            }

        });

        // bluetooth stuff
        // future: more error handling
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Button connectBtn = findViewById(R.id.btBtn);
        connectBtn.setOnClickListener(v -> {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                listDevices();
            }
        });

        //ConnectThread btconnection = new ConnectThread(pairedDevices)
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        btConnection.cancel();
//        connectedThread.cancel();
//    }

    private void speak() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speaketh");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "Your Device Doesn't Support Speech Input", Toast.LENGTH_SHORT).show();
            //Toast.makeText(this, "Not Recognized "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    // called when the tts is completed
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && null != data) {
                // array of 1 element containing the whole string
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                // inference
                if(result.size()>0){
                    if(result.size()>1) Log.e("bruh", "your result size too big");

                    String text = result.get(0);
                    Context context = this.getApplicationContext();
                    model = new BertEmotionModel(context);
                    handler = new Handler();
                    handler.post(() -> { model.load(); });
                    try {
                        Log.e("classify", "do");
                        inferEmotion(text);
                        Log.e("classify", "done");

                        Log.e("model", text);
                        handler.post( () -> { model.unload(); });
                    } catch (Exception e) {
                        // TODO Handle the exception
                    }
                }

                //mTextTv.setText(result.get(0));
            }
        } else if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                listDevices();
            }
        }
    }

    // method for creating notification channel
    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // initializing channel and setting default paramters
            CharSequence channelName = "reminderChannel";
            String desc = "Channel for reminders";
            int priorityLevel = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("notifyMe", channelName, priorityLevel);
            channel.setDescription(desc);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void playMusic(String id, boolean isPlaylist) {
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://www.youtube.com/watch?v=" + id));

        if(isPlaylist){
            webIntent.setClassName("com.google.android.youtube", "com.google.android.youtube.app.froyo.phone.PlaylistActivity");
            startActivity(webIntent);
            return;
        }
        try{
            startActivity(appIntent);
        }
        catch (ActivityNotFoundException ex) {
            startActivity(webIntent);
        }
    }

    private void openLink(String url) {
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(webIntent);
    }

    private void inferEmotion(final String text) {
        ArrayList<String> emotions = new ArrayList<>();
        handler.post(
                () -> {
                    // Run text classification with TF Lite.
                    Map<Integer, Object> out = model.classify(text);

                    // @ music kids: here r examples on how to access the output data
                    // model.getLabels().get(idx) is an emotion
                    // obj[0][idx] is the probability for that emotion
                    for(int i=0; i<out.size(); i++){
                        float[][] obj = (float[][]) out.get(i); // [1,5] by model architecture
                        for(int j=0; j<obj[0].length; j++){
                            if(obj[0][j] > 0.16){
                                emotions.add(model.getLabels().get(j));
                            }
                            Log.e("emote", model.getLabels().get(j) + " : " + String.valueOf(obj[0][j]));
                        }
                    }
                    emotionsReceived = true;

                    ArrayList<JSONObject> suggestions = getSuggestions(this, emotions);

                    assert suggestions != null;
                    showSuggestions(this, suggestions);
                });
    }

    private ArrayList<JSONObject> getSuggestions(Context context, ArrayList<String> emotions) {
        String jsonString;
        try {
            InputStream jsonFile = context.getResources().openRawResource(R.raw.suggestions);
            int size = jsonFile.available();
            byte[] buffer = new byte[size];
            jsonFile.read(buffer);
            jsonFile.close();
            jsonString = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        JSONObject innerObj;
        try {
            JSONObject suggestionObj = new JSONObject(jsonString);
            suggestionArr = suggestionObj.getJSONArray("emotionMLTips");
        } catch (JSONException e){
            e.printStackTrace();
            return null;
        }

        ArrayList<JSONObject> displayList = new ArrayList<>();
        for(int i=0; i<suggestionArr.length(); i++){
            try {
                innerObj = suggestionArr.getJSONObject(i);
                for(String emotion: emotions){
                    if(innerObj.getString("emotion").contains(emotion)){
                        displayList.add(innerObj);
                        break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return displayList;
    }

    private void showSuggestions(Context context, ArrayList<JSONObject> suggestions){
        AlertDialog.Builder suggestionBuilder = new AlertDialog.Builder(context);
        suggestionBuilder.setTitle("Hey bud, I hope life gets better!");

        String[] items = new String[suggestions.size()];
        for(int i=0; i< suggestions.size(); i++){
            try {
                items[i] = suggestions.get(i).getString("msg");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        suggestionBuilder.setItems(items,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        JSONObject current = suggestions.get(id);
                        try {
                            if(current.has("url")){
                                if (current.get("isYoutube").equals(1)) {
                                    startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com/watch?v=" + suggestions.get(id).get("url"))));
                                } else {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(current.getString("url"))));
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.e("model", current.toString());
                    }
                });

        AlertDialog dialog = suggestionBuilder.create();
        dialog.show();
    }

    // BLOOO TOOOOOTTTHHHHHH
    // bluetooth*

    private void listDevices(){
        // only checks for paired devices so far
        // force the user to pair the chair beforehand?
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        String deviceName, deviceAddress;
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceName = device.getName();
                deviceAddress = device.getAddress(); // MAC address
                Log.v("BT device", deviceName + "\t" + deviceAddress);
                if(deviceName.contains("HC-06")){
                    Log.v("BT device", "found");
                    dialog = ProgressDialog.show(this, deviceName,
                            "Connecting...", true);
                    btConnection = new ConnectThread(device);
                    btConnection.start();
                    Log.v("BT device", "connecting");
                }
            }
        }
    }

    private void toggleNumPicker(){
        runOnUiThread(() -> { BuzzSetter.setEnabled(BT_CONNECTED); });
    }
    // nesting a whole class like a boss

    // the code for the two classes below belong to the Bluetooth example under Android
    // developer guide. We do not take credit.
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                for(ParcelUuid pu : device.getUuids()){
                    Log.e(TAG_BT, pu.toString());
                }
                tmp = device.createRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());
            } catch (IOException e) {
                Log.e("Connect Thread", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            // bluetoothAdapter.cancelDiscovery();
            int maxAttempts = 5;
            boolean done = false;
            while(true && !done){
                try {
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    mmSocket.connect();
                    done = true;
                } catch (IOException connectException) {
                    Log.e("Connect thread", "cannot connect");
                    Log.e("Connect thread", connectException.toString());
                    // Unable to connect; close the socket and return.
                    if(maxAttempts == 0){
                        try {
                            mmSocket.close();
                            runOnUiThread(()->{ dialog.dismiss(); });
                        } catch (IOException closeException) {
                            Log.e("Connect Thread", "Could not close the client socket", closeException);
                        }
                        return;
                    }
                }
                maxAttempts--;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            //manageMyConnectedSocket(mmSocket);
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.start();
            BT_CONNECTED = true;
            Log.v(TAG_BT, "connected");
            runOnUiThread(()->{ dialog.dismiss(); });
            toggleNumPicker();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("Connect Thread", "Could not close the client socket", e);
            }
        }
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG_BT, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
                if(!BT_CONNECTED) Log.w(TAG_BT, "bt not set as connected");
            } catch (IOException e) {
                Log.e(TAG_BT, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            Log.v(TAG_BT, "In out streams ready");
            toggleNumPicker();
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = BThandler.obtainMessage(MESSAGE_READ, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG_BT, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = BThandler.obtainMessage(MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG_BT, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg = BThandler.obtainMessage(MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                BThandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG_BT, "Could not close the connect socket", e);
            }
        }
    }
}