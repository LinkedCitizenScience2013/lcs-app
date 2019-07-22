/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.trackdetails;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.widget.Toolbar;
import android.transition.Slide;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;

import org.envirocar.app.R;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.views.utils.MapUtils;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.exception.FuelConsumptionException;
import org.envirocar.core.exception.NoMeasurementsException;
import org.envirocar.core.exception.UnsupportedFuelTypeException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.trackprocessing.TrackStatisticsProvider;
import org.envirocar.core.utils.CarUtils;
import org.envirocar.storage.EnviroCarDB;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.schedulers.Schedulers;

/**
 * @author dewall
 */
public class TrackDetailsActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(TrackDetailsActivity.class);

    private static final String EXTRA_TRACKID = "org.envirocar.app.extraTrackID";
    private static final String EXTRA_TITLE = "org.envirocar.app.extraTitle";

    public static void navigate(Activity activity, View transition, int trackID) {
        Intent intent = new Intent(activity, TrackDetailsActivity.class);
        intent.putExtra(EXTRA_TRACKID, trackID);

        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(activity, transition, "transition_track_details");
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    private static final DecimalFormat DECIMAL_FORMATTER_TWO_DIGITS = new DecimalFormat("#.##");
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();
    private static final DateFormat UTC_DATE_FORMATTER = new SimpleDateFormat("HH:mm:ss", Locale
            .ENGLISH);

    static {
        UTC_DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    @Inject
    protected EnviroCarDB mEnvirocarDB;

    @BindView(R.id.activity_track_details_fab)
    protected FloatingActionButton mFAB;
    @BindView(R.id.activity_track_details_header_map)
    protected MapView mMapView;
    @BindView(R.id.activity_track_details_header_toolbar)
    protected Toolbar mToolbar;
    @BindView(R.id.activity_track_details_attr_description_value)
    protected TextView mDescriptionText;
    @BindView(R.id.track_details_attributes_header_duration)
    protected TextView mDurationText;
    @BindView(R.id.track_details_attributes_header_distance)
    protected TextView mDistanceText;
    @BindView(R.id.activity_track_details_attr_begin_value)
    protected TextView mBeginText;
    @BindView(R.id.activity_track_details_attr_end_value)
    protected TextView mEndText;
    @BindView(R.id.activity_track_details_attr_car_value)
    protected TextView mCarText;
    @BindView(R.id.activity_track_details_attr_emission_value)
    protected TextView mEmissionText;
    @BindView(R.id.activity_track_details_attr_consumption_value)
    protected TextView mConsumptionText;
    @BindView(R.id.activity_track_details_expanded_map)
    protected MapView mMapViewExpanded;
    @BindView(R.id.activity_track_details_expanded_map_container)
    protected RelativeLayout mMapViewExpandedContainer;
    @BindView(R.id.activity_track_details_appbar_layout)
    protected AppBarLayout mAppBarLayout;
    @BindView(R.id.activity_track_details_scrollview)
    protected NestedScrollView mNestedScrollView;
    @BindView(R.id.activity_track_details_expanded_map_cancel)
    protected ImageView mMapViewExpandedCancel;
    @BindView(R.id.activity_track_details_header_map_container)
    protected FrameLayout mMapViewContainer;
    @BindView(R.id.consumption_container)
    protected RelativeLayout mConsumptionContainer;
    @BindView(R.id.co2_container)
    protected RelativeLayout mCo2Container;
    @BindView(R.id.descriptionTv)
    protected TextView descriptionTv;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActivityTransition();
        setContentView(R.layout.activity_track_details_layout);

        // Inject all annotated views.
        ButterKnife.bind(this);

        supportPostponeEnterTransition();

        // Set the toolbar as default actionbar.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get the track to show.
        int mTrackID = getIntent().getIntExtra(EXTRA_TRACKID, -1);
        Track.TrackId trackid = new Track.TrackId(mTrackID);
        Track track = mEnvirocarDB.getTrack(trackid)
                .subscribeOn(Schedulers.io())
                .toBlocking()
                .first();

        String itemTitle = track.getName();
        CollapsingToolbarLayout collapsingToolbarLayout = findViewById
                (R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle(itemTitle);
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color
                .transparent));
        collapsingToolbarLayout.setStatusBarScrimColor(getResources().getColor(android.R.color
                .transparent));

        TextView title = findViewById(R.id.title);
        title.setText(itemTitle);

        // Initialize the mapview and the trackpath
        initMapView();
        initTrackPath(track);
        initViewValues(track);

        updateStatusBarColor();

        mFAB.setOnClickListener(v -> {
           TrackStatisticsActivity.createInstance(TrackDetailsActivity.this, mTrackID);
        });

        //closing the expanded mapview on "cancel" button clicked
        mMapViewExpandedCancel.setOnClickListener(v-> closeExpandedMapView());
        //expanding the expandable mapview on clicking the framelayout which is surrounded by header map view in collapsingtoolbarlayout
        mMapViewContainer.setOnClickListener(v-> expandMapView(track));
    }

    private void updateStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Set the statusbar to be transparent with a grey touch
            getWindow().setStatusBarColor(Color.parseColor("#3f3f3f3f"));
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        supportStartPostponedEnterTransition();
    }

    @Override
    public void onBackPressed() {
        //if the expandable mapview is expanded, then close it first
        if(mMapViewExpandedContainer != null && mMapViewExpandedContainer.getVisibility() == View.VISIBLE){
            closeExpandedMapView();
        }else{
            super.onBackPressed();
        }
    }

    /**
     * Initializes the activity enter and return transitions of the activity.
     */
    private void initActivityTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Create a new slide transition.
            Slide transition = new Slide();
            transition.excludeTarget(android.R.id.statusBarBackground, true);
            Window window = getWindow();

            // Set the created transition as enter and return transition.
            window.setEnterTransition(transition);
            window.setReturnTransition(transition);
        }
    }

    /**
     * Initializes the MapView, its base layers and settings.
     */
    private void initMapView() {
        // Set the openstreetmap tile layer as baselayer of the map.
        WebSourceTileLayer source = MapUtils.getOSMTileLayer();
        mMapView.setTileSource(source);
        mMapViewExpanded.setTileSource(source);

        // set the bounding box and min and max zoom level accordingly.
        BoundingBox box = source.getBoundingBox();
        mMapView.setScrollableAreaLimit(box);
        mMapView.setMinZoomLevel(mMapView.getTileProvider().getMinimumZoomLevel());
        mMapView.setMaxZoomLevel(mMapView.getTileProvider().getMaximumZoomLevel());
        mMapView.setCenter(mMapView.getTileProvider().getCenterCoordinate());
        mMapView.setZoom(0);
    }

    /**
     * @param track
     */
    private void initTrackPath(Track track) {
        // Configure the line representation.
        Paint linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.BLUE);
        linePaint.setStrokeWidth(5);

        TrackSpeedMapOverlay trackMapOverlay = new TrackSpeedMapOverlay(track);
        trackMapOverlay.setPaint(linePaint);

        // Adds the path overlay to the mapview.
        mMapView.getOverlays().add(trackMapOverlay);
        mMapViewExpanded.getOverlays().add(trackMapOverlay);

        final BoundingBox viewBbox = trackMapOverlay.getViewBoundingBox();
        final BoundingBox scrollableLimit = trackMapOverlay.getScrollableLimitBox();

        mMapView.setScrollableAreaLimit(scrollableLimit);
        mMapView.setConstraintRegionFit(true);
        mMapView.zoomToBoundingBox(viewBbox, true);
        mMapViewExpanded.zoomToBoundingBox(viewBbox, true);
    }

    //function which expands the mapview
    private void expandMapView(Track track){
        TrackSpeedMapOverlay trackMapOverlay = new TrackSpeedMapOverlay(track);
        final BoundingBox viewBbox = trackMapOverlay.getViewBoundingBox();
        mMapViewExpanded.zoomToBoundingBox(viewBbox, true);

        animateShowView(mMapViewExpandedContainer,R.anim.translate_slide_in_top_fragment);
        animateHideView(mAppBarLayout,R.anim.translate_slide_out_top_fragment);
        animateHideView(mNestedScrollView,R.anim.translate_slide_out_bottom);
        animateHideView(mFAB,R.anim.fade_out);
    }

    //function which closes the expanded mapview
    private void closeExpandedMapView(){
        animateHideView(mMapViewExpandedContainer,R.anim.translate_slide_out_top_fragment);
        animateShowView(mAppBarLayout,R.anim.translate_slide_in_top_fragment);
        animateShowView(mNestedScrollView,R.anim.translate_slide_in_bottom_fragment);
        animateShowView(mFAB,R.anim.fade_in);
    }

    //general function to animate the view and hide it
    private void animateHideView(View view, int animResource){
       Animation animation = AnimationUtils.loadAnimation(getApplicationContext(),animResource);
       view.startAnimation(animation);
       view.setVisibility(View.GONE);
    }

    //general function to animate and show the view
    private void animateShowView(View view, int animResource){
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(),animResource);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(animation);
    }

    private void initViewValues(Track track) {
        try {
            final String text = UTC_DATE_FORMATTER.format(new Date(
                    track.getDuration()));
            mDistanceText.setText(String.format("%s km",
                    DECIMAL_FORMATTER_TWO_DIGITS.format(((TrackStatisticsProvider) track)
                            .getDistanceOfTrack())));
            mDurationText.setText(text);

            mDescriptionText.setText(track.getDescription());
            mCarText.setText(CarUtils.carToStringWithLinebreak(track.getCar()));
            mBeginText.setText(DATE_FORMAT.format(new Date(track.getStartTime())));
            mEndText.setText(DATE_FORMAT.format(new Date(track.getEndTime())));

            // show consumption and emission either when the fuel type of the track's car is
            // gasoline or the beta setting has been enabled.
            if(!track.hasProperty(Measurement.PropertyKey.SPEED)){
                mConsumptionContainer.setVisibility(View.GONE);
                mCo2Container.setVisibility(View.GONE);
                descriptionTv.setText(R.string.gps_track_details);
            }
            else if (track.getCar().getFuelType() == Car.FuelType.GASOLINE ||
                    PreferencesHandler.isDieselConsumptionEnabled(this)) {
                mEmissionText.setText(DECIMAL_FORMATTER_TWO_DIGITS.format(
                        ((TrackStatisticsProvider) track).getGramsPerKm()) + " g/km");
                mConsumptionText.setText(
                        String.format("%s l/h\n%s l/100 km",
                                DECIMAL_FORMATTER_TWO_DIGITS.format(
                                        ((TrackStatisticsProvider) track)
                                                .getFuelConsumptionPerHour()),

                                DECIMAL_FORMATTER_TWO_DIGITS.format(
                                        ((TrackStatisticsProvider) track).getLiterPerHundredKm())));
            } else {
                mEmissionText.setText(R.string.track_list_details_diesel_not_supported);
                mConsumptionText.setText(R.string.track_list_details_diesel_not_supported);
                mEmissionText.setTextColor(Color.RED);
                mConsumptionText.setTextColor(Color.RED);
            }

        } catch (FuelConsumptionException e) {
            e.printStackTrace();
        } catch (NoMeasurementsException e) {
            e.printStackTrace();
        } catch (UnsupportedFuelTypeException e) {
            e.printStackTrace();
        }
    }
}
