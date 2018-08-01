package de.leoapp_slg.exams

import de.leoapp_slg.core.activity.LeoAppFeatureActivity

class MainActivity : LeoAppFeatureActivity() {
    override fun getContentView(): Int {
        return R.layout.activity_main
    }

    override fun getDrawerLayoutId(): Int {
        return R.id.drawerLayout
    }

    override fun getNavigationViewId(): Int {
        return R.id.navigationView
    }

    override fun getToolbarViewId(): Int {
        return R.id.toolbar
    }

    override fun getProgressBarId(): Int {
        return R.id.progressBar
    }

    override fun getToolbarTextId(): Int {
        return R.string.FeatureTitle
    }

    override fun getNavigationHighlightId(): Int {
        return 0
    }

    override fun getActivityTag(): String {
        return "exams_main_activity"
    }
}