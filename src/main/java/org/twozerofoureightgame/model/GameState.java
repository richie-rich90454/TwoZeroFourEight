package org.twozerofoureightgame.model;
/**
 * An immutable snapshot of everything needed to restore a board exactly as it
 * was: the sixteen cell exponents, the score, the number of moves played, and
 * the two end-of-game flags. The controller keeps one snapshot to implement the
 * single step undo, and the exporter serialises one to write a save file.
 */
public final class GameState{
    /** A private copy of the sixteen cell exponents, where 0 means empty. */
    public final byte[] cells;
    /** The score at the moment the snapshot was taken. */
    public final int score;
    /** The number of moves played at the moment the snapshot was taken. */
    public final int moveCount;
    /** True when a 2048 tile had already been reached. */
    public final boolean won;
    /** True when the game had already been lost. */
    public final boolean lost;
    /**
     * Creates a snapshot. The supplied array is copied so that later board
     * changes cannot alter the snapshot.
     * @param cells the sixteen cell exponents to copy
     * @param score the score to remember
     * @param moveCount the move count to remember
     * @param won the win flag to remember
     * @param lost the loss flag to remember
     */
    public GameState(byte[] cells,int score,int moveCount,boolean won,boolean lost){
        this.cells=cells.clone();
        this.score=score;
        this.moveCount=moveCount;
        this.won=won;
        this.lost=lost;
    }
}
