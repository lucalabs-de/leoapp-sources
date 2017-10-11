package de.slg.leoapp;

import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import de.slg.messenger.Assoziation;
import de.slg.messenger.Chat;
import de.slg.messenger.Message;
import de.slg.messenger.Verschluesseln;
import de.slg.schwarzes_brett.SQLiteConnector;

public class ReceiveService extends Service {
    public  boolean receiveNews;
    private boolean running, socketRunning, idle;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Utils.getController().setContext(getApplicationContext());
        Utils.getController().registerReceiveService(this);

        running = true;
        socketRunning = false;
        receiveNews = false;
        idle = false;

        new ReceiveThread().start();
        new QueueThread().start();

        Log.i("ReceiveService", "Service (re)started!");
        return START_STICKY;
    }

    @Override
    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        running = false;
        Utils.getController().registerReceiveService(null);
        Log.i("ReceiveService", "Service stopped!");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e("ReceiveService", "ReceiveService removed");
        Utils.getController().getMessengerDataBase().close();
        Utils.getController().registerReceiveService(null);
        super.onTaskRemoved(rootIntent);
    }

    public void notifyQueuedMessages() {
        new QueueThread().start();
    }

    private class ReceiveThread extends Thread {
        @Override
        public void run() {
            Looper.prepare();

            while (running) {
                try {
                    if (Utils.checkNetwork()) {
                        if (!socketRunning)
                            new MessengerSocket().start();

                        new ReceiveNews().execute();
                    }

                    while (idle) {
                        sleep(1);
                    }

                    for (int i = 0; i < 2400 && running && !receiveNews; i++)
                        sleep(25);

                    receiveNews = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    private class ReceiveNews extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            idle = true;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (Utils.checkNetwork()) {
                try {
                    HttpsURLConnection connection = (HttpsURLConnection)
                            new URL(Utils.BASE_URL_PHP + "schwarzesBrett/meldungen.php")
                                    .openConnection();
                    connection.setRequestProperty("Authorization", Utils.authorization);
                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            connection.getInputStream(), "UTF-8"));
                    StringBuilder builder = new StringBuilder();
                    String        line;
                    while ((line = reader.readLine()) != null)
                        builder.append(line)
                                .append(System.getProperty("line.separator"));
                    reader.close();

                    SQLiteConnector sqLiteConnector = new SQLiteConnector(getApplicationContext());
                    SQLiteDatabase  sqLiteDatabase  = sqLiteConnector.getWritableDatabase();

                    sqLiteDatabase.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + SQLiteConnector.TABLE_EINTRAEGE + "'");
                    sqLiteDatabase.delete(SQLiteConnector.TABLE_EINTRAEGE, null, null);

                    String[] result = builder.toString().split("_next_");
                    for (String s : result) {
                        String[] res = s.split(";");
                        if (res.length == 8) {
                            sqLiteDatabase.insert(SQLiteConnector.TABLE_EINTRAEGE, null,
                                    sqLiteConnector.getContentValues(
                                            res[0],
                                            res[1],
                                            res[2],
                                            Long.parseLong(res[3] + "000"),
                                            Long.parseLong(res[4] + "000"),
                                            Integer.parseInt(res[5]),
                                            Integer.parseInt(res[6]),
                                            res[7]
                                    )
                            );
                        }
                    }
                    sqLiteDatabase.close();
                    sqLiteConnector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (Utils.getController().getSchwarzesBrettActivity() != null)
                Utils.getController().getSchwarzesBrettActivity().refreshUI();

            idle = false;
        }
    }

    private class MessengerSocket extends Thread {
        @Override
        public void run() {
            socketRunning = true;
            try {
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        new URL(Utils.URL_TOMCAT + "?uid=" + Utils.getUserID() + "&mdate=" + Utils.getController().getMessengerDataBase().getLatestMessage())
                                                .openConnection()
                                                .getInputStream(), "UTF-8"));

                StringBuilder builder = new StringBuilder();
                for (String line = reader.readLine(); running && line != null; line = reader.readLine()) {
                    builder.append(line)
                            .append(System.getProperty("line.separator"));
                    if (line.endsWith("_ next _")) {
                        String   s     = builder.toString();
                        String[] parts = s.substring(1, s.indexOf("_ next _")).split("_ ; _");

                        if (s.startsWith("m") && parts.length == 6) {
                            int    mid   = Integer.parseInt(parts[0]);
                            String mtext = Verschluesseln.decrypt(parts[1], Verschluesseln.decryptKey(parts[2])).replace("_  ;  _", "_ ; _").replace("_  next  _", "_ next _");
                            long   mdate = Long.parseLong(parts[3] + "000");
                            int    cid   = Integer.parseInt(parts[4]);
                            int    uid   = Integer.parseInt(parts[5]);

                            Utils.getController().getMessengerDataBase().insertMessage(new Message(mid, mtext, mdate, cid, uid));
                        } else if (s.startsWith("c") && parts.length == 3) {
                            int           cid   = Integer.parseInt(parts[0]);
                            String        cname = parts[1].replace("_  ;  _", "_ ; _").replace("_  next  _", "_ next _");
                            Chat.ChatType ctype = Chat.ChatType.valueOf(parts[2].toUpperCase());

                            Utils.getController().getMessengerDataBase().insertChat(new Chat(cid, cname, ctype));
                        } else if (s.startsWith("u") && parts.length == 5) {
                            int    uid          = Integer.parseInt(parts[0]);
                            String uname        = parts[1].replace("_  ;  _", "_ ; _").replace("_  next  _", "_ next _");
                            String ustufe       = parts[2];
                            int    upermission  = Integer.parseInt(parts[3]);
                            String udefaultname = parts[4];

                            Utils.getController().getMessengerDataBase().insertUser(new User(uid, uname, ustufe, upermission, udefaultname));
                        } else if (s.startsWith("a")) {
                            assoziationen();
                        } else if (s.startsWith("-")) {
                            Log.e("SocketError", s);
                        }

                        builder.delete(0, builder.length());

                        if (Utils.getController().getMessengerActivity() != null)
                            Utils.getController().getMessengerActivity().notifyUpdate();
                    }
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            socketRunning = false;
        }

        private void assoziationen() {
            try {
                HttpsURLConnection connection = (HttpsURLConnection)
                        new URL(Utils.BASE_URL_PHP + "messenger/getAssoziationen.php?key=5453&userid=" + Utils.getUserID())
                                .openConnection();
                connection.setRequestProperty("Authorization", Utils.authorization);
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream(), "UTF-8"));

                StringBuilder builder = new StringBuilder();
                String        l;
                while ((l = reader.readLine()) != null) {
                    builder.append(l);
                }
                reader.close();

                if (builder.toString().startsWith("-")) {
                    throw new IOException(builder.toString());
                }

                String[]          result = builder.toString().split(";");
                List<Assoziation> list   = new List<>();
                for (String s : result) {
                    String[] current = s.split(",");
                    if (current.length == 2) {
                        list.append(new Assoziation(Integer.parseInt(current[0]), Integer.parseInt(current[1])));
                    }
                }

                Utils.getController().getMessengerDataBase().insertAssoziationen(list);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class QueueThread extends Thread {
        @Override
        public void run() {
            while (Utils.getController().getMessengerDataBase().hasQueuedMessages())
                if (Utils.checkNetwork())
                    new SendMessages().execute();
        }
    }

    private class SendMessages extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Message[] array = Utils.getController().getMessengerDataBase().getQueuedMessages();
            for (Message m : array) {
                if (Utils.checkNetwork()) {
                    try {
                        HttpsURLConnection connection = (HttpsURLConnection)
                                new URL(generateURL(m.mtext, m.cid))
                                        .openConnection();
                        connection.setRequestProperty("Authorization", Utils.authorization);
                        BufferedReader reader =
                                new BufferedReader(
                                        new InputStreamReader(
                                                connection.getInputStream(), "UTF-8"));
                        while (reader.readLine() != null)
                            ;
                        reader.close();
                        Utils.getController().getMessengerDataBase().dequeueMessage(m.mid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        private String generateURL(String message, int cid) {
            return Utils.BASE_URL_PHP + "messenger/addMessage.php?key=5453&userid=" + Utils.getUserID() + "&message=" + message.replace(" ", "%20").replace(System.getProperty("line.separator"), "%0A") + "&chatid=" + cid;
        }
    }
}