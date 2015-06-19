package org.envirocar.app.injection;


import android.app.Activity;
import android.app.Fragment;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.squareup.otto.Bus;

import java.util.List;

import javax.inject.Inject;

import dagger.ObjectGraph;


/**
 * @author dewall
 */
public abstract class BaseInjectorFragment extends Fragment implements Injector, InjectionModuleProvider {
    private static final String TAG = BaseInjectorFragment.class.getSimpleName();
//    private static final LOGGER =


    /**
     * The event bus allows publish-subscribe-style communication. It dispatches
     * events to subscribed listeners, and provides ways for listeners to
     * register themselves.
     */
    @Inject
    protected Bus mBus;

    /**
     * A graph of objects linked by their dependencies. This class is used for
     * dependency injection. (see Dagger library)
     */
    private ObjectGraph mObjectGraph;

    /**
     * Flag that indicates that the fragment is already attached and the object
     * graph was initialized.
     */
    private boolean mAlreadyAttached;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Check if it is the first time where this fragment was attached to the
        // activity. If false, the ObjectGraph will be extended by fragment
        // specific modules and the dependencies will be injected.
        if (!mAlreadyAttached || mObjectGraph == null) {
            ObjectGraph objectGraph = ((Injector) getActivity()).getObjectGraph();
            List<Object> moduleList = getInjectionModules();
            if (moduleList != null) {
                objectGraph = objectGraph.plus(moduleList.toArray());
            }
            this.mObjectGraph = objectGraph;

            // Inject ourselves.
            injectObjects(this);
            mAlreadyAttached = true;

            Preconditions.checkState(mBus != null, "Bus has to be injected before "
                    + "registering the providers and subscribers.");

            // Register ourselves on the bus
            mBus.register(this);
        }
    }

    /**
     * Gets the object graph of the implemented class.
     *
     * @return the object graph
     */
    @Override
    public final ObjectGraph getObjectGraph() {
        return mObjectGraph;
    }

    /**
     * Injects a target object using this object's object graph.
     *
     * @param instance the target object
     */
    @Override
    public void injectObjects(Object instance) {
        Preconditions.checkState(mObjectGraph != null,
                "ObjectGraph must be initialized before injecting objects.");
        mObjectGraph.inject(instance);
        Log.d(TAG, "Objects successfully injected into "
                + instance.getClass().getSimpleName());
    }

    /**
     * Returns a list of modules to be added to the ObjectGraph. Originally this
     * function returns null, but can be extended by subclasses by own modules.
     *
     * @return a list of modules
     */
    @Override
    public List<Object> getInjectionModules() {
        return null;
    }
}