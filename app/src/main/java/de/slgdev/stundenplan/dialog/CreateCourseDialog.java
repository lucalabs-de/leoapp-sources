package de.slgdev.stundenplan.dialog;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialog;
import android.view.View;

import de.slgdev.leoapp.R;

public class CreateCourseDialog extends AppCompatDialog {
    private View contentView1, contentView2;

    public CreateCourseDialog(Activity context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        contentView1 = getLayoutInflater().inflate(R.layout.dialog_create_course, null);
        contentView2 = getLayoutInflater().inflate(R.layout.dialog_create_course_lessons, null);

        contentView1.findViewById(R.id.buttonNext).setOnClickListener(v -> setContentView(contentView2));
        contentView2.findViewById(R.id.buttonBack).setOnClickListener(v -> setContentView(contentView1));
        contentView2.findViewById(R.id.buttonSave).setOnClickListener(v -> {});

        setContentView(contentView1);
    }
}
