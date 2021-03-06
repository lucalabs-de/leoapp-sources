package de.slgdev.messenger.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import de.slgdev.leoapp.R;
import de.slgdev.leoapp.utility.User;
import de.slgdev.leoapp.utility.Utils;
import de.slgdev.leoapp.view.ActionLogActivity;
import de.slgdev.messenger.view.UserAdapter;
import de.slgdev.messenger.task.AddUser;
import de.slgdev.messenger.task.RemoveUser;
import de.slgdev.messenger.task.SendChatname;

public class ChatEditActivity extends ActionLogActivity {
    private int    cid;
    private String cname;

    private LinearLayout userContainer;
    private View         scrollView;
    private ListView     listView;

    private User[] usersInChat, usersNotInChat;
    private UserAdapter uRemove, uAdd;
    private MenuItem confirm;
    private String   mode;

    private View add, remove;
    private Switch notifications;

    private boolean nameRunning;
    private boolean addRunning;
    private boolean removeRunning;

    @Override
    protected void onCreate(Bundle savedInstancesState) {
        super.onCreate(savedInstancesState);
        setContentView(R.layout.activity_chat_edit);
        Utils.getController().registerChatEditActivity(this);

        cid = getIntent().getIntExtra("cid", -1);
        cname = getIntent().getStringExtra("cname");

        initToolbar();
        initUsers();
        initSettings();
    }

    @Override
    protected String getActivityTag() {
        return "ChatEditActivity";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.messenger_confirm_action, menu);
        confirm = menu.findItem(R.id.action_confirm);
        confirm.setVisible(!mode.equals(""));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mi) {
        if (mi.getItemId() == android.R.id.home) {
            onBackPressed();
        } else if (mi.getItemId() == R.id.action_confirm) {
            if (mode.equals("add"))
                addUsers(uAdd.getSelected());
            if (mode.equals("remove"))
                removeUsers(uRemove.getSelected());
        }
        return true;
    }

    @Override
    public void finish() {
        super.finish();

        setResult(1, getIntent().putExtra("cname", cname));
        Utils.getController().getMessengerDatabase().muteChat(cid, !notifications.isChecked());

        Utils.getController().registerChatEditActivity(null);
    }

    @Override
    public void onBackPressed() {
        if (mode.equals("")) {
            super.onBackPressed();
        } else {
            confirm.setVisible(false);
            scrollView.setVisibility(View.VISIBLE);
            mode = "";
        }
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.white));
        toolbar.setTitle(cname);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_left);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void initUsers() {
        userContainer = findViewById(R.id.linearLayoutUsers);
        scrollView = findViewById(R.id.scrollView);
        listView = findViewById(R.id.listView);

        usersInChat = Utils.getController().getMessengerDatabase().getUsersInChat(cid);
        usersNotInChat = Utils.getController().getMessengerDatabase().getUsersNotInChat(cid);
        uRemove = new UserAdapter(getApplicationContext(), usersInChat);
        uAdd = new UserAdapter(getApplicationContext(), usersNotInChat);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            final CheckBox checkBox = view.findViewById(R.id.checkBox);
            final TextView username = view.findViewById(R.id.username);
            checkBox.setChecked(!checkBox.isChecked());
            int color = ContextCompat.getColor(getApplicationContext(), R.color.colorAccent);
            if (!checkBox.isChecked())
                color = ContextCompat.getColor(getApplicationContext(), R.color.colorText);
            username.setTextColor(color);
            switch (mode) {
                case "add":
                    confirm.setVisible(uAdd.selectCount() > 0);
                    break;
                case "remove":
                    confirm.setVisible(uRemove.selectCount() > 0);
                    break;
            }
        });

        fillContainer(usersInChat);
    }

    private void initSettings() {
        mode = "";
        notifications = findViewById(R.id.notifications);
        notifications.setChecked(!Utils.getController().getMessengerDatabase().isMute(cid));

        final View name = findViewById(R.id.changeName);
        name.setOnClickListener(v -> showDialogChatname());

        add = findViewById(R.id.addUser);
        add.setOnClickListener(v -> {
            mode = "add";
            scrollView.setVisibility(View.GONE);
            listView.setAdapter(uAdd);
        });
        if (usersNotInChat.length == 0) {
            add.setVisibility(View.GONE);
        }

        remove = findViewById(R.id.removeUser);
        remove.setOnClickListener(v -> {
            mode = "remove";
            scrollView.setVisibility(View.GONE);
            listView.setAdapter(uRemove);
        });
        if (usersInChat.length == 0) {
            remove.setVisibility(View.GONE);
        }

        final View leave = findViewById(R.id.leaveChat);
        leave.setOnClickListener(v -> {
            final AlertDialog dialog = new AlertDialog.Builder(ChatEditActivity.this).create();
            View              view   = getLayoutInflater().inflate(R.layout.dialog_confirm_leave_chat, null);
            view.findViewById(R.id.buttonDialog1).setOnClickListener(v1 -> dialog.dismiss());
            view.findViewById(R.id.buttonDialog2).setOnClickListener(v12 -> {
                removeUsers(Utils.getCurrentUser());
                finish();
                Utils.getController().getChatActivity().finish();
                dialog.dismiss();
            });
            dialog.setView(view);
            dialog.show();
        });
    }

    private void fillContainer(User[] data) {
        userContainer.removeAllViews();

        for (User u : data) {
            View v = getLayoutInflater().inflate(R.layout.list_item_user, null);

            final TextView username    = v.findViewById(R.id.username);
            final TextView userdefault = v.findViewById(R.id.userdefault);

            username.setText(u.uname);
            userdefault.setText(u.udefaultname + ", " + u.ustufe);

            v.findViewById(R.id.checkBox).setVisibility(View.GONE);

            userContainer.addView(v);
        }

        View v = getLayoutInflater().inflate(R.layout.list_item_user, null);

        final TextView username    = v.findViewById(R.id.username);
        final TextView userdefault = v.findViewById(R.id.userdefault);

        username.setText(Utils.getUserName());
        userdefault.setText(Utils.getUserDefaultName() + ", " + Utils.getUserStufe());

        v.findViewById(R.id.checkBox).setVisibility(View.GONE);

        userContainer.addView(v);
    }

    private void removeUsers(User... users) {
        new RemoveUser(this, cid).execute(users);
    }

    private void addUsers(User... users) {
        new AddUser(this, cid).execute(users);
    }

    private void showDialogChatname() {
        final AlertDialog dialog = new AlertDialog.Builder(this).create();

        View v = getLayoutInflater().inflate(R.layout.dialog_change_chatname, null);

        final TextView textView = v.findViewById(R.id.etChatname);
        textView.setText(cname);

        v.findViewById(R.id.buttonDialog1).setOnClickListener(v1 -> dialog.dismiss());
        v.findViewById(R.id.buttonDialog2).setOnClickListener(v12 -> {
            new SendChatname(ChatEditActivity.this, cid).execute(textView.getText().toString());
            dialog.dismiss();
        });

        dialog.setView(v);

        dialog.show();
    }

    public void notifyTaskStarted(AsyncTask task) {
        confirm.setVisible(false);
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        if (task.getClass() == AddUser.class) {
            addRunning = true;
        } else if (task.getClass() == RemoveUser.class) {
            removeRunning = true;
        } else if (task.getClass() == SendChatname.class) {
            nameRunning = true;
        }
    }

    public void notifyTaskDone(AsyncTask task) {
        if (task.getClass() == AddUser.class) {
            addRunning = false;
        } else if (task.getClass() == RemoveUser.class) {
            removeRunning = false;
        } else if (task.getClass() == SendChatname.class) {
            nameRunning = false;
        }

        usersInChat = Utils.getController().getMessengerDatabase().getUsersInChat(cid);
        usersNotInChat = Utils.getController().getMessengerDatabase().getUsersNotInChat(cid);
        uAdd = new UserAdapter(getApplicationContext(), usersNotInChat);
        uRemove = new UserAdapter(getApplicationContext(), usersInChat);

        fillContainer(usersInChat);

        if (usersNotInChat.length == 0) {
            add.setVisibility(View.GONE);
        } else {
            add.setVisibility(View.VISIBLE);
        }

        if (usersInChat.length == 0) {
            remove.setVisibility(View.GONE);
        } else {
            remove.setVisibility(View.VISIBLE);
        }

        if (!addRunning && !removeRunning)
            scrollView.setVisibility(View.VISIBLE);

        if (!addRunning && !removeRunning && !nameRunning)
            findViewById(R.id.progressBar).setVisibility(View.GONE);

        if (!nameRunning)
            getSupportActionBar().setTitle(cname);
    }

    public void setCname(String cname) {
        this.cname = cname;
    }
}