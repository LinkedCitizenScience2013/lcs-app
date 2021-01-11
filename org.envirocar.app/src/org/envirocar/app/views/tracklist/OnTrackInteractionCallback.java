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
package org.envirocar.app.views.tracklist;

import android.view.View;

import org.envirocar.core.entity.Track;


/**
 * @author dewall
 */
interface OnTrackInteractionCallback {

    /**
     * @param track the track to show the details for.
     */
    void onTrackDetailsClicked(Track track, View transitionView);

    /**
     * @param track the track to delete.
     */
    void onDeleteTrackClicked(Track track);

    /**
     * @param track the track to upload.
     */
    void onUploadTrackClicked(Track track);

    /**
     * @param track the track to export.
     */
    void onExportTrackClicked(Track track);

    /**
     * @param track the track to download.
     */
    void onDownloadTrackClicked(Track track, AbstractTrackListCardAdapter.TrackCardViewHolder holder);

    void onLongPressedTrack(Track track);
}
