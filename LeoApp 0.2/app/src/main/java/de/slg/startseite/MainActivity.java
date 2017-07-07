package de.slg.startseite;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import de.slg.essensqr.WrapperQRActivity;
import de.slg.klausurplan.KlausurplanActivity;
import de.slg.leoapp.NotificationService;
import de.slg.leoapp.PreferenceActivity;
import de.slg.leoapp.R;
import de.slg.leoapp.Start;
import de.slg.leoapp.Utils;
import de.slg.messenger.OverviewWrapper;
import de.slg.schwarzes_brett.SchwarzesBrettActivity;
import de.slg.stimmungsbarometer.StimmungsbarometerActivity;
import de.slg.stundenplan.AuswahlActivity;
import de.slg.stundenplan.WrapperStundenplanActivity;
import de.slg.vertretung.WrapperSubstitutionActivity;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

@SuppressLint("StaticFieldLeak")
public class MainActivity extends AppCompatActivity implements View.OnClickListener, ZXingScannerView.ResultHandler {

    public static View v;
    public static ProgressBar pb;
    public static TextView title, info;
    public static Button verify;
    public static Intent service;
    public static MainActivity ref;

    public ZXingScannerView scV;

    private static boolean verified;
    private static boolean editing;

    private final int MY_PERMISSIONS_REQUEST_USE_CAMERA = 0;

    private boolean runningScan;

    private NavigationView navigationView;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        runningScan = false;
        ref = this;

        setContentView(R.layout.activity_startseite);

        int id = Utils.getUserID();
        boolean hide = Start.pref.getBoolean("pref_key_dont_remind_me", false);
        boolean synchronize = Start.pref.getBoolean("pref_key_level_has_to_be_synchronized", false);

        initToolbar();
        initCardViews();
        initNavigationView();

        synchronizeUsername();

        if (synchronize)
            new UpdateTaskGrade(this).execute();

        if (verified = id > -1) {
            MainActivity.title.setTextColor(Color.GREEN);
            MainActivity.title.setText(getString(R.string.title_info_auth));
            MainActivity.info.setText(getString(R.string.summary_info_auth_success));
            MainActivity.verify.setText(getString(R.string.button_info_noreminder));
            updateButtons();
        }
        if (hide)
            findViewById(R.id.card_view0).setVisibility(View.GONE);
        if (verified)
            updateButtons();

        if (Start.pref.getBoolean("pref_key_notification_essensqr", false) && service == null) {
            service = new Intent(this, NotificationService.class);
            startService(service);
        }

        if (!WrapperQRActivity.mensaModeRunning && Start.pref.getBoolean("pref_key_mensa_mode", false)) {
            startActivity(new Intent(this, WrapperQRActivity.class));
        } else
            WrapperQRActivity.mensaModeRunning = false;

    }

    private void synchronizeUsername() {
        new SyncTaskName().execute();
    }

    private void initToolbar() {
        Toolbar t = (Toolbar) findViewById(R.id.toolbar);
        t.setTitleTextColor(Color.WHITE);
        t.setTitle(getString(R.string.title_home));
        setSupportActionBar(t);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    void updateButtons() { //TODO: Save remove

    }

    @Override
    protected void onResume() {
        super.onResume();
        navigationView.getMenu().findItem(R.id.startseite).setChecked(true);
    }

    private void initNavigationView() {
        navigationView = (NavigationView) findViewById(R.id.navigationView);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        navigationView.getMenu().findItem(R.id.startseite).setChecked(true);

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
                    case R.id.messenger: //Nur bei Verifizierung
                        i = new Intent(getApplicationContext(), OverviewWrapper.class);
                        break;
                    case R.id.newsboard:
                        i = new Intent(getApplicationContext(), SchwarzesBrettActivity.class);
                        break;
                    case R.id.nachhilfe: //Nur bei Verifizierung
                        i = new Intent(getApplicationContext(), MainActivity.class);
                        break;
                    case R.id.stundenplan:
                        i = new Intent(getApplicationContext(), WrapperStundenplanActivity.class);
                        break;
                    case R.id.barometer:
                        i = new Intent(getApplicationContext(), StimmungsbarometerActivity.class);
                        break;
                    case R.id.klausurplan: //Nur bei Verifizierung
                        i = new Intent(getApplicationContext(), KlausurplanActivity.class);
                        break;
                    case R.id.startseite:
                        i = null;
                        break;
                    case R.id.settings:
                        i = new Intent(getApplicationContext(), PreferenceActivity.class);
                        break;
                    case R.id.vertretung:
                        i = new Intent(getApplicationContext(), WrapperSubstitutionActivity.class);
                        break;
                    default:
                        i = new Intent(getApplicationContext(), MainActivity.class);
                        Toast.makeText(getApplicationContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
                if (i != null)
                    startActivity(i);
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

    private void initCardViews() {

        findViewById(R.id.buttonCardView0).setOnClickListener(this);
        findViewById(R.id.buttonDismissCardView0).setOnClickListener(this);

        TextView version = (TextView) findViewById(R.id.versioncode_maincard);
        version.setText(Utils.getAppVersionName());

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recyclerViewCards);
        CardAdapter mAdapter = new CardAdapter();

        boolean quickLayout = Start.pref.getBoolean("pref_key_card_config_quick", false);

        RecyclerView.LayoutManager mLayoutManager = quickLayout

        ?

        new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        }

        :

        new LinearLayoutManager(getApplicationContext()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
        if (item.getItemId() == R.id.action_appinfo) {
            startActivity(new Intent(getApplicationContext(), InfoActivity.class));
        }
        if (item.getItemId() == R.id.action_appedit) {
            editing = true;
            invalidateOptionsMenu();
        }
        if (item.getItemId() == R.id.action_appedit_done) {
            editing = false;
            invalidateOptionsMenu();
        }
        if (item.getItemId() == R.id.action_appinfo_quick) {
            item.setChecked(!item.isChecked());
            SharedPreferences.Editor edit = Start.pref.edit();
            edit.putBoolean("pref_key_card_config_quick", item.isChecked());
            edit.apply();
            finish();
            startActivity(new Intent(this, MainActivity.class));
        }
        return true;
    }

    @Override
    public void onClick(View v) {
            if(v.getId() == R.id.buttonCardView0) {
                if (!isVerified())
                    showDialog();
                else {
                    SharedPreferences.Editor e = Start.pref.edit();
                    e.putBoolean("pref_key_dont_remind_me", true);
                    e.apply();
                    findViewById(R.id.card_view0).setVisibility(View.GONE);
                }
            } else {
                SharedPreferences.Editor e = Start.pref.edit();
                e.putBoolean("pref_key_dont_remind_me", true);
                e.apply();
                findViewById(R.id.card_view0).setVisibility(View.GONE);
            }
    }

    private void showDialog() {
        final AlertDialog builder = new AlertDialog.Builder(this).create();
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_layout, null);
        Button b1, b2;
        b1 = (Button) v.findViewById(R.id.buttonDialog1);
        b2 = (Button) v.findViewById(R.id.buttonDialog2);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.dismiss();
            }
        });
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("LeoApp", "on click");
                startCamera(builder);
            }
        });
        builder.setView(v);
        builder.show();
    }

    public static boolean isVerified() {
        return verified;
    }

    public void setVerified() {
        finish();
        startActivity(getIntent());
        verified = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(editing) {
            getMenuInflater().inflate(R.menu.startseite_edit, menu);
            menu.findItem(R.id.action_appinfo_quick).setChecked(Start.pref.getBoolean("pref_key_card_config_quick", false));
        } else
            getMenuInflater().inflate(R.menu.startseite, menu);

        return true;
    }

    private void startCamera(AlertDialog b) {
        b.dismiss();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d("LeoApp", "No upermission. Checking");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_USE_CAMERA);
            Log.d("LeoApp", "No upermission. Checked");
        } else {
            scV = new ZXingScannerView(getApplicationContext());
            setContentView(scV);
            scV.setResultHandler(this);
            scV.startCamera(0);
            runningScan = true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (runningScan) {
                runningScan = false;
                scV.stopCamera();
                finish();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                return false;
            }
            return super.onKeyDown(keyCode, event);
        } else
            return super.onKeyDown(keyCode, event);
    }

    @Override
    public void handleResult(Result result) {
        runningScan = false;
        scV.stopCamera();
        finish();
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        String results = result.getText();
        Log.d("LeoApp", results);
        Log.d("LeoApp", "checkCode");
        if (isValid(results)) {
            final String[] data = results.split("-");
            Log.d("LeoApp", "validCode");
            final Handler handler = new Handler();
            final Runnable r = new Runnable() {
                @Override
                public void run() {
                    MainActivity.pb.setVisibility(View.VISIBLE);
                    MainActivity.title.setVisibility(View.GONE);
                    MainActivity.info.setVisibility(View.GONE);
                    MainActivity.verify.setVisibility(View.GONE);

                    RegistrationTask t = new RegistrationTask(MainActivity.this);
                    t.execute(data[0], String.valueOf(data[1]));
                }
            };
            handler.postDelayed(r, 100);
        } else {
            final Handler handler = new Handler();
            final Runnable r = new Runnable() {
                @Override
                public void run() {
                    MainActivity.info.setText(getString(R.string.summary_info_auth_failed));
                    MainActivity.title.setText(getString(R.string.error));
                }
            };
            handler.postDelayed(r, 100);
        }

    }

    @Override
    protected void onPause() {
        if (scV != null && scV.isActivated()) {
            scV.stopCamera();
            finish();
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        } else {
            super.onPause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_USE_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    runningScan = true;
                    scV = new ZXingScannerView(getApplicationContext());
                    setContentView(scV);
                    scV.setResultHandler(this);
                    scV.startCamera(0);
                }
            }
        }
    }

    private boolean isValid(String s) {
        String[] parts = s.split("-");
        if (parts.length != 3)
            return false;
        Log.d("LeoApp", "passedLengthTest");
        int priority;
        int birthyear;
        if (parts[0].length() < 6)
            return false;
        Log.d("LeoApp", "passedUsernameLengthTest");
        try {
            priority = Integer.parseInt(parts[1]);
            Log.d("LeoApp", "passedPriorityNumberTest");
            if (priority < 1 || priority > 2)
                return false;
            Log.d("LeoApp", "passedPriorityNumberSizeTest");
            if (priority == 2)
                birthyear = 0x58;
            else if (parts[0].length() != 12)
                return false;
            else
                birthyear = Integer.parseInt(parts[0].substring(10));
            Log.d("LeoApp", "passedBirthyearTest");
        } catch (NumberFormatException e) {
            return false;
        }
        return birthyear >= 0 && getChecksum(parts[0], priority, birthyear).equals(parts[2]);
    }

    private String getChecksum(String username, int priority, int birthyear) {
        Calendar c = new GregorianCalendar();
        c.setTime(new Date());
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);

        int numericName = toInt(username.substring(0, 3));
        int numericLastName = toInt(username.substring(3, 6));

        long checksum = (long) (Long.valueOf((int) (Math.pow(year, 2)) + "" + (int) (Math.pow(day, 2)) + "" + (int) (Math.pow(month, 2))) * username.length() * Math.cos(birthyear) + priority * (numericName - numericLastName));

        return Long.toHexString(checksum);
    }

    private int toInt(String s) {
        int result = 0, i, count = 1;
        String regex = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (char c : s.toCharArray()) {
            for (i = 0; i < regex.length(); i++) {
                if (c == regex.charAt(i))
                    break;
            }
            result += i * count;
            count *= 64;
        }
        return result;
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}