package org.twozerofoureightgame.utility;
/**
 * The four easing curves used by the animations. Each function takes a
 * normalised progress value between zero and one and returns the fraction of
 * the journey that should actually be travelled, which lets a movement start
 * fast and settle gently. Every function is pure, static, and allocates
 * nothing.
 */
public final class Easing{
    /**
     * This class only exposes static helpers and is never instantiated.
     */
    private Easing(){
    }
    /**
     * Travels at a constant speed.
     * @param progress the normalised progress between zero and one
     * @return the same value unchanged
     */
    public static float linear(float progress){
        return progress;
    }
    /**
     * Starts quickly and slows down towards the end, which is the natural feel
     * of a tile sliding into place.
     * @param progress the normalised progress between zero and one
     * @return the eased progress
     */
    public static float easeOutCubic(float progress){
        float remaining=1f-progress;
        return 1f-remaining*remaining*remaining;
    }
    /**
     * Accelerates into the middle of the movement and slows out of it, which
     * suits a tile that swells and settles again.
     * @param progress the normalised progress between zero and one
     * @return the eased progress
     */
    public static float easeInOutQuad(float progress){
        if(progress<0.5f){
            return 2f*progress*progress;
        }
        float remaining=1f-progress;
        return 1f-2f*remaining*remaining;
    }
    /**
     * Travels past the destination slightly before coming back, which adds a
     * small bounce.
     * @param progress the normalised progress between zero and one
     * @return the eased progress, which may briefly exceed one
     */
    public static float easeOutBack(float progress){
        float overshoot=1.70158f;
        float remaining=progress-1f;
        return 1f+(overshoot+1f)*remaining*remaining*remaining+overshoot*remaining*remaining;
    }
}
