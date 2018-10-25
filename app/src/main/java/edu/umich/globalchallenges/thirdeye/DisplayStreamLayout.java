package edu.umich.globalchallenges.thirdeye;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class DisplayStreamLayout extends ConstraintLayout {

    private DisplayStreamFragment displayStreamFragment = null;

    public DisplayStreamLayout(Context context) {
        super(context);
    }

    public DisplayStreamLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DisplayStreamLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Attaches the display stream fragment to this layout
     *
     * This should be changed to use an interface at some point
     * @param ds The DisplayStreamFragment that will use the layout
     */
    public void attachDisplayStream(DisplayStreamFragment ds) {
        displayStreamFragment = ds;
    }

    /**
     * Used to intercept touch events before being sent to the children in the view
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(displayStreamFragment != null) {
            displayStreamFragment.resetTimer();
        }
        return false; // So that touches get sent down to children properly
    }
}
