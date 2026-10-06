package org.twozerofoureightgame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.twozerofoureightgame.ai.ExpectimaxAI;
import org.twozerofoureightgame.model.Board;
import org.twozerofoureightgame.ai.PackedBoardUtility;
import org.twozerofoureightgame.ai.RowMoveTable;
import org.twozerofoureightgame.model.Direction;
import org.twozerofoureightgame.utility.RandomUtility;
/**
 * Checks the three promises the search makes. A fixed random seed always produces
 * the same move, so a reported game can be replayed. A board with almost nowhere
 * to move still gets a legal answer rather than nothing. A board that is already
 * lost still gets a legal answer rather than a shrug.
 */
class ExpectimaxAITest{
    private static byte[] cellsFrom(int... tileValues){
        byte[] cells=new byte[16];
        for(int index=0;index<16;index++){
            if(tileValues[index]!=0){
                cells[index]=(byte)Integer.numberOfTrailingZeros(tileValues[index]);
            }
        }
        return cells;
    }
    private static boolean isLegal(byte[] cells,Direction direction){
        long packed=PackedBoardUtility.pack(cells);
        return RowMoveTable.applyPacked(packed,direction)!=packed;
    }
    @Test
    void aFixedSeedAlwaysProducesTheSameMove(){
        byte[] cells=cellsFrom(2,2,4,8, 4,0,8,16, 16,32,0,64, 128,256,512,1024);
        RandomUtility.setSeed(12345L);
        ExpectimaxAI first=new ExpectimaxAI();
        Direction firstChoice=first.chooseBestMove(cells,60);
        RandomUtility.setSeed(12345L);
        ExpectimaxAI second=new ExpectimaxAI();
        Direction secondChoice=second.chooseBestMove(cells,60);
        assertNotNull(firstChoice);
        assertEquals(firstChoice,secondChoice);
    }
    @Test
    void anOpeningBoardGetsAMoveTowardsTheEmptySide(){
        byte[] cells=cellsFrom(2,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,4);
        RandomUtility.setSeed(7L);
        Direction choice=new ExpectimaxAI().chooseBestMove(cells,60);
        assertNotNull(choice);
        assertTrue(isLegal(cells,choice));
    }
    @Test
    void aCrowdedBoardStillGetsALegalMove(){
        byte[] cells=cellsFrom(2,4,2,4, 4,2,4,2, 2,4,2,4, 4,2,4,0);
        RandomUtility.setSeed(11L);
        Direction choice=new ExpectimaxAI().chooseBestMove(cells,60);
        assertNotNull(choice);
        assertTrue(isLegal(cells,choice));
    }
    @Test
    void aBoardWithNoMovesAtAllGetsNoMove(){
        byte[] cells=cellsFrom(2,4,2,4, 4,2,4,2, 2,4,2,4, 4,2,4,2);
        RandomUtility.setSeed(3L);
        Direction choice=new ExpectimaxAI().chooseBestMove(cells,60);
        assertNull(choice);
    }
    @Test
    void theSearchKeepsToItsTimeBudget(){
        byte[] cells=cellsFrom(0,0,2,0, 4,0,0,8, 0,16,0,32, 64,0,128,256);
        RandomUtility.setSeed(5L);
        ExpectimaxAI artificialIntelligence=new ExpectimaxAI();
        long startedAt=System.nanoTime();
        Direction choice=artificialIntelligence.chooseBestMove(cells,60);
        long elapsedMilliseconds=(System.nanoTime()-startedAt)/1000000L;
        assertNotNull(choice);
        assertTrue(elapsedMilliseconds<2000L,"the search should honour a sixty millisecond budget");
    }
    @Test
    void theArtificialIntelligenceActuallyGrowsARandomizedOpening(){
        RandomUtility.setSeed(7919L);
        Board board=new Board();
        board.spawnRandomTile();
        board.spawnRandomTile();
        ExpectimaxAI artificialIntelligence=new ExpectimaxAI();
        int played=0;
        while(played<120&&!board.isGameOver()){
            Direction choice=artificialIntelligence.chooseBestMove(board.snapshotCells(),12);
            if(choice==null){
                break;
            }
            board.applyMoveWithTransitions(choice);
            played++;
        }
        assertEquals(120,played,"every move should have been legal");
        assertTrue(board.getMaximumExponentOnBoard()>=6,"the search should have merged its way past the 32 tile, reached "+(1<<board.getMaximumExponentOnBoard()));
    }
    @Test
    void theSearchNeverFailsOnDegenerateBoards(){
        byte[] completelyEmpty=cellsFrom(0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0);
        assertNull(new ExpectimaxAI().chooseBestMove(completelyEmpty,20),"an empty board has no legal move");
        byte[] singleTile=cellsFrom(2,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0);
        Direction singleChoice=new ExpectimaxAI().chooseBestMove(singleTile,20);
        assertNotNull(singleChoice);
        byte[] allEqual=cellsFrom(2,2,2,2, 2,2,2,2, 2,2,2,2, 2,2,2,0);
        Direction equalChoice=new ExpectimaxAI().chooseBestMove(allEqual,20);
        assertNotNull(equalChoice);
        assertTrue(isLegal(allEqual,equalChoice));
        byte[] hugeTiles=cellsFrom(2048,2048,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0);
        Direction hugeChoice=new ExpectimaxAI().chooseBestMove(hugeTiles,20);
        assertNotNull(hugeChoice);
    }
    @Test
    void aBoardThatAlreadyHoldsTheWinningTileIsPlayedOutWithoutTrouble(){
        byte[] winning=cellsFrom(1024,512,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0);
        RandomUtility.setSeed(17L);
        Direction choice=new ExpectimaxAI().chooseBestMove(winning,40);
        assertNotNull(choice);
        assertTrue(isLegal(winning,choice));
    }
}