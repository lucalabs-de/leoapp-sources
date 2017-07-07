package de.slg.startseite;

import android.view.View;

public class InfoCard extends Card {

    String buttonDescr;
    String descr;
    View.OnClickListener buttonListener;
    boolean enabled;

    public InfoCard(boolean large) {
        super(large);
        enabled = true;
    }
}
