package org.twozerofoureightgame.ai;
/**
 * The single place where every tuning number used by the artificial
 * intelligence lives. The values were chosen by playing the game at several
 * search depths and watching which features separated the moves that reached
 * 2048 from the moves that did not. Keeping them together makes tuning a matter
 * of editing one file instead of hunting through the evaluator.
 */
public final class AIWeights{
    /** Every empty cell is worth this many points; free space is the strongest survival signal. */
    public static final int EMPTY_CELL_WEIGHT=270;
    /** Each unit of disorder inside a row or a column costs this many points. */
    public static final int MONOTONICITY_WEIGHT=47;
    /** Each unit of difference between neighbouring tiles costs this many points. */
    public static final int SMOOTHNESS_WEIGHT=-10;
    /** The largest tile placed in a corner earns this many points for each of its exponent steps. */
    public static final int CORNER_WEIGHT=100;
    /** Every pair of equal neighbouring tiles earns this many points. */
    public static final int MERGE_POTENTIAL_WEIGHT=50;
    /** The score of the best snake shaped ordering is multiplied by this factor. */
    public static final int SNAKE_PATTERN_WEIGHT=150;
    /** The value returned for a position where a 2048 tile is already on the board. */
    public static final int TERMINAL_WIN_VALUE=1000000000;
    /** The value returned for a position where no move is legal at all. */
    public static final int TERMINAL_LOSS_VALUE=-1000000000;
    /**
     * This class only exposes constants and is never instantiated.
     */
    private AIWeights(){
    }
}
