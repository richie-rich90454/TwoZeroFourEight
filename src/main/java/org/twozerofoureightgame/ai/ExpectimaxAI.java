package org.twozerofoureightgame.ai;
import org.twozerofoureightgame.model.Direction;
import org.twozerofoureightgame.utility.RandomUtility;
/**
 * Chooses the best move by searching with expectimax. The search alternates
 * between two kinds of nodes. A player node is the move the human would make and
 * takes the best child. A chance node is the random tile the game then drops in
 * and averages its children, weighting a new 2 tile at nine tenths and a new 4
 * tile at one tenth. Because there is no opponent, the value of a position is
 * simply how promising it looks, measured by {@link HeuristicEvaluator}.
 * The depth is not fixed. The search starts shallow, doubles its work by going
 * one level deeper, and keeps the best move from the last depth it finished
 * before the time budget ran out, so the answer always arrives on time and is
 * never worse than the previous iteration. Visited boards are cached in a
 * transposition table. An instance may run only one search at a time; the
 * controller guarantees this by never starting a second computation before the
 * first one has reported back.
 */
public class ExpectimaxAI{
    /** The deepest search the artificial intelligence will attempt. */
    public static final int MAXIMUM_SEARCH_DEPTH=8;
    /** The shallowest search the artificial intelligence will attempt. */
    public static final int STARTING_SEARCH_DEPTH=2;
    /** The exponent of the tile that wins the game. */
    public static final int WINNING_EXPONENT=11;
    private static final Direction[] ALL_DIRECTIONS=Direction.values();
    private static final int NODE_CHECK_INTERVAL=512;
    private static final int TIE_BREAK_EPSILON=1;
    private static final double TWO_TILE_PROBABILITY=0.9;
    private static final double FOUR_TILE_PROBABILITY=0.1;
    private final TranspositionTable transpositionTable=new TranspositionTable();
    private long deadlineNanos=0L;
    private int nodeCounter=0;
    private boolean outOfTime=false;
    /**
     * Creates an artificial intelligence with an empty transposition table.
     */
    public ExpectimaxAI(){
    }
    /**
     * Picks the best move for the board as it stands right now.
     * @param boardCells the sixteen cell exponents of the current board
     * @param timeBudgetMilliseconds how long the search may run before it settles for what it has
     * @return the chosen direction, or null when the board has no legal move at all
     */
    public Direction chooseBestMove(byte[] boardCells,int timeBudgetMilliseconds){
        long packed=PackedBoardUtility.pack(boardCells);
        deadlineNanos=System.nanoTime()+(long)timeBudgetMilliseconds*1000000L;
        outOfTime=false;
        nodeCounter=0;
        Direction bestMove=anyLegalMove(packed);
        Direction[] order=orderDirectionsByHeuristic(packed);
        for(int depth=STARTING_SEARCH_DEPTH;depth<=MAXIMUM_SEARCH_DEPTH;depth++){
            int[] values=new int[4];
            Direction iterationBest=null;
            int iterationBestValue=Integer.MIN_VALUE;
            for(int position=0;position<4;position++){
                Direction direction=order[position];
                long moved=RowMoveTable.applyPacked(packed,direction);
                if(moved==packed){
                    values[direction.ordinal()]=TranspositionTable.NOT_FOUND;
                    continue;
                }
                int value=expectimax(moved,depth-1,false);
                values[direction.ordinal()]=value;
                if(value>iterationBestValue){
                    iterationBestValue=value;
                    iterationBest=direction;
                }
            }
            if(outOfTime||iterationBest==null){
                break;
            }
            if(iterationBestValue>=AIWeights.TERMINAL_WIN_VALUE){
                return iterationBest;
            }
            bestMove=selectMoveWithTieBreak(values);
            order=orderDirections(values,iterationBest);
        }
        return bestMove;
    }
    private int expectimax(long packed,int depth,boolean maximumNode){
        if((nodeCounter++&(NODE_CHECK_INTERVAL-1))==0&&System.nanoTime()>deadlineNanos){
            outOfTime=true;
        }
        if(outOfTime){
            return HeuristicEvaluator.evaluate(packed);
        }
        if(PackedBoardUtility.maximumExponent(packed)>=WINNING_EXPONENT){
            return AIWeights.TERMINAL_WIN_VALUE;
        }
        if(depth<=0){
            return HeuristicEvaluator.evaluate(packed);
        }
        int nodeKind=maximumNode?TranspositionTable.FLAG_PLAYER_NODE:TranspositionTable.FLAG_CHANCE_NODE;
        int cachedValue=transpositionTable.lookup(packed,depth,nodeKind);
        if(cachedValue!=TranspositionTable.NOT_FOUND){
            return cachedValue;
        }
        int value;
        if(maximumNode){
            value=Integer.MIN_VALUE;
            boolean hasMove=false;
            for(int position=0;position<4;position++){
                Direction direction=ALL_DIRECTIONS[position];
                long moved=RowMoveTable.applyPacked(packed,direction);
                if(moved==packed){
                    continue;
                }
                hasMove=true;
                int childValue=expectimax(moved,depth-1,false);
                if(childValue>value){
                    value=childValue;
                }
                if(value>=AIWeights.TERMINAL_WIN_VALUE){
                    break;
                }
            }
            if(!hasMove){
                value=AIWeights.TERMINAL_LOSS_VALUE;
            }
        }else{
            int emptyCellCount=PackedBoardUtility.countEmptyCells(packed);
            if(emptyCellCount==0){
                value=expectimax(packed,depth-1,true);
            }else{
                double accumulated=0.0;
                for(int index=0;index<16;index++){
                    if(PackedBoardUtility.nibbleAt(packed,index)!=0){
                        continue;
                    }
                    accumulated+=TWO_TILE_PROBABILITY*expectimax(PackedBoardUtility.setNibbleAt(packed,index,1),depth-1,true)/emptyCellCount;
                    accumulated+=FOUR_TILE_PROBABILITY*expectimax(PackedBoardUtility.setNibbleAt(packed,index,2),depth-1,true)/emptyCellCount;
                }
                value=(int)Math.round(accumulated);
            }
        }
        transpositionTable.store(packed,depth,value,nodeKind);
        return value;
    }
    private Direction anyLegalMove(long packed){
        for(int position=0;position<4;position++){
            long moved=RowMoveTable.applyPacked(packed,ALL_DIRECTIONS[position]);
            if(moved!=packed){
                return ALL_DIRECTIONS[position];
            }
        }
        return null;
    }
    private Direction selectMoveWithTieBreak(int[] values){
        int bestValue=Integer.MIN_VALUE;
        int qualifyingCount=0;
        for(int position=0;position<4;position++){
            int value=values[ALL_DIRECTIONS[position].ordinal()];
            if(value>bestValue){
                bestValue=value;
            }
        }
        for(int position=0;position<4;position++){
            int value=values[ALL_DIRECTIONS[position].ordinal()];
            if(value>Integer.MIN_VALUE&&value>=bestValue-TIE_BREAK_EPSILON){
                qualifyingCount++;
            }
        }
        if(qualifyingCount==0){
            return null;
        }
        int chosen=RandomUtility.randomInt(qualifyingCount);
        for(int position=0;position<4;position++){
            Direction direction=ALL_DIRECTIONS[position];
            int value=values[direction.ordinal()];
            if(value>Integer.MIN_VALUE&&value>=bestValue-TIE_BREAK_EPSILON){
                if(chosen==0){
                    return direction;
                }
                chosen--;
            }
        }
        return null;
    }
    private Direction[] orderDirectionsByHeuristic(long packed){
        int[] values=new int[4];
        for(int position=0;position<4;position++){
            Direction direction=ALL_DIRECTIONS[position];
            long moved=RowMoveTable.applyPacked(packed,direction);
            values[direction.ordinal()]=moved==packed?Integer.MIN_VALUE:HeuristicEvaluator.evaluate(moved);
        }
        return orderDirections(values,null);
    }
    private Direction[] orderDirections(int[] values,Direction preferredFirst){
        Direction[] ordered=new Direction[4];
        boolean[] taken=new boolean[4];
        for(int slot=0;slot<4;slot++){
            int bestOrdinal=-1;
            for(int position=0;position<4;position++){
                int ordinal=ALL_DIRECTIONS[position].ordinal();
                if(taken[ordinal]){
                    continue;
                }
                if(bestOrdinal<0||values[ordinal]>values[bestOrdinal]){
                    bestOrdinal=ordinal;
                }
            }
            taken[bestOrdinal]=true;
            ordered[slot]=ALL_DIRECTIONS[bestOrdinal];
        }
        if(preferredFirst!=null){
            for(int slot=1;slot<4;slot++){
                if(ordered[slot]==preferredFirst){
                    for(int shift=slot;shift>0;shift--){
                        ordered[shift]=ordered[shift-1];
                    }
                    ordered[0]=preferredFirst;
                    break;
                }
            }
        }
        return ordered;
    }
    /**
     * Empties the cache of visited boards. The controller calls this when a new
     * game begins, because tiles from an old game say nothing about a new one.
     */
    public void resetTranspositionTable(){
        transpositionTable.clear();
    }
}
