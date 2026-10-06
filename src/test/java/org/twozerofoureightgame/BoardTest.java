package org.twozerofoureightgame;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.twozerofoureightgame.model.Board;
import org.twozerofoureightgame.model.Direction;
import org.twozerofoureightgame.model.MoveResult;
import org.twozerofoureightgame.model.TileTransition;
/**
 * Reads like the specification of the board: a move slides tiles towards one
 * edge, merges equal neighbours once each, pays out the value of every merged
 * tile, and refuses a move that would change nothing.
 */
class BoardTest{
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
            byte exponent=board.getExponentAt(index/4,index%4);
            tileValues[index]=exponent==0?0:1<<exponent;
        }
        return tileValues;
    }
    @Test
    void aMoveTowardsLeftSlidesTilesTowardsTheFirstColumn(){
        Board board=boardFrom(0,0,4,0, 0,0,0,0, 0,0,0,0, 0,0,0,0);
        int scoreGained=board.applyMove(Direction.LEFT);
        assertEquals(2,board.getExponentAt(0,0));
        assertEquals(0,board.getExponentAt(0,1));
        assertEquals(0,board.getExponentAt(0,2));
        assertEquals(0,scoreGained);
    }
    @Test
    void aMoveTowardsLeftMergesTwoEqualNeighboursAndPaysTheirValue(){
        Board board=boardFrom(2,2,4,0, 0,0,0,0, 0,0,0,0, 0,0,0,0);
        int scoreGained=board.applyMove(Direction.LEFT);
        assertEquals(2,board.getExponentAt(0,0));
        assertEquals(2,board.getExponentAt(0,1));
        assertEquals(4,scoreGained);
    }
    @Test
    void aTileThatJustAppearedCannotMergeAgainInTheSameMove(){
        Board board=boardFrom(2,2,2,2, 0,0,0,0, 0,0,0,0, 0,0,0,0);
        board.applyMove(Direction.LEFT);
        assertArrayEquals(new int[]{4,4,0,0},firstRowOf(tilesOf(board)));
    }
    @Test
    void scoreAddsUpForEveryRowThatMerges(){
        Board board=boardFrom(2,2,0,0, 2,2,0,0, 0,0,0,0, 0,0,0,0);
        int firstMove=board.applyMove(Direction.LEFT);
        int secondMove=board.applyMove(Direction.DOWN);
        assertEquals(8,firstMove);
        assertEquals(8,secondMove);
    }
    @Test
    void aDirectionThatChangesNothingIsRefused(){
        Board board=boardFrom(2,4,8,0, 0,0,0,0, 0,0,0,0, 0,0,0,0);
        assertFalse(board.canMove(Direction.LEFT));
        assertEquals(0,board.applyMove(Direction.LEFT));
        assertTrue(board.canMove(Direction.RIGHT));
    }
    @Test
    void aFullBoardOfAlternatingTilesIsAGameOver(){
        Board board=boardFrom(2,4,2,4, 4,2,4,2, 2,4,2,4, 4,2,4,2);
        assertTrue(board.isGameOver());
        assertEquals(0,board.countEmptyCells());
    }
    @Test
    void aBoardWithOneMatchingPairIsNotAGameOver(){
        Board board=boardFrom(2,4,2,4, 4,2,4,2, 2,4,2,4, 4,2,4,2);
        board.setExponentAt(3,3,(byte)2);
        assertTrue(board.canMove(Direction.RIGHT));
        assertFalse(board.isGameOver());
    }
    @Test
    void packingAndUnpackingAWholeBoardPreservesEveryCell(){
        Board board=boardFrom(2,4,8,16, 32,64,128,256, 512,1024,2048,0, 4,4,4,4);
        long packed=board.toPackedLong();
        Board restored=new Board();
        restored.fromPackedLong(packed);
        assertArrayEquals(tilesOf(board),tilesOf(restored));
        assertEquals(board.countEmptyCells(),restored.countEmptyCells());
        assertEquals(board.getMaximumExponentOnBoard(),restored.getMaximumExponentOnBoard());
    }
    @Test
    void aMoveReportsWhereEveryTileTravelled(){
        Board board=boardFrom(0,0,2,2, 0,0,0,0, 0,0,0,0, 0,0,0,0);
        MoveResult result=board.applyMoveWithTransitions(Direction.LEFT);
        assertTrue(result.boardChanged);
        assertEquals(1,result.mergeCount);
        assertEquals(4,result.scoreGained);
        assertEquals(3,result.tileTransitionCount);
        assertEquals(2,result.tileTransitions[0].fromColumn);
        assertEquals(0,result.tileTransitions[0].toColumn);
        assertEquals(3,result.tileTransitions[1].fromColumn);
        assertEquals(0,result.tileTransitions[1].toColumn);
        assertTrue(result.tileTransitions[1].merged);
        assertTrue(result.tileTransitions[2].isNewSpawn);
        assertEquals(2,board.getExponentAt(0,0));
        assertEquals(14,board.countEmptyCells());
        int tilesOnBoard=0;
        for(int row=0;row<4;row++){
            for(int column=0;column<4;column++){
                if(board.getExponentAt(row,column)!=0){
                    tilesOnBoard++;
                }
            }
        }
        assertEquals(2,tilesOnBoard,"the merged tile plus the one freshly spawned tile");
    }
    @Test
    void aSnapshotIsUnaffectedByLaterChangesToTheBoard(){
        Board board=boardFrom(2,2,4,4, 8,8,0,0, 0,0,0,0, 0,0,0,0);
        byte[] snapshot=board.snapshotCells();
        board.applyMove(Direction.LEFT);
        assertEquals(1,snapshot[0]);
        assertEquals(1,snapshot[1]);
    }
    @Test
    void theRecordedJourneysAlwaysRebuildExactlyTheBoardTheModelProduced(){
        java.util.Random random=new java.util.Random(99L);
        for(int attempt=0;attempt<500;attempt++){
            Board board=boardFrom(randomTiles(random));
            byte[] before=board.snapshotCells();
            Direction direction=Direction.values()[random.nextInt(4)];
            if(!board.canMove(direction)){
                continue;
            }
            MoveResult result=board.applyMoveWithTransitions(direction);
            byte[] rebuilt=new byte[16];
            int spawnCount=0;
            for(int index=0;index<result.tileTransitionCount;index++){
                TileTransition transition=result.tileTransitions[index];
                assertTrue(transition.fromRow>=0&&transition.fromRow<4&&transition.toRow>=0&&transition.toRow<4);
                assertTrue(transition.fromColumn>=0&&transition.fromColumn<4&&transition.toColumn>=0&&transition.toColumn<4);
                if(transition.isNewSpawn){
                    spawnCount++;
                    continue;
                }
                assertTrue(before[transition.fromRow*4+transition.fromColumn]!=0,"a tile can only travel from a cell that held one");
                rebuilt[transition.toRow*4+transition.toColumn]=transition.newExponent;
            }
            assertTrue(spawnCount<=1);
            for(int index=0;index<result.tileTransitionCount;index++){
                TileTransition transition=result.tileTransitions[index];
                if(transition.isNewSpawn){
                    rebuilt[transition.toRow*4+transition.toColumn]=transition.newExponent;
                }
            }
            assertArrayEquals(board.snapshotCells(),rebuilt,"journeys for "+direction);
        }
    }
    @Test
    void thePackedEmptyCellCountAgreesWithTheBoardOnEveryLayout(){
        java.util.Random random=new java.util.Random(4242L);
        for(int attempt=0;attempt<300;attempt++){
            Board board=boardFrom(randomTiles(random));
            long packed=board.toPackedLong();
            assertEquals(board.countEmptyCells(),org.twozerofoureightgame.ai.PackedBoardUtility.countEmptyCells(packed));
            assertEquals(board.getMaximumExponentOnBoard(),org.twozerofoureightgame.ai.PackedBoardUtility.maximumExponent(packed));
        }
        Board fresh=new Board();
        assertEquals(16,org.twozerofoureightgame.ai.PackedBoardUtility.countEmptyCells(fresh.toPackedLong()));
        Board pair=boardFrom(2,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0);
        assertEquals(15,org.twozerofoureightgame.ai.PackedBoardUtility.countEmptyCells(pair.toPackedLong()));
        Board full=boardFrom(2,4,2,4, 4,2,4,2, 2,4,2,4, 4,2,4,2);
        assertEquals(0,org.twozerofoureightgame.ai.PackedBoardUtility.countEmptyCells(full.toPackedLong()));
    }
    @Test
    void everyMergeSendsBothOfItsTilesToTheSameCellSoThePopCanBePaired(){
        java.util.Random random=new java.util.Random(777L);
        for(int attempt=0;attempt<500;attempt++){
            Board board=boardFrom(randomTiles(random));
            Direction direction=Direction.values()[random.nextInt(4)];
            if(!board.canMove(direction)){
                continue;
            }
            MoveResult result=board.applyMoveWithTransitions(direction);
            int[] arrivalsAtEachCell=new int[16];
            int[] mergedArrivalsAtEachCell=new int[16];
            for(int index=0;index<result.tileTransitionCount;index++){
                TileTransition transition=result.tileTransitions[index];
                if(transition.isNewSpawn){
                    continue;
                }
                int destination=transition.toRow*4+transition.toColumn;
                arrivalsAtEachCell[destination]++;
                if(transition.merged){
                    mergedArrivalsAtEachCell[destination]++;
                }
            }
            for(int destination=0;destination<16;destination++){
                if(mergedArrivalsAtEachCell[destination]==0){
                    continue;
                }
                assertEquals(2,mergedArrivalsAtEachCell[destination],"a merge needs both tiles, destination "+destination+" for "+direction);
                assertEquals(2,arrivalsAtEachCell[destination],"only the merged pair may share a cell, destination "+destination+" for "+direction);
            }
        }
    }
    @Test
    void twoTwosThatAlreadySitTogetherStillReportBothTilesAsMerging(){
        Board board=boardFrom(2,2,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0);
        MoveResult result=board.applyMoveWithTransitions(Direction.LEFT);
        int mergedCount=0;
        for(int index=0;index<result.tileTransitionCount;index++){
            if(result.tileTransitions[index].merged){
                mergedCount++;
                assertEquals(0,result.tileTransitions[index].toRow);
                assertEquals(0,result.tileTransitions[index].toColumn);
                assertEquals(2,result.tileTransitions[index].newExponent);
            }
        }
        assertEquals(2,mergedCount);
        assertEquals(2,board.getExponentAt(0,0));
    }
    private static int[] randomTiles(java.util.Random random){
        int[] tileValues=new int[16];
        for(int index=0;index<16;index++){
            tileValues[index]=random.nextInt(100)<55?0:(1<<random.nextInt(6));
        }
        return tileValues;
    }
    private static int[] firstRowOf(int[] tileValues){
        int[] row=new int[4];
        for(int column=0;column<4;column++){
            row[column]=tileValues[column];
        }
        return row;
    }
}
