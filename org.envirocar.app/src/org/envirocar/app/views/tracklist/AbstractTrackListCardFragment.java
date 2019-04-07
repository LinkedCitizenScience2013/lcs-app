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
package org.envirocar.app.views.tracklist;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.envirocar.app.BuildConfig;
import org.envirocar.app.R;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.TermsOfUseManager;
import org.envirocar.app.handler.TrackDAOHandler;
import org.envirocar.app.handler.TrackUploadHandler;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.app.views.utils.DialogUtils;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.serializer.TrackSerializer;
import org.envirocar.storage.EnviroCarDB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public abstract class AbstractTrackListCardFragment<E extends RecyclerView.Adapter>
        extends Fragment {
    private static final Logger LOG = Logger.getLogger(AbstractTrackListCardFragment.class);

    @Inject
    protected UserHandler mUserManager;
    @Inject
    protected EnviroCarDB mEnvirocarDB;
    @Inject
    protected TermsOfUseManager mTermsOfUseManager;
    @Inject
    protected DAOProvider mDAOProvider;
    @Inject
    protected TrackDAOHandler mTrackDAOHandler;
    @Inject
    protected TrackUploadHandler mTrackUploadHandler;

    @BindView(R.id.fragment_tracklist_info)
    protected View infoView;
    @BindView(R.id.fragment_tracklist_info_img)
    protected ImageView infoImg;
    @BindView(R.id.fragment_tracklist_info_text)
    protected TextView infoText;
    @BindView(R.id.fragment_tracklist_info_subtext)
    protected TextView infoSubtext;

    @BindView(R.id.fragment_tracklist_progress_view)
    protected View mProgressView;
    @BindView(R.id.fragment_tracklist_progress_text)
    protected TextView mProgressText;
    @BindView(R.id.fragment_tracklist_progress_progressBar)
    protected ProgressBar mProgressBar;
    @BindView(R.id.fragment_tracklist_recycler_view)
    protected RecyclerView mRecyclerView;
    @BindView(R.id.fragment_tracklist_fab)
    protected FloatingActionButton mFAB;

    protected E mRecyclerViewAdapter;
    protected RecyclerView.LayoutManager mRecylcerViewLayoutManager;

    protected boolean tracksLoaded = false;
    protected final List<Track> mTrackList = Collections.synchronizedList(new ArrayList<>());

    // Different workers for main and background threads.
    protected Scheduler.Worker mMainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    protected Scheduler.Worker mBackgroundWorker = Schedulers.computation().createWorker();

    protected final Object attachingActivityLock = new Object();
    protected boolean isAttached = false;

    private int REQUEST_STORAGE_PERMISSION_REQUEST_CODE = 109;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the view and inject the annotated view.
        View view = inflater.inflate(R.layout.fragment_tracklist, container, false);
        ButterKnife.bind(this, view);

        // Initiate the recyclerview
        mRecylcerViewLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mRecylcerViewLayoutManager);

        // setup the adapter for the recyclerview.
        mRecyclerViewAdapter = getRecyclerViewAdapter();
        mRecyclerView.setAdapter(mRecyclerViewAdapter);

        // notify the waiting thread that the activity has been attached.
        synchronized (attachingActivityLock) {
            isAttached = true;
            attachingActivityLock.notifyAll();
        }

        return view;
    }

    /**
     * @return
     */
    public abstract E getRecyclerViewAdapter();

    /**
     * This method is responsible for loading the track dataset into the cardlist.
     */
    protected abstract void loadDataset();

    protected void exportTrack(Track track) {

        try {
            if(checkStoragePermissions()){
                // Create an sharing intent.
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("application/json");
                //  Uri shareBody = Uri.fromFile(TrackSerializer.exportTrack(track).getFile());
                Uri shareBody = FileProvider.getUriForFile(
                        getActivity(),
                        getActivity().getApplicationContext()
                                .getPackageName() + ".provider", TrackSerializer.exportTrack(track).getFile());
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                        "EnviroCar Track " + track.getName());
                sharingIntent.putExtra(android.content.Intent.EXTRA_STREAM, shareBody);
                sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                // Wrap the intent with a chooser.
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
            }else{
                requestStoragePermissions();
            }

        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
            Snackbar.make(getView(),
                    R.string.general_error_please_report,
                    Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkStoragePermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            LOG.debug("Requesting Storage permission. Displaying permission rationale to provide additional context.");

            DialogUtils.createDefaultDialogBuilder(getContext(),
                    R.string.request_storage_permission_title,
                    R.drawable.others_settings,
                    R.string.permission_rationale_file)
                    .positiveText(R.string.ok)
                    .onPositive((dialog, which) -> {
                        // Request permission
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_STORAGE_PERMISSION_REQUEST_CODE);
                    })
                    .show();

        } else {
            LOG.info("Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION_REQUEST_CODE);
        }
    }



    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        LOG.info("onRequestPermissionResult");
        if(requestCode == REQUEST_STORAGE_PERMISSION_REQUEST_CODE){
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                LOG.info("User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LOG.info("Storage permission granted");
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, view -> {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        });
            }
        }
    }


    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                getActivity().findViewById(R.id.navigation),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Creates a Dialog for the deletion of a track. On a positive click, the track gets deleted.
     *
     * @param track the track to delete.
     */
    protected void createDeleteTrackDialog(Track track) {
        // Get the up to date reference of the current track.
        // Create a dialog that deletes on click on the positive button the track.
        final Track upToDateRef = mEnvirocarDB.getTrack(track.getTrackID())
                .toBlocking()
                .first();

        View contentView = getActivity().getLayoutInflater().inflate(
                R.layout.fragment_tracklist_delete_track_dialog, null, false);
        ((TextView) contentView.findViewById(
                R.id.fragment_tracklist_delete_track_dialog_trackname)).setText(track.getName());

        // Create a dialog that deletes on click on the positive button the track.
        DialogUtils.createDefaultDialogBuilder(getActivity(),
                R.string.trackviews_delete_track_dialog_headline,
                R.drawable.ic_delete_white_24dp,
                contentView)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive((materialDialog, dialogAction) ->
                        mBackgroundWorker.schedule(() -> {
                            // On a positive button click, then delete the track.
                            if (upToDateRef.isLocalTrack())
                                deleteLocalTrack(track);
                            else
                                deleteRemoteTrack(track);
                        }))
                .show();
    }

    protected void showText(int imgResource, int textResource, int subtextResource) {
        if (mTrackList.isEmpty()) {
            mMainThreadWorker.schedule(new Action0() {
                @Override
                public void call() {
                    infoImg.setImageResource(imgResource);
                    infoText.setText(textResource);
                    infoSubtext.setText(subtextResource);
                    ECAnimationUtils.animateShowView(getActivity(), infoView, R.anim.fade_in);
                }
            });
        }
    }

    protected void deleteRemoteTrack(Track track) {
        LOG.info("deleteRemoteTrack()");

        mEnvirocarDB.getTrack(track.getTrackID())
                .map(new Func1<Track, Boolean>() {
                    @Override
                    public Boolean call(Track upToDateRef) {
                        if (upToDateRef.isLocalTrack()) {
                            LOG.info("Track to delete is a local track");
                            return false;
                        }

                        try {
                            mTrackDAOHandler.deleteRemoteTrack(upToDateRef);
                            return true;
                        } catch (Exception e) {
                            throw OnErrorThrowable.from(e);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getDeleteTrackSubscriber(track));
    }

    /**
     * Deletes a local track in the database.
     *
     * @param track
     */
    protected void deleteLocalTrack(final Track track) {
        // Get the up to date reference of the current track and delete it
        Observable.defer(() -> mEnvirocarDB.getTrack(track.getTrackID()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(upToDateRef -> {
                    // If the track is a local track, then delete and return whether it was
                    // successful.
                    return upToDateRef.isLocalTrack() &&
                            mTrackDAOHandler.deleteLocalTrack(upToDateRef.getTrackID());
                })
                .subscribe(getDeleteTrackSubscriber(track));
    }

    protected Subscriber<Boolean> getDeleteTrackSubscriber(final Track track) {
        return new Subscriber<Boolean>() {
            @Override
            public void onStart() {
                LOG.info(String.format("onStart() delete track -> [%s]", track.getName()));
                showProgressView(getString(R.string.track_list_deleting_track));
            }

            @Override
            public void onCompleted() {
                LOG.info(String.format("onCompleted() delete track -> [%s]",
                        track.getName()));
            }

            @Override
            public void onError(Throwable e) {
                LOG.error(String.format("onError() delete track -> [%s]",
                        track.getName()), e);

                if (e instanceof UnauthorizedException) {
                    LOG.error("The logged in user is not authorized to do that.", e);
                    showSnackbar(R.string.track_list_deleting_track_unauthorized);
                } else if (e instanceof NotConnectedException) {
                    LOG.error("Not connected", e);
                    showSnackbar(R.string.track_list_communication_error);
                } else {
                    showSnackbar(String.format(
                            getString(R.string.track_list_delete_track_error_template),
                            track.getName()));
                }

                hideProgressView();
            }

            @Override
            public void onNext(Boolean success) {
                LOG.info("onNext() -> " + track.getName());
                if (success) {
                    LOG.info("deleteLocalTrack: Successfully deleted track with" +
                            " id=" + track.getTrackID());

                    mTrackList.remove(track);
                    mRecyclerViewAdapter.notifyDataSetChanged();
                    showSnackbar(String.format(getString(R.string
                            .track_list_delete_track_success_template), track.getName()));
                    hideProgressView();
                } else {
                    showSnackbar(String.format(
                            getString(R.string.track_list_delete_track_error_template),
                            track.getName()));
                }
            }
        };
    }

    protected void showSnackbar(final int message) {
        mMainThreadWorker.schedule(() -> {
            if (getView() != null) {
                Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    protected void showSnackbar(final String message) {
        mMainThreadWorker.schedule(() -> {
            if (getView() != null) {
                Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    protected void showProgressView(String text) {
        mMainThreadWorker.schedule(() -> {
            mProgressView.setVisibility(View.VISIBLE);
            mProgressText.setText(text);
        });
    }

    protected void hideProgressView() {
        ECAnimationUtils.animateHideView(getActivity(), mProgressView, R.anim.fade_out);
    }
}
