package org.twozerofoureightgame.model;
/**
 * The complete description of a single applied move: how much score it earned,
 * how many merges happened, whether the board changed at all, and where every
 * individual tile travelled. The view layer needs the individual tile journeys
 * to animate a slide, a merge pop, and a spawn fade.
 * The transition array is pooled and sized generously: a full board can produce
 * sixteen tile transitions plus one spawn transition, so the capacity is
 * thirty-two. Only one move is ever in flight, so a single reusable instance is
 * owned by the board and consumed synchronously by the view.
 */
public final class MoveResult{
    /** How many points this move added to the score. */
    public int scoreGained;
    /** How many pairs of tiles merged during this move. */
    public int mergeCount;
    /** False when the requested direction was not a legal move at all. */
    public boolean boardChanged;
    /** The pooled per-tile journeys produced by this move. */
    public final TileTransition[] tileTransitions=new TileTransition[32];
    /** How many entries of {@link #tileTransitions} are meaningful. */
    public int tileTransitionCount;
    /**
     * Creates a reusable move result with a pre-built pool of transition objects.
     */
    public MoveResult(){
        for(int index=0;index<tileTransitions.length;index++){
            tileTransitions[index]=new TileTransition();
        }
    }
    /**
     * Clears the result so that it can describe the next move.
     */
    public void reset(){
        scoreGained=0;
        mergeCount=0;
        boardChanged=false;
        tileTransitionCount=0;
    }
    /**
     * Records one tile journey into the pool. The call is ignored when the pool
     * is already full, which cannot happen for a legal four by four move.
     * @param fromRow the row the tile came from
     * @param fromColumn the column the tile came from
     * @param toRow the row the tile travels to
     * @param toColumn the column the tile travels to
     * @param oldExponent the exponent the tile carried before the move
     * @param newExponent the exponent the tile carries after the move
     * @param merged true when this tile merged into an existing tile
     * @param isNewSpawn true when this transition describes a spawned tile
     */
    public void addTransition(int fromRow,int fromColumn,int toRow,int toColumn,byte oldExponent,byte newExponent,boolean merged,boolean isNewSpawn){
        if(tileTransitionCount>=tileTransitions.length){
            return;
        }
        TileTransition transition=tileTransitions[tileTransitionCount];
        transition.fromRow=fromRow;
        transition.fromColumn=fromColumn;
        transition.toRow=toRow;
        transition.toColumn=toColumn;
        transition.oldExponent=oldExponent;
        transition.newExponent=newExponent;
        transition.merged=merged;
        transition.isNewSpawn=isNewSpawn;
        tileTransitionCount++;
    }
}
