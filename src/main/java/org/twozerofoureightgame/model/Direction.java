package org.twozerofoureightgame.model;
/**
 * The four directions a tile can travel across the board. Every movement in the
 * game, in the artificial intelligence search, and in the animations is
 * expressed with one of these four values, which keeps the movement vocabulary
 * of the whole program small and easy to reason about.
 */
public enum Direction{
    LEFT,
    RIGHT,
    UP,
    DOWN;
    /**
     * Returns the direction that travels the same distance in the opposite
     * sense. The value is computed with a switch so that no lookup table and no
     * object is created at run time.
     * @return the opposite direction
     */
    public Direction oppositeDirection(){
        switch(this){
            case LEFT:{
                return RIGHT;
            }
            case RIGHT:{
                return LEFT;
            }
            case UP:{
                return DOWN;
            }
            default:{
                return UP;
            }
        }
    }
    /**
     * Reports whether this direction travels along a row of the board instead of
     * along a column. The row move table only handles horizontal movement, so
     * vertical movement is produced by transposing the board first.
     * @return true for LEFT and RIGHT, false for UP and DOWN
     */
    public boolean isHorizontal(){
        return this==LEFT||this==RIGHT;
    }
}
