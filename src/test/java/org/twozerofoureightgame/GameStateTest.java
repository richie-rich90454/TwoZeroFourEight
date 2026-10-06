package org.twozerofoureightgame;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.twozerofoureightgame.model.Board;
import org.twozerofoureightgame.model.Direction;
import org.twozerofoureightgame.model.GameState;
/**
 * Checks that a snapshot is a photograph rather than a window: it keeps the score,
 * the move count, and both end of game flags, and it keeps the cells even after
 * the board it came from has been changed. Restoring one puts every cell back
 * exactly where it was.
 */
class GameStateTest{
    private static Board boardFrom(int... tileValues){
        Board board=new Board();
        for(int index=0;index<16;index++){
            byte exponent=0;
            if(tileValues[index]!=0){
                exponent=(byte)Integer.numberOfTrailingZeros(tileValues[index]);
            }
            board.setExponentAt(index/4,index%4,exponent);
        }
        return board;
    }
    private static int[] tilesOf(Board board){
        int[] tileValues=new int[16];
        for(int index=0;index<16;index++){
            tileValues[index]=1<<board.getExponentAt(index/4,index%4);
        }
        return tileValues;
    }
    @Test
    void aSnapshotKeepsTheScoreTheMoveCountAndBothFlags(){
        GameState state=new GameState(new byte[16],1234,42,true,false);
        assertEquals(1234,state.score);
        assertEquals(42,state.moveCount);
        assertTrue(state.won);
        assertFalse(state.lost);
    }
    @Test
    void aSnapshotIsUnchangedByLaterEditsToTheArrayItWasGiven(){
        byte[] cells=new byte[16];
        cells[0]=3;
        GameState state=new GameState(cells,0,0,false,false);
        cells[0]=9;
        assertEquals(3,state.cells[0]);
    }
    @Test
    void restoringASnapshotPutsEveryCellBack(){
        Board board=boardFrom(2,2,4,4, 8,0,0,16, 32,64,0,128, 256,512,1024,2);
        int[] before=tilesOf(board);
        GameState state=new GameState(board.snapshotCells(),900,17,false,false);
        board.applyMove(Direction.LEFT);
        board.applyMove(Direction.UP);
        board.setExponentAt(3,3,(byte)7);
        assertFalse(java.util.Arrays.equals(before,tilesOf(board)));
        board.restoreFrom(state.cells);
        assertArrayEquals(before,tilesOf(board));
        assertEquals(10,board.getMaximumExponentOnBoard());
    }
    @Test
    void restoringASnapshotAlsoRebuildsTheEmptyCellCount(){
        Board board=boardFrom(2,4,8,16, 32,64,128,256, 512,1024,2048,1024, 512,256,128,64);
        assertEquals(0,board.countEmptyCells());
        GameState state=new GameState(board.snapshotCells(),0,0,false,false);
        board.setExponentAt(0,0,(byte)0);
        assertEquals(1,board.countEmptyCells());
        board.restoreFrom(state.cells);
        assertEquals(0,board.countEmptyCells());
    }
}
