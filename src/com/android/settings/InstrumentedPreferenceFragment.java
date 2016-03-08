package com.android.settings;
import android.preference.PreferenceFragment;
import com.android.internal.logging.MetricsLogger;
/**
 * Instrumented preference fragment that logs visibility state.
 */
public abstract class InstrumentedPreferenceFragment extends PreferenceFragment {
    /**
     * Declare the view of this category.
     *
     * Categories are defined in {@link com.android.internal.logging.MetricsLogger}
     * or if there is no relevant existing category you may define one in
     * {@link com.android.settings.InstrumentedFragment}.
     */
    protected abstract int getMetricsCategory();
    @Override
    public void onResume() {
        super.onResume();
        MetricsLogger.visible(getActivity(), getMetricsCategory());
    }
    @Override
    public void onPause() {
        super.onPause();
        MetricsLogger.hidden(getActivity(), getMetricsCategory());
    }
}