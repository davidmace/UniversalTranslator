package davidmace.speechtranslator;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Locale;

/**
 * This is a simple project that uses Android's text to speech, language translation services, and
 * Android's speech to text to create a universal translator.
 */
public class Translator extends Activity implements OnInitListener {

    protected static final int RESULT_SPEECH = 1;
    private TextToSpeech myTTS;
    private ImageButton btnSpeak, btnSpeak2;
    private LinearLayout talkSpace;
    private Spinner lang1, lang2;
    TextView waitingForAnswer;
    boolean translationFinished = false;
    String translation = "";

    //text to speech options
    static final Locale[] locales = new Locale[]{
            Locale.ENGLISH,
            Locale.FRENCH,
            Locale.GERMAN,
            Locale.ITALIAN,
            new Locale("spa", "ESP")

    };

    //speech to text options
    String[] stt = new String[]{
            "en-EN",
            "fr-FR",
            "de-DE",
            "it-IT",
            "es-ES",
    };

    //language names for display for each language because some don't translate to others
    static final String[] enlangs = new String[]{
            "French",
            "German",
            "Italian",
            "Spanish"
    };
    static final String[] frlangs = new String[]{
            "English",
            "German",
    };
    static final String[] gelangs = new String[]{
            "English",
            "French",
            "Italian",
            "Spanish"
    };
    static final String[] itlangs = new String[]{
            "English",
            "German",
    };
    static final String[] splangs = new String[]{
            "English",
            "German",
    };
    static final String[] langs = new String[]{
            "English",
            "French",
            "German",
            "Italian",
            "Spanish"
    };
    static final String[] shortlangs = new String[]{
            "en",
            "fr",
            "de",
            "it",
            "es"
    };

    protected static final int MY_DATA_CHECK_CODE = 0;
    String s;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //check if tts and stt are downloaded and if not then start the activity to download them
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);

        //check if always_use_my_settings is disabled and otherwise send the user to disable it
        try {
            int tts_use_defaults = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.TTS_USE_DEFAULTS, 0);
            if (tts_use_defaults == 1) {
                Toast.makeText(getApplicationContext(), "Please turn off \"Always Use My Settings\" and press Back to return to the application!", Toast.LENGTH_LONG).show();
                startActivity(new Intent("com.android.settings.TTS_SETTINGS"));
            }
        } catch (Exception e) {
        }

        //make link work
        TextView t2 = (TextView) findViewById(R.id.acknowledgment);
        t2.setMovementMethod(LinkMovementMethod.getInstance());

        Toast.makeText(getApplicationContext(),
                "Click on a microphone or select a language to begin translating",
                Toast.LENGTH_LONG).show();

        talkSpace = (LinearLayout) findViewById(R.id.talkspace);

        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);
        btnSpeak2 = (ImageButton) findViewById(R.id.btnSpeak2);

        //create clickable acknowledgment at bottom of screen
        TextView acknowledgment = (TextView) findViewById(R.id.acknowledgment);
        String text = "<a href='http://translate.yandex.com/'> Powered By Yandex.Translate </a>";
        acknowledgment.setText(Html.fromHtml(text));

        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),
                        "Translate " + sels + " to " + sel2s,
                        Toast.LENGTH_SHORT).show();

                //create new speech to text intent
                Intent intent = new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, stt[sel]);

                //start the intent for Android's speech to text
                try {
                    startActivityForResult(intent, RESULT_SPEECH);
                } catch (ActivityNotFoundException a) {
                    Toast t = Toast.makeText(getApplicationContext(),
                            "Oops! Your device doesn't support Speech to Text",
                            Toast.LENGTH_SHORT);
                    t.show();
                }

            }
        });

        //right button goes in opposite translation direction
        btnSpeak2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),
                        "Translate " + sel2s + " to " + sels,
                        Toast.LENGTH_SHORT).show();

                //create new speech to text intent
                Intent intent = new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, stt[sel2]);

                //start the intent for Android's speech to text
                try {
                    startActivityForResult(intent, RESULT_SPEECH + 10);
                } catch (ActivityNotFoundException a) {
                    Toast t = Toast.makeText(getApplicationContext(),
                            "Opps! Your device doesn't support Speech to Text",
                            Toast.LENGTH_SHORT);
                    t.show();
                }
            }
        });

        myTTS = new TextToSpeech(this, this);

        lang1 = (Spinner) findViewById(R.id.lang1);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, langs);
        lang1.setAdapter(spinnerArrayAdapter);

        lang2 = (Spinner) findViewById(R.id.lang2);
        spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, langs);
        lang2.setAdapter(spinnerArrayAdapter);

        lang1.setOnItemSelectedListener(new MyOnItemSelectedListener());
        lang2.setOnItemSelectedListener(new My2OnItemSelectedListener());

        final ScrollView scrollView = (ScrollView) findViewById(R.id.scrollview);
        scrollView.post(new Runnable() {

            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

    }


    /**
     * Fairly straightforward controls for changing the selected languages and displaying the spinners
     */
    int sel = 0, sel2 = 0;
    String sels = "", sel2s = "";
    Context context = this;

    public class MyOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String selected = parent.getItemAtPosition(pos).toString();
            if (selected.equals("English")) {
                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, enlangs);
                lang2.setAdapter(spinnerArrayAdapter);
                sel = 0;
                sels = "English";
            } else if (selected.equals("French")) {
                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, frlangs);
                lang2.setAdapter(spinnerArrayAdapter);
                sel = 1;
                sels = "French";
            } else if (selected.equals("German")) {
                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, gelangs);
                lang2.setAdapter(spinnerArrayAdapter);
                sel = 2;
                sels = "German";
            } else if (selected.equals("Italian")) {
                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, itlangs);
                lang2.setAdapter(spinnerArrayAdapter);
                sel = 3;
                sels = "Italian";
            } else if (selected.equals("Spanish")) {
                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, splangs);
                lang2.setAdapter(spinnerArrayAdapter);
                sel = 4;
                sels = "Spanish";
            }
        }

        public void onNothingSelected(AdapterView parent) {
            // Do nothing.
        }
    }

    public class My2OnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            String selected = parent.getItemAtPosition(pos).toString();
            if (selected.equals("English")) {
                myTTS.setLanguage(locales[0]);
                sel2 = 0;
                sel2s = "English";
            } else if (selected.equals("French")) {
                myTTS.setLanguage(locales[1]);
                sel2 = 1;
                sel2s = "French";
            } else if (selected.equals("German")) {
                myTTS.setLanguage(locales[2]);
                sel2 = 2;
                sel2s = "German";
            } else if (selected.equals("Italian")) {
                myTTS.setLanguage(locales[3]);
                sel2 = 3;
                sel2s = "Italian";
            } else if (selected.equals("Spanish")) {
                myTTS.setLanguage(locales[4]);
                sel2 = 4;
                sel2s = "Spanish";
            }
        }

        public void onNothingSelected(AdapterView parent) {
            // Do nothing.
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public void onInit(int initStatus) {

    }


    /**
     * Continues the flow after the text is returned from the Android speech to text engine
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            //left side
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    //create spoken text
                    LinearLayout l = new LinearLayout(context);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    l.setLayoutParams(params);
                    l.setOrientation(LinearLayout.HORIZONTAL);
                    View view = new View(context);
                    view.setLayoutParams(new LinearLayout.LayoutParams(3, ViewGroup.LayoutParams.FILL_PARENT));
                    view.setBackgroundColor(Color.rgb(0, 0, 100));
                    view.setPadding(5, 0, 0, 0);
                    l.addView(view);
                    TextView tv = new TextView(context);
                    params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    tv.setLayoutParams(params);
                    tv.setPadding(10, 0, 10, 0);
                    tv.setText(text.get(0));
                    tv.setTextSize(18);
                    l.addView(tv);
                    talkSpace.addView(l, 0);
                    curtask = "left";

                    //create translated text
                    l = new LinearLayout(context);
                    params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    l.setLayoutParams(params);
                    l.setOrientation(LinearLayout.HORIZONTAL);
                    view = new View(context);
                    view.setLayoutParams(new LinearLayout.LayoutParams(3, ViewGroup.LayoutParams.FILL_PARENT));
                    view.setBackgroundColor(Color.rgb(0, 0, 100));
                    view.setPadding(10, 0, 10, 0);
                    l.addView(view);
                    waitingForAnswer = new TextView(context);
                    params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    waitingForAnswer.setLayoutParams(params);
                    waitingForAnswer.setText("");
                    waitingForAnswer.setPadding(10, 0, 10, 0);
                    waitingForAnswer.setTextSize(18);
                    waitingForAnswer.setTypeface(null, Typeface.ITALIC);
                    l.addView(waitingForAnswer);
                    talkSpace.addView(l, 0);
                    makeNewWaitingText();

                    translateWords(text.get(0));
                }


            }
            break;

            //right side
            case RESULT_SPEECH + 10: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    //make text for spoken
                    LinearLayout l = new LinearLayout(context);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    l.setLayoutParams(params);
                    l.setOrientation(LinearLayout.HORIZONTAL);
                    View v = new View(context);
                    v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    l.addView(v);
                    TextView tv = new TextView(context);
                    params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    tv.setLayoutParams(params);
                    tv.setText(text.get(0));
                    tv.setPadding(10, 0, 10, 0);
                    tv.setTextSize(18);
                    l.addView(tv);
                    View view = new View(context);
                    view.setLayoutParams(new LinearLayout.LayoutParams(3, ViewGroup.LayoutParams.FILL_PARENT));
                    view.setBackgroundColor(Color.rgb(0, 0, 100));
                    view.setPadding(10, 0, 10, 0);
                    l.addView(view);
                    talkSpace.addView(l, 0);
                    curtask = "right";

                    //make traslated text
                    l = new LinearLayout(context);
                    params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    l.setLayoutParams(params);
                    l.setOrientation(LinearLayout.HORIZONTAL);
                    v = new View(context);
                    v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    l.addView(v);
                    waitingForAnswer = new TextView(context);
                    params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    waitingForAnswer.setLayoutParams(params);
                    waitingForAnswer.setText("");
                    waitingForAnswer.setTextSize(18);
                    waitingForAnswer.setPadding(10, 0, 10, 0);
                    waitingForAnswer.setTypeface(null, Typeface.ITALIC);
                    l.addView(waitingForAnswer);
                    view = new View(context);
                    view.setLayoutParams(new LinearLayout.LayoutParams(3, ViewGroup.LayoutParams.FILL_PARENT));
                    view.setBackgroundColor(Color.rgb(0, 0, 100));
                    l.addView(view);
                    talkSpace.addView(l, 0);
                    makeNewWaitingText();

                    //translate using yandex online service
                    translateWords(text.get(0));
                }


            }
            break;

            //if user needs to install the text to speech data then this is called to begin the android flow
            case MY_DATA_CHECK_CODE: {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    myTTS = new TextToSpeech(this, this);
                } else {
                    Intent installTTSIntent = new Intent();
                    installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(installTTSIntent);
                }
            }

        }
    }

    /**
     * Create waiting ellipses in a new thread when we are waiting for the results
     */
    public void makeNewWaitingText() {
        //make the waiting ... text
        final Handler h = new Handler();

        h.postDelayed(new Runnable() {
            int debugmax = 100;

            public void run() {
                debugmax--;
                if (debugmax == 0)
                    return;
                String cur = waitingForAnswer.getText().toString();
                if (cur.equals("") || cur.equals(".") || cur.equals(".."))
                    waitingForAnswer.setText(cur + '.');
                else
                    waitingForAnswer.setText("");
                if (!translationFinished) {
                    h.postDelayed(this, 300);
                } else {
                    waitingForAnswer.setText(translation);
                    translationFinished = false;
                }
            }
        }, 300);
    }

    public void translateWords(String words) {
        new TranslateWordsThread().execute(words);
    }

    String curtask = "left";

    /**
     * Handles the call to yandex to get the correct translation of the text from of the speech and returns a string
     */
    class TranslateWordsThread extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... queries) {
            try {
                int index = 0;
                while ((index = queries[0].indexOf(' ', index)) >= 0)
                    queries[0] = queries[0].substring(0, index) + "+" + queries[0].substring(index + 1);
                String begin = curtask.equals("left") ? shortlangs[sel] : shortlangs[sel2];
                String end = curtask.equals("left") ? shortlangs[sel2] : shortlangs[sel];
                String url = "https://translate.yandex.net/api/v1.5/tr/translate?key=trnsl.1.1.20130528T200757Z.9dd85563bbc84aae.45ac0b76e6eabf8cee1c7c9db068110b5624f2fe&lang=" +
                        begin + "-" + end + "&text=" + queries[0];

                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response = httpclient.execute(new HttpGet(url));
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    String responseString = out.toString();
                    String text = responseString.substring(responseString.indexOf("<text>") + "<text>".length(), responseString.indexOf("</text>"));
                    //extract noncharacters
                    int in = 0;
                    while (true) {
                        in = text.indexOf('-', in);
                        if (in < 0)
                            break;
                        text = text.substring(0, in) + " " + (in + 1 < text.length() ? text.substring(in + 1) : "");
                    }
                    while (true) {
                        in = text.indexOf("&apos;", in);
                        if (in < 0)
                            break;
                        text = text.substring(0, in) + "'" + (in + 6 < text.length() ? text.substring(in + 6) : "");
                    }
                    for (int i = 0; i < text.length(); i++) {
                        if (text.charAt(i) == '?' || text.charAt(i) == '.' || text.charAt(i) == ',' || text.charAt(i) == '!' ||
                                text.charAt(i) == ':' || text.charAt(i) == ';') {
                            text = text.substring(0, in) + " " + (in + 1 < text.length() ? text.substring(in + 1) : "");
                        }
                    }
                    return text;
                } else {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    Toast.makeText(context, "Please Try Again", Toast.LENGTH_SHORT).show();
                    //throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (Exception e) {

            }
            return "";
        }

        protected void onPostExecute(String s) {
            speakWords(s);
        }
    }

    /**
     * Perform the text to speech of the returned string in the chosen translated language
     *
     * @param speech
     */
    public void speakWords(String speech) {
        //set translated text
        translationFinished = true;
        translation = speech;

        //speak words
        try {
            if (curtask.equals("left"))
                myTTS.setLanguage(locales[sel2]);
            else
                myTTS.setLanguage(locales[sel]);
            myTTS.speak(speech, TextToSpeech.QUEUE_FLUSH, null);

            TextView filler = new TextView(this);
            filler.setText("");
            filler.setTextSize(6);
            talkSpace.addView(filler, 0);
        } catch (Exception e) {
            Toast.makeText(context, "Please Try Again", Toast.LENGTH_SHORT).show();
        }
    }

}