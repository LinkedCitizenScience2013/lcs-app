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
package org.envirocar.remote.serializer;


import org.envirocar.core.entity.PrivacyStatement;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class PrivacyStatementListDeserializer extends AbstractSimpleListSerde<PrivacyStatement> {

    /**
     * Constructor.
     *
     * @param rootKey     the root key in the json containing the list
     * @param entityClass the entity class to parse the json for
     */
    public PrivacyStatementListDeserializer(String rootKey, Class<PrivacyStatement> entityClass) {
        super(rootKey, entityClass);
    }
}
