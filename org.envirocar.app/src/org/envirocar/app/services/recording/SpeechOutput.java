package org.envirocar.app.services.recording;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.core.events.gps.GpsSatelliteFix;
import org.envirocar.core.events.gps.GpsSatelliteFixEvent;
import org.envirocar.core.logging.Logger;

import java.util.Locale;

import javax.inject.Inject;

import rx.Subscription;

/**
 * Handles the text to speech output of the enviroCar app.
 *
 * @author dewall
 */
public class SpeechOutput implements LifecycleObserver {
    private static final Logger LOG = Logger.getLogger(SpeechOutput.class);

    @Inject
    protected Bus eventBus;

    private Context context;

    // text to speech variables
    private boolean ttsAvailable = false;
    private TextToSpeech tts;

    // preference subscription
    private boolean ttsEnabled = false;
    private Subscription ttsPrefSubscription;

    // gps satellite events
    private GpsSatelliteFix latestFix;

    /**
     * Constructor.
     *
     * @param context
     */
    public SpeechOutput(Context context) {
        this.context = context;

        // init
        this.tts = new TextToSpeech(context, status -> {
            try {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.ENGLISH);
                    ttsAvailable = true;
                } else {
                    LOG.warn("TextToSpeech is not available.");
                }
            } catch (IllegalArgumentException e) {
                LOG.warn("TextToSpeech is not available");
            }
        });
    }

    /**
     * Performs a text to speech.
     *
     * @param text the text to speech.
     */
    public void doTextToSpeech(String text) {
        if (this.ttsEnabled && ttsAvailable) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null);
        }
    }

    /**
     * Lifecycle Event hook. Should be called when the onCreate method of the Recording Service was called.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    protected void onCreate() {
        // subscription that handles preference changes
        ttsPrefSubscription =
                PreferencesHandler.getTextToSpeechObservable(this.context)
                        .subscribe(aBoolean -> ttsEnabled = aBoolean);

        LOG.info("Registering on eventBus");
        this.eventBus.register(this);
    }

    /**
     * Needs to be called when the recording service has been stopped.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    protected void onDestroy() {
        // unsubscribe
        if (ttsPrefSubscription != null && !ttsPrefSubscription.isUnsubscribed()) {
            ttsPrefSubscription.unsubscribe();
        }

        // try to unregister from event bus.
        try {
            this.eventBus.unregister(this);
        } catch (IllegalArgumentException e){
            // nothing
        }
    }

    /**
     * Subscribes for GPS Satellite fix events.
     *
     * @param event
     */
    @Subscribe
    public void onReceiveSatelliteFixEvent(GpsSatelliteFixEvent event) {
        boolean isFix = event.mGpsSatelliteFix.isFix();
        if (latestFix != null && isFix != latestFix.isFix()) {
            if (isFix) {
                if (isFix) {
                    doTextToSpeech("GPS positioning established");
                } else {
                    doTextToSpeech("GPS positioning lost. Try to move the phone");
                }
                this.latestFix = event.mGpsSatelliteFix;
            }
        }
    }
}
