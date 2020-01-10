package com.example.chatbotpsp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.chatbotpsp.API.ChatterBot;
import com.example.chatbotpsp.API.ChatterBotFactory;
import com.example.chatbotpsp.API.ChatterBotSession;
import com.example.chatbotpsp.API.ChatterBotType;
import com.example.chatbotpsp.API.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private Button btEnviar;
    private EditText etInput;
    private ImageView ivMic;
    private ChatterBot bot;
    private ChatterBotSession botSession;
    private RecyclerView rvMessages;
    private AdapterMultiType adapter;
    private String languageFrom = "es", languageTo = "en";
    private String cadNoTraducida = "";
    private String traduccion = "";
    private TextToSpeech voiceEsp;
    Locale locSpanish = new Locale("spa", "MEX");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initComponents();
        initEvents();
        initBot();
    }

    private void initComponents() {
        voiceEsp = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS){
                    int result = voiceEsp.setLanguage(locSpanish);
                    if(result ==TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("XYZ" , "Language Error");
                    }else{
                        Log.d("XYZ", "ALL GOOD");
                    }
                }else{
                    Log.e("XYZ" , "Language Error");
                }
            }
        });

        btEnviar = findViewById(R.id.btSend);
        etInput = findViewById(R.id.etInput);
        rvMessages = findViewById(R.id.recyclerView);
        ivMic = findViewById(R.id.ivMic);
        adapter = new AdapterMultiType(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    private void initEvents() {
        btEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cadNoTraducida = etInput.getText().toString();
                TraduccirAIngles translateTask = new TraduccirAIngles(cadNoTraducida);
                adapter.mensajes.add(new Mensaje(etInput.getText().toString(), true));
                adapter.notifyDataSetChanged();
                rvMessages.scrollToPosition(adapter.mensajes.size() - 1);
                etInput.setText("");
                languageFrom = "es";
                languageTo = "en";
                translateTask.execute();
            }
        });

        ivMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voiceEntry();
            }
        });
    }

    private void voiceEntry() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locSpanish);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hableme");
        try{
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        }catch (ActivityNotFoundException e){
            e.getMessage();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case REQ_CODE_SPEECH_INPUT:
                if(resultCode == RESULT_OK && data != null){
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    etInput.setText(result.get(0));
                }
            break;
        }
    }

    private void initBot() {
        ChatterBotFactory factory = new ChatterBotFactory();

        try {
            bot = factory.create(ChatterBotType.PANDORABOTS, "b0dafd24ee35a477");
        } catch (Exception e) {
            e.printStackTrace();
        }

        botSession = bot.createSession();
    }

    // POST
    // https://www.bing.com/ttranslatev3

    // HEADERS
    // HEADER NAME: Content-type / application/x-www-form-urlencoded
    // User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36

    // BODY
    // fromLang=es
    // text=Hola
    // to=en

    public void doTheChat(){
        new Chat().execute();
    }

    private void chat(String msg) {
        try {
            String response = botSession.think(msg);
            new TraduccirAEsp(response).execute();
        }catch(Exception e){
            Log.v("xyz", "Error: " + e.getMessage());
        }
    }

    private void showBotResponse(){
        adapter.mensajes.add(new Mensaje(traduccion, false));
        adapter.notifyDataSetChanged();
        voiceEsp.speak(traduccion, TextToSpeech.QUEUE_FLUSH, null);
        rvMessages.scrollToPosition(adapter.mensajes.size() - 1);
    }


    public String decomposeJson(String json){
        String translationResult = "Could not get";
        try {
            JSONArray arr = new JSONArray(json);
            JSONObject jObj = arr.getJSONObject(0);
            translationResult = jObj.getString("translations");
            JSONArray arr2 = new JSONArray(translationResult);
            JSONObject jObj2 = arr2.getJSONObject(0);
            translationResult = jObj2.getString("text");
        } catch (JSONException e) {
            translationResult = e.getLocalizedMessage();
        }
        return translationResult;
    }

    private class Chat extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            chat(traduccion);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            adapter.notifyDataSetChanged();
            rvMessages.scrollToPosition(adapter.mensajes.size() - 1);
        }
    }

    private class TraduccirAIngles extends AsyncTask<Void, Void, Void>{

        private final Map<String, String> headers;
        private final Map<String, String> vars;
        String s = "Error";

        private TraduccirAIngles(String message) {
            headers = new LinkedHashMap<String, String>();
            headers.put("Content-type","application/x-www-form-urlencoded");
            headers.put("User-Agent:","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");

            vars = new HashMap<String, String>();
            vars.put("fromLang", "es");
            vars.put("text",message);
            vars.put("to","en");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                s = Utils.performPostCall("https://www.bing.com/ttranslatev3", (HashMap) vars);
            } catch (Exception e) {
                e.printStackTrace();
                Log.v("xyz", "Error: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            traduccion = decomposeJson(s);
            doTheChat();
        }
    }

    private class TraduccirAEsp extends AsyncTask<Void, Void, Void>{

        private final Map<String, String> headers;
        private final Map<String, String> vars;
        String s = "Error";

        private TraduccirAEsp(String message) {
            headers = new LinkedHashMap<String, String>();
            headers.put("Content-type","application/x-www-form-urlencoded");
            headers.put("User-Agent:","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");

            vars = new HashMap<String, String>();
            vars.put("fromLang", "en");
            vars.put("text",message);
            vars.put("to","es");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                s = Utils.performPostCall("https://www.bing.com/ttranslatev3", (HashMap) vars);
            } catch (Exception e) {
                e.printStackTrace();
                Log.v("xyz", "Error: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            traduccion = decomposeJson(s);
            showBotResponse();
        }
    }

}

