/*
MODIFIED Nov 26, 2021
by UWaterloo SE101 Fall '21 Chairapy project group

In compliance with the below license (APLv2 section 4a):
This file has been modified to suit the current application at

    https://github.com/alexshaoo/Chairapy/tree/main/android

In particular, the modified sections are:
  -  class name
  -  select resource locations
  -  convertTextToFeature()
     -  using tokenizer from TensorFlow
     -  different data structures to fit model architecture
  -  loadLabelFile(): added boolean to avoid null lines
  -  loadDictionaryFile(): different data structures


DO NOT remove this section. We do not want to hold legal responsibility
as we are bright young adults looking to get rich and successful.
==============================================================================

Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/
package com.se101.chairapy;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import com.se101.chairapy.tokenization.FullTokenizer;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.metadata.MetadataExtractor;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BertEmotionModel {
    private static final String TAG = "Interpreter";

    private static final int SENTENCE_LEN = 128; // The maximum length of an input sentence.
    // Simple delimiter to split words.
    private static final String SIMPLE_SPACE_OR_PUNCTUATION = " |\\,|\\.|\\!|\\?|\n";
    private static final String MODEL_PATH = "emotion.tflite";

    private static final String START = "<START>";
    private static final String PAD = "<PAD>";
    private static final String UNKNOWN = "<UNKNOWN>";

    /** Number of results to show in the UI. */
    private static final int MAX_RESULTS = 3;

    private final Context context;
    private final Map<String, Integer> tokensDic = new HashMap<>();
    private final List<String> labels = new ArrayList<>();
    private Interpreter model;

    public BertEmotionModel(Context context) {
        this.context = context;
    }

    /** Load the TF Lite model and dictionary so that the client can start classifying text. */
    public void load() {
        loadModel();
    }

    /** Load TF Lite model. */
    /** MODIFIED FROM ORIGINAL (see file header) */
    private synchronized void loadModel() {
        try {
            // Load the TF Lite model
            ByteBuffer buffer = loadModelFile(this.context.getAssets(), MODEL_PATH);
            model = new Interpreter(buffer);
            Log.v(TAG, "TFLite model loaded.");

            // Use metadata extractor to extract the dictionary and label files.

            // Extract and load the dictionary file.
            InputStream dictionaryFile = context.getResources().openRawResource(R.raw.vocab);
            loadDictionaryFile(dictionaryFile);
            Log.v(TAG, "Dictionary loaded.");

            // Extract and load the label file.
            InputStream labelFile = context.getResources().openRawResource(R.raw.labels);
            loadLabelFile(labelFile);
            Log.v(TAG, "Labels loaded.");

        } catch (IOException ex) {
            Log.e(TAG, "Error loading TF Lite model.\n", ex);
        }
    }

    /** Free up resources as the client is no longer needed. */
    public synchronized void unload() {
        model.close();
        tokensDic.clear();
        labels.clear();
    }

    /** Classify an input string and returns the classification results. */
    public synchronized Map<Integer, Object> classify(String text) {
        Log.e(TAG, text);
        // Pre-prosessing.
        Object[] input = convertTextToFeature(text);

        // Run inference.
        Log.v(TAG, "Classifying text with TF Lite...");
        Map<Integer, Object> output = new HashMap<>();
        for(int i=0; i<model.getOutputTensorCount(); i++){
            int[] shape = model.getOutputTensor(i).shape();
            float[][] out = new float[shape[0]][shape[1]];
            //hardcoded - we know it's 2 dims from tflite file
            output.put(i, out);
        }

        if(output == null) Log.e("out", "outputs null");
        if(output.isEmpty()) Log.e("out", "outputs empty");

        model.runForMultipleInputsOutputs(input, output);

        // Find the best classifications.
//        PriorityQueue<Result> pq =
//                new PriorityQueue<>(
//                        MAX_RESULTS, (lhs, rhs) -> Float.compare(rhs.getConfidence(), lhs.getConfidence()));
//        for (int i = 0; i < labels.size(); i++) {
//            pq.add(new Result("" + i, labels.get(i), output[0][i]));
//        }
//        final ArrayList<Result> results = new ArrayList<>();
//        while (!pq.isEmpty()) {
//            results.add(pq.poll());
//        }
//
//        Collections.sort(results);
        // Return the probability of each class.
        return output;
    }

    /** Load TF Lite model from assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath)
            throws IOException {
        try (AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    /** Load label from model file. */
    /** MODIFIED FROM ORIGINAL (see file header) */
    private void loadLabelFile(InputStream ins) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
        String line;
        // Each line in the label file is a label.
        while (reader.ready() && (line = reader.readLine()) != null) {
            labels.add(line);
        }
    }

    /** Load dictionary from model file. */
    /** MODIFIED FROM ORIGINAL (see file header) */
    private void loadDictionaryFile(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        while(reader.ready()) {
            // init line so we can use try catch
            String[] line = reader.readLine().split(" ");
            if(line.length < 2) continue;
            tokensDic.put(line[0], Integer.valueOf(line[1]));
        }

        Log.e("size", String.valueOf(tokensDic.size()));
    }

    /** Pre-prosessing: tokenize and map the input words into a float array. */
    /** MODIFIED FROM ORIGINAL (see file header) */
    Object[] convertTextToFeature(String text) {
        FullTokenizer tokenizer = new FullTokenizer(tokensDic, /* doLowerCase= */ true);
        List<String> resTokens = tokenizer.tokenize(text);

        List<Integer> resIds = tokenizer.convertTokensToIds(resTokens);

        int[][] ids = new int[1][SENTENCE_LEN];
        int[][] segs = new int[1][SENTENCE_LEN];
        int[][] mask = new int[1][SENTENCE_LEN];
        Object[] features = new Object[3];

        //List<String> array = Arrays.asList(text.split(SIMPLE_SPACE_OR_PUNCTUATION));

        for (int i=0; i<resIds.size(); i++) {
            if(i==SENTENCE_LEN){
                features[0] = ids;
                features[1] = segs;
                features[2] = mask;
                return features;
            }
            Log.v("resid", String.valueOf(resIds.get(i)));
            ids[0][i] = resIds.get(i);
            segs[0][i] = 0; // for now we only work with one segment
            mask[0][i] = 1;
        }

        // Padding and wrapping.
        Arrays.fill(ids[0], resIds.size(), SENTENCE_LEN - 1, (int) tokensDic.get(PAD));
        Arrays.fill(segs[0], resIds.size(), SENTENCE_LEN - 1, 0);
        Arrays.fill(mask[0], resIds.size(), SENTENCE_LEN - 1, 0);

        features[0] = ids;
        features[1] = segs;
        features[2] = mask;
        return features;
    }

    Map<String, Integer> getDic() {
        return this.tokensDic;
    }

    Interpreter getModel() {
        return this.model;
    }

    List<String> getLabels() {
        return this.labels;
    }
}
