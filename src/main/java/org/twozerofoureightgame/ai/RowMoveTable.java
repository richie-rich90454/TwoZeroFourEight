package org.twozerofoureightgame.ai;
import org.twozerofoureightgame.model.Direction;
/**
 * Precomputes, for every one of the sixty five thousand five hundred and
 * thirty six possible four cell rows, what a single slide does to that row. A
 * row holds four exponents, so it fits in sixteen bits, and the answer for a row
 * is stored in a single int: bits 0 through 15 hold the new row pattern and bits
 * 16 through 31 hold the score the slide earns. Whether a row actually moved is
 * not stored, because a slide is legal exactly when the new pattern differs from
 * the old one, and comparing two patterns costs nothing.
 * Applying a move to a whole board is therefore four table lookups, and vertical
 * moves reuse the same tables after transposing the board.
 * The two tables occupy six hundred and fifty kilobytes of memory and are built
 * once when the class is first loaded.
 * NOTE: the score field is sixteen bits wide rather than twelve. A single row can
 * earn far more than four thousand and ninety five points once play continues
 * past the 2048 tile, because a row of four 2048 tiles alone merges into eight
 * thousand one hundred and ninety two points, and twelve bits would silently
 * corrupt both the score and the pattern. Two further ceilings follow from the
 * sixteen bit score and the four bit cells: a merged exponent is held at fifteen
 * so that a merge can never spill into the neighbouring nibble, and a row score
 * is held at sixty five thousand five hundred and thirty five. Both ceilings sit
 * far beyond any position a player can reach, because the game normally ends the
 * moment a 2048 tile appears.
 */
public final class RowMoveTable{
    /** Keeps only the sixteen bit row pattern out of a packed outcome. */
    public static final int PATTERN_MASK=0xFFFF;
    /** The bit position where the score begins inside a packed outcome. */
    public static final int SCORE_SHIFT=16;
    /** How many table entries exist, one for every possible row pattern. */
    public static final int TABLE_SIZE=1<<16;
    /** The largest score a single row may store before it is clamped. */
    public static final int MAXIMUM_PACKED_SCORE=0xFFFF;
    /** The largest exponent a nibble can hold. */
    public static final int MAXIMUM_EXPONENT=15;
    private static final int[] LEFT_TABLE=new int[TABLE_SIZE];
    private static final int[] RIGHT_TABLE=new int[TABLE_SIZE];
    static{
        for(int pattern=0;pattern<TABLE_SIZE;pattern++){
            LEFT_TABLE[pattern]=buildLeftOutcome(pattern);
            RIGHT_TABLE[pattern]=buildRightOutcome(pattern);
        }
    }
    /**
     * This class only exposes static helpers and is never instantiated.
     */
    private RowMoveTable(){
    }
    private static int buildLeftOutcome(int pattern){
        int[] exponents=new int[4];
        int tileCount=0;
        for(int position=0;position<4;position++){
            int exponent=(pattern>>(position*4))&0xF;
            if(exponent!=0){
                exponents[tileCount++]=exponent;
            }
        }
        int newPattern=0;
        int writePosition=0;
        int score=0;
        for(int readIndex=0;readIndex<tileCount;readIndex++){
            if(readIndex+1<tileCount&&exponents[readIndex]==exponents[readIndex+1]){
                int mergedExponent=clampExponent(exponents[readIndex]+1);
                newPattern|=mergedExponent<<(writePosition*4);
                score=clampScore(score+(1<<mergedExponent));
                writePosition++;
                readIndex++;
            }else{
                newPattern|=exponents[readIndex]<<(writePosition*4);
                writePosition++;
            }
        }
        return newPattern|(score<<SCORE_SHIFT);
    }
    private static int buildRightOutcome(int pattern){
        int[] exponents=new int[4];
        int tileCount=0;
        for(int position=3;position>=0;position--){
            int exponent=(pattern>>(position*4))&0xF;
            if(exponent!=0){
                exponents[tileCount++]=exponent;
            }
        }
        int newPattern=0;
        int writePosition=3;
        int score=0;
        for(int readIndex=0;readIndex<tileCount;readIndex++){
            if(readIndex+1<tileCount&&exponents[readIndex]==exponents[readIndex+1]){
                int mergedExponent=clampExponent(exponents[readIndex]+1);
                newPattern|=mergedExponent<<(writePosition*4);
                score=clampScore(score+(1<<mergedExponent));
                writePosition--;
                readIndex++;
            }else{
                newPattern|=exponents[readIndex]<<(writePosition*4);
                writePosition--;
            }
        }
        return newPattern|(score<<SCORE_SHIFT);
    }
    private static int clampExponent(int mergedExponent){
        if(mergedExponent>MAXIMUM_EXPONENT){
            return MAXIMUM_EXPONENT;
        }
        return mergedExponent;
    }
    private static int clampScore(int score){
        if(score>MAXIMUM_PACKED_SCORE){
            return MAXIMUM_PACKED_SCORE;
        }
        return score;
    }
    /**
     * Forces the two tables to be built now. Building them costs about
     * twenty five milliseconds, and if that cost were paid inside the first
     * move's time budget the search would starve on the opening move and the
     * artificial intelligence would fall back on its first guess. Calling this
     * during start up moves the cost to a moment when it does not matter.
     */
    public static void prepare(){
        if(LEFT_TABLE[0]==Integer.MIN_VALUE){
            throw new IllegalStateException("The row move tables were not built.");
        }
    }
    /**
     * Looks up what a slide to the left does to one row.
     * @param pattern the sixteen bit row pattern before the slide
     * @return the packed outcome holding the new pattern and the earned score
     */
    public static int moveLeft(int pattern){
        return LEFT_TABLE[pattern];
    }
    /**
     * Looks up what a slide to the right does to one row.
     * @param pattern the sixteen bit row pattern before the slide
     * @return the packed outcome holding the new pattern and the earned score
     */
    public static int moveRight(int pattern){
        return RIGHT_TABLE[pattern];
    }
    /**
     * Extracts the new row pattern from a packed outcome.
     * @param outcome the packed outcome
     * @return the sixteen bit row pattern after the slide
     */
    public static int outcomePattern(int outcome){
        return outcome&PATTERN_MASK;
    }
    /**
     * Extracts the earned score from a packed outcome.
     * @param outcome the packed outcome
     * @return the number of points the row slide earned
     */
    public static int outcomeScore(int outcome){
        return outcome>>>SCORE_SHIFT;
    }
    /**
     * Reports whether a row slide actually changed anything.
     * @param outcome the packed outcome of the slide
     * @param originalPattern the row pattern before the slide
     * @return true when the row changed, which is exactly when a tile moved
     */
    public static boolean outcomeChanged(int outcome,int originalPattern){
        return outcomePattern(outcome)!=originalPattern;
    }
    /**
     * Applies one direction to a whole packed board. Horizontal moves use the
     * matching row table on each row; vertical moves transpose the board so that
     * columns become rows, apply the horizontal table, and transpose back.
     * @param packed the packed board before the move
     * @param direction the direction to move towards
     * @return the packed board after the move, which equals the input when the
     *         move was illegal
     */
    public static long applyPacked(long packed,Direction direction){
        boolean horizontal=direction.isHorizontal();
        boolean useLeftTable=direction==Direction.LEFT||direction==Direction.UP;
        long source=horizontal?packed:PackedBoardUtility.transpose(packed);
        long result=0L;
        for(int row=0;row<4;row++){
            int pattern=(int)((source>>>(row*16))&PATTERN_MASK);
            int outcome=useLeftTable?LEFT_TABLE[pattern]:RIGHT_TABLE[pattern];
            result|=(long)(outcome&PATTERN_MASK)<<(row*16);
        }
        if(horizontal){
            return result;
        }
        return PackedBoardUtility.transpose(result);
    }
    /**
     * Sums the score earned by every row that changed during a move.
     * @param packedBefore the packed board before the move
     * @param packedAfter the packed board after the move
     * @param direction the direction that was applied
     * @return the total number of points the move earned
     */
    public static int scoreGained(long packedBefore,long packedAfter,Direction direction){
        if(packedBefore==packedAfter){
            return 0;
        }
        boolean horizontal=direction.isHorizontal();
        boolean useLeftTable=direction==Direction.LEFT||direction==Direction.UP;
        long sourceBefore=horizontal?packedBefore:PackedBoardUtility.transpose(packedBefore);
        long sourceAfter=horizontal?packedAfter:PackedBoardUtility.transpose(packedAfter);
        int total=0;
        for(int row=0;row<4;row++){
            int patternBefore=(int)((sourceBefore>>>(row*16))&PATTERN_MASK);
            int patternAfter=(int)((sourceAfter>>>(row*16))&PATTERN_MASK);
            if(patternBefore!=patternAfter){
                int outcome=useLeftTable?LEFT_TABLE[patternBefore]:RIGHT_TABLE[patternBefore];
                total+=outcomeScore(outcome);
            }
        }
        return total;
    }
}
