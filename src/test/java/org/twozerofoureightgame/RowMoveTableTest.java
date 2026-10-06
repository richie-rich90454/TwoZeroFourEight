package org.twozerofoureightgame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.twozerofoureightgame.ai.RowMoveTable;
/**
 * Checks the precomputed row tables against a plain reference implementation
 * written in the most obvious way possible. The reference builds an ordinary
 * list of tiles, packs them towards one edge, and merges neighbours in pairs,
 * which is exactly how a person would describe the rule. The tables are then
 * asked about several thousand fixed and random patterns and must agree with
 * that description every time.
 */
class RowMoveTableTest{
    private static int[] naiveRowMoveOutcome(int[] originalExponents,boolean towardsRight){
        int[] packed=new int[4];
        int tileCount=0;
        for(int position=0;position<4;position++){
            int readPosition=towardsRight?3-position:position;
            if(originalExponents[readPosition]!=0){
                packed[tileCount++]=originalExponents[readPosition];
            }
        }
        int[] resultExponents=new int[4];
        int writePosition=towardsRight?3:0;
        int score=0;
        int readIndex=0;
        while(readIndex<tileCount){
            if(readIndex+1<tileCount&&packed[readIndex]==packed[readIndex+1]){
                int mergedExponent=packed[readIndex]+1;
                if(mergedExponent>RowMoveTable.MAXIMUM_EXPONENT){
                    mergedExponent=RowMoveTable.MAXIMUM_EXPONENT;
                }
                resultExponents[writePosition]=mergedExponent;
                score+=1<<mergedExponent;
                if(score>RowMoveTable.MAXIMUM_PACKED_SCORE){
                    score=RowMoveTable.MAXIMUM_PACKED_SCORE;
                }
                readIndex+=2;
            }else{
                resultExponents[writePosition]=packed[readIndex];
                readIndex++;
            }
            writePosition+=towardsRight?-1:1;
        }
        return new int[]{patternOf(resultExponents),score};
    }
    private static int patternOf(int[] exponents){
        int pattern=0;
        for(int position=0;position<4;position++){
            pattern|=exponents[position]<<(position*4);
        }
        return pattern;
    }
    private static int[] exponentsOf(int pattern){
        int[] exponents=new int[4];
        for(int position=0;position<4;position++){
            exponents[position]=(pattern>>(position*4))&0xF;
        }
        return exponents;
    }
    private static void compareAgainstReference(int pattern){
        int[] exponents=exponentsOf(pattern);
        int leftOutcome=RowMoveTable.moveLeft(pattern);
        int[] expectedLeft=naiveRowMoveOutcome(exponents,false);
        assertEquals(expectedLeft[0],RowMoveTable.outcomePattern(leftOutcome),"left pattern for "+Integer.toHexString(pattern));
        assertEquals(expectedLeft[1],RowMoveTable.outcomeScore(leftOutcome),"left score for "+Integer.toHexString(pattern));
        assertEquals(pattern!=RowMoveTable.outcomePattern(leftOutcome),RowMoveTable.outcomeChanged(leftOutcome,pattern),"left changed for "+Integer.toHexString(pattern));
        int rightOutcome=RowMoveTable.moveRight(pattern);
        int[] expectedRight=naiveRowMoveOutcome(exponents,true);
        assertEquals(expectedRight[0],RowMoveTable.outcomePattern(rightOutcome),"right pattern for "+Integer.toHexString(pattern));
        assertEquals(expectedRight[1],RowMoveTable.outcomeScore(rightOutcome),"right score for "+Integer.toHexString(pattern));
        assertEquals(pattern!=RowMoveTable.outcomePattern(rightOutcome),RowMoveTable.outcomeChanged(rightOutcome,pattern),"right changed for "+Integer.toHexString(pattern));
    }
    @Test
    void anEmptyRowStaysEmptyAndEarnsNothing(){
        assertEquals(0,RowMoveTable.outcomePattern(RowMoveTable.moveLeft(0)));
        assertEquals(0,RowMoveTable.outcomeScore(RowMoveTable.moveLeft(0)));
        assertEquals(false,RowMoveTable.outcomeChanged(RowMoveTable.moveLeft(0),0));
    }
    @Test
    void aSingleTileSlidesIntoTheFirstCell(){
        int pattern=2<<8;
        assertEquals(2,RowMoveTable.outcomePattern(RowMoveTable.moveLeft(pattern)));
        assertEquals(2<<12,RowMoveTable.outcomePattern(RowMoveTable.moveRight(pattern)));
    }
    @Test
    void twoEqualTilesMergeAndPayTheirNewValue(){
        int pattern=3|3<<4;
        assertEquals(4,RowMoveTable.outcomePattern(RowMoveTable.moveLeft(pattern)));
        assertEquals(16,RowMoveTable.outcomeScore(RowMoveTable.moveLeft(pattern)));
        assertEquals(4<<12,RowMoveTable.outcomePattern(RowMoveTable.moveRight(pattern)));
        assertEquals(16,RowMoveTable.outcomeScore(RowMoveTable.moveRight(pattern)));
    }
    @Test
    void aRowOfFourEqualTilesBecomesTwoMergedTiles(){
        int pattern=11|11<<4|11<<8|11<<12;
        assertEquals(12|12<<4,RowMoveTable.outcomePattern(RowMoveTable.moveLeft(pattern)));
        assertEquals(4096+4096,RowMoveTable.outcomeScore(RowMoveTable.moveLeft(pattern)));
    }
    @Test
    void theTablesAgreeWithTheReferenceOnSeveralThousandPatterns(){
        java.util.Random random=new java.util.Random(2026L);
        for(int attempt=0;attempt<4000;attempt++){
            compareAgainstReference(random.nextInt(RowMoveTable.TABLE_SIZE));
        }
    }
    @Test
    void theTablesAgreeWithTheReferenceOnEverySmallPattern(){
        for(int pattern=0;pattern<RowMoveTable.TABLE_SIZE;pattern+=97){
            compareAgainstReference(pattern);
        }
    }
}
