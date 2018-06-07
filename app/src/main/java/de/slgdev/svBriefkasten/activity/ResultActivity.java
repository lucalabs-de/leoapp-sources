package de.slgdev.svBriefkasten.activity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.slgdev.leoapp.R;
import de.slgdev.leoapp.sqlite.SQLiteConnectorSv;
import de.slgdev.svBriefkasten.task.SyncTopicTask;

public class ResultActivity extends AppCompatActivity {

    private ExpandableListView resultsELW;
    private ExpandableListAdapter listAdapter;
    private List<String> listDataHeader;
    private HashMap<String,List<String>> listHash;

    private static SQLiteConnectorSv sqLiteConnector;
    private static SQLiteDatabase sqLiteDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultsELW = (ExpandableListView) findViewById(R.id.result);
        if(sqLiteConnector==null)
            sqLiteConnector = new SQLiteConnectorSv(this);
        if(sqLiteDatabase==null)
            sqLiteDatabase = sqLiteConnector.getReadableDatabase();

        receive();

        initELW();

    }

    public void initELW(){
        Cursor cursor;
        //cursor = sqLiteDatabase.query(SQLiteConnectorSV.TABLE_LETTERBOX, new String[]{sqLiteConnector.LETTERBOX_TOPIC, sqLiteConnector.LETTERBOX_PROPOSAL1, sqLiteConnector.LETTERBOX_PROPOSAL2, sqLiteConnector.LETTERBOX_DateOfCreation, sqLiteConnector.LETTERBOX_CREATOR, sqLiteConnector.LETTERBOX_LIKES},null, null, null, null ,sqLiteConnector.LETTERBOX_LIKES + " DESC");
        cursor= sqLiteDatabase.rawQuery("SELECT * FROM " + sqLiteConnector.TABLE_LETTERBOX,null);

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            String topic = cursor.getString(0);
            String proposal1=cursor.getString(1);
            String proposal2=cursor.getString(2);

            startActivity(new Intent(getApplicationContext(),BriefkastenActivity.class));

            listDataHeader.add(topic);
            List<String> loesungen = new ArrayList<>();
            if (proposal1 != null && proposal1 != "")
                loesungen.add(proposal1);
            if (proposal2 != null && proposal2 != "")
                loesungen.add(proposal2);

            listHash.put(listDataHeader.get(listDataHeader.size()-1),loesungen);
        }
    }

    public void receive(){
        new SyncTopicTask().execute();
    }
}
