package de.slg.essensbons.task;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import de.slg.leoapp.sqlite.SQLiteConnectorEssensbons;
import de.slg.leoapp.task.general.TaskStatusListener;
import de.slg.leoapp.utility.Utils;
import de.slg.leoapp.utility.datastructure.List;

public class QRReadTask extends AsyncTask<String, Void, Boolean> {
    private int orderedMenu;
    private SQLiteDatabase dbh;

    private final List<TaskStatusListener> listeners;


    public final AsyncTask<String, Void, Boolean> addListener(TaskStatusListener listener) {
        listeners.append(listener);
        return this;
    }

    protected final List<TaskStatusListener> getListeners() {
        return listeners;
    }

    public QRReadTask() {
        SQLiteConnectorEssensbons db = new SQLiteConnectorEssensbons(Utils.getContext());
        dbh = db.getWritableDatabase();

        listeners = new List<>();
    }

    @Override
    protected Boolean doInBackground(String... params) {

        if (checkValid(params[0])) {

            Utils.logDebug(params[0]);

            String[] parts = params[0].split("-");
            String day = parts[2].substring(0, 2);
            String month = parts[2].substring(2, 4);
            String year = parts[2].substring(4, 7);

            orderedMenu = Integer.parseInt(String.valueOf(params[0].charAt(8)));

            String[] projection = {
                    SQLiteConnectorEssensbons.SCAN_DATE,
            };

            String selection = SQLiteConnectorEssensbons.SCAN_DATE + " = ? AND " + SQLiteConnectorEssensbons.SCAN_CUSTOMERID + " = ?";
            String[] selectionArgs = {"2" + year + "-" + month + "-" + day, parts[0]};

            Cursor cursor = dbh.query(
                    SQLiteConnectorEssensbons.TABLE_SCAN,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );

            if (cursor.getCount() == 0) {
                cursor.close();
                ContentValues values = new ContentValues();
                values.put(SQLiteConnectorEssensbons.SCAN_CUSTOMERID, params[0].split("-")[0]);
                values.put(SQLiteConnectorEssensbons.SCAN_DATE, "2" + year + "-" + month + "-" + day);
                dbh.insert(SQLiteConnectorEssensbons.TABLE_SCAN, null, values);
                return true;
            } else {
                cursor.close();
                return false;
            }

        } else {
            return false;
        }

    }


    @SuppressLint("DefaultLocale")
    private boolean checkValid(String s) {
        String[] parts = s.split("-");

        Utils.logDebug("passed no test yet");

        if (parts.length != 4)
            return false;
        Utils.logDebug("passed module test");

        if (parts[1].length() != 2 || parts[1].charAt(0) != 'M' || (parts[1].charAt(1) != '1' && parts[1].charAt(1) != '2'))
            return false;
        Utils.logDebug("passed menu-format test");

        if (parts[2].length() != 7)
            return false;
        Utils.logDebug("passed date size test");

        try {
            int day = Integer.parseInt(parts[2].substring(0, 2));
            int month = Integer.parseInt(parts[2].substring(2, 4));
            if (day > 31 || day < 1)
                return false;
            if (month > 12 || month < 1)
                return false;
        } catch (NumberFormatException e) {
            return false;
        }
        Utils.logDebug("passed logic date test");

        String subsum = "" + parts[2].substring(0, 2) + "" + parts[2].substring(4);
        Utils.logDebug(subsum);

        try {
            int orderId = Integer.parseInt(parts[0]);
            int checksum = Integer.parseInt(subsum) + orderId;

            int mod = checksum % 97;
            int fin = 98 - mod;

            if (!String.format("%02d", fin).equals(parts[3]))
                return false;

        } catch (NumberFormatException e) {
            return false;
        }
        Utils.logDebug("passed checksum test");

        return true;
    }

    @SuppressLint({"SetTextI18n", "InflateParams"})
    @Override
    protected void onPostExecute(Boolean result) {
        dbh.close();

        for (TaskStatusListener listener : getListeners()) {
            listener.taskFinished(result, orderedMenu);
        }
    }
}