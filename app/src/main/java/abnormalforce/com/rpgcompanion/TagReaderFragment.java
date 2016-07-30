//package abnormalforce.com.rpgcompanion;
//
//import android.app.Activity;
//import android.app.PendingIntent;
//import android.content.Intent;
//import android.nfc.NfcAdapter;
//import android.os.Bundle;
//import android.support.v4.app.Fragment;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//import android.widget.Toast;
//
///**
// * Created by Icarus on 7/25/2016.
// */
//public class TagReaderFragment extends Fragment {
//
//    private static final String TAG = "TagReaderFragment";
//
//    public static TagReaderFragment newInstance() {
//        return new TagReaderFragment();
//    }
//
//    private TextView tv;
//    private NfcAdapter mNfcAdapter;
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        mNfcAdapter = NfcAdapter.getDefaultAdapter(getContext());
//
//        if(mNfcAdapter == null) {
//            Toast.makeText(getContext(), "This device does not support NFC", Toast.LENGTH_LONG).show();
//            return;
//        } else {
//            Toast.makeText(getContext(), "NFC supported on this device", Toast.LENGTH_LONG).show();
//        }
//
//        if(!mNfcAdapter.isEnabled()) {
//            Toast.makeText(getContext(), "NFC is currently disabled", Toast.LENGTH_LONG).show();
//        }
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//
//        /**
//         * It's important, that the activity is in the foreground (resumed). Otherwise
//         * an IllegalStateException is thrown.
//         */
//        setupForegroundDispatch(getActivity(), mNfcAdapter);
//    }
//
//    @Override
//    public void onPause() {
//        /**
//         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
//         */
//        stopForegroundDispatch(getActivity(), mNfcAdapter);
//
//        super.onPause();
//    }
//
//    @Override
//    public void onNewIntent(Intent intent) {
//        /**
//         * This method gets called, when a new Intent gets associated with the current activity instance.
//         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
//         * at the documentation.
//         *
//         * In our case this method gets called, when the user attaches a Tag to the device.
//         */
//        handleIntent(intent);
//    }
//
//    private void handleIntent(Intent intent) {
//        // TODO: handle Intent
//    }
//
//    /**
//     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
//     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
//     */
//    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
//        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
//        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//
//        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
//
//        IntentFilter[] filters = new IntentFilter[1];
//        String[][] techList = new String[][]{};
//
//        // Notice that this is the same filter as in our manifest.
//        filters[0] = new IntentFilter();
//        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
//        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
//        try {
//            filters[0].addDataType(MIME_TEXT_PLAIN);
//        } catch (MalformedMimeTypeException e) {
//            throw new RuntimeException("Check your mime type.");
//        }
//
//        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
//    }
//
//    /**
//     * @param activity The corresponding {@link BaseActivity} requesting to stop the foreground dispatch.
//     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
//     */
//    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
//        adapter.disableForegroundDispatch(activity);
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View v = (View) inflater.inflate(R.layout.activity_main, container, false);
//
//        Log.d(TAG, "reached onCreateView");
//
//        tv = (TextView) v.findViewById(R.id.sample_txt);
//        tv.setText("IF YOU SEE THIS THEN THE FRAGMENT IS WORKING");
//
//
//        return v;
//    }
//}
