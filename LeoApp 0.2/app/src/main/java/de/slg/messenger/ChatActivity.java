package de.slg.messenger;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import de.slg.leoapp.List;
import de.slg.leoapp.R;
import de.slg.leoapp.Utils;

public class ChatActivity extends AppCompatActivity {
    static Chat currentChat;
    private Message[] messagesArray;

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton sendButton;
    private Snackbar snackbar;
    private String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.registerChatActivity(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messagesArray = new Message[0];
        Utils.receive();

        initToolbar();
        initSendMessage();
        initRecyclerView();
        initSnackbar();

        Utils.getDB().setMessagesRead(currentChat);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.messenger_chat, menu);
        if (currentChat.ctype == Chat.Chattype.PRIVATE || !Utils.getDB().userInChat(Utils.getCurrentUser(), currentChat))
            menu.clear();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.action_edtiParticipants) {
            startActivity(new Intent(getApplicationContext(), ChatEditActivity.class));
        }
        return true;
    }

    @Override
    public void finish() {
        super.finish();
        Utils.registerChatActivity(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportActionBar().setTitle(currentChat.cname);
        refreshUI(false, true);
    }

    private void initRecyclerView() {
        rvMessages = (RecyclerView) findViewById(R.id.recyclerViewMessages);
        rvMessages.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        refreshUI(true, true);
    }

    private void initToolbar() {
        Toolbar actionBar = (Toolbar) findViewById(R.id.actionBarChat);
        actionBar.setTitleTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.white));
        setSupportActionBar(actionBar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void initSendMessage() {
        etMessage = (EditText) findViewById(R.id.inputMessage);

        sendButton = (ImageButton) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        if (getIntent().getBooleanExtra("loading", false)) {
            new WaitForLoad().execute();
        } else if (!Utils.getDB().userInChat(Utils.getCurrentUser(), currentChat)) {
            etMessage.setEnabled(false);
            etMessage.setHint("Du bist nicht in diesem Chat!");
            sendButton.setEnabled(false);
        }
    }

    private void initSnackbar() {
        snackbar = Snackbar
                .make(findViewById(R.id.coordinatorLayout),
                        "Something went wrong! Please restart the app",
                        Snackbar.LENGTH_LONG)
                .setActionTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                .setAction(getString(R.string.snackbar_undo), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        snackbar.dismiss();
                    }
                });
    }

    private String getMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                message = etMessage.getText().toString();
            }
        });
        while (message.length() > 0 && message.charAt(0) == ' ')
            message = message.substring(1);
        while (message.length() > 0 && message.charAt(message.length() - 1) == ' ')
            message = message.substring(0, message.length() - 1);
        return message;
    }

    private void sendMessage() {
        String message = getMessage();
        if (message.length() > 0 && currentChat != null) {
            new SendMessage().execute(message);
            etMessage.setText("");
            Utils.receive();
        }
    }

    public void refreshUI(boolean refreshMessages, final boolean scroll) {
        if (refreshMessages)
            messagesArray = Utils.getDB().getMessagesFromChat(currentChat.cid);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rvMessages.swapAdapter(new MessageAdapter(), false);
                if (scroll)
                    rvMessages.scrollToPosition(messagesArray.length - 1);
            }
        });
    }

    private class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
        private Chat.Chattype chattype;
        private LayoutInflater inflater;
        private TextView nachricht, absender, uhrzeit, datum;
        private LinearLayout l;
        private View chatbubble, space, progressbar;

        MessageAdapter() {
            super();
            this.inflater = getLayoutInflater();
            this.chattype = currentChat.ctype;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(inflater.inflate(R.layout.list_item_message, null));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Message current = messagesArray[position];
            View v = holder.itemView;
            datum = (TextView) v.findViewById(R.id.textViewDate);
            nachricht = (TextView) v.findViewById(R.id.nachricht);
            absender = (TextView) v.findViewById(R.id.absender);
            uhrzeit = (TextView) v.findViewById(R.id.datum);
            l = (LinearLayout) v.findViewById(R.id.chatbubblewrapper);
            chatbubble = v.findViewById(R.id.chatbubble);
            space = v.findViewById(R.id.space);
            progressbar = v.findViewById(R.id.progressBar);

            nachricht.setText(current.mtext);
            absender.setText(current.uname);
            uhrzeit.setText(current.getTime());
            datum.setText(current.getDate());

            boolean mine = current.uid == Utils.getUserID();
            chatbubble.setEnabled(mine);
            if (mine) {
                l.setGravity(Gravity.RIGHT);
                absender.setVisibility(View.GONE);
                nachricht.setTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.background_light));
                uhrzeit.setTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.background_light));
            } else {
                l.setGravity(Gravity.LEFT);
                nachricht.setTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.background_dark));
                uhrzeit.setTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.background_dark));
                if (chattype == Chat.Chattype.PRIVATE) {
                    absender.setVisibility(View.GONE);
                } else {
                    absender.setVisibility(View.VISIBLE);
                }
            }

            boolean send = uhrzeit.getText().toString().equals("");
            if (send) {
                uhrzeit.setVisibility(View.GONE);
                progressbar.setVisibility(View.VISIBLE);
            } else {
                uhrzeit.setVisibility(View.VISIBLE);
                progressbar.setVisibility(View.GONE);
            }

            boolean first = position == 0 || !gleicherTag(current.mdate, messagesArray[position - 1].mdate);
            if (first) {
                datum.setVisibility(View.VISIBLE);
            } else {
                datum.setVisibility(View.GONE);
                if (current.uid == messagesArray[position - 1].uid) {
                    absender.setVisibility(View.GONE);
                    space.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public int getItemCount() {
            return messagesArray.length;
        }

        private boolean gleicherTag(Date pDate1, Date pDate2) {
            Calendar c1 = new GregorianCalendar(), c2 = new GregorianCalendar();
            c1.setTime(pDate1);
            c2.setTime(pDate2);
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH) && c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
            }
        }
    }

    private class SendMessage extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            if (currentChat.cid == -1) {
                snackbar.show();
            } else {
                List<Message> messageList = new List<>(messagesArray);
                messageList.append(
                        new Message(0,
                                params[0],
                                0,
                                currentChat.cid,
                                Utils.getUserID(),
                                true));
                messagesArray = messageList.fill(new Message[messageList.length()]);
                if (!Utils.checkNetwork()) {
                    refreshUI(false, true);
                    Utils.getDB().insertUnsendMessage(params[0], currentChat.cid);
                } else {
                    messagesArray[messagesArray.length - 1].mdate = new Date();
                    messagesArray[messagesArray.length - 1].sending = true;
                    refreshUI(false, true);
                    try {
                        BufferedReader reader =
                                new BufferedReader(
                                        new InputStreamReader(
                                                new URL(generateURL(params[0]))
                                                        .openConnection()
                                                        .getInputStream(), "UTF-8"));
                        while (reader.readLine() != null) ;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        private String generateURL(String message) {
            return "http://moritz.liegmanns.de/messenger/addMessage.php?key=5453&userid=" + Utils.getUserID() + "&message=" + message.replace(" ", "%20").replace(System.getProperty("line.separator"), "%0A") + "&chatid=" + currentChat.cid;
        }
    }

    private class WaitForLoad extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            findViewById(R.id.progressBar3).setVisibility(View.VISIBLE);
            sendButton.setEnabled(false);
            Utils.receive();
        }

        @Override
        protected Void doInBackground(Void... params) {
            while (currentChat.cid == -1) ;
            while (!Utils.getDB().userInChat(Utils.getCurrentUser(), currentChat)) ;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            findViewById(R.id.progressBar3).setVisibility(View.GONE);
            sendButton.setEnabled(true);
        }
    }
}