package org.twozerofoureightgame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.twozerofoureightgame.ai.HeuristicEvaluator;
import org.twozerofoureightgame.ai.PackedBoardUtility;
/**
 * States the three promises the evaluator makes. It judges a board the same way
 * whichever way up or left the board is turned, it prefers a board whose tiles
 * climb in a tidy snake over the same tiles scattered at random, and it prefers
 * the largest tile sitting in a corner rather than in the middle of an edge.
 */
class HeuristicEvaluatorTest{
    private static byte[] cellsFrom(int... tileValues){
        byte[] cells=new byte[16];
        for(int index=0;index<16;index++){
            if(tileValues[index]!=0){
                cells[index]=(byte)Integer.numberOfTrailingZeros(tileValues[index]);
            }
        }
        return cells;
    }
    private static long packedFrom(int... tileValues){
        return PackedBoardUtility.pack(cellsFrom(tileValues));
    }
    private static long rotateClockwise(long packed){
        byte[] cells=new byte[16];
        PackedBoardUtility.unpack(packed,cells);
        byte[] rotated=new byte[16];
        for(int row=0;row<4;row++){
            for(int column=0;column<4;column++){
                rotated[column*4+3-row]=cells[row*4+column];
            }
        }
        return PackedBoardUtility.pack(rotated);
    }
    private static long mirrorHorizontally(long packed){
        byte[] cells=new byte[16];
        PackedBoardUtility.unpack(packed,cells);
        byte[] mirrored=new byte[16];
        for(int row=0;row<4;row++){
            for(int column=0;column<4;column++){
                mirrored[row*4+3-column]=cells[row*4+column];
            }
        }
        return PackedBoardUtility.pack(mirrored);
    }
    @Test
    void turningTheBoardChangesTheScoreBecauseTheEvaluatorPrefersOneCorner(){
        long upright=packedFrom(2,0,0,0, 4,0,0,0, 0,0,0,0, 0,0,0,8);
        long turned=rotateClockwise(upright);
        assertTrue(HeuristicEvaluator.evaluate(upright)!=HeuristicEvaluator.evaluate(turned),"an evaluator that ignored turning could not tell the search where to build");
    }
    @Test
    void theLargestTileWorthMostInTheCornerTheSnakeClimbsTowards(){
        long inPreferredCorner=packedFrom(0,0,0,0, 0,0,0,0, 0,0,0,0, 2,4,0,8);
        long inOppositeCorner=packedFrom(2,4,0,8, 0,0,0,0, 0,0,0,0, 0,0,0,0);
        assertTrue(HeuristicEvaluator.evaluate(inPreferredCorner)>HeuristicEvaluator.evaluate(inOppositeCorner));
    }
    @Test
    void transposingThePackedBoardMovesTheTilesToTheMirroredPosition(){
        long packed=packedFrom(2,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,4);
        byte[] original=new byte[16];
        PackedBoardUtility.unpack(packed,original);
        byte[] transposed=new byte[16];
        PackedBoardUtility.unpack(PackedBoardUtility.transpose(packed),transposed);
        for(int row=0;row<4;row++){
            for(int column=0;column<4;column++){
                assertEquals(original[row*4+column],transposed[column*4+row]);
            }
        }
        assertEquals(original[0],transposed[0]);
        assertEquals(original[15],transposed[15]);
    }
    @Test
    void aSnakeOfTilesIsPreferredOverTheSameTilesScattered(){
        long snake=packedFrom(2,4,8,16, 32,0,0,0, 64,0,0,0, 128,0,0,0);
        long scattered=packedFrom(2,0,0,128, 0,0,64,0, 0,0,0,0, 0,32,0,16);
        assertTrue(HeuristicEvaluator.evaluate(snake)>HeuristicEvaluator.evaluate(scattered));
    }

    @Test
    void twoEqualNeighboursAreWorthMoreThanTheSameTilesKeptApart(){
        long adjacent=packedFrom(0,0,0,0, 0,0,0,0, 0,0,0,0, 2,2,0,0);
        long apart=packedFrom(0,0,0,0, 0,0,0,0, 0,0,0,0, 2,0,0,2);
        assertTrue(HeuristicEvaluator.evaluate(adjacent)>HeuristicEvaluator.evaluate(apart));
    }
    @Test
    void tilesInTheSamePlacesScoreTheSameWhateverTheBoardLooksLikeAround(){
        long one=packedFrom(2,4,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0);
        long two=packedFrom(2,4,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0);
        assertEquals(HeuristicEvaluator.evaluate(one),HeuristicEvaluator.evaluate(two));
    }
    @Test
    void theSnakeIsTheDominantTermSoThatTheLargestTileDecidesTheScore(){
        long anchorHere=packedFrom(2,4,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,512);
        long anchorMoved=packedFrom(2,4,0,0, 0,0,0,0, 0,0,0,0, 512,0,0,0);
        long difference=HeuristicEvaluator.evaluate(anchorMoved)-HeuristicEvaluator.evaluate(anchorHere);
        assertTrue(difference>1000000,"moving the largest tile a few cells should matter enormously");
    }

}
