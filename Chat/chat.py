import json
import numpy as np
from tensorflow import keras
from sklearn.preprocessing import LabelEncoder

import colorama
colorama.init()
from colorama import  Fore, Style, Back

import random
import pickle

with open("./data3.json") as file:
    data = json.load(file)

def chat():

    model = keras.models.load_model('chat_model')
    with open('tokenizer.pickle', 'rb') as handle:
        tokenizer = pickle.load(handle)

    with open('label_encoder.pickle', 'rb') as enc:
        lbl_encoder = pickle.load(enc)
    
    max_len = 20

    while True:
        print(Fore.LIGHTBLUE_EX + "USER: " + Style.RESET_ALL, end = "")
        inp = input()
        if inp.lower() == "quit":
            break

        result = model.predict(keras.preprocessing.sequence.pad_sequences(tokenizer.texts_to_sequences([inp]), truncating = 'post', maxlen = max_len))
        tag = lbl_encoder.inverse_transform([np.argmax(result)])

        for i in data['intents']:
            if i['intent'] == tag:
                print(Fore.GREEN + "ChatBot:" + Style.RESET_ALL, np.random.choice(i['responses']))

print(Fore.YELLOW + "Start Messaging" + Style.RESET_ALL)
chat()