package com.se101.chairapy;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.se101.chairapy.ml.Emotion;
import com.se101.chairapy.tokenization.FullTokenizer;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;

    //TextView mTextTv;
    ImageButton mVoiceBtn;
    private HashMap<String, Integer> tokensDic;;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //mTextTv = findViewById(R.id.textTV);
        mVoiceBtn = findViewById(R.id.voiceBtn);

        mVoiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak();
            }
        });

        // get tokens for ml
        InputStream is = getApplicationContext().getResources().openRawResource(R.raw.vocab);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        tokensDic = new HashMap<String, Integer>();

        int count = 0;
        while(true) {
            // replaces while(reader.read()) in case reader is badly initialized
            try {
                if (!reader.ready()) break;
            } catch (IOException e) {
                e.printStackTrace();
            }

            // init line so we can use try catch
            String line = null;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            tokensDic.put(line, count);
            count++;
        }

        Log.e("size", String.valueOf(tokensDic.size()));

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

                    FullTokenizer tokenizer = new FullTokenizer(tokensDic, /* doLowerCase= */ true);
                    List<String> resTokens = tokenizer.tokenize(result.get(0));

                    List<Integer> resIds = tokenizer.convertTokensToIds(resTokens);

//                    try {
//                        Emotion model = Emotion.newInstance(getApplicationContext());
//
//                        //Creates inputs for reference.
//                        TensorBuffer ids = TensorBuffer.createFixedSize(new int[]{1, 128}, DataType.INT32);
//                        ids.loadBuffer(byteBuffer);
//                        TensorBuffer segmentIds = TensorBuffer.createFixedSize(new int[]{1, 128}, DataType.INT32);
//                        segmentIds.loadBuffer(byteBuffer);
//                        TensorBuffer mask = TensorBuffer.createFixedSize(new int[]{1, 128}, DataType.INT32);
//                        mask.loadBuffer(byteBuffer);
//
//                        // Runs model inference and gets result.
//                        Emotion.Outputs outputs = model.process(ids, segmentIds, mask);
//                        List<Category> probability = outputs.getProbabilityAsCategoryList();
//
//                        // Releases model resources if no longer used.
//                        model.close();
//                    } catch (IOException e) {
//                        // TODO Handle the exception
//                    }
                }

                //mTextTv.setText(result.get(0));
            }
        }
    }
}