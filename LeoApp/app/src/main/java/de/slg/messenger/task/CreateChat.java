package de.slg.messenger.task;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import de.slg.leoapp.Start;
import de.slg.leoapp.service.ReceiveService;
import de.slg.leoapp.utility.Utils;
import de.slg.messenger.activity.ChatActivity;
import de.slg.messenger.utility.Chat;

public class CreateChat extends AsyncTask<Integer, Void, Intent> {
    private final String         cname;
    private final Activity       activity;
    private       ReceiveService service;
    private       int            cid;

    public CreateChat(Activity activity, String cname) {
        this.activity = activity;
        this.cname = cname;
        this.cid = -1;
        this.service = Utils.getController().getReceiveService();

        if (service == null) {
            Start.startReceiveService();
            this.service = Utils.getController().getReceiveService();
        }
    }

    @Override
    protected Intent doInBackground(Integer... params) {
        sendChat();

        if (cid != -1) {
            sendAssoziation(Utils.getUserID());
            for (Integer i : params) {
                sendAssoziation(i);
            }
        }

        return new Intent(activity, ChatActivity.class)
                .putExtra("cid", cid)
                .putExtra("cname", cname)
                .putExtra("ctype", Chat.ChatType.GROUP.toString());
    }

    private void sendChat() {
        try {
            URLConnection connection = new URL(generateURL(cname))
                    .openConnection();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream(), "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String        l;
            while ((l = reader.readLine()) != null)
                builder.append(l);
            reader.close();
            Utils.logDebug(builder);

            cid = Integer.parseInt(builder.toString());

            Utils.getController().getMessengerDatabase().insertChat(new Chat(cid, cname, Chat.ChatType.GROUP));
        } catch (IOException e) {
            Utils.logError(e);
        }
    }

    private void sendAssoziation(int uid) {
        service.startIfNotRunning();

        service.send("a+ " + cid + ';' + uid);
    }

    private String generateURL(String cname) throws UnsupportedEncodingException {
        return Utils.BASE_URL_PHP + "messenger/addChat.php?cname=" + URLEncoder.encode(cname, "UTF-8") + "&ctype=" + Chat.ChatType.GROUP.toString().toLowerCase();
    }

    @Override
    protected void onPostExecute(Intent intent) {
        activity.startActivity(intent);
        activity.finish();
    }
}
