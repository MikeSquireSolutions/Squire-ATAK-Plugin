
package com.atakmap.android.helloworld;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Address;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.atakmap.android.contact.ContactLocationView;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.cot.UIDHandler;
import com.atakmap.android.cot.detail.CotDetailHandler;
import com.atakmap.android.cot.detail.CotDetailManager;
import com.atakmap.android.cotdetails.ExtendedInfoView;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.helloworld.aidl.ILogger;
import com.atakmap.android.helloworld.aidl.SimpleService;
import com.atakmap.android.helloworld.db.ReportsRepository;
import com.atakmap.android.helloworld.models.HeartRate;
import com.atakmap.android.helloworld.models.LZ;
import com.atakmap.android.helloworld.models.MIST;
import com.atakmap.android.helloworld.models.NineLine;
import com.atakmap.android.helloworld.models.Patient;
import com.atakmap.android.helloworld.models.PolarDevice;
import com.atakmap.android.helloworld.models.Report;
import com.atakmap.android.helloworld.models.SignsAndSymptoms;
import com.atakmap.android.helloworld.models.Treatment;
import com.atakmap.android.helloworld.plugin.R;
import com.atakmap.android.helloworld.routes.RouteExportMarshal;
import com.atakmap.android.importexport.ExporterManager;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importexport.ImportReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.ipc.DocumentedExtra;
import com.atakmap.android.layers.LayersMapComponent;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapEventDispatcher.MapEventDispatchListener;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.graphics.GLMapItemFactory;
import com.atakmap.android.munitions.DangerCloseReceiver;
import com.atakmap.android.radiolibrary.RadioMapComponent;
import com.atakmap.android.statesaver.StateSaverPublisher;
import com.atakmap.android.user.geocode.GeocodeManager;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.CotDispatcher;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.net.DeviceProfileClient;
import com.google.gson.Gson;
import com.polar.sdk.api.PolarBleApi;
import com.polar.sdk.api.PolarBleApiCallback;
import com.polar.sdk.api.PolarBleApiDefaultImpl;
import com.polar.sdk.api.errors.PolarInvalidArgument;
import com.polar.sdk.api.model.PolarDeviceInfo;
import com.polar.sdk.api.model.PolarHrData;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * This is an example of a MapComponent within the ATAK
 * ecosphere.   A map component is the building block for all
 * activities within the system.   This defines a concrete
 * thought or idea.
 */
public class SquireMapComponent extends DropDownMapComponent {

    public static final String TAG = "HelloWorldMapComponent";

    public static final String SQUIRE_NOTIFICATION_CHANNEL_ID = "Squire";
    public static final String SQUIRE_NOTIFICATION_ACTION = "com.atakmap.android.squire.NOTIFICATION";
    public static final String SQUIRE_NOTIFICATION_KEY_UID = "uid";

    private static ReentrantLock polarApiLock = new ReentrantLock();
    private static Map<String, UUID> deviceMap = new HashMap<>(); // maps device id to patient uuid
    private static PolarBleApi polarApi = null;

    private Context pluginContext;
    private SquireDropDownReceiver dropDown;
    private WebViewDropDownReceiver wvdropDown;
    private SquireMapOverlay mapOverlay;
    private View genericRadio;
    private JoystickView _joystickView;
    private SpecialDetailHandler sdh;
    private CotDetailHandler aaaDetailHandler;
    private ContactLocationView.ExtendedSelfInfoFactory extendedselfinfo;

    //Squire
    private TextToSpeech.OnInitListener ttsInitListener;
    private MapView mapView;
    TextToSpeech tts;
    HashSet<String> currentAlerts = new HashSet<>();

    public class JoystickView extends RelativeLayout {

        public JoystickView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public boolean onGenericMotionEvent(MotionEvent event) {
            Log.d(TAG, "onGenericMotionEvent: " + event.toString());
            return super.onGenericMotionEvent(event);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            Log.d(TAG, "onKeyDown: " + event.toString());
            return super.onKeyDown(keyCode, event);
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            Log.d(TAG, "onKeyUp: " + event.toString());
            return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public void onStart(final Context context, final MapView view) {
        Log.d(TAG, "onStart");
    }

    @Override
    public void onPause(final Context context, final MapView view) {
        Log.d(TAG, "onPause");
    }

    @Override
    public void onResume(final Context context,
                         final MapView view) {
        Log.d(TAG, "onResume");
    }

    @Override
    public void onStop(final Context context,
                       final MapView view) {
        Log.d(TAG, "onStop. Stopping location updates");
    }

    /**
     * Simple uncalled example for how to import a file.
     */
    private void importFileExample(final File file) {
        /**
         * Case 1 where the file type is known and in this example, the file is a map type.
         */
        Log.d(TAG, "testImport: " + file.toString());
        Intent intent = new Intent(
                ImportExportMapComponent.ACTION_IMPORT_DATA);
        intent.putExtra(ImportReceiver.EXTRA_URI,
                file.getAbsolutePath());
        intent.putExtra(ImportReceiver.EXTRA_CONTENT, LayersMapComponent.IMPORTER_CONTENT_TYPE);
        intent.putExtra(ImportReceiver.EXTRA_MIME_TYPE, LayersMapComponent.IMPORTER_DEFAULT_MIME_TYPE);

        AtakBroadcast.getInstance().sendBroadcast(intent);
        Log.d(TAG, "testImportDone: " + file.toString());


        /**
         * Case 2 where the file type is unknown and the file is just imported.
         */
        Log.d(TAG, "testImport: " + file.toString());
        intent = new Intent(ImportExportMapComponent.USER_HANDLE_IMPORT_FILE_ACTION);
        intent.putExtra("filepath", file.toString());
        intent.putExtra("importInPlace", false); // copies it over to the general location if true
        intent.putExtra( "promptOnMultipleMatch", true); //prompts the users if this could be multiple things
        intent.putExtra("zoomToFile", false); // zoom to the outer extents of the file.
        AtakBroadcast.getInstance().sendBroadcast(intent);
        Log.d(TAG, "testImportDone: " + file.toString());

    }


    @Override
    public void onCreate(final Context context, Intent intent, final MapView view) {

        // Set the theme.  Otherwise, the plugin will look vastly different
        // than the main ATAK experience.   The theme needs to be set
        // programatically because the AndroidManifest.xml is not used.
        context.setTheme(R.style.ATAKPluginTheme);

        super.onCreate(context, intent, view);
        pluginContext = context;
        mapView = view;

        GLMapItemFactory.registerSpi(GLSpecialMarker.SPI);

        // Register capability to handle detail tags that TAK does not normally process.
        CotDetailManager.getInstance().registerHandler("__special", sdh = new SpecialDetailHandler());

        CotDetailManager.getInstance().registerHandler(aaaDetailHandler = new CotDetailHandler("__aaa") {
            private final String TAG = "AAACotDetailHandler";

            @Override
            public CommsMapComponent.ImportResult toItemMetadata(MapItem item, CotEvent event, CotDetail detail) {
                Log.d(TAG, "detail received: " + detail + " in:  " + event);
                return CommsMapComponent.ImportResult.SUCCESS;
            }

            @Override
            public boolean toCotDetail(MapItem item, CotEvent event, CotDetail root) {
                Log.d(TAG, "converting to cot detail from: " + item.getUID());
                return true;
            }
        });


        ReportsRepository.loadReportsFromPrefs(view.getContext());

        createNotificationChannel(view.getContext());

        IntentFilter reportIntentFilter = new IntentFilter();
        reportIntentFilter.addAction(Intent.ACTION_SEND);
        android.util.Log.d(TAG, "Broadcast receiver registered");

        view.getContext().registerReceiver(
                squireNotificationReceiver,
                new IntentFilter(SQUIRE_NOTIFICATION_ACTION)
        );

        ttsInitListener = status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS - This Language is not supported");
                }
            } else {
                Log.e(TAG, "TTS Initialization Failed!");
            }
        };

        tts = new TextToSpeech(view.getContext(), ttsInitListener);
        ReportsRepository.loadReportsFromPrefs(view.getContext());

        //HelloWorld MapOverlay added to Overlay Manager.
        this.mapOverlay = new SquireMapOverlay(view, pluginContext);
        view.getMapOverlayManager().addOverlay(this.mapOverlay);



        //MapView.getMapView().getRootGroup().getChildGroupById(id).setVisible(true);

        /*Intent new_cot_intent = new Intent();
        new_cot_intent.setAction("com.atakmap.android.maps.COT_PLACED");
        new_cot_intent.putExtra("uid", point.getUID());
        AtakBroadcast.getInstance().sendBroadcast(
                new_cot_intent);*/

        // End of Overlay Menu Test ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        // In this example, a drop down receiver is the
        // visual component within the ATAK system.  The
        // trigger for this visual component is an intent.
        // see the plugin.HelloWorldTool where that intent
        // is triggered.
        this.dropDown = new SquireDropDownReceiver(view, context, this.mapOverlay, this);



        // We use documented intent filters within the system
        // in order to automatically document all of the
        // intents and their associated purposes.

        Log.d(TAG, "registering the show hello world filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(SquireDropDownReceiver.SHOW_HELLO_WORLD,
                "Show the Hello World drop-down");
        ddFilter.addAction(SquireDropDownReceiver.CHAT_HELLO_WORLD,
                "Chat message sent to the Hello World contact");
        ddFilter.addAction(SquireDropDownReceiver.SEND_HELLO_WORLD,
                "Sending CoT to the Hello World contact");
        ddFilter.addAction(SquireDropDownReceiver.LAYER_DELETE,
                "Delete example layer");
        ddFilter.addAction(SquireDropDownReceiver.LAYER_VISIBILITY,
                "Toggle visibility of example layer");
        this.registerDropDownReceiver(this.dropDown, ddFilter);
        Log.d(TAG, "registered the show hello world filter");

        this.wvdropDown = new WebViewDropDownReceiver(view, context);
        Log.d(TAG, "registering the webview filter");
        DocumentedIntentFilter wvFilter = new DocumentedIntentFilter();
        wvFilter.addAction(WebViewDropDownReceiver.SHOW_WEBVIEW,
                "web view");
        this.registerDropDownReceiver(this.wvdropDown, wvFilter);

        // in this case we also show how one can register
        // additional information to the uid detail handle when
        // generating cursor on target.   Specifically the
        // NETT-T service specification indicates the the
        // details->uid should be filled in with an appropriate
        // attribute.

        // add in the nett-t required uid entry.
        UIDHandler.getInstance().addAttributeInjector(
                new UIDHandler.AttributeInjector() {
                    public void injectIntoDetail(Marker marker,
                                                 CotDetail detail) {
                        if (marker.getType().startsWith("a-f"))
                            return;
                        detail.setAttribute("nett", "XX");
                    }

                    public void injectIntoMarker(CotDetail detail,
                                                 Marker marker) {
                        if (marker.getType().startsWith("a-f"))
                            return;
                        String callsign = detail.getAttribute("nett");
                        if (callsign != null)
                            marker.setMetaString("nett", callsign);
                    }

                });

        // In order to use shared preferences with a plugin you will need
        // to use the context from ATAK since it has the permission to read
        // and write preferences.
        // Additionally - in the XML file you cannot use PreferenceCategory
        // to enclose your Prefences - otherwise the preference will not
        // be persisted.   You can fake a PreferenceCategory by adding an
        // empty preference category at the top of each group of preferences.
        // See how this is done in the current example.

        DangerCloseReceiver.ExternalMunitionQuery emq = new DangerCloseReceiver.ExternalMunitionQuery() {
            @Override
            public String queryMunitions() {
                return BuildExternalMunitionsQuery();
            }
        };

        DangerCloseReceiver.getInstance().setExternalMunitionQuery(emq);

        // for custom preferences
        ToolsPreferenceFragment
                .register(
                        new ToolsPreferenceFragment.ToolPreference(
                                "Squire Preferences",
                                "Squire Medevac Preferences",
                                "helloWorldPreference",
                                context.getResources().getDrawable(
                                        R.drawable.ic_launcher, null),
                                new SquirePreferenceFragment(context)));

        // example for how to register a radio with the radio map control.

        LayoutInflater inflater = LayoutInflater.from(pluginContext);
        genericRadio = inflater.inflate(R.layout.radio_item_generic, null);

        RadioMapComponent.getInstance().registerControl(genericRadio);

        // demonstrate how to customize the view for ATAK contacts.   In this case
        // it will show a customized line of test when pulling up the contact
        // detail view.
        ContactLocationView.register(extendedselfinfo =
                new ContactLocationView.ExtendedSelfInfoFactory() {
                    @Override
                    public ExtendedInfoView createView() {
                        return new ExtendedInfoView(view.getContext()) {
                            @Override
                            public void setMarker(PointMapItem m) {
                                Log.d(TAG, "setting the marker: " + m.getMetaString("callsign", ""));
                                TextView tv = new TextView(view.getContext());
                                tv.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
                                this.addView(tv);
                                tv.setText("Example: " + m.getMetaString("callsign", "unknown"));

                            }
                        };
                    }
                }
        );


        // send out some customized information as part of the SA or PPLI message.
        CotDetail cd = new CotDetail("temp");
        cd.setAttribute("temp", Integer.toString(76));
        CotMapComponent.getInstance().addAdditionalDetail(cd.getElementName(), cd);


        // register a listener for when a the radial menu asks for a special
        // drop down.  SpecialDetail is really a skeleton of a class that
        // shows a very basic drop down.
        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction("com.atakmap.android.helloworld.myspecialdetail",
                "this intent launches the special drop down",
                new DocumentedExtra[] {
                        new DocumentedExtra("targetUID",
                                "the map item identifier used to populate the drop down")
                });
        registerDropDownReceiver(new SpecialDetail(view, pluginContext),
                filter);

        //see if any hello profiles/data are available on the TAK Server. Requires the server to be
        //properly configured, and "Apply TAK Server profile updates" setting enabled in ATAK prefs
        Log.d(TAG, "Checking for Hello profile on TAK Server");
        DeviceProfileClient.getInstance().getProfile(view.getContext(),
                "hello");

        //register profile request to run upon connection to TAK Server, in case we're not yet
        //connected, or the the request above fails
        CotMapComponent.getInstance().addToolProfileRequest("hello");

        registerSpisVisibilityListener(view);

        view.addOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent event) {
                Log.d(TAG, "dispatchKeyEvent: " + event.toString());
                return false;
            }
        });

        //_joystickView = new JoystickView(context, (AttributeSet) new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        _joystickView = new JoystickView(context, null);

        ViewGroup v = (ViewGroup) view.getParent().getParent();
        v.addView(_joystickView);

        GeocodeManager.getInstance(context).registerGeocoder(fakeGeoCoder);

        TextView tv = new TextView(context);
        LayoutParams lp_tv = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        lp_tv.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        tv.setText("Test Center Layout");
        tv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Test Test Test");
            }
        });
        com.atakmap.android.video.VideoDropDownReceiver.registerVideoViewLayer(
                new com.atakmap.android.video.VideoViewLayer("test-layer", tv,
                        lp_tv));


        ExporterManager.registerExporter(
                context.getString(R.string.route_exporter_name),
                context.getDrawable(R.drawable.ic_route),
                RouteExportMarshal.class);


        // Code to listen for when a state saver is completely loaded or wait to perform some action
        // after all of the markers are completely loaded.

        final BroadcastReceiver ssLoadedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // action for when the statesaver is completely loaded.
            }
        };
        AtakBroadcast.getInstance().registerReceiver(ssLoadedReceiver,
                new DocumentedIntentFilter(StateSaverPublisher.STATESAVER_COMPLETE_LOAD));
        // because the plugin can be loaded after the above intent has been fired, there is a method
        // to check to see if a load has already occured.

        if (StateSaverPublisher.isFinished()) {
            // no need to listen for the intent
            AtakBroadcast.getInstance().unregisterReceiver(ssLoadedReceiver);
            // action for when the statesaver is completely loaded
        }
    }

    private final GeocodeManager.Geocoder fakeGeoCoder = new GeocodeManager.Geocoder() {
        @Override
        public String getUniqueIdentifier() {
            return "fake-geocoder";
        }

        @Override
        public String getTitle() {
            return "Gonna get you Lost";
        }

        @Override
        public String getDescription() {
            return "Sample Geocoder implementation registered with TAK";
        }

        @Override
        public boolean testServiceAvailable() {
            return true;
        }

        @Override
        public List<Address> getLocation(GeoPoint geoPoint) {
            Address a = new Address(Locale.getDefault());
            a.setAddressLine(0, "100 WrongWay Street");
            a.setAddressLine(1, "Boondocks, Nowhere");
            a.setCountryCode("UNK");
            a.setPostalCode("999999");
            a.setLatitude(geoPoint.getLatitude());
            a.setLongitude(geoPoint.getLongitude());
            return new ArrayList<>(Collections.singleton(a));
        }

        @Override
        public List<Address> getLocation(String s, GeoBounds geoBounds) {
            Address a = new Address(Locale.getDefault());
            a.setAddressLine(0, "100 WrongWay Street");
            a.setAddressLine(1, "Boondocks, Nowhere");
            a.setCountryCode("UNK");
            a.setPostalCode("999999");
            a.setLatitude(0);
            a.setLongitude(0);
            return new ArrayList<>(Collections.singleton(a));
        }
    };

    private void registerSpisVisibilityListener(MapView view) {
        spiListener = new SpiListener(view);
        for (int i = 0; i < 4; ++i) {
            MapItem mi = view
                    .getMapItem(view.getSelfMarker().getUID() + ".SPI" + i);
            if (mi != null) {
                mi.addOnVisibleChangedListener(spiListener);
            }
        }

        final MapEventDispatcher dispatcher = view.getMapEventDispatcher();
        dispatcher.addMapEventListener(MapEvent.ITEM_REMOVED, spiListener);
        dispatcher.addMapEventListener(MapEvent.ITEM_ADDED, spiListener);

    }

    private SpiListener spiListener;

    private class SpiListener implements MapEventDispatchListener,
            MapItem.OnVisibleChangedListener {
        private final MapView view;

        SpiListener(MapView view) {
            this.view = view;
        }

        @Override
        public void onMapEvent(MapEvent event) {
            MapItem item = event.getItem();
            if (item == null)
                return;
            if (event.getType().equals(MapEvent.ITEM_ADDED)) {
                if (item.getUID()
                        .startsWith(view.getSelfMarker().getUID() + ".SPI")) {
                    item.addOnVisibleChangedListener(this);
                    Log.d(TAG, "visibility changed for: " + item.getUID() + " "
                            + item.getVisible());
                }
            } else if (event.getType().equals(MapEvent.ITEM_REMOVED)) {
                if (item.getUID()
                        .startsWith(view.getSelfMarker().getUID() + ".SPI"))
                    item.removeOnVisibleChangedListener(this);
            }
        }

        @Override
        public void onVisibleChanged(MapItem item) {
            Log.d(TAG, "visibility changed for: " + item.getUID() + " "
                    + item.getVisible());
        }
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        Log.d(TAG, "calling on destroy");
        ContactLocationView.unregister(extendedselfinfo);
        GLMapItemFactory.unregisterSpi(GLSpecialMarker.SPI);
        this.dropDown.dispose();
        ToolsPreferenceFragment.unregister("helloWorldPreference");
        RadioMapComponent.getInstance().unregisterControl(genericRadio);
        view.getMapOverlayManager().removeOverlay(mapOverlay);
        CotDetailManager.getInstance().unregisterHandler(
                sdh);
        CotDetailManager.getInstance().unregisterHandler(aaaDetailHandler);
        ExporterManager.unregisterExporter(
                context.getString(R.string.route_exporter_name));

        view.getContext().unregisterReceiver(squireNotificationReceiver);

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroyImpl(context, view);

        // Example call on how to end ATAK if the plugin is unloaded.
        // It would be important to possibly show the user a dialog etc.

        //Intent intent = new Intent("com.atakmap.app.QUITAPP");
        //intent.putExtra("FORCE_QUIT", true);
        //AtakBroadcast.getInstance().sendBroadcast(intent);

    }

    private String BuildExternalMunitionsQuery() {
        String xmlString = "";
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory
                    .newDocumentBuilder();
            Document doc = documentBuilder.newDocument();

            Element rootEl = doc.createElement("Current_Flights");
            Element catEl = doc.createElement("category");
            catEl.setAttribute("name", "lead");
            Element weaponEl = doc.createElement("weapon");
            weaponEl.setAttribute("name", "GBU-12");
            weaponEl.setAttribute("proneprotected", "130");
            weaponEl.setAttribute("standing", "175");
            weaponEl.setAttribute("prone", "200");
            weaponEl.setAttribute("description", "(500-lb LGB)");
            weaponEl.setAttribute("active", "false");
            weaponEl.setAttribute("id", "1");
            catEl.appendChild(weaponEl);
            rootEl.appendChild(catEl);
            doc.appendChild(rootEl);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();

            DOMSource domSource = new DOMSource(doc.getDocumentElement());
            OutputStream output = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(output);

            transformer.transform(domSource, result);
            xmlString = output.toString();
        } catch (Exception ex) {
            Log.d(TAG, "Exception in BuildExternalMunitionsQuery: "
                    + ex.getMessage());
        }
        return xmlString;
    }

    public static String submissionToMessage(Report submission) {
        String message = "";
        if (submission == null) return message;

        LZ lz = submission.lz;
        NineLine nineLine = submission.nineline;
        List<Patient> patientList = submission.patients;
        List<MIST> mistList = submission.mists;

        Log.d(TAG, "Vars done");
        String patientsByPrio = getPatientsString(patientList);
        String patientsByStatus = getNumPatientsString(patientList);

        Log.d(TAG, "Doing nineline");
        if (nineLine != null && !nineLine.empty()) {
            message += "Nine Line:\n";
            if (nineLine.getLocation() != null)
                message += "Location: " + prettifyString(nineLine.getLocation()) + "\n";
            if (nineLine.getRadioFrequency() != null)
                message += "Radio Freq: " + prettifyString(nineLine.getRadioFrequency()) + "\n";
            if (patientsByPrio.length() > 0)
                message += "Patients: " + prettifyString(patientsByPrio) + "\n";
            if (nineLine.getSpecialEquipmentRequired() != null)
                message += "Special Equipment: " + prettifyString(nineLine.getSpecialEquipmentRequired()) + "\n";
            if (patientsByStatus.length() > 0)
                message += "Patients: " + prettifyString(patientsByStatus) + "\n";
            if (nineLine.getSecurityAtPickupSite() != null)
                message += "Security: " + prettifyString(nineLine.getSecurityAtPickupSite()) + "\n";
            if (nineLine.getMethodOfMarkingPickupSite() != null)
                message += "Method of Marking: " + prettifyString(nineLine.getMethodOfMarkingPickupSite()) + "\n";
            if (nineLine.getNationality() != null)
                message += "Nationality: " + nineLine.getNationality() + "\n";
            if (nineLine.getNbcContamination() != null)
                message += "NBC: " + prettifyString(nineLine.getNbcContamination()) + "\n";
        }

        if (mistList != null && mistList.size() > 0) {
            if (message.length() > 0)
                message += "\n";
            message += "Mist Report(s):";

            for (int i = 0; i < mistList.size(); ++i) {
                MIST mist = mistList.get(i);
                Patient p = null;
                if (patientList != null) {
                    p = patientList.get(i);
                }

                if (message.length() > 0) message += "\n";
                if (mist.getMechanismOfInjury() != null)
                    message += "Mechanism: " + prettifyString(mist.getMechanismOfInjury()) + "\n";
                if (mist.getInjury() != null)
                    message += "Injury: " + prettifyString(mist.getInjury()) + "\n";
                if (mist.getSignsAndSymptoms() != null)
                    message += "Signs and Symptoms: " + prettifyString(mist.getSignsAndSymptoms().getDisplayString()) + "\n";
                if (mist.getTreatment() != null)
                    message += "Treatment: " + prettifyString(mist.getTreatment().getDisplayString()) + "\n";
                if (p != null && p.getHeartRate() > 0) {
                    message += "Patient heart rate: " + p.getHeartRate() + "\n";
                }
            }
        }

        if (patientList != null && patientList.size() > 0) {
            if (message.length() > 0)
                message += "\n";
            message += "Patient(s):";

            for (Patient patient : patientList) {
                if (message.length() > 0) message += "\n";
                if (patient.getCallSign() != null)
                    message += "Call-Sign: " + prettifyString(patient.getCallSign()) + "\n";
                if (patient.getPriority() != null)
                    message += "Priority: " + prettifyString(patient.getPriority()) + "\n";
                if (patient.getStatus() != null)
                    message += "Status: " + prettifyString(patient.getStatus()) + "\n";
            }
        }

        Log.d(TAG, "Doing LZ");
        if (lz != null && !lz.empty()) {
            if (message.length() > 0)
                message += "\n";
            message += "LZ:\n";
            if (lz.getName() != null)
                message += "Name: " + prettifyString(lz.getName());
            if (lz.getMgrs() != null)
                message += "MGRS: " + prettifyString(lz.getMgrs());
            if (lz.getDescription() != null)
                message += "Description: " + prettifyString(lz.getDescription());
        }

        return message;
    }

    public static String prettifyString(Object o) {
        if (o == null) return "";
        StringBuilder retVal = new StringBuilder();
        String[] splits = o.toString().replaceAll("_", " ").split("\\s+");

        for (String s : splits) {
            if (s.length() < 1) continue;
            if (retVal.length() > 0) retVal.append(" ");

            String lower = s.toLowerCase();
            retVal.append(("" + lower.charAt(0)).toUpperCase());
            if (s.length() > 1) retVal.append(lower.substring(1));
        }

        return retVal.toString();
    }


    // Patient counts by priority/precedence
    public static String getPatientsString(List<Patient> patients){
        String retVal = "";
        if (patients == null) return retVal;

        int numUrgent = 0;
        int numUrgentSurgical = 0;
        int numPriority = 0;
        int numRoutine = 0;
        int numConvenience = 0;
        for (Patient p : patients) {
            switch (p.getPriority()) {
                case URGENT:
                    numUrgent++;
                    break;
                case URGENT_SURGICAL:
                    numUrgentSurgical++;
                    break;
                case PRIORITY:
                    numPriority++;
                    break;
                case ROUTINE:
                    numRoutine++;
                    break;
                case CONVENIENCE:
                    numConvenience++;
                    break;
            }
        }

        if (numUrgent > 0) {
            retVal += numUrgent + " Urgent";
        }
        if (numUrgentSurgical > 0) {
            if (retVal.length() > 0) retVal += "\n";
            retVal += numUrgentSurgical + " Urgent Surgical";
        }
        if (numPriority > 0) {
            if (retVal.length() > 0) retVal += "\n";
            retVal += numPriority + " Priority";
        }
        if (numRoutine > 0) {
            if (retVal.length() > 0) retVal += "\n";
            retVal += numRoutine + " Routine";
        }
        if (numConvenience > 0) {
            if (retVal.length() > 0) retVal += "\n";
            retVal += numConvenience + " Convenience\n";
        }

        return retVal;
    }

    public static String getNumPatientsString(List<Patient> patients) {
        String retVal = "";
        if (patients == null) return retVal;

        int numLitter = 0;
        int numAmbulatory = 0;
        for (Patient p : patients) {
            switch (p.getStatus()) {
                case LITTER:
                    numLitter++;
                    break;
                case AMBULATORY:
                    numAmbulatory++;
                    break;
            }
        }
        if (numLitter > 0) {
            retVal += numLitter + " Litter";
        }
        if (numAmbulatory > 0) {
            if (retVal.length() > 0) retVal += "\n";
            retVal += numAmbulatory + " Ambulatory";
        }

        return retVal;
    }

    /**
     * @return message
     */
    String displayAlert(MapView mapView, String title, Report submission) {
        String message = submissionToMessage(submission);

        if (!currentAlerts.contains(submission.uid)) {
            currentAlerts.add(submission.uid);

            Context context = mapView.getContext();
            ContextCompat.getMainExecutor(context).execute(() -> new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setOnCancelListener(dialogInterface -> {
                        currentAlerts.remove(submission.uid);
                    })
                    .setPositiveButton("Ok", (dialog, which) -> {
                        currentAlerts.remove(submission.uid);
                        Log.d(SquireDropDownReceiver.TAG, "Clicked accept");
                    })
                    .setNegativeButton("Read Back", (dialogInterface, i) -> {
                        currentAlerts.remove(submission.uid);
                        if (tts != null) {
                            audioReadback(submission);
                        }
                    })
                    .show());
        }

        return message;
    }

    void audioReadback(Report submission) {
        if (submission == null) return;
        LZ lz = submission.lz;
        NineLine nineLine = submission.nineline;
        List<MIST> mistList = submission.mists;
        List<Patient> patientList = submission.patients;

        List<String> toSpeak = new ArrayList<>();
        toSpeak.add("The following reports will be submitted to the A TAK network");
        if (nineLine != null) {
            String locationStr = nineLine.getLocation();
            String rfStr = nineLine.getRadioFrequency();
            String patientsStr = "NONE";
            String equipmentStr = "NONE";
            String numPatientsStr = "NONE";
            String securityStr = "NONE";
            String momStr = "NONE";
            String natStr = "NONE";
            String nbcStr = "NONE";

            NineLine.SpecialEquipmentRequired equip = nineLine.getSpecialEquipmentRequired();
            NineLine.SecurityAtPickupSite sec = nineLine.getSecurityAtPickupSite();
            NineLine.MethodOfMarkingPickupSite mom = nineLine.getMethodOfMarkingPickupSite();
            NineLine.Nationality nat = nineLine.getNationality();
            NineLine.NBCContamination nbc = nineLine.getNbcContamination();

            if (patientList != null) {
                patientsStr = getPatientsString(patientList);
                numPatientsStr = getNumPatientsString(patientList);
            }

            if (locationStr == null || locationStr.length() == 0) locationStr = "NONE";
            if (rfStr == null || rfStr.length() == 0) rfStr = "NONE";
            if (equip != null) equipmentStr = equip.toString().replaceAll("_", " ");
            if (sec != null) securityStr = sec.toString().replaceAll("_", " ");
            if (mom != null) momStr = mom.toString().replaceAll("_", " ");
            if (nat != null) natStr = nat.toString().replaceAll("_", " ");
            if (nbc != null) nbcStr = nbc.toString().replaceAll("_", " ");

            toSpeak.add("Nine Line to follow");
            toSpeak.add("Location");
            toSpeak.add(locationStr);
            toSpeak.add("Radio Frequency");
            toSpeak.add(rfStr);
            toSpeak.add("Patients");
            toSpeak.add(patientsStr);
            toSpeak.add("Special Equipment Required");
            toSpeak.add(equipmentStr);
            toSpeak.add("Number of Patients");
            toSpeak.add(numPatientsStr);
            toSpeak.add("Security at Pickup Site");
            toSpeak.add(securityStr);
            toSpeak.add("Method of Marking Pickup Site");
            toSpeak.add(momStr);
            toSpeak.add("Nationality");
            toSpeak.add(natStr);
            toSpeak.add("NBC Contamination");
            toSpeak.add(nbcStr);
            toSpeak.add("Nine Line Complete");
        }

        if (mistList != null) {
            List<MIST> clone = new ArrayList<>();
            for (MIST mist : mistList) {
                if (!mist.empty()) clone.add(mist);
            }
            mistList = clone;

            toSpeak.add(mistList.size() + " MIST Reports to follow");
            for (int i = 0; i < mistList.size(); i++) {
                toSpeak.add("MIST Report " + (i + 1));
                MIST mist = mistList.get(i);

                String mechStr = "NONE";
                String injStr = "NONE";
                String sasStr = "NONE";
                String treatStr = "NONE";

                MIST.MechanismOfInjury mech = mist.getMechanismOfInjury();
                MIST.Injury injury = mist.getInjury();
                SignsAndSymptoms sas = mist.getSignsAndSymptoms();
                Treatment treatment = mist.getTreatment();

                if (mech != null) {
                    mechStr = mech.toString().replace("_", " ");
                }
                if (injury != null) {
                    injStr = injury.toString().replace("_", " ");
                }
                if (sas != null) {
                    sasStr = sas.getDisplayString();
                }
                if (treatment != null) {
                    treatStr = treatment.getDisplayString();
                }

                toSpeak.add("Mechanism of Injury is");
                toSpeak.add(mechStr);
                toSpeak.add("Injury is");
                toSpeak.add(injStr);
                toSpeak.add("Signs and Symptoms are");
                toSpeak.add(sasStr);
                toSpeak.add("Treatment is");
                toSpeak.add(treatStr);
            }
            toSpeak.add("MIST Reports Complete");
        }
        if (lz != null) {
            toSpeak.add("L");
            toSpeak.add("Z");
            String name = lz.getName();
            String desc = lz.getDescription();
            String mgrs = lz.getMgrs();
            if (name != null && name.length() > 0) {
                toSpeak.add("Name is " + name);
            }
            if (desc != null && desc.length() > 0) {
                toSpeak.add("Described as");
                toSpeak.add(desc);
            }
            if (mgrs != null && mgrs.length() > 0) {
                toSpeak.add("At location " + mgrs);
            }
        }

        for (String s : toSpeak) {
            tts.speak(s, TextToSpeech.QUEUE_ADD, null);
            tts.playSilentUtterance(800, TextToSpeech.QUEUE_ADD, null);
        }
    }

    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Squire";
            String description = "Squire Module for ATAK";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(SQUIRE_NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.createNotificationChannel(channel);
        }
    }

    BroadcastReceiver squireNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

            if (mapView != null) {
                Map<String, Report> reportsMap = ReportsRepository.getReportsMap();
                Report report = reportsMap.get(intent.getStringExtra(SQUIRE_NOTIFICATION_KEY_UID));

                if (report != null) {
                    displayAlert(mapView, "Squire Report", report);
                }
            }
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {

        SimpleService service;

        // Allow for the print out to use the atak logging mechanism that is unavaible from
        // the service.
        final ILogger logger = new ILogger.Stub() {
            @Override
            public void e(String tag, String msg, String exception)
                    throws RemoteException {
                Log.e(tag, "SERVICE: " + msg + "" + exception);
            }

            @Override
            public void d(String tag, String msg, String exception)
                    throws RemoteException {
                Log.d(tag, "SERVICE: " + msg + "" + exception);
            }
        };

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder iBinder) {

            service = SimpleService.Stub.asInterface(iBinder);
            Log.d(TAG, "connected to the simple service");
            try {
                service.registerLogger(logger);
            } catch (RemoteException ignored) {
            }

            // this could be anywhere in your plugin code.
            try {
                Log.d(TAG, "result from the service: " + service.add(2, 2));
            } catch (RemoteException ignored) {
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "disconnected from the simple service");
        }
    };

    public static void setupAPI(Context ctx) {
        polarApiLock.lock();
        if (polarApi != null || ctx == null) {
            polarApiLock.unlock();
            return;
        }
        polarApi = PolarBleApiDefaultImpl.defaultImplementation(ctx,
                PolarBleApi.FEATURE_HR | PolarBleApi.FEATURE_BATTERY_INFO);
        polarApi.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void blePowerStateChanged(boolean powered) {
                android.util.Log.d(TAG, "BLE power: " + powered);
            }

            @Override
            public void deviceConnected(@NonNull PolarDeviceInfo polarDeviceInfo) {
                android.util.Log.d(TAG, "CONNECTED: " + polarDeviceInfo.deviceId);
            }

            @Override
            public void deviceConnecting(@NonNull PolarDeviceInfo polarDeviceInfo) {
                android.util.Log.d(TAG, "CONNECTING: " + polarDeviceInfo.deviceId);
            }

            @Override
            public void deviceDisconnected(@NonNull PolarDeviceInfo polarDeviceInfo) {
                android.util.Log.d(TAG, "DISCONNECTED: " + polarDeviceInfo.deviceId);
            }

            @Override
            public void hrFeatureReady(@NonNull String identifier) {
                android.util.Log.d(TAG, "HR READY: " + identifier);
            }

            @Override
            public void hrNotificationReceived(@NonNull String identifier, @NonNull PolarHrData data) {
                UUID patientUuid = deviceMap.get(identifier);
                if (patientUuid == null) {
                    android.util.Log.d(TAG, "Patient was not found in device map, returning. ID I want: + " +
                            identifier + " vs what I have: " + deviceMap.keySet().toString() + " for map " +
                            deviceMap);
                    return;
                }
                int x = 0;

                android.util.Log.d(TAG,"HR: " + data.hr + " from dev id " + identifier);
                HeartRate heartRate = new HeartRate(patientUuid, data.hr);
                Intent hrIntent = new Intent(Intent.ACTION_SEND);
                hrIntent.putExtra("hr", data.hr);
                android.util.Log.d(TAG, "Before set patient ui");
                SquireDropDownReceiver.setPatientUIHR(data.hr, patientUuid);

                // Send to others on ATAK network
                {
                    android.util.Log.d(TAG, "After set patient ui");
                    Gson gson = new Gson();
                    UUID uuid = UUID.randomUUID();
                    CotEvent event = new CotEvent();
                    event.setUID(uuid.toString());
                    CoordinatedTime time = new CoordinatedTime();
                    event.setTime(time);
                    event.setStart(time);
                    event.setStale(time.addHours(1));
                    event.setStale(time);
                    event.setHow(gson.toJson(heartRate));
                    event.setType("medevac-hr");

                    Bundle bundle = new Bundle();
                    android.util.Log.d(TAG, "before external dispatcher");
                    CotDispatcher dispatcher = CotMapComponent.getExternalDispatcher();
                    android.util.Log.d(TAG, "before dispatch");
                    dispatcher.dispatch(event, bundle);
                    android.util.Log.d(TAG, "Sent event " + event.getType() + " over atak network");
                }
            }

            @Override
            public void batteryLevelReceived(@NonNull String identifier, int level) {
                android.util.Log.d(TAG, "Battery Level: " + identifier + " " + level);
            }
        });
        android.util.Log.d(TAG, "API setup (supposedly) " + ctx.toString());
        android.util.Log.d(TAG, "Connecting to " + deviceMap.keySet().size() + " devices for HR");
        for (String addr : deviceMap.keySet()) {
            try {
                polarApi.connectToDevice(addr);
            } catch (PolarInvalidArgument polarInvalidArgument) {
                polarInvalidArgument.printStackTrace();
            }
        }
        android.util.Log.d(TAG, "Start listening to devices " + deviceMap.keySet().toString());
        polarApi.startListenForPolarHrBroadcasts(deviceMap.keySet());

        polarApiLock.unlock();

        /*
        // Permission requests should be irrelevant with ATAK
        this.requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
        }, 1);
        */
    }

    public static void shutdownAPI() {
        polarApiLock.lock();
        if (polarApi == null) {
            android.util.Log.d(TAG, "Api was already shut down");
            polarApiLock.unlock();
            return;
        }

        polarApi.shutDown();
        polarApi = null;
        android.util.Log.d(TAG, "Api shut down now");
        polarApiLock.unlock();
    }

    // TODO move scan thread out of this
    public static void startScanThread(Context ctx) {
        if (polarApi == null) {
            android.util.Log.d(TAG, "Could not start scan thread, api is null");
        }

        Thread scanThread = new Thread(() -> {
            if (polarApi == null) return;
            android.util.Log.d(TAG, "Top of scan thread!");
            List<PolarDevice> foundDevices = new ArrayList<>();
            Flowable<PolarDeviceInfo> devices = polarApi.searchForDevice();
            Disposable disposable = devices.subscribe(
                    new Consumer<PolarDeviceInfo>() {
                        @Override
                        public void accept(PolarDeviceInfo polarDeviceInfo) throws Throwable {
                            PolarDevice device = new PolarDevice(polarDeviceInfo.deviceId, polarDeviceInfo.name,
                                    polarDeviceInfo.address, false);
                            device.setStatusMessage("Connect");
                            android.util.Log.d(TAG, "Found device {ID: " + polarDeviceInfo.deviceId +
                                    ", Address: " + polarDeviceInfo.address +
                                    ", Name: " + polarDeviceInfo.name +
                                    ", Connectable: " + polarDeviceInfo.isConnectable +
                                    ", RSSI: " + polarDeviceInfo.rssi +
                                    "}");
                            foundDevices.add(device);
                            Gson gson = new Gson();
                            Intent connectDevice = new Intent(Intent.ACTION_SEND);
                            connectDevice.putExtra("listDevice", gson.toJson(device));
                            ctx.sendBroadcast(connectDevice);

                            //polarApi.connectToDevice(device.getAddress());
                            //Set<String> heartbeat = new HashSet<>();
                            //heartbeat.add(polarDeviceInfo.address);
                            //polarApi.startListenForPolarHrBroadcasts(heartbeat);
                            // broadcast new device here
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable t) throws Throwable {
                            android.util.Log.e(TAG, "On error in subscribe!");
                        }
                    }
            );

            // Wait 60s for BTE devices to be found
            android.util.Log.d(TAG, "Waiting 60s");
            Date start = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                start = Date.from(Instant.now());
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            calendar.add(Calendar.SECOND, 60);
            Date end = calendar.getTime();
            while (!Thread.currentThread().isInterrupted()) {
                Date now = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    now = Date.from(Instant.now());
                }
                if (now.after(end)) {
                    break;
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            android.util.Log.d(TAG, "Finished waiting, done");
            disposable.dispose();
        });
        scanThread.start();
    }

    public static PolarBleApi getPolarApi() {
        return polarApi;
    }

    // Returns true if added, false if failed or already existed
    public static boolean addDeviceToHRSet(String addr, UUID patientUuid) {
        boolean retVal = false;
        polarApiLock.lock();
        deviceMap.put(addr, patientUuid);
        android.util.Log.d(TAG, "Put patient uuid " + patientUuid + " in map with addr " + addr +
                " to map " + deviceMap);

        retVal = deviceMap.containsKey(addr);
        polarApiLock.unlock();
        return retVal;
    }

    public static boolean isDeviceInMap(String identifier) {
        boolean retVal = false;
        polarApiLock.lock();
        retVal = deviceMap.containsKey(identifier);
        polarApiLock.unlock();
        return retVal;
    }

    public static UUID getPatientUUID(String identifier) {
        UUID uuid = null;
        polarApiLock.lock();
        uuid = deviceMap.get(identifier);
        polarApiLock.unlock();
        return uuid;
    }

    public static void removeDeviceFromHRSet(String identifier) {
        android.util.Log.d(TAG, "Removing id " + identifier + " from map " + deviceMap);
        polarApiLock.lock();
        deviceMap.remove(identifier);
        polarApiLock.unlock();
    }
}
