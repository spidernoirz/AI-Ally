package com.example.aially;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int MAX_SPEECH_DURATION = 20000; // 20 seconds
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private boolean isSpeaking = false; // To track if speech is ongoing
    private ProgressBar progressBar;
    private TextView responseTextView; // Declare responseTextView at the class level
    private final Handler handler = new Handler();
    private final int SPEECH_REQUEST_CODE = 0;
    private final ActivityResultLauncher<Intent> pickContactLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    // Handle the contact data here
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        

        textToSpeech = new TextToSpeech(this, this);

        ImageButton speechInputButton = findViewById(R.id.speechInputButton);
        TextInputEditText queryEditText = findViewById(R.id.queryEditText);
        Button sendQueryButton = findViewById(R.id.sendPromptButton);
        responseTextView = findViewById(R.id.modelResponseTextView); // Initialize responseTextView
        progressBar = findViewById(R.id.sendPromptProgressBar); // Initialize progressBar

        ImageButton localButton = findViewById(R.id.button2);
        localButton.setOnClickListener(v -> toggleVoiceRecognition());

        // Check for microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            int RECORD_AUDIO_PERMISSION_REQUEST = 200;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_REQUEST);
        }

        speechInputButton.setOnClickListener(v -> {
            if (!isListening) {
                startSpeechRecognition();
            } else {
                stopSpeechRecognition();
            }
        });

        sendQueryButton.setOnClickListener(v -> {
            String query = queryEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                sendQuery(query);
            } else {
                Toast.makeText(this, "Please enter a query", Toast.LENGTH_SHORT).show();
            }
        });

        checkRecordAudioPermission();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Initialization failed", Toast.LENGTH_SHORT).show();
        }
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.getDefault());

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language data is missing or the language is not supported.
                // Log error or handle accordingly.
            }
        } else {
            // Initialization failed.
            // Log error or handle accordingly.
        }
    }

    private void startSpeechRecognition() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {}

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {}

                @Override
                public void onError(int error) {
                    isListening = false;
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String query = matches.get(0);
                        sendQuery(query);
                    }
                    isListening = false;
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        isListening = true;
        speechRecognizer.startListening(intent);
    }

    private void stopSpeechRecognition() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            speechRecognizer.destroy();
        }
        isListening = false;
    }

    private void checkRecordAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Record audio permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Record audio permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendQuery(String query) {
        progressBar.setVisibility(View.VISIBLE);
        responseTextView.setText("");

        GeminiPro model = new GeminiPro();
        model.getResponse(query, new ResponseCallback() {
            @Override
            public void onResponse(String response) {
                // Filter out "*" characters from the output
                response = response.replaceAll("\\*", "");

                responseTextView.setText(response);
                progressBar.setVisibility(View.GONE);
                speak(response);
            }

            @Override
            public void onError(Throwable throwable) {
                Toast.makeText(MainActivity.this, "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void speak(String text) {
        isSpeaking = true; // Set speaking flag to true
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        // Post a delayed task to stop speaking after 10 seconds
        handler.postDelayed(() -> {
            if (isSpeaking) {
                textToSpeech.stop();
                isSpeaking = false; // Reset speaking flag
            }
        }, MAX_SPEECH_DURATION);
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
    private void toggleVoiceRecognition() {
        startVoiceRecognition();
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            processVoiceInput(spokenText);
        }
    }

    private void processVoiceInput(String spokenText) {
        EditText queryEditText = findViewById(R.id.queryEditText);
        queryEditText.setText(spokenText);
        handleQuery(spokenText);
    }

    private void handleQuery(String query) {
        TextView modelResponseTextView = findViewById(R.id.modelResponseTextView);

        if (query.contains("weather")) {
            String weather = getWeather();
            modelResponseTextView.setText(weather);
            speak(weather);
        } else if (query.contains("time")) {
            String time = displayTime();
            modelResponseTextView.setText(time);
            speak(time);
        } else if (query.contains("date")) {
            String date = displayDate();
            modelResponseTextView.setText(date);
            speak(date);
        } else if (query.contains("alarm")) {
            setAlarm();
            modelResponseTextView.setText("Alarm set successfully.");
            speak("Alarm set successfully.");
        } else if (query.contains("call")) {
            callContact();
            modelResponseTextView.setText("Calling emergency services.");
            speak("Calling emergency services.");
        } else if (query.contains("joke")) {
            String joke = tellJoke();
            modelResponseTextView.setText(joke);
            speak(joke);

        } else if (query.contains("destruction")) {
            String destructline = destruct();
            modelResponseTextView.setText("ACTIVATED!");
            speak(destructline);
        }

        else if (query.contains("open")) {
            String appName = query.substring(query.indexOf("open") + 4).trim();
            openApp(appName);
        } else {
            modelResponseTextView.setText("I'm sorry, I didn't understand that.");
            speak("I'm sorry, I didn't understand that.");
        }
    }

    private String getWeather() {
        // Integrate Weather API and fetch weather data here
        // Placeholder code for demonstration
        return "The weather is sunny.";
    }

    private String displayTime() {
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        return "The current time is " + currentTime + ".";
    }

    private String displayDate() {
        String currentDate = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(new Date());
        return "Today's date is " + currentDate + ".";
    }

    private void setAlarm() {
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
        startActivity(intent);
    }

    private void callContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        pickContactLauncher.launch(intent);
    }

    private String tellJoke() {
        List<String> jokes = List.of(
                "Why don't scientists trust atoms? Because they make up everything!",
                "I'm reading a book on anti-gravity. It's impossible to put down!",
                "Parallel lines have so much in common. It’s a shame they’ll never meet.",
                "Why couldn't the bicycle stand up by itself? Because it was two-tired."
        );
        return jokes.get(new Random().nextInt(jokes.size()));
    }

    private String destruct() {
        List<String> destructline = List.of(
                "Self destruct mode activated. Destruction in T minus 5 seconds... 5... 4... 3... 2... 1... Cover your EARS! OH no 404 Error error",
                "Activating Destruction sequence command code 21. Range covered 5 meters. Encountering several casualties... Destruction in T minus 5 seconds... 5. 4. 3. 2. 1. Meow"
        );
        return destructline.get(new Random().nextInt(destructline.size()));
    }

    private void openApp(String appName) {
        PackageManager packageManager = getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(appName);
        if (intent != null) {
            startActivity(intent);
        } else {
            // If app is not found, try to find the package name from a predefined list
            String packageName = getAppPackageName(appName);
            if (packageName != null) {
                Intent appIntent = packageManager.getLaunchIntentForPackage(packageName);
                if (appIntent != null) {
                    startActivity(appIntent);
                } else {
                    speak("Sorry, I couldn't find the " + appName + " app on your device.");
                }
            } else {
                speak("Sorry, I couldn't find the " + appName + " app on your device.");
            }
        }
    }

    private String getAppPackageName(String appName) {
        // Define a map of common app names and their corresponding package names
        // Add more app names and package names as needed
        return switch (appName.toLowerCase(Locale.getDefault())) {
            case "gmail" -> "com.google.android.gm";
            case "whatsapp" -> "com.whatsapp";
            case "facebook" -> "com.facebook.katana";
            case "instagram" -> "com.instagram.android";
            case "twitter" -> "com.twitter.android";
            case "outlook" -> "com.microsoft.office.outlook";
            case "spotify" -> "com.spotify.music";
            case "youtube" -> "com.google.android.youtube";
            case "snapchat" -> "com.snapchat.android";
            case "linkedin" -> "com.linkedin.android";
            case "amazon" -> "com.amazon.mShop.android.shopping";
            case "netflix" -> "com.netflix.mediaclient";
            case "teams" -> "com.microsoft.teams";
            case "drive" -> "com.google.android.apps.docs";
            case "calendar" -> "com.google.android.calendar";
            case "dropbox" -> "com.dropbox.android";
            case "maps" -> "com.google.android.apps.maps";
            case "photos" -> "com.google.android.apps.photos";
            default -> null;
        };
    }

}
