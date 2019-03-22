/**
 * Copyright (C) 2013 - 2015 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.services;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.injection.BaseInjectorService;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.notifications.NotificationHandler;
import org.envirocar.app.notifications.ServiceStateForNotificationForNotification;
import org.envirocar.core.events.NewCarTypeSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothDeviceSelectedEvent;
import org.envirocar.core.events.bluetooth.BluetoothStateChangedEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.ServiceUtils;
import org.envirocar.obd.events.TrackRecordingServiceStateChangedEvent;
import org.envirocar.obd.service.BluetoothServiceState;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.observers.SafeSubscriber;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static org.envirocar.app.notifications.NotificationHandler.context;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class AutomaticOBDTrackService extends BaseInjectorService {
    private static final Logger LOGGER = Logger.getLogger(AutomaticOBDTrackService.class);

    public static final void startService(Context context) {
        ServiceUtils.startService(context, AutomaticOBDTrackService.class);
    }

    public static final void stopService(Context context) {
        ServiceUtils.stopService(context, AutomaticOBDTrackService.class);
    }

    private static final int REDISCOVERY_INTERVAL = 30;

    // Static identifiers for actions for the broadcast receiver.
    public static final String ACTION_START_BT_DISCOVERY = "action_start_bt_discovery";
    public static final String ACTION_STOP_BT_DISCOVERY = "action_stop_bt_discvoery";
    public static final String ACTION_START_TRACK_RECORDING = "action_start_track_recording";
    public static final String ACTION_STOP_TRACK_RECORDING = "action_stop_track_recording";

    // Injected variables
    @Inject
    protected BluetoothHandler mBluetoothHandler;
    @Inject
    protected CarPreferenceHandler mCarManager;

    private Scheduler.Worker mWorkerThread = Schedulers.newThread().createWorker();
    private Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    private boolean mIsAutoconnect = PreferencesHandler.DEFAULT_BLUETOOTH_AUTOCONNECT;
    private boolean hasCarSelected = false;
    private int mDiscoveryInterval = PreferencesHandler.DEFAULT_BLUETOOTH_DISCOVERY_INTERVAL;

    // private member fields.
    private Subscription mWorkerSubscription;
    private Subscription mDiscoverySubscription;
    private CompositeSubscription subscriptions = new CompositeSubscription();

    // Broadcast receiver that handles the different actions that could be issued by the
    // corresponding notification of the notification bar.
    private final BroadcastReceiver mBroadcastReciever = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Received action matches the command for starting the discovery process for the
            // selected OBDII-Adapter.
            if (ACTION_START_BT_DISCOVERY.equals(action)) {
                LOGGER.info("Received Broadcast: Start Discovery.");

                // If the bluetooth is currently disabled, then do not issue the discovery.
                if (!mBluetoothHandler.isBluetoothEnabled()) {
                    LOGGER.severe("Bluetooth is disabled. No Bluetooth discovery is issued");
                }

                startDiscoveryForSelectedDevice();
            }

            // Received action matches the command for stopping the Bluetooth discovery process.
            else if (ACTION_STOP_BT_DISCOVERY.equals(action)) {
                LOGGER.info("Received Broadcast: Stop Discovery.");

                mBluetoothHandler.stopBluetoothDeviceDiscovery();

                // Set the notification state to unconnected.
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.UNCONNECTED);

                // UNUSED: This leads sometimes to some errors if you always ski
                if (mDiscoverySubscription != null) {
                    mDiscoverySubscription.unsubscribe();
                    mDiscoverySubscription = null;
                }
            }

            // Received action matches the command for starting the recording of a track.
            else if (ACTION_START_TRACK_RECORDING.equals(action)) {
                LOGGER.info("Received Broadcast: Start Track Recording.");
                startOBDConnectionService();
                //                mNotificationHandler.setNotificationState(AutomaticOBDTrackService
                // .this,
                //                        NotificationHandler.NotificationState.OBD_FOUND);
            }
        }
    };

    @Override
    public void onCreate() {
        LOGGER.info("onCreate()");
        LOGGER.info("DEBUG onCreate()");
        super.onCreate();

        // Register on the event bus.
        this.bus.register(this);

        // Get the required preference settings.
        this.mDiscoveryInterval = PreferencesHandler.getDiscoveryInterval(context);

        // Register a new BroadcastReceiver that waits for different incoming actions issued from
        // the notification.
        IntentFilter notificationClickedFilter = new IntentFilter();
        notificationClickedFilter.addAction(ACTION_START_BT_DISCOVERY);
        notificationClickedFilter.addAction(ACTION_STOP_BT_DISCOVERY);
        notificationClickedFilter.addAction(ACTION_START_TRACK_RECORDING);
        registerReceiver(mBroadcastReciever, notificationClickedFilter);

        // Set the Notification to
       if (OBDConnectionService.CURRENT_SERVICE_STATE != BluetoothServiceState.SERVICE_STARTED
                && this.mBluetoothHandler.isBluetoothEnabled()) {
            // State: No OBD device selected.
            if (mBluetoothHandler.getSelectedBluetoothDevice() == null) {
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.NO_OBD_SELECTED);
            } else if (mCarManager.getCar() == null) {
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.NO_CAR_SELECTED);
            } else {
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.UNCONNECTED);
            }
        }

        subscriptions.add(
                PreferencesHandler.getSelectedCarObsevable()
                        .map(car -> (car != null))
                        .subscribe(hasCar -> {
                            LOGGER.info(String.format("Received changed selected car -> [%s]",
                                    hasCar));

                            if(!hasCarSelected){
                                if(hasCar){
                                    hasCarSelected = hasCar;
                                    scheduleDiscovery(mDiscoveryInterval);
                                }
                            } else if (hasCarSelected && !hasCar){
                                hasCarSelected = hasCar;
                                unscheduleDiscovery();
                            }

                            hasCarSelected = hasCar;
                        }));

        subscriptions.add(
                PreferencesHandler.getDiscoveryIntervalObservable(getApplicationContext())
                        .subscribe(integer -> {
                            LOGGER.info(String.format("Received changed discovery interval -> [%s]",
                                    integer));
                            mDiscoveryInterval = integer;

                            scheduleDiscovery(mDiscoveryInterval);
                        })
        );

        subscriptions.add(
                PreferencesHandler.getAutoconnectObservable(getApplicationContext())
                        .subscribe(aBoolean -> {
                            LOGGER.info(String.format("Received changed autoconnect -> [%s]",
                                    aBoolean));
                            mIsAutoconnect = aBoolean;

                            scheduleDiscovery(mDiscoveryInterval);
                        })
        );

        subscriptions.add(
                PreferencesHandler.getBackgroundHandlerEnabledObservable(getApplicationContext())
                        .subscribe(aBoolean -> {
                            LOGGER.info(String.format("Received changed autostart -> [%s]",
                                    aBoolean));
                            if(!aBoolean) stopSelf();

                        })
        );


    }


    @Override
    protected void injectDependencies(BaseApplicationComponent appComponent) {
        appComponent.inject(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOGGER.info("onStartCommand()");
        LOGGER.info("DEBUG onStartCommand()");
        // only start the discovery process if the required settings has been selected.
        if (mBluetoothHandler.isBluetoothEnabled() &&
                mBluetoothHandler.getSelectedBluetoothDevice() != null &&
                mCarManager.getCar() != null &&
                mIsAutoconnect) {
            scheduleDiscovery(-1);
        }

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        LOGGER.info("onDestroy()");
        super.onDestroy();
        LOGGER.info("DEBUG onDestroy()");
        // unregister all boradcast receivers.
        unregisterReceiver(mBroadcastReciever);

        // Unsubscribe subscriptions.
        if (mWorkerSubscription != null)
            mWorkerSubscription.unsubscribe();
        if (mDiscoverySubscription != null)
            mDiscoverySubscription.unsubscribe();

        if (!subscriptions.isUnsubscribed()) {
            subscriptions.unsubscribe();
        }

        this.bus.unregister(this);

        // Close the corresponding notification.
        NotificationHandler.closeNotification();

        //Stopping the bluetooth discovery
        mBluetoothHandler.stopBluetoothDeviceDiscovery();
    }

    @Subscribe
    public void onReceiveBluetoothStateChangedEvent(BluetoothStateChangedEvent event) {
        LOGGER.info(String.format("Received event. %s", event.toString()));
        if (!event.isBluetoothEnabled) {
            // When Bluetooth has been turned off, then this remoteService is required to be closed.
            if (mBluetoothHandler.isDiscovering())
                mBluetoothHandler.stopBluetoothDeviceDiscovery();
            stopSelf();
        }
    }

    @Subscribe
    public void onReceiveBluetoothDeviceSelectedEvent(BluetoothDeviceSelectedEvent event) {
        LOGGER.info(String.format("Received event. %s", event.toString()));
        if (event.mDevice == null) {
            NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.NO_OBD_SELECTED);
        } else if (NotificationHandler.getRecordingState() == ServiceStateForNotificationForNotification.NO_OBD_SELECTED) {
            if (mCarManager.getCar() == null) {
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.NO_CAR_SELECTED);
            } else {
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.UNCONNECTED);
            }
        }
    }

    /**
     * Receiver method for {@link TrackRecordingServiceStateChangedEvent}s posted on the event bus.
     *
     * @param event the corresponding event type.
     */
    @Subscribe
    public void onReceiveTrackRecordingServiceStateChangedEvent(
            TrackRecordingServiceStateChangedEvent event) {
        LOGGER.info(String.format("onReceiveTrackRecordingServiceStateChangedEvent(): %s",
                event.toString()));

        // Update the notification state depending on the event's state.
        switch (event.mState) {
            case SERVICE_STARTING:
                //NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.CONNECTING);
                NotificationHandler.closeNotification();
                break;
            case SERVICE_STARTED:
               // NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.CONNECTED);
                NotificationHandler.closeNotification();
                if (mWorkerSubscription != null)
                    mWorkerSubscription.unsubscribe();
                break;
            case SERVICE_STOPPING:
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.STOPPING);
                break;
            case SERVICE_STOPPED:
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.UNCONNECTED);
                scheduleDiscovery(REDISCOVERY_INTERVAL);
                break;
            case SERVICE_DEVICE_DISCOVERY_RUNNING:
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.DISCOVERING);
                break;
            case SERVICE_DEVICE_DISCOVERY_PENDING:
                break;
        }
    }


    @Subscribe
    public void onReceiveNewCarTypeSelectedEvent(NewCarTypeSelectedEvent event) {
        LOGGER.info(String.format("onReceiveNewCarTypeSelectedEvent(): %s", event.toString()));
        if (event.mCar == null) {
            updateNotificationState(ServiceStateForNotificationForNotification.NO_CAR_SELECTED);
        } else if (OBDConnectionService.CURRENT_SERVICE_STATE == BluetoothServiceState
                .SERVICE_STOPPED) {
            updateNotificationState(ServiceStateForNotificationForNotification.UNCONNECTED);
        }
    }


    private void updateNotificationState(ServiceStateForNotificationForNotification state) {
        if (OBDConnectionService.CURRENT_SERVICE_STATE == BluetoothServiceState.SERVICE_STOPPED) {
            if (mBluetoothHandler.getSelectedBluetoothDevice() == null) {
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.NO_OBD_SELECTED);
            } else if (mCarManager.getCar() == null) {
                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.NO_CAR_SELECTED);
            } else {
                ServiceStateForNotificationForNotification currentState = NotificationHandler.getRecordingState();
                if (currentState != ServiceStateForNotificationForNotification.DISCOVERING &&
                        state != ServiceStateForNotificationForNotification.DISCOVERING) {
                    if (mIsAutoconnect) {
                        scheduleDiscovery(REDISCOVERY_INTERVAL);
                    }
                }
                NotificationHandler.setRecordingState(state);
            }
        }
    }

    /**
     * Schedules the immediate discovery for the selected OBDII adapter.
     */
    private void scheduleDiscovery() {
        this.scheduleDiscovery(-1);
    }

    /**
     * Schedules the discovery for the selected OBDII adapter with a specific delay.
     *
     * @param delay time to wait before the scheduled action gets executes. A non-positive delay
     *              indicate an undelayed execution.
     */
    private void scheduleDiscovery(int delay) {
        // Unschedule all outstanding work.
        unscheduleDiscovery();

        // if autoconnect has been enabled and a car has been selected, then schedule a new
        // discovery.
        if (mIsAutoconnect && hasCarSelected && this.mBluetoothHandler.isBluetoothEnabled()) {
            // Reschedule a fresh discovery.
            mWorkerSubscription = mWorkerThread.schedule(() -> {
                startDiscoveryForSelectedDevice();
            }, delay, TimeUnit.SECONDS);

            LOGGER.info("Discovery subscription has been scheduled -> [%s]", "" +
                    delay);
        }
    }

    /**
     * Stops the current discovery and/or the scheduled upcoming discovery.
     */
    private void unscheduleDiscovery() {
        if (mWorkerSubscription != null) {
            if (mBluetoothHandler.isDiscovering())
                mBluetoothHandler.stopBluetoothDeviceDiscovery();
            mWorkerSubscription.unsubscribe();
        }
    }

    //    /**
    //     * Establishes a binding to the OBDConnectionService if the remoteService is running.
    //     */
    //    private void bindOBDConnectionService() {
    //        if (ServiceUtils.isServiceRunning(getApplicationContext(),
    //                // Defines callbacks for the remoteService binding, passed to bindService()
    //                OBDConnectionService.class)) {
    //
    //            // Bind to OBDConnectionService
    //            Intent intent = new Intent(this, OBDConnectionService.class);
    //            bindService(intent, mOBDConnectionServiceCon,
    //                    Context.BIND_ABOVE_CLIENT);
    //        }
    //    }

    /**
     * Starts the OBDConnectionService if it is not already running. This also initiates the
     * start of a new track.
     */
    private void startOBDConnectionService() {
        if (!ServiceUtils
                .isServiceRunning(getApplicationContext(), OBDConnectionService.class)) {

            // Start the OBD Connection Service
            getApplicationContext().startService(
                    new Intent(getApplicationContext(), OBDConnectionService.class));

            // binds the OBD Connection Service.
            //            bindOBDConnectionService();
        }
    }

    /**
     * Removes a binding to the OBDConnection remoteService if the remoteService is running and
     * this remoteService
     * is bound.
     */
    //    private void unbindOBDConnectionService() {
    //        // Only when the remoteService is running and this remoteService is bounded to that
    //        // remoteService.
    //        if (mOBDConnectionService != null && ServiceUtils
    //                .isServiceRunning(getApplicationContext(), OBDConnectionService.class)) {
    //
    //            // Unbinds the OBD connection remoteService.
    //            unbindService(mOBDConnectionServiceCon);
    //        }
    //    }

    /**
     * Starts the discovery for the selected OBDII device. If the device has been found then the
     * device either auto-connects or updates the notification accordinlgy depending on the
     * individual settings.
     */
    private void startDiscoveryForSelectedDevice() {
        BluetoothDevice device = mBluetoothHandler.getSelectedBluetoothDevice();
        if (device == null) {
            mMainThreadWorker.schedule(() -> Toast.makeText(getApplicationContext(), "No paired " +
                    "bluetooth device selected", Toast.LENGTH_SHORT).show());
        } else {
            // If the remoteService is already discovering, then skip the current discovery and
            // unsubscribe on the corresponding subscription.
            if (mDiscoverySubscription != null) {
                mBluetoothHandler.stopBluetoothDeviceDiscovery();
                mDiscoverySubscription.unsubscribe();
                mDiscoverySubscription = null;
            }

            // Initialize a new discovery of the bluetooth.
            mDiscoverySubscription = mBluetoothHandler
                    .startBluetoothDiscoveryForSingleDevice(device)
                    .subscribe(new SafeSubscriber<BluetoothDevice>(new Subscriber<BluetoothDevice>() {

                        private boolean isFound = false;

                        @Override
                        public void onStart() {
                            LOGGER.info("Device Discovery started...");
                            NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.DISCOVERING);
                        }

                        @Override
                        public void onNext(BluetoothDevice device) {
                            LOGGER.info("Device Discovered...");

                            // The device has been successfully discovered. Set the flag to true
                            // and stop the discovery process.
                            isFound = true;
                            mBluetoothHandler.stopBluetoothDeviceDiscovery();

                            LOGGER.info("Try to start the connection to " +
                                    "the selected OBD adapter.");

                            getApplicationContext().startService(
                                    new Intent(getApplicationContext(), OBDConnectionService
                                            .class));

                           /* // Depending on the individual settings either start the background
                            // remoteService or update the notification state.
                            if (mIsAutoconnect) {
                                LOGGER.info("[Autoconnect is on]. Try to start the connection to " +
                                        "the selected OBD adapter.");

                                getApplicationContext().startService(
                                        new Intent(getApplicationContext(), OBDConnectionService
                                                .class));
                            } else {
                                LOGGER.info("[Autoconnect is off]. Update the notification.");

                                // If the device has been successful discovered, set the
                                // notification state to OBD_FOUND and stop the bluetooth discovery.
                                // TODO
//                                NotificationHandler.setRecordingState();
//                                mNotificationHandler.setNotificationState(AutomaticOBDTrackService
// .this,
//                                        NotificationHandler.NotificationState.OBD_FOUND);
                                scheduleDiscovery(REDISCOVERY_INTERVAL);
                            }*/
                        }

                        @Override
                        public void onCompleted() {
                            LOGGER.info("Device Discovery finished...");

                            // If the device to search for has not been found during the
                            // discovery period, then set back the notification state to
                            // unconnected.
                            if (!isFound) {
                                LOGGER.info("The selected OBDII device has not been found. " +
                                        "Schedule a new discovery in " + mDiscoveryInterval + " " +
                                        "seconds.");
                                NotificationHandler.setRecordingState(ServiceStateForNotificationForNotification.UNCONNECTED);

                                // Reschedule the discovery if it is enabled.
                                if (mIsAutoconnect) {
                                    scheduleDiscovery(mDiscoveryInterval);
                                }
                            }

                            if (mDiscoverySubscription != null) {
                                mDiscoverySubscription.unsubscribe();
                                mDiscoverySubscription = null;
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            LOGGER.error("Error while discovering for the selected Bluetooth " +
                                    "devices", e);
                        }
                    }) {
                        @Override
                        public void onStart() {
                            getActual().onStart();
                        }
                    });
        }
    }
}
