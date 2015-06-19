package org.envirocar.app.injection.module;

import android.app.Application;
import android.content.Context;

import com.squareup.otto.Bus;

import org.envirocar.app.FeatureFlags;
import org.envirocar.app.NotificationHandler;
import org.envirocar.app.activity.DashboardFragment;
import org.envirocar.app.activity.ListTracksFragment;
import org.envirocar.app.activity.LogbookFragment;
import org.envirocar.app.activity.LoginFragment;
import org.envirocar.app.activity.RegisterFragment;
import org.envirocar.app.activity.SettingsActivity;
import org.envirocar.app.activity.preference.CarSelectionPreference;
import org.envirocar.app.application.CommandListener;
import org.envirocar.app.application.service.BackgroundService;
import org.envirocar.app.application.service.BackgroundServiceImpl;
import org.envirocar.app.fragments.SettingsFragment;
import org.envirocar.app.injection.InjectionForActivity;
import org.envirocar.app.injection.InjectionForApplication;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.services.SystemStartupService;
import org.envirocar.app.view.preferences.BluetoothPairingPreference;
import org.envirocar.app.application.CarManager;
import org.envirocar.app.application.TemporaryFileManager;
import org.envirocar.app.application.TermsOfUseManager;
import org.envirocar.app.application.UserManager;
import org.envirocar.app.bluetooth.service.BluetoothHandler;
import org.envirocar.app.dao.DAOProvider;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.storage.DbAdapter;
import org.envirocar.app.storage.DbAdapterImpl;
import org.envirocar.app.view.preferences.SelectBluetoothPreference;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A module for application-specific dependencies which require a Application-
 * {@link android.content.Context} or to create. This includes specific Bus-dependencies.
 *
 *
 * @author dewall
 */
@Module(
        injects = {
                TermsOfUseManager.class,
                CarManager.class,
                DashboardFragment.class,
                ListTracksFragment.class,
                LogbookFragment.class,
                LoginFragment.class,
                RegisterFragment.class,
                SettingsActivity.class,
                CarSelectionPreference.class,
                BluetoothPairingPreference.class,
                BluetoothHandler.class,
                SettingsFragment.class,
                SelectBluetoothPreference.class,
                TemporaryFileManager.class,
                SystemStartupService.class,
                NotificationHandler.class,
                BackgroundServiceImpl.class,
                CommandListener.class
        },
        library = true,
        complete = false
)
public class InjectionApplicationModule {
    private static final Logger LOGGER = Logger.getLogger(InjectionApplicationModule.class);

    private final Application mApplication;
    private final Context mAppContext;

    /**
     * Constructor.
     *
     * @param application   the current application.
     */
    public InjectionApplicationModule(Application application) {
        this.mApplication = application;
        this.mAppContext = application.getApplicationContext();
    }

    /**
     * Provides the Application of the App.
     *
     * @return the Application.
     */
    @Provides
    Application provideApplication() {
        return mApplication;
    }

    /**
     * Provides the Application Context.
     *
     * @return the context of the application.
     */
    @Provides
    @InjectionForApplication
    Context provideApplicationContext() {
        return mAppContext;
    }

    /**
     * Provides the Application Injector.
     *
     * @return the Injector of the application.
     */
    @Provides
    @Singleton
    Injector provideApplicationInjector() {
        return (Injector) mApplication;
    }

    /**
     * Provides the event bus for the application.
     *
     * @return the application event bus.
     */
    @Provides
    @Singleton
    Bus provideBus() {
        return new Bus();
    }

    /**
     * Provides the DAOProvider fot the application
     *
     * @return the DAOprovider of the application
     */
    @Provides
    @Singleton
    DAOProvider provideDAOProvider() {
        return new DAOProvider(mAppContext);
    }

    /**
     * Provides the UserManager of the application
     *
     * @return the UserManager of the application
     */
    @Provides
    @Singleton
    UserManager provideUserManager() {
        return new UserManager(mAppContext);
    }

    /**
     * Provides the FeatureFlags of the application
     *
     * @return the FeatureFlags of the application
     */
    @Provides
    @Singleton
    FeatureFlags provideFeatureFlagsManager() {
        return new FeatureFlags(mAppContext);
    }

    /**
     * Provides the TemporaryFileManager of the application
     *
     * @return the TemporaryFileManager of the application.
     */
    @Provides
    @Singleton
    TemporaryFileManager provideTemporaryFileManager() {
        return new TemporaryFileManager(mAppContext);
    }

    /**
     * Provides the TemporaryFileManager of the application
     *
     * @return the TemporaryFileManager of the application.
     */
    @Provides
    @Singleton
    DbAdapter provideDBAdapter() {

        DbAdapter adapter = null;
        try {
            adapter = new DbAdapterImpl(mAppContext);
        } catch (InstantiationException e) {
            LOGGER.warn("Could not initalize the database layer. The app will probably work " +
                    "unstable.");
            LOGGER.warn(e.getMessage(), e);
        }

        return adapter;
    }

    /**
     * Provides the TermsOfUseManager of the application
     *
     * @return the TermsOfUseManager of the application.
     */
    @Provides
    @Singleton
    TermsOfUseManager provideTermsOfUseManager() {
        return new TermsOfUseManager(mAppContext);
    }

    /**
     * Provides the CarManager of the application
     *
     * @return the CarManager of the application.
     */
    @Provides
    @Singleton
    CarManager provideCarManager() {
        return new CarManager(mAppContext);
    }

    /**
     * Provides the CarManager of the application
     *
     * @return the CarManager of the application.
     */
    @Provides
    @Singleton
    NotificationHandler provideNotificationHandler() {
        return new NotificationHandler(mAppContext);
    }

    @Provides
    @Singleton
    BluetoothHandler provideBluetoothHandler(){
        return new BluetoothHandler(mAppContext);
    }

}