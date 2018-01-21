package de.slgdev.umfragen.dialog;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import de.slgdev.leoapp.R;
import de.slgdev.leoapp.utility.Utils;
import de.slgdev.schwarzes_brett.utility.ResponseCode;

import static android.view.View.GONE;

/**
 * Ergebnisdialog
 * <p>
 * Dieser Dialog wird zum Anzeigen der Umfrageergebnisse verwendet.
 *
 * @author Gianni
 * @version 2017.2110
 * @since 0.5.6
 */
public class ResultDialogManual extends AlertDialog {

    private int       id;
    private AsyncTask asyncTask;

    private TextView[]    answers;
    private TextView[]    percentages;
    private ProgressBar[] progressBars;
    private ProgressBar   load;
    private Button        b1;
    private TextView      t1;
    private TextView      t2;

    private String to;

    /**
     * Konstruktor.
     *
     * @param context Context-Objekt
     */
    public ResultDialogManual(@NonNull Context context, String description, HashMap<String, Integer> answers) {
        super(context);
        this.id = id;
        this.to = to;
    }

    /**
     * Hier werden View-Objekte instanziiert und der Synchronisationsvorgang gestartet.
     *
     * @param b Metadata
     */
    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.dialog_survey_result_simplified);

        b1 = findViewById(R.id.buttonOK);

        t1 = findViewById(R.id.question);

        load = findViewById(R.id.progressBarLoading);

        ProgressBar p1 = findViewById(R.id.progressBar1);
        ProgressBar p2 = findViewById(R.id.progressBar2);
        ProgressBar p3 = findViewById(R.id.progressBar3);
        ProgressBar p4 = findViewById(R.id.progressBar4);
        ProgressBar p5 = findViewById(R.id.progressBar5);
        progressBars = new ProgressBar[]{p1, p2, p3, p4, p5};

        TextView op1 = findViewById(R.id.answer1);
        TextView op2 = findViewById(R.id.answer2);
        TextView op3 = findViewById(R.id.answer3);
        TextView op4 = findViewById(R.id.answer4);
        TextView op5 = findViewById(R.id.answer5);
        answers = new TextView[]{op1, op2, op3, op4, op5};

        TextView pe1 = findViewById(R.id.percent1);
        TextView pe2 = findViewById(R.id.percent2);
        TextView pe3 = findViewById(R.id.percent3);
        TextView pe4 = findViewById(R.id.percent4);
        TextView pe5 = findViewById(R.id.percent5);
        percentages = new TextView[]{pe1, pe2, pe3, pe4, pe5};

        for (TextView cur : answers)
            cur.setVisibility(View.GONE);
        for (ProgressBar cur : progressBars)
            cur.setVisibility(View.GONE);

        t1.setVisibility(View.INVISIBLE);
        t2.setVisibility(View.INVISIBLE);

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                stopLoading();
            }
        });

        asyncTask = new SyncResults().execute();
    }

    @SuppressWarnings("unchecked")
    private void animateChanges(int amount, HashMap<String, Integer> answerMap, int target, int votes) {

        Map.Entry<String, Integer>[] entries = answerMap.entrySet().toArray(new Map.Entry[0]);
        for (int i = 0; i < amount; i++) {
            answers[i].setText(entries[i].getKey());
            answers[i].setVisibility(View.VISIBLE);
            progressBars[i].setVisibility(View.VISIBLE);

            if (votes == 0)
                continue;

            ObjectAnimator animation = ObjectAnimator.ofInt(progressBars[i], "progress", entries[i].getValue() * 100 / votes);
            animation.setDuration(1250);
            animation.setInterpolator(new DecelerateInterpolator());
            animation.start();

            percentages[i].setText(String.valueOf(entries[i].getValue()));
        }
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) b1.getLayoutParams();
        params.addRule(RelativeLayout.BELOW, answers[amount - 1].getId());
        b1.setLayoutParams(params);

        for (int i = amount; i < answers.length; i++) {
            answers[i].setVisibility(GONE);
        }

        double        percentage = (double) votes * 100d / (double) target;
        DecimalFormat df         = new DecimalFormat("####0.00");

        t2.setText(Utils.getContext().getString(R.string.statistics_result, votes, target, df.format(percentage)));
    }

    private void stopLoading() {
        asyncTask.cancel(true);
    }

    private class SyncResults extends AsyncTask<Void, Void, ResponseCode> {

        private int                            amountAnswers;
        private int                            target;
        private int                            sumVotes;
        private String                         title;
        private LinkedHashMap<String, Integer> answerResults;

        @Override
        protected ResponseCode doInBackground(Void... params) {
            try {
                if (!Utils.checkNetwork()) {
                    return ResponseCode.NO_CONNECTION;
                }

                URL            updateURL = new URL(Utils.BASE_URL_PHP + "survey/getAllResults.php?survey=" + id +  "&to=" + to);
                BufferedReader reader    = new BufferedReader(new InputStreamReader(updateURL.openConnection().getInputStream()));

                Utils.logError(updateURL);

                String        cur;
                StringBuilder result = new StringBuilder();
                while ((cur = reader.readLine()) != null) {
                    result.append(cur);
                }

                String resString = result.toString();
                if (resString.contains("-ERR"))
                    return ResponseCode.SERVER_ERROR;

                String[] data = resString.split("_;;_");

                target = Integer.parseInt(data[0]);
                title = data[2];
                sumVotes = Integer.parseInt(data[3]);

                String[] answers = data[1].split("_next_");

                amountAnswers = answers.length;
                answerResults = new LinkedHashMap<>();

                for (String s : answers) {
                    answerResults.put(s.split("_;_")[0], Integer.parseInt(s.split("_;_")[1]));
                }
            } catch (IOException e) {
                Utils.logError(e);
            }

            return ResponseCode.SUCCESS;
        }

        @Override
        protected void onPostExecute(ResponseCode b) {
            load.setVisibility(GONE);
            switch (b) {
                case NO_CONNECTION:
                    findViewById(R.id.imageViewError).setVisibility(View.VISIBLE);
                    final Snackbar snack = Snackbar.make(findViewById(R.id.snackbar), Utils.getString(R.string.snackbar_no_connection_info), Snackbar.LENGTH_LONG);
                    snack.setActionTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                    snack.setAction(getContext().getString(R.string.confirm), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snack.dismiss();
                        }
                    });
                    snack.show();
                    break;
                case SERVER_ERROR:
                    findViewById(R.id.imageViewError).setVisibility(View.VISIBLE);
                    final Snackbar snackbar = Snackbar.make(findViewById(R.id.wrapper), Utils.getString(R.string.error_later), Snackbar.LENGTH_SHORT);
                    snackbar.setActionTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                    snackbar.setAction(getContext().getString(R.string.confirm), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snackbar.dismiss();
                        }
                    });
                    snackbar.show();
                    break;
                case SUCCESS:
                    t1.setText(title);
                    t1.setVisibility(View.VISIBLE);
                    t2.setVisibility(View.VISIBLE);
                    animateChanges(amountAnswers, answerResults, target, sumVotes);
                    break;
            }
        }
    }
}
