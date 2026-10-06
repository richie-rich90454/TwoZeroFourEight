package org.twozerofoureightgame.model;
/**
 * One tile movement recorded while a single move is applied to the board. The
 * object is mutable on purpose: the view layer pools these objects so that a
 * move never allocates. A transition either describes a slide, a merge, or the
 * appearance of a freshly spawned tile.
 */
public final class TileTransition{
    /** The row the tile occupied before the move, counted from the top. */
    public int fromRow;
    /** The column the tile occupied before the move, counted from the left. */
    public int fromColumn;
    /** The row the tile occupies after the move, counted from the top. */
    public int toRow;
    /** The column the tile occupies after the move, counted from the left. */
    public int toColumn;
    /** The exponent the tile carried before the move, where 0 means empty. */
    public byte oldExponent;
    /** The exponent the tile carries after the move, where 0 means empty. */
    public byte newExponent;
    /** True when this tile merged into the tile that was already in the target cell. */
    public boolean merged;
    /** True when this transition describes a newly spawned tile rather than a move. */
    public boolean isNewSpawn;
    /**
     * Creates an empty transition. The fields are filled in by the board through
     * {@link MoveResult#addTransition}.
     */
    public TileTransition(){
    }
}
