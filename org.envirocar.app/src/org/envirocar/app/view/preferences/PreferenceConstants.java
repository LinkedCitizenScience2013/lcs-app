package org.envirocar.app.view.preferences;

/**
 * @author dewall
 */
public interface PreferenceConstants {
    String PREFERENCE_TAG_EMPTY = "";
    String PREFERENCE_TAG_BLUETOOTH_LIST = "bluetooth_list";
    String PREFERENCE_TAG_BLUETOOTH_NAME = "bluetooth_name";
    String PREFERENCE_TAG_BLUETOOTH_ADDRESS = "bluetooth_address";
    String PREFERENCE_TAG_BLUETOOTH_ENABLER = "bluetooth_enabler";
    String PREFERENCE_TAG_BLUETOOTH_PAIRING = "bluetooth_pairing";

    String PREFERENCE_TAG_BLUETOOTH_SERVICE_AUTOSTART = "pref_bluetooth_service_autostart";
    String PREFERENCE_TAG_BLUETOOTH_AUTOCONNECT = "pref_bluetooth_autoconnect";
    String PREFERENCE_TAG_BLUETOOTH_DISCOVERY_INTERVAL = "pref_bluetooth_discovery_interval";

    String PREFERENCE_TAG_TEXT_TO_SPEECH = "pref_text_to_speech";

    int DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL = 60;
    boolean DEFAULT_BLUETOOTH_AUTOCONNECT = false;


    String PREFERENCE_TAG_CAR = "pref_selected_car";
    String CAR_HASH_CODE = "pref_selected_car_hash_code";
    String PREFERENCE_TAG_CARS = "pref_cars";


    public static final String AUTO_BLUETOOH = "pref_auto_bluetooth";
    public static final String WIFI_UPLOAD = "pref_wifi_upload";
    public static final String ALWAYS_UPLOAD = "pref_always_upload";
    public static final String DISPLAY_STAYS_ACTIV = "pref_display_always_activ";
    public static final String IMPERIAL_UNIT = "pref_imperial_unit";
    public static final String OBFUSCATE_POSITION = "pref_privacy";
    public static final String PERSISTENT_SEEN_ANNOUNCEMENTS = "persistent_seen_announcements";
    public static final String SAMPLING_RATE = "ec_sampling_rate";
    public static final String ENABLE_DEBUG_LOGGING = "pref_enable_debug_logging";
}