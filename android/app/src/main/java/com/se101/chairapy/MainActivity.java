package com.se101.chairapy;

import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private Handler handler;
    BertEmotionModel model;
    JSONArray suggestionArr;
    private boolean emotionsReceived = false;

    //TextView mTextTv;
    ImageButton mVoiceBtn;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        //mTextTv = findViewById(R.id.textTV);
        mVoiceBtn = findViewById(R.id.voiceBtn);

        NumberPicker BuzzSetter = (NumberPicker) findViewById(R.id.buzzSet);
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
                Log.e("Selected Number", String.valueOf(newVal));
            }
        });


        mVoiceBtn.setOnClickListener(v -> speak());

        Button notificationButton = findViewById(R.id.notificationButton);
        AtomicBoolean toggle = new AtomicBoolean(false);

        notificationButton.setOnClickListener(v -> {
            if (!toggle.get()) {
                Toast.makeText(this, "Set reminder", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
                @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

                // period for reminders to 'get up'
                int period = BuzzSetter.getValue() * 1000 * 60;
                Toast.makeText(this, Integer.toString(BuzzSetter.getValue() * 1000 * 60), Toast.LENGTH_SHORT).show();
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
    }

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
        }
    }

    // method for creating notification channel
    private void createNotificationChannel() {

        //
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

    /* make a json
    anger -> suggest to go work out
    fear -> show me some cute cats
    joy -> show me dance songs /show me some cute cats / go workout
    neutral -> ANIMENZ HEHEHEHEH
    sadness -> show me uplifting songs/ show me some sad songs
    cancel -> Thanks!
    sad songs: https://www.youtube.com/watch?v=CveANi17YfU&list=PL3-sRm8xAzY-w9GS19pLXMyFRTuJcuUjy
     */

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
                            if(obj[0][j] > 0.2){
                                emotions.add(model.getLabels().get(j));
                                Log.e("emote", model.getLabels().get(j));
                            }
                        }
                    }
                    emotionsReceived = true;

                    ArrayList<JSONObject> suggestions = getSuggestions(this, emotions);

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

        String items[] = new String[suggestions.size()];
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
                        Log.e("model", suggestions.get(id).toString());
                    }
                });

        AlertDialog dialog = suggestionBuilder.create();
        dialog.show();
    }
}