package org.twozerofoureightgame.utility;
import org.twozerofoureightgame.view.TileView;
/**
 * One running animation, taken from the pool owned by {@link AnimationManager}
 * so that starting an animation never creates an object. The object records
 * which kind of animation it is, when it should begin, how long it lasts, and
 * the handful of numbers that describe what it moves. The tile reference is the
 * one non primitive field: storing it costs no memory churn, and it lets the
 * manager update the tile directly without a callback, a closure, or a lookup
 * through an identifier.
 */
public final class Animation{
    /** The clock reading when this animation was started. */
    public long startTimeNanos;
    /** How long after the start this animation should begin moving. */
    public long startDelayNanos;
    /** How long the animation lasts once it has begun. */
    public long durationNanos;
    /** Which kind of animation this is; see the constants in the manager. */
    public int identifier;
    /** True while the animation still needs to be applied on every tick. */
    public boolean active;
    /** The tile this animation drives, or null for the score float. */
    public TileView view;
    /** The second tile of a merge, the one that is consumed and recycled. */
    public TileView consumedView;
    /** The row the tile left, or the destination row of the tile being spawned. */
    public int sourceRow;
    /** The column the tile left. */
    public int sourceColumn;
    /** The row the tile arrives at. */
    public int targetRow;
    /** The column the tile arrives at. */
    public int targetColumn;
    /** The horizontal centre the tile starts at. */
    public float fromPixelX;
    /** The vertical centre the tile starts at. */
    public float fromPixelY;
    /** The horizontal centre the tile ends at. */
    public float toPixelX;
    /** The vertical centre the tile ends at. */
    public float toPixelY;
    /** The exponent a merge reveals once the two tiles have arrived. */
    public byte newExponent;
    /** The text of the score float, for example plus forty. */
    public String floatText;
    /** The horizontal centre the score float starts at. */
    public float floatStartPixelX;
    /** The vertical centre the score float starts at. */
    public float floatStartPixelY;
    /** True when a slide should drop its tile into the grid when it lands. */
    public boolean settlesOnCompletion;
    /**
     * Creates an inactive animation ready to be filled in and started.
     */
    public Animation(){
    }
}
