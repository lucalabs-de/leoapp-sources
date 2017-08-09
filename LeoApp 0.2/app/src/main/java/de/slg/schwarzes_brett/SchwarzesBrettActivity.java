package de.slg.schwarzes_brett;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.slg.essensqr.WrapperQRActivity;
import de.slg.klausurplan.KlausurplanActivity;
import de.slg.leoapp.PreferenceActivity;
import de.slg.leoapp.R;
import de.slg.leoapp.Utils;
import de.slg.messenger.OverviewWrapper;
import de.slg.nachhilfe.NachhilfeboerseActivity;
import de.slg.startseite.MainActivity;
import de.slg.stimmungsbarometer.StimmungsbarometerActivity;
import de.slg.stundenplan.WrapperStundenplanActivity;
import de.slg.vertretung.WrapperSubstitutionActivity;

public class SchwarzesBrettActivity extends AppCompatActivity {
    private List<String> groupList;
    private List<String> childList;
    private Map<String, List<String>> schwarzesBrett;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schwarzesbrett);

        Utils.receiveNews();

        initToolbar();
        initNavigationView();
        initButton();
        initExpandableListView();
    }

    private void initToolbar() {
        Toolbar myToolbar = (Toolbar) findViewById(R.id.actionBarSchwarzesBrett);
        myToolbar.setTitleTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.white));
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle(R.string.title_news);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initNavigationView() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigationView);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        navigationView.getMenu().findItem(R.id.newsboard).setChecked(true);

        navigationView.getMenu().findItem(R.id.nachhilfe).setEnabled(Utils.isVerified());
        navigationView.getMenu().findItem(R.id.messenger).setEnabled(Utils.isVerified());
        navigationView.getMenu().findItem(R.id.klausurplan).setEnabled(Utils.isVerified());
        navigationView.getMenu().findItem(R.id.stundenplan).setEnabled(Utils.isVerified());

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                drawerLayout.closeDrawers();
                Intent i;
                switch (menuItem.getItemId()) {
                    case R.id.foodmarks:
                        i = new Intent(getApplicationContext(), WrapperQRActivity.class);
                        break;
                    case R.id.messenger:
                        i = new Intent(getApplicationContext(), OverviewWrapper.class);
                        break;
                    case R.id.newsboard:
                        return true;
                    case R.id.nachhilfe:
                        i = new Intent(getApplicationContext(), NachhilfeboerseActivity.class);
                        break;
                    case R.id.stundenplan:
                        i = new Intent(getApplicationContext(), WrapperStundenplanActivity.class);
                        break;
                    case R.id.barometer:
                        i = new Intent(getApplicationContext(), StimmungsbarometerActivity.class);
                        break;
                    case R.id.klausurplan:
                        i = new Intent(getApplicationContext(), KlausurplanActivity.class);
                        break;
                    case R.id.startseite:
                        i = null;
                        break;
                    case R.id.vertretung:
                        i = new Intent(getApplicationContext(), WrapperSubstitutionActivity.class);
                        break;
                    case R.id.settings:
                        i = new Intent(getApplicationContext(), PreferenceActivity.class);
                        break;
                    default:
                        i = new Intent(getApplicationContext(), MainActivity.class);
                        Toast.makeText(getApplicationContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
                if (i != null)
                    startActivity(i);
                finish();
                return true;
            }
        });
        TextView username = (TextView) navigationView.getHeaderView(0).findViewById(R.id.username);
        username.setText(Utils.getUserName());

        TextView grade = (TextView) navigationView.getHeaderView(0).findViewById(R.id.grade);
        grade.setText(Utils.getUserStufe());
        ImageView mood = (ImageView) navigationView.getHeaderView(0).findViewById(R.id.profile_image);
        mood.setImageResource(Utils.getCurrentMoodRessource());
    }

    private void initExpandableListView() {
        createGroupList();

        ExpandableListView expListView = (ExpandableListView) findViewById(R.id.eintraege);
        ExpandableListAdapter expandableListAdapter = new ExpandableListAdapter(getLayoutInflater(), groupList, schwarzesBrett);
        expListView.setAdapter(expandableListAdapter);

        if (groupList.size() == 0) {
            findViewById(R.id.textView6).setVisibility(View.VISIBLE);
        }
    }

    private void initButton() {
        View button = findViewById(R.id.floatingActionButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://moritz.liegmanns.de/schwarzes_brett/NeueMeldung.php"));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
    }

    private void createGroupList() {
        groupList = new ArrayList<>();
        SQLiteConnector db = new SQLiteConnector(getBaseContext());
        SQLiteDatabase dbh = db.getWritableDatabase();
        Cursor myCursor;
        String stufe = Utils.getUserStufe();
        if (!stufe.equals("N/A")) {
            myCursor = dbh.query(SQLiteConnector.TABLE_EINTRAEGE, new String[]{SQLiteConnector.EINTRAEGE_ADRESSAT, SQLiteConnector.EINTRAEGE_TITEL, SQLiteConnector.EINTRAEGE_INHALT, SQLiteConnector.EINTRAEGE_ERSTELLDATUM, SQLiteConnector.EINTRAEGE_ABLAUFDATUM}, SQLiteConnector.EINTRAEGE_ADRESSAT + " = '" + stufe + "'", null, null, null, null);
        }
        else {
            myCursor = dbh.query(SQLiteConnector.TABLE_EINTRAEGE, new String[]{SQLiteConnector.EINTRAEGE_ADRESSAT, SQLiteConnector.EINTRAEGE_TITEL, SQLiteConnector.EINTRAEGE_INHALT, SQLiteConnector.EINTRAEGE_ERSTELLDATUM, SQLiteConnector.EINTRAEGE_ABLAUFDATUM}, null, null, null, null, null);
        }
        schwarzesBrett = new LinkedHashMap<>();
        for (myCursor.moveToFirst(); !myCursor.isAfterLast(); myCursor.moveToNext()) {
            try {
                groupList.add(myCursor.getString(myCursor.getColumnIndexOrThrow(SQLiteConnector.EINTRAEGE_TITEL)));
                Date erstelldatum = new Date(myCursor.getLong(3));
                Date ablaufdatum = new Date(myCursor.getLong(4));
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
                String[] children = {myCursor.getString(0),
                        myCursor.getString(2),
                        "Gültig vom " + simpleDateFormat.format(erstelldatum) +
                                " bis zum " + simpleDateFormat.format(ablaufdatum)};
                loadChild(children);
                schwarzesBrett.put(myCursor.getString(myCursor.getColumnIndexOrThrow(SQLiteConnector.EINTRAEGE_TITEL)), childList);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        myCursor.close();
        dbh.close();
    }

    private void loadChild(String[] laptopModels) {
        childList = new ArrayList<>();
        Collections.addAll(childList, laptopModels);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mi) {
        if (mi.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
        return true;
    }
}