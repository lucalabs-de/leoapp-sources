package de.slgdev.svBriefkasten.task;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import de.slgdev.leoapp.utility.ResponseCode;
import de.slgdev.leoapp.utility.Utils;

/**
 * Created by sili- on 23.06.2018.
 */

public class UpdateLikes extends AsyncTask<Object, Void, ResponseCode> {

    /**
     * Die Anzahl der Likes wird entsprechen der Auswahl des Benutzers in der Datenbank verändert
     */
    @Override
        protected ResponseCode doInBackground(Object... params) {
            if (!Utils.isNetworkAvailable())
                return ResponseCode.NO_CONNECTION;

            String topic = (String) params[0];
            String likes = (String) String.valueOf(params[1]);
            Utils.logDebug(topic + "Test");
            Utils.logDebug(likes + "Test");

            try {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                new URL(
                                        "http://www.moritz.liegmanns.de/leoapp_php/svBriefkasten/updateLikes.php?" +
                                                "topic=" + topic + "&" +
                                                "likes=" + likes
                                )
                                        .openConnection()
                                        .getInputStream(),
                                "UTF-8"
                        )
                );

                StringBuilder builder = new StringBuilder();
                String        line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                reader.close();

                if (builder.toString().startsWith("-"))
                    return ResponseCode.SERVER_FAILED;

            } catch (IOException e) {
                Utils.logError(e);
                return ResponseCode.SERVER_FAILED;
            }
            return ResponseCode.SUCCESS;
        }
    }
