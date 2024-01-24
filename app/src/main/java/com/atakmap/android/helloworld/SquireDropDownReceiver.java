
package com.atakmap.android.helloworld;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.chat.ChatManagerMapComponent;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.IndividualContact;
import com.atakmap.android.contact.IpConnector;
import com.atakmap.android.contact.PluginConnector;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.cot.detail.SensorDetailHandler;
import com.atakmap.android.cot.importer.CotEventTypeImporter;
import com.atakmap.android.cot.importer.CotImporterManager;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.helloworld.db.ReportsRepository;
import com.atakmap.android.helloworld.fragment.LZFragment;
import com.atakmap.android.helloworld.fragment.MISTFragment;
import com.atakmap.android.helloworld.fragment.MicrophoneFragment;
import com.atakmap.android.helloworld.fragment.NineLineFragment;
import com.atakmap.android.helloworld.fragment.PatientsFragment;
import com.atakmap.android.helloworld.fragment.PolarDeviceFragment;
import com.atakmap.android.helloworld.fragment.ReportFragment;
import com.atakmap.android.helloworld.models.HeartRate;
import com.atakmap.android.helloworld.models.LZ;
import com.atakmap.android.helloworld.models.MIST;
import com.atakmap.android.helloworld.models.NineLine;
import com.atakmap.android.helloworld.models.Patient;
import com.atakmap.android.helloworld.models.Report;
import com.atakmap.android.helloworld.plugin.R;
import com.atakmap.android.helloworld.recyclerview.RecyclerViewDropDown;
import com.atakmap.android.helloworld.samplelayer.ExampleLayer;
import com.atakmap.android.helloworld.samplelayer.ExampleMultiLayer;
import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapActivity;
import com.atakmap.android.maps.MapComponent;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MapView.RenderStack;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.SensorFOV;
import com.atakmap.android.menu.PluginMenuParser;
import com.atakmap.android.routes.Route;
import com.atakmap.android.routes.RouteMapComponent;
import com.atakmap.android.routes.RouteMapReceiver;
import com.atakmap.android.toolbar.widgets.TextContainer;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.CotDispatcher;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.comms.CotStreamListener;
import com.atakmap.comms.app.CotPortListActivity;
import com.atakmap.comms.app.CotPortListActivity.CotPort;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;


/**
 * The DropDown Receiver should define the visual experience
 * that a user might have while using this plugin.   At a
 * basic level, the dropdown can be a view of your own design
 * that is inflated.   Please be wary of the type of context
 * you use.   As noted in the Map Component, there are two
 * contexts - the plugin context and the atak context.
 * When using the plugin context - you cannot build thing or
 * post things to the ui thread.   You use the plugin context
 * to lookup resources contained specifically in the plugin.
 */
public class SquireDropDownReceiver extends DropDownReceiver implements
        OnStateListener, SensorEventListener {

    //private final NotificationManager nm;

    public static final String TAG = "HelloWorldDropDownReceiver";

    public static final String SHOW_HELLO_WORLD = "com.atakmap.android.helloworld.SHOW_HELLO_WORLD";
    public static final String CHAT_HELLO_WORLD = "com.atakmap.android.helloworld.CHAT_HELLO_WORLD";
    public static final String SEND_HELLO_WORLD = "com.atakmap.android.helloworld.SEND_HELLO_WORLD";
    public static final String LAYER_DELETE = "com.atakmap.android.helloworld.LAYER_DELETE";
    public static final String LAYER_VISIBILITY = "com.atakmap.android.helloworld.LAYER_VISIBILITY";
    public static final String NINLINE_TITLE_STR = "Nine Line Report";
    public static final String MIST_TITLE_STR = "MIST Report";
    public static final String PATIENT_TITLE_STR = "Patients";
    public static final String LZ_TITLE_STR = "LZ";

    public static View helloView;
    public static View patientsFragView;
    public static View nineLineFragView;
    public static View mistFragView;
    public static View micFragView;
    public static View lzFragView;
    public static View deviceAdapterView;
    public static View polarDeviceFragView;
    public static View reportFragView;
    public static PolarDeviceFragment polarDialog;
    public static ReportFragment reportDialog;

    public static Callable testMe = null;
    public static Callable openReportDialog = null;
    public static Callable submitReportsToAtak = null;

    private final Context pluginContext;
    private final Contact squireMedevacContact;
    private final SquireMapOverlay mapOverlay;
    private final SquireMapComponent mapComponent;
    private final RecyclerViewDropDown recyclerView;
    private final TabViewDropDown tabView;

    // Not including prefixes like "Medevac - "
    private String plaintextTitle;

    private Route r;
    private FrameLayout fragmentContainer;
    private RelativeLayout micOverlayContainer;
    private ImageView medevacPttButton;
    // Static to allow speech recognizer classes to make decisions based on current title
    private static TextView medevacTitleTextView;
    private static ScrollView scrollView;

    private PolarDeviceFragment polarDeviceFragment;
    private NineLineFragment nineLineFragment;
    private static PatientsFragment patientsFragment;
    private LZFragment lzFragment;
    private MISTFragment mistFragment;

    private List<CotEvent> cotSendQueue;
    private ReentrantLock cotSendLock;
    private Thread reportSenderThread;

    public static Button ninelineSelectButton;
    public static Button mistSelectButton;
    public static Button patientSelectButton;
    public static Button lzSelectButton;
    public static Button reportSelectButton;

    private static int ATAK_ACTION_BAR_HEIGHT;
    private double currWidth = HALF_WIDTH;
    private double currHeight = HALF_HEIGHT;

    private final Object reportedUidsLock = new Object();

    private final MicrophoneFragment.SpeechDataListener sd1a = new MicrophoneFragment.SpeechDataListener();
    private final MicrophoneFragment.SpeechDataReceiver sdra = new MicrophoneFragment.SpeechDataReceiver() {
        public void onSpeechDataReceived(Bundle activityInfoBundle) {
            Log.d(TAG, "in onSpeechDataReceived");
            String bestChoice = activityInfoBundle.getString("best_choice");
            String args = activityInfoBundle.getString("args");
            if (args != null) args = args.trim();

            if (bestChoice == null) {
                Log.e(TAG, "Cannot handle speech. Recognizer could not determine a best choice.");
                return;
            }

            Log.d(TAG, "best_choice: " + bestChoice);
            Log.d(TAG, "args: " + args);

            try {
                if (plaintextTitle.equals(MIST_TITLE_STR)) {
                    mistFragment.handleSpeech(bestChoice, args);
                } else if (plaintextTitle.equals(NINLINE_TITLE_STR)) {
                    nineLineFragment.handleSpeech(bestChoice, args);
                } else if (plaintextTitle.equals(PATIENT_TITLE_STR)) {
                    patientsFragment.handleSpeech(bestChoice, args);
                } else if (plaintextTitle.equalsIgnoreCase(LZ_TITLE_STR)) {
                    lzFragment.handleSpeech(bestChoice, args);
                }
            } catch (Exception e) {
                Log.e("KYLE", e.getMessage(), e);
            }
        }
    };

    private boolean connected = false;

    final CotServiceRemote.ConnectionListener cl = new CotServiceRemote.ConnectionListener() {
        @Override
        public void onCotServiceConnected(Bundle fullServiceState) {
            Log.d("KYLE", "onCotServiceConnected: ");
            connected = true;
        }

        @Override
        public void onCotServiceDisconnected() {
            Log.d("KYLE", "onCotServiceDisconnected: ");
            connected = false;
        }

    };

    final CotStreamListener csl;
    final CotServiceRemote.OutputsChangedListener _outputsChangedListener = new CotServiceRemote.OutputsChangedListener() {
        @Override
        public void onCotOutputRemoved(Bundle descBundle) {
            Log.d(TAG, "stream removed");
        }

        @Override
        public void onCotOutputUpdated(Bundle descBundle) {
            Log.d(TAG,
                    "Received ADD message for "
                            + descBundle
                            .getString(CotPort.DESCRIPTION_KEY)
                            + ": enabled="
                            + descBundle.getBoolean(
                            CotPort.ENABLED_KEY, true)
                            + ": connected="
                            + descBundle.getBoolean(
                            CotPort.CONNECTED_KEY, false) + " -- " + descBundle.toString());
        }
    };

    CotServiceRemote.InputsChangedListener inputsChangedListener = new CotServiceRemote.InputsChangedListener() {
        @Override
        public void onCotInputAdded(Bundle bundle) {
            Log.d(TAG, "Input added: " + bundle.toString());
        }

        @Override
        public void onCotInputRemoved(Bundle bundle) {
            Log.d(TAG, "Input removed: " + bundle.toString());
        }
    };

    // Clears shared prefs of saved reports/lzs/patients/whatever
    // used after successfully submission to TAK network
    private void clearSavedData() {
        SharedPreferences.Editor editor = getSharedPreferences(getMapView().getContext()).edit();
        editor.remove("currentNineline");
        editor.remove("mistList");
        editor.remove("patientList");
        editor.remove("currentLZ");
        editor.apply();

        nineLineFragment.clearData();
        patientsFragment.clearData();
        mistFragment.clearData();

        updateCurrentUI();
    }

    private void updateCurrentUI() {
        if (plaintextTitle.equals(MIST_TITLE_STR)) {
            mistFragment.updateUI();
        } else if (plaintextTitle.equals(NINLINE_TITLE_STR)) {
            nineLineFragment.updateUI();
        } else if (plaintextTitle.equals(PATIENT_TITLE_STR)) {
            patientsFragment.updateUI();
        } else if (plaintextTitle.equalsIgnoreCase(LZ_TITLE_STR)) {
            lzFragment.updateUI();
        }
    }

    public SquireDropDownReceiver(final MapView mapView,
                                  final Context context, SquireMapOverlay overlay, SquireMapComponent mapComponent) {
        super(mapView);
        this.pluginContext = context;
        this.mapOverlay = overlay;
        this.mapComponent = mapComponent;
        final Activity parentActivity = (Activity) mapView.getContext();
        CotServiceRemote csr = new CotServiceRemote();
        csr.setOutputsChangedListener(_outputsChangedListener);
        csr.setInputsChangedListener(inputsChangedListener);
        ATAK_ACTION_BAR_HEIGHT = mapView.getActionBarHeight();


        //csr.setCotEventListener(cotEventListener);
        //csr.connect(cl);

        csl = new CotStreamListener(mapView.getContext(), "KYLE", null, true) {
            @Override
            public void onCotOutputRemoved(Bundle bundle) {
                Log.d("KYLE", "stream outputremoved");
            }

            @Override
            protected void enabled(CotPortListActivity.CotPort port, boolean enabled) {
                //Log.d("KYLE", "stream enabled");
            }

            @Override
            protected void connected(CotPortListActivity.CotPort port,
                                     boolean connected) {
                //Log.d("KYLE", "stream connected: " + port.getURL(true));
            }

            @Override
            public void onCotOutputUpdated(Bundle descBundle) {
                //Log.d("KYLE", "stream added/updated: " + descBundle.toString());
            }

        };

        Context ctx = getMapView().getContext();

        Log.d(TAG, "Setting cot importer");
        CotEventTypeImporter cotEventTypeImporter = new CotEventTypeImporter(mapView, "medevac") {
            @Override
            public CommsMapComponent.ImportResult importData(CotEvent event, Bundle extras) {
                String eventString = event != null ? event.toString() : "\"\"";
                String extrasString = extras != null ? extras.toString() : "\"\"";

                Log.d(SquireDropDownReceiver.TAG, "CoT received:\n\t" + eventString + "\n\t" + extrasString);
                if (event == null) {
                    Log.d(SquireDropDownReceiver.TAG, "Event was null");
                } else {
                    synchronized (reportedUidsLock) {
                        String title = "Squire Report Received";

                        String submissionStr = event.getHow();
                        Report submission = new Gson().fromJson(submissionStr, Report.class);
                        if (submission != null) {
                            Report seenReport = ReportsRepository.getReportsMap().get(submission.uid);

                            // If report has not been seen yet on this device, Add to repository and notify
                            if (seenReport == null) {
                                ReportsRepository.addReport(submission, ctx);

                                String message = mapComponent.displayAlert(mapView, title, submission);

                                // If SubmissionStr was valid & alert was displayed.
                                Log.d(SquireDropDownReceiver.TAG, "Supposed to be doing alert");

                                // Build notification
                                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ctx, SquireMapComponent.SQUIRE_NOTIFICATION_CHANNEL_ID)
                                        .setSmallIcon(ctx.getApplicationInfo().icon)
                                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.squire_icon))
                                        .setContentTitle(title)
                                        .setWhen(submission.time)
                                        .setAutoCancel(false);

                                String[] notificationMessageLines = TextUtils.split(message, "\n");
                                if (notificationMessageLines.length > 0) {
                                    notificationBuilder.setContentText(notificationMessageLines[0]);
                                }

                                // Store pendingIntent (notification tap action)
                                Intent intent = new Intent();
                                intent.setAction(SquireMapComponent.SQUIRE_NOTIFICATION_ACTION);
                                intent.putExtra(SquireMapComponent.SQUIRE_NOTIFICATION_KEY_UID, submission.uid);

                                // notificationId is a unique int for each notification that you must define
                                int notificationId = (int) event.getStart().getMilliseconds();

                                //TODO: Update to API 31 and add PendingIntent.FLAG_MUTABLE
                                PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                                notificationBuilder.setContentIntent(pendingIntent);

                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(ctx);

                                // Display notification
                                notificationManager.notify(notificationId, notificationBuilder.build());
                            }
                        }
                    }
                }

                return super.importData(event, extras);
            }
        };
        CotEventTypeImporter hrEventImporter = new CotEventTypeImporter(mapView, "medevac-hr") {
            @Override
            public CommsMapComponent.ImportResult importData(CotEvent event, Bundle extras) {
                Log.d(TAG, "Received HR! " + event.getHow());
                Gson gson = new Gson();
                HeartRate hr = gson.fromJson(event.getHow(), HeartRate.class);
                if (hr != null) {
                    // If we don't know who the patient is we aren't storing their data
                    if (ReportsRepository.hasPatient(hr.getPatientUuid())) {
                        ReportsRepository.addHeartRate(hr, mapView.getContext());
                    }
                }

                return super.importData(event, extras);
            }
        };
        CotImporterManager.getInstance().registerImporter(cotEventTypeImporter);
        CotImporterManager.getInstance().registerImporter(hrEventImporter);

        Log.d(TAG, "Done setting cot importer");

        Log.d(TAG, "Initializing sender queue + thread");
        cotSendQueue = new ArrayList<>();
        cotSendLock = new ReentrantLock();
        reportSenderThread = new Thread(() -> {
            while (true) {
                cotSendLock.lock();
                // Shitty clone so insert doesnt wait for sleeps and sends
                List<CotEvent> keepList = new ArrayList<>();
                List<CotEvent> dupList = new ArrayList<>();
                for (CotEvent event : cotSendQueue) {
                    dupList.add(event);
                }
                cotSendQueue.clear();
                cotSendLock.unlock();

                CotDispatcher dispatcher = CotMapComponent.getExternalDispatcher();
                Iterator<CotEvent> itr = dupList.iterator();
                while (itr.hasNext()) {
                    CotEvent event = itr.next();
                    CoordinatedTime nowTime = new CoordinatedTime();
                    CoordinatedTime staleTime = event.getStale();

                    if (nowTime.getMilliseconds() < staleTime.getMilliseconds()) {
                        // Send if it's still time
                        dispatcher.dispatch(event, new Bundle());
                        keepList.add(event);
                    } /*else {
                        // Pop o.w.; Don't break/continue, still sleep
                        itr.remove();
                    } */
                }
                cotSendQueue.addAll(keepList);

                try {
                    Thread.sleep(3 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Log.d(TAG, "Sender ready");
        reportSenderThread.setPriority(Thread.NORM_PRIORITY);
        reportSenderThread.setDaemon(true);
        reportSenderThread.start();
        Log.d(TAG, "Sender thread started");

        //printNetworks();

        AtakBroadcast.getInstance().registerReceiver(
                fakePhoneCallReceiver,
                new AtakBroadcast.DocumentedIntentFilter("com.atakmap.android.helloworld.FAKE_PHONE_CALL")
        );

        // If you are using a custom layout you need to make use of the PluginLayoutInflator to clear
        // out the layout cache so that the plugin can be properly unloaded and reloaded.
        helloView = PluginLayoutInflater.inflate(pluginContext,
                R.layout.squire_hello_world_layout, null);
        patientsFragView = PluginLayoutInflater.inflate(pluginContext,
                R.layout.patients_fragment, null);
        nineLineFragView = PluginLayoutInflater.inflate(pluginContext,
                R.layout.nineline_fragment, null);
        mistFragView = PluginLayoutInflater.inflate(pluginContext,
                R.layout.mist_fragment, null);
        lzFragView = PluginLayoutInflater.inflate(pluginContext,
                R.layout.lz_fragment, null);
        deviceAdapterView = PluginLayoutInflater.inflate(pluginContext,
                R.layout.polar_device_info_item, null);
        polarDeviceFragView = PluginLayoutInflater.inflate(pluginContext,
                R.layout.polar_device_fragment, null);
        reportFragView = PluginLayoutInflater.inflate(pluginContext,
                R.layout.reports_fragment, null);
        this.squireMedevacContact = addPluginContact(pluginContext.getString(
                R.string.squire_medevac));

        recyclerView = new RecyclerViewDropDown(getMapView(), pluginContext);
        tabView = new TabViewDropDown(getMapView(), pluginContext);
        fragmentContainer = helloView.findViewById(R.id.squire_medevac_fragment_container);
        scrollView = helloView.findViewById(R.id.squire_menu_scroll_view);

        polarDeviceFragment = new PolarDeviceFragment();
        nineLineFragment = new NineLineFragment(pluginContext);
        mistFragment = new MISTFragment(pluginContext);
        patientsFragment = new PatientsFragment(pluginContext);
        lzFragment = new LZFragment(pluginContext);

        medevacPttButton = helloView
                .findViewById(R.id.squirePTTButton);
        medevacPttButton.setOnClickListener(v -> {
            micFragView = PluginLayoutInflater.inflate(pluginContext,
                    R.layout.microphone_fragment, null);

            Bundle args = new Bundle();
            args.putString("title", plaintextTitle);
            MicrophoneFragment micDialog = MicrophoneFragment.newInstance();
            sd1a.register(getMapView().getContext(), sdra, micDialog);
            micDialog.setArguments(args);
            micDialog.show(((Activity) mapView.getContext()).getFragmentManager().beginTransaction(), "MicDialog");
        });

        testMe = (Callable) () -> {
            Log.d(TAG, "heart rate button clicked");
            Log.d(TAG, "Starting fragment dialog?");
            polarDeviceFragView = PluginLayoutInflater.inflate(pluginContext,
                    R.layout.polar_device_fragment, null);
            polarDialog = PolarDeviceFragment.newInstance();
            Gson gson = new Gson();
            Bundle args = new Bundle();
            args.putString("patientUUID", gson.toJson(patientsFragment.getCurrentPatientUUID()));
            Log.d(TAG, "patientUUID " + args.getString("patientUUID"));
            polarDialog.setArguments(args);
            polarDialog.show(((Activity) mapView.getContext()).getFragmentManager().beginTransaction(), "PolarDeviceDialog");

            Log.d(TAG, "Trying to scan...");
            SquireMapComponent.startScanThread(ctx);
            Log.d(TAG, "Hopefully started");
            return null;
        };

        openReportDialog = (Callable) () -> {
            reportFragView = PluginLayoutInflater.inflate(pluginContext,
                    R.layout.reports_fragment, null);
            reportDialog = ReportFragment.newInstance();

            Gson gson = new Gson();
            Bundle args = new Bundle();
            Type reportToken = new TypeToken<ArrayList<Report>>(){}.getType();
            Collection<Report> currReports = ReportsRepository.getReports();
            args.putString("reports", gson.toJson(currReports, reportToken));
            reportDialog.show(((Activity) mapView.getContext()).getFragmentManager().beginTransaction(), "ReportFragmentDialog");
            return null;
        };

        // Move submission to the button in report fragment
        submitReportsToAtak = (Callable) () -> {
            SharedPreferences sharedSettingsPreferences = getSharedPreferences(mapView.getContext());
            boolean readbackFlag = sharedSettingsPreferences.getBoolean("readback", true);
            String message = "";

            Type patientListType = new TypeToken<ArrayList<Patient>>() {
            }.getType();
            Type lzMapToken = new TypeToken<HashMap<String, LZ>>() {
            }.getType();
            Type mistListType = new TypeToken<ArrayList<MIST>>() {
            }.getType();
            String ninelineJson = sharedSettingsPreferences.getString("currentNineline", null);
            String lzJson = sharedSettingsPreferences.getString("currentLZ", null);
            String lzMapJson = sharedSettingsPreferences.getString("lzMap", null);
            String mistListJson = sharedSettingsPreferences.getString("mistList", null);
            String patientListJson = sharedSettingsPreferences.getString("patientList", null);

            LZ lz = null;
            NineLine nineLine = null;
            List<MIST> mistList = null;
            Map<String, LZ> lzMap = null;
            List<Patient> patientList = null;

            Gson gson = new Gson();
            if (lzJson != null) lz = gson.fromJson(lzJson, LZ.class);
            if (lzMapJson != null) lzMap = gson.fromJson(lzMapJson, lzMapToken);
            if (mistListJson != null) mistList = gson.fromJson(mistListJson, mistListType);
            if (ninelineJson != null) nineLine = gson.fromJson(ninelineJson, NineLine.class);
            if (patientListJson != null)
                patientList = gson.fromJson(patientListJson, patientListType);

            if (lz == null &&
                    nineLine == null &&
                    (mistList == null || mistList.size() == 0) &&
                    (patientList == null || patientList.size() == 0)) return null;

            final Report submission = new Report(nineLine, mistList, patientList, lz);

            if (readbackFlag) {
                message += "Please listen to the audio read back and confirm that all data is correct.\n\n";
                mapComponent.audioReadback(submission);
            } else {
                message += "";
                message += "Please confirm the following data is correct.\n\n";
                message += SquireMapComponent.submissionToMessage(submission) + "\n\n";
            }

            List<LZ> lzList = new ArrayList<>();
            if (lz != null) lzList.add(lz);
            if (lzMap != null) {
                for (LZ val : lzMap.values()) {
                    if (val != null) lzList.add(val);
                }
            }

            message += "Pressing 'Confirm' will submit your report(s) on the ATAK network.";

            AlertDialog confirmDialog = new AlertDialog.Builder(mapView.getContext())
                    .setTitle("Confirm Report(s)")
                    .setMessage(message)
                    .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            CotEvent event = new CotEvent();
                            if (submission.nineline.getDateTime() == null || submission.nineline.getDateTime().length() == 0) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    submission.nineline.setDateTime(Date.from(Instant.now()).toString());
                                }
                            }
                                /*
                                if (finalNineline != null) {
                                    String mgrsStr = null;
                                    String nineLineLocation = finalNineline.getLocation();
                                    if (nineLineLocation != null && nineLineLocation.length() > 0) {
                                        boolean matched = false;
                                        for (LZ lz : finalLZList) {
                                            if (nineLineLocation.equalsIgnoreCase(lz.getName())) {
                                                mgrsStr = lz.getMgrs();
                                                matched = true;
                                                break;
                                            }
                                        }

                                        if (!matched) mgrsStr = nineLineLocation;

                                        if (mgrsStr != null && mgrsStr.length() > 0) {
                                            MGRSPoint mgrsPoint = MGRSPoint.decodeString(mgrsStr, Ellipsoid.WGS_84, null);
                                            double[] latlong = new double[2];
                                            mgrsPoint.toLatLng(latlong);
                                            GeoPoint geoPoint = new GeoPoint(latlong[0], latlong[1]);
                                            CotPoint point = new CotPoint(geoPoint);
                                            event.setPoint(point);
                                        }
                                    }
                                }
                                */

                            CoordinatedTime time = new CoordinatedTime();
                            submission.confirm(time.getMilliseconds());

                            event.setUID(submission.uid);
                            event.setTime(time);
                            event.setStart(time);
                            //event.setStale(time.addSeconds(15));
                            event.setStale(time.addMinutes(15));
                            //event.setStale(time);
                            event.setHow(gson.toJson(submission));
                            event.setType("medevac");

                            cotSendLock.lock();
                            cotSendQueue.add(event);
                            cotSendLock.unlock();
                            Log.d(TAG, "Cot event added to send queue");
                            //Bundle bundle = new Bundle();
                            //CotDispatcher dispatcher = CotMapComponent.getExternalDispatcher();
                            //dispatcher.dispatch(event, bundle);
                            //Log.d(TAG, "Message has been sent to everyone: " + event.toString());


                            ReportsRepository.addReport(submission, ctx);

                            clearSavedData();

                            mapComponent.tts.stop();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Reject", (dialog, which) -> {
                        mapComponent.tts.stop();
                        dialog.dismiss();
                    })
                    .setIcon(R.drawable.squire_icon)
                    .show();
            confirmDialog.setOnDismissListener(dialog -> mapComponent.tts.stop());
            closeReportDialog();
            return null;
        };

        medevacTitleTextView = helloView.findViewById(R.id.squire_medevac_title_text_view);
        float density = context.getResources().getDisplayMetrics().density;

        ninelineSelectButton = helloView.findViewById(R.id.squire_nineline_select);
        ninelineSelectButton.setOnClickListener(view -> {
            Activity activity = (Activity) mapView.getContext();
            FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
            transaction.replace(R.id.squire_medevac_fragment_container, nineLineFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            setHeight(Math.round(600 * density));

            // Do last
            setTitle(context, NINLINE_TITLE_STR);
            scrollView.scrollTo(0, 0);
        });

        mistSelectButton = helloView.findViewById(R.id.squire_mist_select);
        mistSelectButton.setOnClickListener(view -> {
            Activity activity = (Activity) mapView.getContext();
            FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
            transaction.replace(R.id.squire_medevac_fragment_container, mistFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            setHeight(Math.round(300 * density));

            // Do last
            setTitle(context, MIST_TITLE_STR);
            scrollView.scrollTo(0, 0);
        });

        // "Hidden" button
        patientSelectButton = new Button(mapView.getContext());//helloView.findViewById(R.id.squire_patient_select);
        patientSelectButton.setOnClickListener(view -> {
            Activity activity = (Activity) mapView.getContext();
            FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
            transaction.replace(R.id.squire_medevac_fragment_container, patientsFragment);
            transaction.addToBackStack(null);
            transaction.commit();

            // Do last
            setTitle(context, PATIENT_TITLE_STR);
            scrollView.scrollTo(0, 0);
        });


        lzSelectButton = helloView.findViewById(R.id.squire_lz_select);
        lzSelectButton.setOnClickListener(view -> {
            Activity activity = (Activity) mapView.getContext();
            FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
            transaction.replace(R.id.squire_medevac_fragment_container, lzFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            setHeight(Math.round(300 * density));
            Log.d(TAG, "Setting height 600 for LZ button");

            // Do last
            setTitle(context, LZ_TITLE_STR);
            scrollView.scrollTo(0, 0);
        });

        reportSelectButton = helloView.findViewById(R.id.squire_report_select);
        reportSelectButton.setOnClickListener(view -> {
            try {
                submitReportsToAtak.call();
                //openReportDialog.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Allow a default view of "nothing selected" (empty menu)
        setTitle("Powered by voice", false);
    }

    public static void scrollToTopOfInnerView(Activity activity) {
        if (helloView  == null) return;
        if (scrollView == null) return;

        int[] coords = new int[2];
        DisplayMetrics dm = new DisplayMetrics();
        FrameLayout fragCont = helloView.findViewById(R.id.squire_medevac_fragment_container);
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);

        int topOffset = dm.heightPixels - scrollView.getMeasuredHeight();
        fragCont.getLocationInWindow(coords);
        scrollView.scrollTo(0, ATAK_ACTION_BAR_HEIGHT - topOffset - coords[1]);
    }

    public static void scrollToTop() {
        scrollView.scrollTo(0, 0);
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.d(TAG, "showing hello world drop down");

        final String action = intent.getAction();
        Log.d(TAG, "onReceive. Action: " + action);
        if (action == null)
            return;

        // Show drop-down
        switch (action) {
            case SHOW_HELLO_WORLD:
                if (!isClosed()) {
                    Log.d(TAG, "the drop down is already open");
                    unhideDropDown();
                    return;
                }

                showDropDown(helloView, HALF_WIDTH, FULL_HEIGHT,
                        FULL_WIDTH, HALF_HEIGHT, false, this);
                setAssociationKey("helloWorldPreference");

                break;

            // Chat message sent to Hello World contact
            case CHAT_HELLO_WORLD:
                Bundle cotMessage = intent.getBundleExtra(
                        ChatManagerMapComponent.PLUGIN_SEND_MESSAGE_EXTRA);

                String msg = cotMessage.getString("message");

                if (!FileSystemUtils.isEmpty(msg)) {
                    // Display toast to show the message was received
                    toast(squireMedevacContact.getName() + " received: " + msg);
                }
                break;

            // Sending CoT to Hello World contact
            case SEND_HELLO_WORLD:
                // Map item UID
                String uid = intent.getStringExtra("targetUID");
                MapItem mapItem = getMapView().getRootGroup().deepFindUID(uid);
                if (mapItem != null) {
                    // Display toast to show the CoT was received
                    toast(squireMedevacContact.getName() + " received request to send: "
                            + ATAKUtilities.getDisplayName(mapItem));
                }
                break;

            // Toggle visibility of example layer
            case LAYER_VISIBILITY: {
                Log.d(TAG, "used the custom action to toggle layer visibility on: " + intent
                        .getStringExtra("uid"));
                ExampleLayer l = mapOverlay.findLayer(intent
                        .getStringExtra("uid"));
                if (l != null) {
                    l.setVisible(!l.isVisible());
                } else {
                    ExampleMultiLayer ml = mapOverlay.findMultiLayer(intent
                            .getStringExtra("uid"));
                    if (ml != null)
                        ml.setVisible(!ml.isVisible());
                }
                break;
            }

            // Delete example layer
            case LAYER_DELETE: {
                Log.d(TAG, "used the custom action to delete the layer on: " + intent
                        .getStringExtra("uid"));
                ExampleLayer l = mapOverlay.findLayer(intent
                        .getStringExtra("uid"));
                if (l != null) {
                    getMapView().removeLayer(RenderStack.MAP_SURFACE_OVERLAYS, l);
                } else {
                    ExampleMultiLayer ml = mapOverlay.findMultiLayer(intent
                            .getStringExtra("uid"));
                    if (ml != null)
                        getMapView().removeLayer(RenderStack.MAP_SURFACE_OVERLAYS, ml);
                }
                break;
            }
        }
    }

    @Override
    protected void disposeImpl() {

    }

    @Override
    protected void onStateRequested(int state) {
        if (state == DROPDOWN_STATE_FULLSCREEN) {
            if (!isPortrait()) {
                if (Double.compare(currWidth, HALF_WIDTH) == 0) {
                    resize(FULL_WIDTH - HANDLE_THICKNESS_LANDSCAPE,
                            FULL_HEIGHT);
                }
            } else {
                if (Double.compare(currHeight, HALF_HEIGHT) == 0) {
                    resize(FULL_WIDTH, FULL_HEIGHT - HANDLE_THICKNESS_PORTRAIT);
                }
            }
        } else if (state == DROPDOWN_STATE_NORMAL) {
            if (!isPortrait()) {
                resize(HALF_WIDTH, FULL_HEIGHT);
            } else {
                resize(FULL_WIDTH, HALF_HEIGHT);
            }
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {

    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
        currWidth = width;
        currHeight = height;
    }


    @Override
    public void onDropDownClose() {
        Log.d(TAG, "Closing drop down");
    }

    /************************* Helper Methods *************************/

    private RouteMapReceiver getRouteMapReceiver() {

        // TODO: this code was copied from another plugin.
        // Not sure why we can't just callRouteMapReceiver.getInstance();
        MapActivity activity = (MapActivity) getMapView().getContext();
        MapComponent mc = activity.getMapComponent(RouteMapComponent.class);
        if (mc == null || !(mc instanceof RouteMapComponent)) {
            Log.w(TAG, "Unable to find route without RouteMapComponent");
            return null;
        }

        RouteMapComponent routeComponent = (RouteMapComponent) mc;
        return routeComponent.getRouteMapReceiver();
    }

    public void toast(String str) {
        Toast.makeText(getMapView().getContext(), str,
                Toast.LENGTH_LONG).show();
    }

    public void createSpeechMarker(HashMap<String, String> s) {
        final GeoPoint mgrsPoint;
        try {
            String[] coord = new String[]{
                    s.get("numericGrid") + s.get("alphaGrid"),
                    s.get("squareID"),
                    s.get("easting"),
                    s.get("northing")
            };
            mgrsPoint = CoordinateFormatUtilities.convert(coord,
                    CoordinateFormat.MGRS);

        } catch (IllegalArgumentException e) {
            String msg = "An error has occurred getting the MGRS point";
            Log.e(TAG, msg, e);
            toast(msg);
            return;
        }

        Marker m = new Marker(mgrsPoint, UUID
                .randomUUID().toString());
        Log.d(TAG, "creating a new unit marker for: " + m.getUID());

        switch (s.get("markerType").charAt(0)) {
            case 'U':
                m.setType("a-u-G-U-C-F");
                break;
            case 'N':
                m.setType("a-n-G-U-C-F");
                break;
            case 'F':
                m.setType("a-f-G-U-C-F");
                break;
            case 'H':
            default:
                m.setType("a-h-G-U-C-F");
                break;
        }

        new Thread(new Runnable() {
            public void run() {
                getMapView().getMapController().zoomTo(.00001d, false);

                getMapView().getMapController().panTo(mgrsPoint, false);
                try {
                    Thread.sleep(1000);
                } catch (Exception ignored) {
                }

            }
        }).start();

        m.setMetaBoolean("readiness", true);
        m.setMetaBoolean("archive", true);
        m.setMetaString("how", "h-g-i-g-o");
        m.setMetaBoolean("editable", true);
        m.setMetaBoolean("movable", true);
        m.setMetaBoolean("removable", true);
        m.setMetaString("entry", "user");
        m.setMetaString("callsign", "Speech Marker");
        m.setTitle("Speech Marker");

        MapGroup _mapGroup = getMapView().getRootGroup()
                .findMapGroup("Cursor on Target")
                .findMapGroup(s.get("markerType"));
        _mapGroup.addItem(m);

        m.persist(getMapView().getMapEventDispatcher(), null,
                this.getClass());

        Intent new_cot_intent = new Intent();
        new_cot_intent.setAction("com.atakmap.android.maps.COT_PLACED");
        new_cot_intent.putExtra("uid", m.getUID());
        AtakBroadcast.getInstance().sendBroadcast(
                new_cot_intent);
    }

    public void createAircraftWithRotation() {
        PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(
                getMapView().getPointWithElevation());
        mc.setUid(UUID.randomUUID().toString());
        mc.setCallsign("SNF");
        mc.setType("a-f-A");
        mc.showCotDetails(false);
        mc.setNeverPersist(true);
        Marker m = mc.placePoint();
        // the stle of the marker is by default set to show an arrow, this will allow for full
        // rotation.   You need to enable the heading mask as well as the noarrow mask
        m.setStyle(m.getStyle()
                | Marker.STYLE_ROTATE_HEADING_MASK
                | Marker.STYLE_ROTATE_HEADING_NOARROW_MASK);
        m.setTrack(310, 20);
        m.setMetaInteger("color", Color.YELLOW);
        m.setMetaString(UserIcon.IconsetPath,
                "34ae1613-9645-4222-a9d2-e5f243dea2865/Military/A10.png");
        m.refresh(getMapView().getMapEventDispatcher(), null,
                this.getClass());

    }


    public void createOrModifySensorFOV() {
        final MapView mapView = getMapView();
        final String cameraID = "sensor-fov-example-uid";
        final GeoPointMetaData point = mapView.getCenterPoint();
        final int color = 0xFFFF0000;

        MapItem mi = mapView.getMapItem(cameraID);
        if (mi == null) {
            PlacePointTool.MarkerCreator markerCreator = new PlacePointTool.MarkerCreator(point);
            markerCreator.setUid(cameraID);
            //this settings automatically pops open to CotDetails page after dropping the marker
            markerCreator.showCotDetails(false);
            //this settings determines if a CoT persists or not.
            markerCreator.setArchive(true);
            //this is the type of the marker.  Could be set to a known 2525B value or custom
            markerCreator.setType("b-m-p-s-p-loc");
            //this shows under the marker
            markerCreator.setCallsign("Sensor");
            //this also determines if the marker persists or not??
            markerCreator.setNeverPersist(false);
            mi = markerCreator.placePoint();

        }
        // blind cast, ensure this is really a marker.
        Marker camera1 = (Marker)mi;
        camera1.setPoint(point);

        mi = mapView.getMapItem(camera1.getUID()+"-fov");
        if (mi instanceof SensorFOV) {
            SensorFOV sFov = (SensorFOV)mi;
            float r = ((0x00FF0000 & color) >> 16) / 256f;
            float g = ((0x0000FF00 & color) >> 8) / 256f;
            float b = ((0x000000FF & color) >> 0) / 256f;

            sFov.setColor(color); // currently broken
            sFov.setColor(r,g,b);
            sFov.setMetrics((int)(90 * Math.random()), (int)(70 * Math.random()), 400);
        } else { // use this case
            float r = ((0x00FF0000 & color) >> 16) / 256f;
            float g = ((0x0000FF00 & color) >> 8) / 256f;
            float b = ((0x000000FF & color) >> 0) / 256f;
            SensorDetailHandler.addFovToMap(camera1, 90, 70, 400, new float[]{r, g, b, 90}, true);
        }
    }


    public void createUnit() {

        Marker m = new Marker(getMapView().getPointWithElevation(), UUID
                .randomUUID().toString());
        Log.d(TAG, "creating a new unit marker for: " + m.getUID());
        m.setType("a-f-G-U-C-I");
        // m.setMetaBoolean("disableCoordinateOverlay", true); // used if you don't want the coordinate overlay to appear
        m.setMetaBoolean("readiness", true);
        m.setMetaBoolean("archive", true);
        m.setMetaString("how", "h-g-i-g-o");
        m.setMetaBoolean("editable", true);
        m.setMetaBoolean("movable", true);
        m.setMetaBoolean("removable", true);
        m.setMetaString("entry", "user");
        m.setMetaString("callsign", "Test Marker");
        m.setTitle("Test Marker");
        m.setMetaString("menu", getMenu());

        MapGroup _mapGroup = getMapView().getRootGroup()
                .findMapGroup("Cursor on Target")
                .findMapGroup("Friendly");
        _mapGroup.addItem(m);

        m.persist(getMapView().getMapEventDispatcher(), null,
                this.getClass());

        Intent new_cot_intent = new Intent();
        new_cot_intent.setAction("com.atakmap.android.maps.COT_PLACED");
        new_cot_intent.putExtra("uid", m.getUID());
        AtakBroadcast.getInstance().sendBroadcast(
                new_cot_intent);

    }

    void printNetworks() {
        /*
         *    CotPort.DESCRIPTION_KEY
         *    CotPort.ENABLED_KEY
         *    CotPort.CONNECTED_KEY
         *    CotPort.CONNECT_STRING_KEY
         */
        Bundle b = CommsMapComponent.getInstance().getAllPortsBundle();
        Bundle[] streams = (Bundle[]) b.getParcelableArray("streams");
        Bundle[] outputs = (Bundle[]) b.getParcelableArray("outputs");
        Bundle[] inputs = (Bundle[]) b.getParcelableArray("inputs");
        if (inputs != null) {
            for (Bundle input : inputs)
                Log.d(TAG, "input " + input.getString(CotPort.DESCRIPTION_KEY)
                        + ": " + input.getString(CotPort.CONNECT_STRING_KEY));
        }
        if (outputs != null) {
            for (Bundle output : outputs)
                Log.d(TAG, "output " + output.getString(CotPort.DESCRIPTION_KEY)
                        + ": " + output.getString(CotPort.CONNECT_STRING_KEY));
        }
        if (streams != null) {
            for (Bundle stream : streams)
                Log.d(TAG, "stream " + stream.getString(CotPort.DESCRIPTION_KEY)
                        + ": " + stream.getString(CotPort.CONNECT_STRING_KEY));
        }
    }

    /**
     * For plugins to have custom radial menus, we need to set the "menu" metadata to
     * contain a well formed xml entry.   This only allows for reskinning of existing
     * radial menus with icons and actions that already exist in ATAK.
     * In order to perform a completely custom radia menu instalation. You need to
     * define the radial menu as below and then uuencode the sub elements such as
     * images or instructions.
     */
    private String getMenu() {
        return PluginMenuParser.getMenu(pluginContext, "menu.xml");
    }

    /**
     * This is an example of a completely custom xml definition for a menu.   It uses the
     * plaintext stringified version of the current menu language plus uuencoded images
     * and actions.
     */
    public String getMenu2() {
        return PluginMenuParser.getMenu(pluginContext, "menu2.xml");
    }

    /**
     * Add a plugin-specific contact to the contacts list
     * This contact fires an intent when a message is sent to it,
     * instead of using the default chat implementation
     *
     * @param name Contact display name
     * @return New plugin contact
     */
    public Contact addPluginContact(String name) {

        // Add handler for messages
        SquireContactHandler contactHandler = new SquireContactHandler(
                pluginContext);
        CotMapComponent.getInstance().getContactConnectorMgr()
                .addContactHandler(contactHandler);

        // Create new contact with name and random UID
        IndividualContact contact = new IndividualContact(
                name, UUID.randomUUID().toString());

        // Add plugin connector which points to the intent action
        // that is fired when a message is sent to this contact
        contact.addConnector(new PluginConnector(CHAT_HELLO_WORLD));

        // Add IP connector so the contact shows up when sending CoT or files
        contact.addConnector(new IpConnector(SEND_HELLO_WORLD));

        // Set default connector to plugin connector
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(
                        getMapView().getContext());
        sharedPreferences.edit().putString("contact.connector.default." + contact.getUID(),
                PluginConnector.CONNECTOR_TYPE).apply();

        // Add new contact to master contacts list
        Contacts.getInstance().addContact(contact);

        return contact;
    }

    /**
     * Remove a contact from the master contacts list
     * This will remove it from the contacts list drop-down
     *
     * @param contact Contact object
     */
    public void removeContact(Contact contact) {
        Contacts.getInstance().removeContact(contact);
    }

    private void plotISSLocation() {
        double lat = Double.NaN, lon = Double.NaN;
        try {
            final java.io.InputStream input = new java.net.URL(
                    "http://api.open-notify.org/iss-now.json").openStream();
            final String returnJson = FileSystemUtils.copyStreamToString(input,
                    true, FileSystemUtils.UTF8_CHARSET);

            Log.d(TAG, "return json: " + returnJson);

            android.util.JsonReader jr = new android.util.JsonReader(
                    new java.io.StringReader(returnJson));
            jr.beginObject();
            while (jr.hasNext()) {
                String name = jr.nextName();
                switch (name) {
                    case "iss_position":
                        jr.beginObject();
                        while (jr.hasNext()) {
                            String n = jr.nextName();
                            switch (n) {
                                case "latitude":
                                    lat = jr.nextDouble();
                                    break;
                                case "longitude":
                                    lon = jr.nextDouble();
                                    break;
                                case "message":
                                    jr.skipValue();
                                    break;
                            }
                        }
                        jr.endObject();
                        break;
                    case "timestamp":
                        jr.skipValue();
                        break;
                    case "message":
                        jr.skipValue();
                        break;
                }
            }
            jr.endObject();
        } catch (Exception e) {
            Log.e(TAG, "error", e);
        }
        if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
            final MapItem mi = getMapView().getMapItem("iss-unique-identifier");
            if (mi != null) {
                if (mi instanceof Marker) {
                    Marker marker = (Marker)mi;

                    GeoPoint newPoint = new GeoPoint(lat, lon);
                    GeoPoint lastPoint = ((Marker) mi).getPoint();
                    long currTime = SystemClock.elapsedRealtime();

                    double dist = lastPoint.distanceTo(newPoint);
                    double dir = lastPoint.bearingTo(newPoint);

                    double delta = currTime -
                            mi.getMetaLong("iss.lastUpdateTime", 0);

                    double speed = dist / (delta / 1000f);

                    marker.setTrack(dir, speed);

                    marker.setPoint(newPoint);
                    mi.setMetaLong("iss.lastUpdateTime", SystemClock.elapsedRealtime());

                }
            } else {
                PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(
                        new GeoPoint(lat, lon));
                mc.setUid("iss-unique-identifier");
                mc.setCallsign("International Space Station");
                mc.setType("a-f-P-T");
                mc.showCotDetails(false);
                mc.setNeverPersist(true);
                Marker m = mc.placePoint();
                // don't forget to turn on the arrow so that we know where the ISS is going
                m.setStyle(Marker.STYLE_ROTATE_HEADING_MASK);
                //m.setMetaBoolean("editable", false);
                m.setMetaBoolean("movable", false);
                m.setMetaString("how", "m-g");
            }
        }
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            final float[] values = event.values;
            // Movement
            float x = values[0];
            float y = values[1];
            float z = values[2];

            float asr = (x * x + y * y + z * z)
                    / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
            if (Math.abs(x) > 6 || Math.abs(y) > 6 || Math.abs(z) > 8)
                Log.d(TAG, "gravity=" + SensorManager.GRAVITY_EARTH + " x=" + x + " y=" + y + " z=" + z + " asr=" + asr);
            if (y > 7) {
                TextContainer.getTopInstance().displayPrompt("Tilt Right");
                Log.d(TAG, "tilt right");
            } else if (y < -7) {
                TextContainer.getTopInstance().displayPrompt("Tilt Left");
                Log.d(TAG, "tilt left");
            } else if (x > 7) {
                TextContainer.getTopInstance().displayPrompt("Tilt Up");
                Log.d(TAG, "tilt up");
            } else if (x < -7) {
                TextContainer.getTopInstance().displayPrompt("Tilt Down");
                Log.d(TAG, "tilt down");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Log.d(TAG, "accuracy for the accelerometer: " + accuracy);
        }
    }

    final BroadcastReceiver fordReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getMapView().getContext(),
                    "Ford Tow Truck Application", Toast.LENGTH_SHORT).show();
        }
    };

    BroadcastReceiver fakePhoneCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            Log.d(TAG, "intent: " + intent.getAction() + " " + intent.getStringExtra("mytime"));
            getMapView().post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getMapView().getContext(),
                            "intent: " + intent.getAction() + " " + intent.getStringExtra("mytime"),
                            Toast.LENGTH_LONG).show();
                }
            });
            NotificationManager nm = (NotificationManager) getMapView().getContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            int id = intent.getIntExtra("notificationId", 0);
            Log.d(TAG, "cancelling id: " + id);
            if (id > 0) {
                nm.cancel(id);
            }
        }
    };

    // Sets the current title of the Squire Medevac Menu. Optionally shows/hides ptt button (if needed)
    private void setTitle(Context ctx, String txt) {
        setTitle(txt, true);
    }

    private void setTitle(String txt, boolean pttVisible) {
        if (medevacTitleTextView == null) {
            return;
        }

        plaintextTitle = txt;
        medevacPttButton.setVisibility(pttVisible ? View.VISIBLE : View.INVISIBLE);
        medevacTitleTextView.setText("Medevac - " + txt);
    }


    public static void setSquireFragmentHeightDP(int dp) {
        //Log.d(TAG, "supposed to be setting height to " + dp + "dp");
        float density = helloView.getContext().getResources().getDisplayMetrics().density;
        setHeight(Math.round(density * dp));
    }

    public static void setHeight(int newFragHeight) {
        ImageView logo = helloView.findViewById(R.id.squire_logo);
        Button button = helloView.findViewById(R.id.squire_nineline_select);
        ImageView ptt = helloView.findViewById(R.id.squirePTTButton);
        TextView title = helloView.findViewById(R.id.squire_medevac_title_text_view);
        FrameLayout fragCont = helloView.findViewById(R.id.squire_medevac_fragment_container);
        ConstraintLayout menuCont = helloView.findViewById(R.id.squire_menu_container);

        int logoHeight = 0;
        int buttonHeight = 0;
        int pttHeight = 0;
        int titleHeight = 0;
        int fragHeight = 0;

        if (logo != null) {
            logoHeight = logo.getHeight();
        }
        if (button != null) {
            buttonHeight = button.getHeight();
        }
        if (ptt != null) {
            pttHeight = ptt.getHeight();
        }
        if (title != null) {
            titleHeight = title.getHeight();
        }
        if (fragCont != null) {
            if (newFragHeight == -1) {
                fragHeight = fragCont.getHeight();
            } else {
                fragHeight = newFragHeight;
                fragCont.getLayoutParams().height = fragHeight;
                //Log.d(TAG, "fragHeight passed arg: " + fragHeight);
            }
        }
        int total = fragHeight + pttHeight + buttonHeight;
        //Log.d(TAG, "FragHeight:    " + fragHeight);
        //Log.d(TAG, "PttHeight:     " + pttHeight);
        //Log.d(TAG, "ButtonHeight:  " + buttonHeight);
        //Log.d(TAG, "Total height:  " + total);

        if (menuCont != null) {
            menuCont.getLayoutParams().height = total;
            //Log.d(TAG, "set menuscroll: " + menuCont.getHeight());
        }
    }

    static void patientsDeviceButtonClick() {

    }

    public static void closePolarDialog() {
        if (polarDialog == null) return;
        polarDialog.dismiss();
    }

    public static void closeReportDialog() {
        if (reportDialog == null) return;
        reportDialog.dismiss();
    }

    public static void setPatientUIHR(int hr, UUID uuid) {
        patientsFragment.setHR(hr, uuid);
    }

    public static void showReport(String msg) {
        reportDialog.showReport(msg);
    }

    private SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("squire_medevac", Context.MODE_PRIVATE);
    }
}
