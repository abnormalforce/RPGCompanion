package abnormalforce.com.rpgcompanion;
//
//import android.support.v4.app.Fragment;
//import android.support.v7.app.AppCompatActivity;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//
//public class MainActivity extends SingleFragmentActivity {
//
//    @Override
//    protected Fragment createFragment() {
//        return new TagReaderFragment();
//    }
//}


        import android.app.Activity;
        import android.app.PendingIntent;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.content.IntentFilter.MalformedMimeTypeException;
        import android.content.res.AssetFileDescriptor;
        import android.content.res.AssetManager;
        import android.media.AudioManager;
        import android.media.SoundPool;
        import android.nfc.NdefMessage;
        import android.nfc.NdefRecord;
        import android.nfc.NfcAdapter;
        import android.nfc.Tag;
        import android.nfc.tech.Ndef;
        import android.os.AsyncTask;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.widget.TextView;
        import android.widget.Toast;

        import java.io.IOException;
        import java.io.UnsupportedEncodingException;
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;
        import java.util.Random;

/**
 * Activity for reading data from an NDEF Tag.
 *
 * @author Ralf Wondratschek
 *
 */
public class MainActivity extends Activity {

    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NfcDemo";
    private static final String SOUNDS_FOLDER = "rpg_sounds";
    private static final int MAX_SOUNDS = 1;
    private static final int DIFFICULTY_FACTOR = 20;
    private static final int STARTING_DIFFICULTY = 0;

    private AssetManager mAssets;
    private TextView mTextView;
    private NfcAdapter mNfcAdapter;
    private SoundPool mSoundPool;

    private List<String> players = new ArrayList<>();
    private Map<String, Integer> mSoundIds = new HashMap<>();
    private Map<String, String> playerMap = new HashMap<>();
    private Map<String, Integer> playerStats = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        populatePlayerList();

        mTextView = (TextView) findViewById(R.id.sample_txt);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;

        }

        if (!mNfcAdapter.isEnabled()) {
            mTextView.setText("NFC is disabled.");
        } else {
            mTextView.setText(R.string.app_name);
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAssets = getApplicationContext().getAssets();
        mSoundPool = new SoundPool(MAX_SOUNDS, AudioManager.STREAM_MUSIC, 0);
        populatePlayerList();
        loadSounds();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */
        setupForegroundDispatch(this, mNfcAdapter);
    }

    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        }
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding {BaseActivity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }


    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     * @author Ralf Wondratschek
     *
     */
    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

            byte[] payload = record.getPayload();

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            String result = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
            Log.d(TAG, "Result: " + result);
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                mTextView.setText("Read content: " + result);

                Random r = new Random();

                Log.d(TAG, "Text: " + result);
                Log.d(TAG, "Roll: " + r.nextInt(20));

                parseTag(result);
            }
        }
    }

    private void parseTag(String tagContents) {

        if(playerMap.keySet().contains(tagContents)) {
            Log.d(TAG, "Found player: " + tagContents + " with Sound " + playerMap.get(tagContents).toString());
            Random roll = new Random();

            int currDifficulty = DIFFICULTY_FACTOR - playerStats.get(tagContents);

            // roll d20
            int rollInt = roll.nextInt(20);
            boolean success = (rollInt <= currDifficulty);
//
//            Map<String, Sound> currSounds = playerMap.get(tagContents);

            if(success) {
                String currTag = playerMap.get(tagContents).concat("_success");
                mTextView.setText("Curr tag: " + currTag);
                Log.d(TAG, "Sound id " + mSoundIds.get(currTag) + " for player " + currTag);
                mSoundPool.play(mSoundIds.get(currTag), 1.0f, 1.0f, 1, 0, 1.0f);
            } else {
                String currTag = playerMap.get(tagContents).concat("_failure");
                mTextView.setText("Curr tag: " + currTag);
                Log.d(TAG, "Sound id " + mSoundIds.get(currTag) + " for player " + currTag);
                mSoundPool.play(mSoundIds.get(currTag), 1.0f, 1.0f, 1, 0, 1.0f);
            }

            //Game gets harder
//            playerStats.put(tagContents, rollInt + 1);
            /**
             * Ideas:
             * Every time a player tags in, increase likelihood of computer freakout.  First X are safe.
             * Maybe have master chip that increases "static" a la lacuna?
             *
             * Also, ominous countdown.  Maybe with and without dire consequences?
             * ALso lasers
             */
//            mSoundPool.play(soundId.nextInt(5)+1 , 1.0f, 1.0f, 1, 0, 1.0f);
//            mSoundPool.play(playerMap.get(tagContents).getSoundId(), 1.0f, 1.0f, 1, 0, 1.0f);
        } else if (tagContents.equals("ambient")) {
            for(String s : playerStats.keySet()) {
                playerStats.put(s, playerStats.get(s) + 1);
            }

            Random r = new Random();
            int ambientId = r.nextInt(5)+1;
            String currTag = tagContents.concat(ambientId+"");
            mSoundPool.play(mSoundIds.get(currTag), 1.0f, 1.0f, 1, 0, 1.0f);
        } else if (tagContents.equals("countdown")) {
            mSoundPool.play(mSoundIds.get(tagContents), 1.0f, 1.0f, 1, 0, 1.0f);
        } else if (tagContents.equals("laser_gun")) {
            mSoundPool.play(mSoundIds.get(tagContents), 1.0f, 1.0f, 1, 0, 1.0f);
        } else if (tagContents.equals("laser_attack")) {
            mSoundPool.play(mSoundIds.get(tagContents), 1.0f, 1.0f, 1, 0, 1.0f);
        }
        else {
            Log.d(TAG, "Unrecognized tag detected");
        }
    }

    private void populatePlayerList() {
//        Sound leaderSuccess = new Sound(SOUNDS_FOLDER + "/leader_success.wav");
//        Sound leaderFailure = new Sound(SOUNDS_FOLDER + "/leader_failure.wav");
//        leaderSuccess.setSoundId(1);
//        leaderFailure.setSoundId(2);
//        HashMap<String, Sound> leaderSounds = new HashMap<>();
//        sounds.put("leadersuccess", leaderSuccess);
//        sounds.put("leaderfailure", leaderFailure);
        playerMap.put("Team Leader", "leader");
        playerStats.put("Team Leader", STARTING_DIFFICULTY);

//        Sound commSuccess = new Sound(SOUNDS_FOLDER + "/comm_success.wav");
//        Sound commFailure =  new Sound(SOUNDS_FOLDER + "/comm_failure.wav");
//        commSuccess.setSoundId(3);
//        commFailure.setSoundId(4);
//        HashMap<String, Sound> commSounds = new HashMap<>();
//        sounds.put("commsuccess", commSuccess);
//        sounds.put("commfailure", commFailure);
        playerMap.put("Communications Officer", "comm");
        playerStats.put("Communications Officer", STARTING_DIFFICULTY);

//        Sound loyaltySuccess = new Sound(SOUNDS_FOLDER + "/loyal_success.wav");
//        Sound loyaltyFailure =  new Sound(SOUNDS_FOLDER + "/loyal_failure.wav");
//        loyaltySuccess.setSoundId(5);
//        loyaltyFailure.setSoundId(6);
//        HashMap<String, Sound> loyaltySounds = new HashMap<>();
//        sounds.put("loyalsuccess", loyaltySuccess);
//        sounds.put("loyalfailure", loyaltyFailure);
        playerMap.put("Loyalty Officer", "loyal");
        playerStats.put("Loyalty Officer", STARTING_DIFFICULTY);

//        Sound equipSuccess = new Sound(SOUNDS_FOLDER + "/equip_success.wav");
//        Sound equipFailure =  new Sound(SOUNDS_FOLDER + "/equip_failure.wav");
//        equipSuccess.setSoundId(7);
//        equipFailure.setSoundId(8);
//        HashMap<String, Sound> equipSounds = new HashMap<>();
//        equipSounds.put("success", equipSuccess);
//        equipSounds.put("failure", equipFailure);
//        playerMap.put("Equipment Officer", equipSounds);
        playerMap.put("Equipment Officer", "equip");
        playerStats.put("Equipment Officer", STARTING_DIFFICULTY);

//        Sound hygieneSuccess = new Sound(SOUNDS_FOLDER + "/hygiene_success.wav");
//        Sound hygieneFailure =  new Sound(SOUNDS_FOLDER + "/hygiene_failure.wav");
//        hygieneSuccess.setSoundId(9);
//        hygieneFailure.setSoundId(10);
//        HashMap<String, Sound> hygieneSounds = new HashMap<>();
//        hygieneSounds.put("success", hygieneSuccess);
//        hygieneSounds.put("failure", hygieneFailure);
//        playerMap.put("Hygiene Officer", hygieneSounds);
        playerMap.put("Hygiene Officer", "hygiene");
        playerStats.put("Hygiene Officer", STARTING_DIFFICULTY);

//        Sound happySuccess = new Sound(SOUNDS_FOLDER + "/happy_success.wav");
//        Sound happyFailure =  new Sound(SOUNDS_FOLDER + "/happy_failure.wav");
//        happySuccess.setSoundId(11);
//        happyFailure.setSoundId(12);play
//        HashMap<String, Sound> happySounds = new HashMap<>();
//        happySounds.put("success", happySuccess);
//        happySounds.put("failure", happyFailure);
//        playerMap.put("Happiness Officer", happySounds);
        playerMap.put("Happiness Officer", "happy");
        playerStats.put("Happiness Officer", STARTING_DIFFICULTY);
    }


    private void loadSounds() {

        String[] soundNames;
        try {
            soundNames = mAssets.list(SOUNDS_FOLDER);
            Log.i(TAG, "Found " + soundNames.length + " sounds");
        } catch (IOException ioe) {
            Log.e(TAG, "Could not list assets", ioe);
            return;
        }

//        mSounds = new ArrayList<Sound>();
        for (String filename : soundNames) {
            try {
                String assetPath = SOUNDS_FOLDER + "/" + filename;
                Sound sound = new Sound(assetPath);
                load(sound);
//                mSounds.add(sound);
            } catch (IOException ioe) {
                Log.e(TAG, "Could not load sound " + filename, ioe);
            }
        }
    }

    private void load(Sound sound) throws IOException {
        AssetFileDescriptor afd = mAssets.openFd(sound.getAssetPath());
        int soundId = mSoundPool.load(afd, 1);
        String soundName = sound.getName();
        sound.setSoundId(soundId);
        mSoundIds.put(soundName, soundId);
    }
}