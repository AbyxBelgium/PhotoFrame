package be.abyx.photoframe;

import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * This GestureListener listens to single taps only and allows to specify some sort of action as
 * reaction to a tap.
 *
 * @author Pieter Verschaffelt
 */
public class TapGestureListener extends GestureDetector.SimpleOnGestureListener {
    TapGestureListener.TapListener action;

    public TapGestureListener(TapGestureListener.TapListener action) {
        this.action = action;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        System.out.println("TAPTAPTAP!!!");
        action.onTapped();
        return super.onSingleTapUp(e);
    }

    public interface TapListener {
        void onTapped();
    }
}
