package de.slg.stimmungsbarometer;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

import de.slg.leoapp.R;

public abstract class Utils {
    public static boolean showVoteOnStartup() {
        if (getLastVote().equals(getCurrentDate()))
            return false;
        boolean b = de.slg.leoapp.Utils.isVerified() && de.slg.leoapp.Utils.checkNetwork();
        if (b) {
            AsyncTask<Void, Void, Boolean> t = new AsyncTask<Void, Void, Boolean>() {
                private boolean b;

                @Override
                protected Boolean doInBackground(Void... params) {
                    try {
                        HttpsURLConnection connection = (HttpsURLConnection)
                                new URL(de.slg.leoapp.Utils.BASE_URL_PHP + "stimmungsbarometer/voted.php?key=5453&userid=" + de.slg.leoapp.Utils.getUserID())
                                        .openConnection();
                        connection.setRequestProperty("Authorization", de.slg.leoapp.Utils.authorization);
                        BufferedReader reader =
                                new BufferedReader(
                                        new InputStreamReader(
                                                connection.getInputStream(), "UTF-8"));
                        b = !Boolean.parseBoolean(reader.readLine());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return b;
                }
            };
            t.execute();
            try {
                return t.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static String getCurrentDate() {
        return new SimpleDateFormat("dd.MM").format(new Date());
    }

    public static int getCurrentMoodRessource() {
        int i = de.slg.leoapp.Utils.getController().getPreferences().getInt("pref_key_general_vote_id", -1);
        switch (i) {
            case 1:
                return R.drawable.ic_sentiment_very_satisfied_white_24px;
            case 2:
                return R.drawable.ic_sentiment_satisfied_white_24px;
            case 3:
                return R.drawable.ic_sentiment_neutral_white_24px;
            case 4:
                return R.drawable.ic_sentiment_dissatisfied_white_24px;
            case 5:
                return R.drawable.ic_sentiment_very_dissatisfied_white_24px;
            default:
                return R.drawable.ic_account_circle_black_24dp;
        }
    }

    private static String getLastVote() {
        return de.slg.leoapp.Utils.getController().getPreferences().getString("pref_key_general_last_vote", "00.00");
    }

    public static void setLastVote(int vote) {
        de.slg.leoapp.Utils.getController().getPreferences().edit()
                .putString("pref_key_general_last_vote", getCurrentDate())
                .putInt("pref_key_general_vote_id", vote)
                .apply();
    }
}