package com.atakmap.android.helloworld.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.atakmap.android.helloworld.SquireDropDownReceiver;
import com.atakmap.android.helloworld.models.LZ;
import com.atakmap.android.helloworld.plugin.R;
import com.atakmap.android.helloworld.utils.RecognizerUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MicrophoneFragment extends DialogFragment implements RecognitionListener {
    public static final String MEDEVAC_SPEECH_INFO = "com.atakmap.android.helloworld.SQUIREMEDEVACSPEECH";
    public static final String ACTIVITY_INFO_BUNDLE = "com.atakmap.android.helloworld.ACTIVITYINFOBUNDLE";
    private static final String prefs_name_string = "squire_medevac";
    private static final String TAG = "KYLE";
    private static final int REQUEST_RECORD_PERMISSION = 100;

    private float mRms = Float.MIN_VALUE;

    SpeechRecognizer speech;
    Intent recognizerIntent;
    Intent returnIntent;

    Thread thread;
    CircleVisualizerView circleVisualizerView;
    float[] amplitudes = {0, 0};

    // Data based by intent on PTT trigger by plugin
    private String squireTitle;

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.i(TAG, "Ready for speech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(TAG, "beginning of speech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {

        mRms = rmsdB;
        amplitudes[0] = amplitudes[1];
        amplitudes[1] = mRms;

        if (amplitudes[0] < 0)
            amplitudes[0] = 0;

        if (amplitudes[1] < 0)
            amplitudes[1] = 0;
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(TAG, "buffer recv");
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(TAG, "End of speech.");

    }

    @Override
    public void onError(int error) {
        Log.i(TAG, "error: " + error);
        if (thread != null) {
            thread.interrupt();
        }

        if (speech != null) {
            Log.i(TAG, "Stopping listening.");

            speech.stopListening();
            speech.destroy();
            getFragmentManager().popBackStack();
        }
    }

    // Processes Mist / Nineline, etc with squire text analytics.
    // Argument is returned from NLP engine's best guess at spoken data.
    void squireProcess(String result) {
        if (result == null || result.length() == 0) return;

        String speechText = result.toUpperCase(Locale.ENGLISH)
                .replace(" IS ", " ")
                .replace(" ARE ", " ")
                .replace("1", "ONE")
                .replace("2", "TWO")
                .replace("3", "THREE")
                .replace("4", "FOUR")
                .replace("5", "FIVE")
                .replace("6", "SIZE")
                .replace("7", "SEVEN")
                .replace("8", "EIGHT")
                .replace("9", "NINE");

        speechText = speechText.replaceAll(" +", " ");
        String completeOriginal = speechText;
        Pair<String, String> bestChoice = null;

        if (squireTitle == null) {
            Log.d(TAG, "Title is null");
            return;
        }
        Log.d(TAG, "Title: \"" + squireTitle + "\"");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (squireTitle.equalsIgnoreCase(SquireDropDownReceiver.MIST_TITLE_STR)) {
                bestChoice = RecognizerUtil.findBestChoice(speechText, RecognizerUtil.MIST_CHOICES);
            } else if (squireTitle.equalsIgnoreCase(SquireDropDownReceiver.NINLINE_TITLE_STR)) {
                bestChoice = RecognizerUtil.findBestChoice(speechText, RecognizerUtil.NINELINE_CHOICES);
            } else if (squireTitle.equalsIgnoreCase(SquireDropDownReceiver.PATIENT_TITLE_STR)) {
                bestChoice = RecognizerUtil.findBestChoice(speechText, RecognizerUtil.PATIENT_CHOICES);
            } else if (squireTitle.equalsIgnoreCase(SquireDropDownReceiver.LZ_TITLE_STR)) {
                bestChoice = RecognizerUtil.findBestChoice(speechText, RecognizerUtil.LZ_CHOICES);
            }
        }

        if (bestChoice == null) {
            Log.e("KYLE", "Doing speech recognize on bad view: \"" + squireTitle + "\". I don't know how to handle this.");
            return;
        } else Log.d(TAG, "Speech rec for " + squireTitle);

        speechText = bestChoice.getLeft();
        if (speechText == null || speechText.length() == 0) {
            Log.d(TAG, "Bad speechtext: " + speechText);
            return;
        }
        speechText = speechText.toUpperCase();
        String spokenPhrase = bestChoice.getRight();
        int spokenIdx = completeOriginal.indexOf(spokenPhrase);
        String args = completeOriginal.substring(spokenIdx + spokenPhrase.length()); // original text - detected keyword(s)


        // Should I check the args for more matches or let it go as a loose "whatever was said"?
        Map<String, String[]> reCheckMap = new HashMap<>();
        reCheckMap.put(RecognizerUtil.NBC, RecognizerUtil.NBC_CHOICES);
        reCheckMap.put(RecognizerUtil.NATIONALITY, RecognizerUtil.NATIONALITY_CHOICES);
        reCheckMap.put(RecognizerUtil.RADIO_FREQUENCY, RecognizerUtil.RADIO_FREQUENCY_CHOICES);
        reCheckMap.put(RecognizerUtil.METHOD_OF_MARKING, RecognizerUtil.METHOD_OF_MARKING_CHOICES);
        reCheckMap.put(RecognizerUtil.SECURITY_AT_PICKUP_SITE, RecognizerUtil.SECURITY_AT_PICKUP_CHOICES);
        reCheckMap.put(RecognizerUtil.SPECIAL_EQUIPMENT_REQUIRED, RecognizerUtil.SPECIAL_EQUIPMENT_CHOICES);
        reCheckMap.put(RecognizerUtil.MECHANISM_OF_INJURY, RecognizerUtil.MECHANISM_OF_INJURY_CHOICES);
        reCheckMap.put(RecognizerUtil.INJURY, RecognizerUtil.INJURY_CHOICES);
        reCheckMap.put(RecognizerUtil.SIGNS_AND_SYMPTOMS, RecognizerUtil.SIGNS_AND_SYMPTOMS_CHOICES);
        reCheckMap.put(RecognizerUtil.TREATMENT, RecognizerUtil.TREATMENT_CHOICES);
        reCheckMap.put(RecognizerUtil.ZMIST, RecognizerUtil.ZMIST_CHOICES);
        reCheckMap.put(RecognizerUtil.PATIENT_STATUS, RecognizerUtil.PATIENT_STATUS_CHOICES);
        reCheckMap.put(RecognizerUtil.PATIENT_PRIORITY, RecognizerUtil.PATIENT_PRIORITY_CHOICES);

        // Location is used by different view but I only want to parse more if it's a nineline location
        if (squireTitle.equalsIgnoreCase(SquireDropDownReceiver.NINLINE_TITLE_STR)) {
            Gson gson = new Gson();
            Set<String> locationChoices = new HashSet<>();
            SharedPreferences medevacPrefs = getActivity().getSharedPreferences(prefs_name_string, Context.MODE_PRIVATE);
            String currentLzJson = medevacPrefs.getString("currentLZ", null);
            String lzMapJson = medevacPrefs.getString("lzMap", null);

            locationChoices.add("CURRENT LOCATION");
            if (currentLzJson != null) {
                LZ currentLz = gson.fromJson(currentLzJson, LZ.class);
                if (currentLz.getName() != null && currentLz.getName().length() > 0) {
                    locationChoices.add(currentLz.getName());
                }
            }


            // TODO remove; Example LZs for demo; KYLE
            locationChoices.add("CORONADO");
            locationChoices.add("SOCCER FIELD");


            Type lzMapToken = new TypeToken<HashMap<String, LZ>>(){}.getType();
            if (lzMapJson != null) {
                Map<String, LZ> lzMap = gson.fromJson(lzMapJson, lzMapToken);
                for (String key : lzMap.keySet()) {
                    if (key.length() > 0) locationChoices.add(key);
                }
            }

            reCheckMap.put(RecognizerUtil.LOCATION, locationChoices.toArray(new String[1]));
            Log.d(TAG, "Location choices: ");
            String[] locs = reCheckMap.get(RecognizerUtil.LOCATION);
            for (String loc : locs) {
                Log.d(TAG, "\t" + loc);
            }
        }

        int lastArgsLength = 0;
        String argsToKeep = "";
        boolean keepAllArgs = true; // Keep all args if key isn't in reCheckMap
        for (String key : reCheckMap.keySet()) {
            if (!speechText.equalsIgnoreCase(key)) continue;
            keepAllArgs = false;

            do {
                lastArgsLength = args.length();
                Log.d(TAG, "Re-checking Args. Prior: " + args);
                String[] choices = reCheckMap.get(key);
                Pair<String, String> match = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    match = RecognizerUtil.findBestChoice(args, choices);
                }
                String newArgs = match.getLeft();
                String originalArg = match.getRight();
                Log.d(TAG, "arg: " + newArgs + ", orig: " + originalArg);

                if (newArgs != null && newArgs.length() > 0) {
                    // Preserve the match we just found, and keep looping without it.
                    argsToKeep += newArgs + " ";
                    int idx = args.indexOf(originalArg);
                    args = args.substring(0, idx) + args.substring(idx + originalArg.length());
                }
            // Loop as long as we still have args and keep finding matches
            } while (args.length() > 0 && args.length() != lastArgsLength);
            break;
        }

        if (keepAllArgs) argsToKeep = args;

        Log.d(TAG, "Best Choice: \"" + speechText + "\"");
        Log.d(TAG, "Args: \"" + argsToKeep + "\"");

        returnIntent.putExtra(ACTIVITY_INFO_BUNDLE, new Bundle());
        returnIntent.putExtra("best_choice", speechText);
        returnIntent.putExtra("args", argsToKeep);
    }


    @Override
    public void onResults(Bundle results) {
        Log.i(TAG, "results");
        List<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if (matches.size() > 0) {
            String r = matches.get(0);//.toLowerCase();
            Log.i(TAG, "Text: \"" + r + "\"");
            circleVisualizerView.finalizeText(r);

            try {
                Thread.sleep(600);
                if (thread != null) {
                    thread.interrupt();
                }

                Log.i(TAG, "Confirmed words (setText): " + r);
                squireProcess(r);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    getContext().sendBroadcast(returnIntent);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        List<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && matches.size() > 0) {
            String r = matches.get(0).toLowerCase();
            circleVisualizerView.setText(r);
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.i(TAG, "on Event");
    }

    public static class CircleVisualizerView extends View {

        public MicrophoneFragment frag;

        // Black text
        String confirmedWords = "";
        // Gray text
        String suspectedWords = "";

        TextView words;

        public CircleVisualizerView(Context context) {
            super(context);
        }

        public CircleVisualizerView(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        public void finalizeText(String msg) {
            confirmedWords = msg;
            suspectedWords = "";
        }

        public void setText(String msg) {
            String[] oldWords = suspectedWords.split(" ");
            String[] newWords = msg.split(" ");

            confirmedWords = "";

            int i;
            for (i = 0; i < oldWords.length; i++) {
                if (i >= newWords.length)
                    break;

                if (oldWords[i].equalsIgnoreCase(newWords[i])) {
                    confirmedWords += oldWords[i] + " ";

                } else {
                    break;
                }
            }

            suspectedWords = "";
            for (int j = i; j < newWords.length; j++) {
                suspectedWords += newWords[j] + " ";
            }
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            Paint pc = new Paint();
            pc.setColor(getResources().getColor(R.color.squire_transparency));

            canvas.drawCircle(dp2px(getResources(), 151), dp2px(getResources(), 100 + 57), 15 * frag.mRms + 100, pc);

            if (confirmedWords != null && confirmedWords.length() > 0) {
                Spannable confirmedSpanned = new SpannableString(confirmedWords);
                confirmedSpanned.setSpan(new ForegroundColorSpan(Color.WHITE),
                        0,
                        confirmedWords.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                words.setText(confirmedSpanned);

                if (suspectedWords != null && suspectedWords.length() > 0) {
                    Spannable tempWords = new SpannableString(suspectedWords);
                    tempWords.setSpan(new ForegroundColorSpan(Color.LTGRAY),
                            0,
                            suspectedWords.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    words.append(tempWords);
                }
            }
        }

        public void init() {
            words = getRootView().findViewById(R.id.squire_spoken_text);
            String str = "Say Something...";
            Spannable tmp = new SpannableString(str);
            tmp.setSpan(new ForegroundColorSpan(Color.LTGRAY), 0, str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            words.setText(tmp);
        }
    }


    public MicrophoneFragment() {
        // Required empty public constructor
    }

    public static MicrophoneFragment newInstance() {
        return new MicrophoneFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedSettingsPreferences = getActivity().getSharedPreferences(prefs_name_string, MODE_PRIVATE);

        Log.i(TAG, "Registering mic fragment");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            speech = SpeechRecognizer.createSpeechRecognizer(getContext());
        }
        speech.setRecognitionListener(this);
        returnIntent = new Intent(MEDEVAC_SPEECH_INFO);
        boolean offlineFlag = sharedSettingsPreferences.getBoolean("offline",  true);

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        if (offlineFlag) {
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        }

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1); // Debug

        Log.i(TAG, "Starting listening mic fragment");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Not allowed, trying to request permission");

                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_RECORD_PERMISSION);

            } else {
                Log.i(TAG, "Allowed, should be good.");
                speech.startListening(recognizerIntent);
            }
        }
    }

    @Override
    public void setArguments(Bundle args) {
        squireTitle = args.getString("title");
        super.setArguments(args);
    }

    @Override
    public void onRequestPermissionsResult(int resultCode, @NonNull String[] permissions, @NonNull  int[] grantResults) {
        super.onRequestPermissionsResult(resultCode, permissions, grantResults);
        if (resultCode == REQUEST_RECORD_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                speech.startListening(recognizerIntent);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Toast.makeText(getContext(), "Permission denied. Cannot listen.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onStop() {
        Log.i(TAG, "Got to onStop mic fragment");
        super.onStop();
        thread.interrupt();

        if (speech != null) {
            Log.i(TAG, "Stopping listening.");

            speech.stopListening();
            speech.destroy();
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Got to onDestroy mic fragment");
        super.onDestroy();
        //thread.interrupt();

        //getFragmentManager().popBackStack();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "Got to onStart for mic fragment");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "Got to onResume for mic fragment");
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "Paused mic fragment");
        //thread.interrupt();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(TAG, "on create view for mic fragment");
        View view = SquireDropDownReceiver.micFragView;
        Window window = getDialog().getWindow();

        this.circleVisualizerView = view.findViewById(R.id.mic_main_box);
        this.circleVisualizerView.setWillNotDraw(false);

        this.circleVisualizerView.frag = this;

        this.circleVisualizerView.init();
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }

        thread = new Thread(() -> {
            while (!Thread.interrupted()) {
                circleVisualizerView.invalidate();
            }
        });
        thread.start();

        // Should never be null but Android Studio was complaining, so rather safe than sorry
        // Maybe if the fragment gets instantiated as a normal fragment not a dialog?
        if (window != null) {
            window.requestFeature(Window.FEATURE_NO_TITLE);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            setStyle(STYLE_NO_FRAME, android.R.style.Theme_Holo_Dialog_NoActionBar);
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        Log.i(TAG, "Got to onAttach mic fragment");
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        Log.i(TAG, "Got to onDetach mic fragment");
        if (thread != null) {
            Log.i(TAG, "Stopping visualizer thread");
            thread.interrupt();
        }
        super.onDetach();
    }

    public static int dp2px(Resources resource, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,   dp,resource.getDisplayMetrics());
    }


    public static class SpeechDataListener extends BroadcastReceiver {
        private boolean registered = false;
        private SpeechDataReceiver sdra = null;
        private MicrophoneFragment microphoneFragment;

        synchronized public void register(Context context, SpeechDataReceiver sdra, MicrophoneFragment micFrag) {
            if (!registered) {
                context.registerReceiver(this, new IntentFilter(MEDEVAC_SPEECH_INFO));
            }

            this.sdra = sdra;
            this.microphoneFragment = micFrag;
            registered = true;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "In onReceive in SpeechDataListener");
            synchronized (this) {
                try {
                    Bundle activityInfoBundle = intent.getBundleExtra(ACTIVITY_INFO_BUNDLE);
                    Log.d(TAG, "AIB null?" + (activityInfoBundle == null) + ", sdra null? " + (sdra == null));

                    if (activityInfoBundle != null && sdra != null) {
                        activityInfoBundle.putString("best_choice", intent.getStringExtra("best_choice"));
                        activityInfoBundle.putString("args", intent.getStringExtra("args"));
                        sdra.onSpeechDataReceived(activityInfoBundle);
                    }

                    Log.d(TAG, "Closing dialog");
                    microphoneFragment.dismiss();

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                if (registered) {
                    context.unregisterReceiver(this);
                    registered = false;
                }
            }
        }
    }

    public interface SpeechDataReceiver {
        void onSpeechDataReceived(Bundle activityInfoBundle);
    }
}
