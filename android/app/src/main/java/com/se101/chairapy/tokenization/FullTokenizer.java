/*
MODIFIED Nov 25, 2021
by UWaterloo SE101 Fall '21 Chairapy project group

In compliance with the below license (APLv2 section 4a):
This file has been modified to suit the current application at

    https://github.com/alexshaoo/Chairapy/tree/main/android

In particular, the modified sections are:
  -  string constant for start/sep token
  -  convertTokensToIds:
       -  added start token at beginning of list if token exists
       -  added sep token at end of list if token exists

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
package com.se101.chairapy.tokenization;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A java realization of Bert tokenization. Original python code:
 * https://github.com/google-research/bert/blob/master/tokenization.py runs full tokenization to
 * tokenize a String into split subtokens or ids.
 */
public final class FullTokenizer {
  private final BasicTokenizer basicTokenizer;
  private final WordpieceTokenizer wordpieceTokenizer;
  private final Map<String, Integer> dic;

  private static final String START = "[CLS]";
  private static final String SEP = "[SEP]";

  public FullTokenizer(Map<String, Integer> inputDic, boolean doLowerCase) {
    dic = inputDic;
    basicTokenizer = new BasicTokenizer(doLowerCase);
    wordpieceTokenizer = new WordpieceTokenizer(inputDic);
  }

  public List<String> tokenize(String text) {
    List<String> splitTokens = new ArrayList<>();
    for (String token : basicTokenizer.tokenize(text)) {
      splitTokens.addAll(wordpieceTokenizer.tokenize(token));
    }
    return splitTokens;
  }

  public List<Integer> convertTokensToIds(List<String> tokens) {
    List<Integer> outputIds = new ArrayList<>();
    if (dic.containsKey(START)) {
      outputIds.add(dic.get(START));
    }
    for (String token : tokens) {
      outputIds.add(dic.get(token));
    }
    if (dic.containsKey(SEP)) {
      outputIds.add(dic.get(SEP));
    }
    return outputIds;
  }
}