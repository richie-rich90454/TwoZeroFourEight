package org.twozerofoureightgame.ai;
/**
 * Scores how promising a board looks without searching any further. The score
 * is a single integer built from six cheap features, all computed in one pass
 * over sixteen cells: free space, order, smoothness, corner control, merge
 * availability, and a snake shaped ordering of the tiles.
 * The snake feature is the strongest one. A snake visits the board in a
 * boustrophedon path, starting in the bottom left corner and climbing upwards,
 * and weights the cells so that the path runs from the smallest tile to the
 * largest. Because the path is fixed rather than chosen from eight rotated
 * copies, the evaluator rewards the player for building one consistent
 * structure instead of any structure at all.
 * That fixedness matters enormously. An earlier version took the best of all
 * eight rotations and reflections, which made the score of a board identical
 * under rotation, and therefore made UP and DOWN score exactly the same on a
 * sparse board, and LEFT and RIGHT score exactly the same. The search then had
 * two equally good moves that undid one another, and it played UP followed by
 * DOWN for as long as the clock allowed, never merging anything. A symmetry
 * invariant evaluation cannot express the idea of building somewhere on purpose,
 * so the symmetry was deliberately given up.
 */
public final class HeuristicEvaluator{
    /**
     * The one snake the evaluator uses. Rank zero is the smallest tile and rank
     * fifteen is the largest, which sits in the bottom left corner. The path
     * runs along the bottom row, doubles back along the next, and so on upwards.
     */
    private static final int[] SNAKE_ORDER={12,13,14,15, 11,10,9,8, 4,5,6,7, 3,2,1,0};
    /**
     * The weight of each rank along the snake. The weights climb gently, by one
     * per rank, so that the feature expresses an ordering of the tiles rather
     * than a single preferred cell. An earlier version doubled the weight at
     * every rank, which made the term worth thirty thousand times more at the
     * last rank than at the first. That collapsed the whole term into a
     * question of which one cell held the largest tile, and it drowned the free
     * space, corner, and merge features that the evaluator also needs.
     */
    private static final int[] RANK_WEIGHTS=new int[16];
    static{
        for(int rank=0;rank<16;rank++){
            RANK_WEIGHTS[rank]=1<<(15-rank);
        }
    }
    /**
     * This class only exposes static helpers and is never instantiated.
     */
    private HeuristicEvaluator(){
    }
    /**
     * Scores one packed board.
     * @param packed the packed board to score
     * @return a single integer where a larger number means a more promising board
     */
    public static int evaluate(long packed){
        int emptyCellCount=PackedBoardUtility.countEmptyCells(packed);
        int smoothnessPenalty=0;
        int mergePotential=0;
        int monotonicityPenalty=0;
        for(int row=0;row<4;row++){
            for(int step=0;step<3;step++){
                int firstExponent=PackedBoardUtility.nibbleAt(packed,row*4+step);
                int secondExponent=PackedBoardUtility.nibbleAt(packed,row*4+step+1);
                if(firstExponent!=0&&secondExponent!=0){
                    smoothnessPenalty+=Math.abs(firstExponent-secondExponent);
                }
                if(firstExponent!=0&&firstExponent==secondExponent){
                    mergePotential++;
                }
            }
        }
        for(int column=0;column<4;column++){
            for(int step=0;step<3;step++){
                int firstExponent=PackedBoardUtility.nibbleAt(packed,step*4+column);
                int secondExponent=PackedBoardUtility.nibbleAt(packed,(step+1)*4+column);
                if(firstExponent!=0&&secondExponent!=0){
                    smoothnessPenalty+=Math.abs(firstExponent-secondExponent);
                }
                if(firstExponent!=0&&firstExponent==secondExponent){
                    mergePotential++;
                }
            }
        }
        for(int row=0;row<4;row++){
            monotonicityPenalty+=lineMonotonicityPenalty(packed,row,true);
        }
        for(int column=0;column<4;column++){
            monotonicityPenalty+=lineMonotonicityPenalty(packed,column,false);
        }
        int cornerExponent=Math.max(Math.max(PackedBoardUtility.nibbleAt(packed,0),PackedBoardUtility.nibbleAt(packed,3)),Math.max(PackedBoardUtility.nibbleAt(packed,12),PackedBoardUtility.nibbleAt(packed,15)));
        int snakeScore=0;
        for(int rank=0;rank<16;rank++){
            snakeScore+=PackedBoardUtility.nibbleAt(packed,SNAKE_ORDER[rank])*RANK_WEIGHTS[rank];
        }
        return emptyCellCount*AIWeights.EMPTY_CELL_WEIGHT
            -monotonicityPenalty*AIWeights.MONOTONICITY_WEIGHT
            +smoothnessPenalty*AIWeights.SMOOTHNESS_WEIGHT
            +cornerExponent*AIWeights.CORNER_WEIGHT
            +mergePotential*AIWeights.MERGE_POTENTIAL_WEIGHT
            +snakeScore*AIWeights.SNAKE_PATTERN_WEIGHT;
    }
    /**
     * Measures how far one row or one column is from being perfectly ordered in
     * one direction or the other, and keeps the smaller of the two penalties so
     * that a line ordered from the right is not punished.
     * @param packed the packed board
     * @param line the row number or the column number
     * @param horizontal true when the line is a row, false when it is a column
     * @return the disorder measured along that line
     */
    private static int lineMonotonicityPenalty(long packed,int line,boolean horizontal){
        int forwardPenalty=0;
        int backwardPenalty=0;
        for(int step=0;step<3;step++){
            int firstIndex=horizontal?line*4+step:step*4+line;
            int secondIndex=horizontal?line*4+step+1:(step+1)*4+line;
            int firstExponent=PackedBoardUtility.nibbleAt(packed,firstIndex);
            int secondExponent=PackedBoardUtility.nibbleAt(packed,secondIndex);
            if(firstExponent>secondExponent){
                forwardPenalty+=firstExponent-secondExponent;
            }
            if(secondExponent>firstExponent){
                backwardPenalty+=secondExponent-firstExponent;
            }
        }
        return Math.min(forwardPenalty,backwardPenalty);
    }
}
