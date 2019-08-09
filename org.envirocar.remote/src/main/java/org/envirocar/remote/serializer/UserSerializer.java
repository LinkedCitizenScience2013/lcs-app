/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.remote.serializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserImpl;

import java.lang.reflect.Type;

/**
 * @author dewall
 */
public class UserSerializer implements JsonSerializer<User>, JsonDeserializer<User> {

    @Override
    public JsonElement serialize(User src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject user = new JsonObject();
        if (src.getUsername() != null) {
            user.addProperty(User.KEY_USER_NAME, src.getUsername());
        }

        if (src.getToken() != null) {
            user.addProperty(User.KEY_USER_TOKEN, src.getToken());
        }

        if (src.getFirstName() != null) {
            user.addProperty(User.KEY_USER_FIRST_NAME, src.getFirstName());
        }

        if (src.getLastName() != null) {
            user.addProperty(User.KEY_USER_LAST_NAME, src.getLastName());
        }

        if (src.getMail() != null) {
            user.addProperty(User.KEY_USER_MAIL, src.getMail());
        }

        if (src.getTermsOfUseVersion() != null) {
            user.addProperty(User.KEY_USER_TOU_ACCEPTED, src.getTermsOfUseVersion());
        }

        if (src.getPrivacyStatementVersion() != null) {
            user.addProperty(User.KEY_USER_PRIVACY_STATEMENT_ACCEPTED, src.getPrivacyStatementVersion());
        }

        return user;
    }

    @Override
    public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject userObject = json.getAsJsonObject();
        String username = userObject.get(User.KEY_USER_NAME).getAsString();
        String mail = userObject.get(User.KEY_USER_MAIL).getAsString();

        JsonElement firstNameElement = userObject.get(User.KEY_USER_FIRST_NAME);
        JsonElement lastNameElement = userObject.get(User.KEY_USER_LAST_NAME);

        User user = new UserImpl();
        user.setUsername(username);
        user.setMail(mail);
        if (firstNameElement != null) {
            user.setFirstName(firstNameElement.getAsString());
        }
        if (lastNameElement != null) {
            user.setLastName(lastNameElement.getAsString());
        }

        if (userObject.has(User.KEY_USER_TOU_ACCEPTED)) {
            String touVersion = userObject.get(User.KEY_USER_TOU_ACCEPTED).getAsString();
            user.setTermsOfUseVersion(touVersion);
        }

        if (userObject.has(User.KEY_USER_PRIVACY_STATEMENT_ACCEPTED)) {
            String psVersion = userObject.get(User.KEY_USER_TOU_ACCEPTED).getAsString();
            user.setPrivacyStatementVersion(psVersion);
        }

        return user;
    }
}
