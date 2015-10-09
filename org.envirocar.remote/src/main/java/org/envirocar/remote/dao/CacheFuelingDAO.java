/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.remote.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.envirocar.core.dao.FuelingDAO;
import org.envirocar.core.entity.Announcement;
import org.envirocar.core.entity.Fueling;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.remote.serializer.FuelingSerializer;

import java.io.IOException;
import java.util.List;

import rx.Observable;
import rx.Subscriber;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class CacheFuelingDAO extends AbstractCacheDAO implements FuelingDAO {
    private static final String FUELING_CACHE = "fuelings";

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Fueling.class, new FuelingSerializer()).create();


    @Override
    public List<Fueling> getFuelings() throws DataRetrievalFailureException {
        try {
            return GSON.fromJson(readCache(FUELING_CACHE),
                    new TypeToken<List<Announcement>>() {
                    }.getType());
        } catch (IOException e) {
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public Observable<List<Fueling>> getFuelingsObservable() {
        return Observable.create(new Observable.OnSubscribe<List<Fueling>>() {
            @Override
            public void call(Subscriber<? super List<Fueling>> subscriber) {
                try {
                    subscriber.onNext(getFuelings());
                } catch (DataRetrievalFailureException e) {
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        });
    }

    @Override
    public void createFueling(Fueling fueling) throws NotConnectedException {
        throw new NotConnectedException("CacheFuelingDAO does not support saving.");
    }
}