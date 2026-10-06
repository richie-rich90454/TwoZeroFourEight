package org.twozerofoureightgame.model;
import org.twozerofoureightgame.ai.PackedBoardUtility;
import org.twozerofoureightgame.ai.RowMoveTable;
import org.twozerofoureightgame.utility.RandomUtility;
/**
 * The mutable four by four game grid. Each cell stores a small exponent instead
 * of the tile value itself: an exponent of zero means the cell is empty, an
 * exponent of one means a tile worth 2, and an exponent of eleven means a tile
 * worth 2048. Merging two tiles is therefore a single addition, and four cells
 * always fit inside sixteen bits so the whole board fits inside one long.
 * The class keeps an incremental count of empty cells and the largest exponent
 * on the board so that both questions are answered without scanning, and it
 * reuses one move result object so that applying a move allocates nothing.
 */
public class Board{
    /** The sixteen cell exponents in row major order, where 0 means empty. */
    private final byte[] cellExponents=new byte[16];
    /** How many of the sixteen cells are currently empty. */
    private int emptyCellCount=16;
    /** The largest exponent currently on the board, where 0 means an empty board. */
    private byte maximumExponentOnBoard=0;
    /** One reusable description of the most recently applied move. */
    private final MoveResult reusableMoveResult=new MoveResult();
    /**
     * Creates an empty board with sixteen empty cells.
     */
    public Board(){
    }
    /**
     * Converts a row and a column into the flat index used by the cell array.
     * @param row the row counted from the top
     * @param column the column counted from the left
     * @return the flat index between 0 and 15
     */
    public static int rowColumnToIndex(int row,int column){
        return row*4+column;
    }
    /**
     * Converts a flat index back into its row.
     * @param index the flat index between 0 and 15
     * @return the row counted from the top
     */
    public static int indexToRow(int index){
        return index/4;
    }
    /**
     * Converts a flat index back into its column.
     * @param index the flat index between 0 and 15
     * @return the column counted from the left
     */
    public static int indexToColumn(int index){
        return index%4;
    }
    /**
     * Reads the exponent stored in one cell.
     * @param row the row counted from the top
     * @param column the column counted from the left
     * @return the exponent, where 0 means the cell is empty
     */
    public byte getExponentAt(int row,int column){
        return cellExponents[rowColumnToIndex(row,column)];
    }
    /**
     * Writes the exponent into one cell and keeps the empty cell count and the
     * largest exponent correct.
     * @param row the row counted from the top
     * @param column the column counted from the left
     * @param exponent the exponent to store, where 0 means empty
     */
    public void setExponentAt(int row,int column,byte exponent){
        int index=rowColumnToIndex(row,column);
        byte previousExponent=cellExponents[index];
        if(previousExponent==exponent){
            return;
        }
        cellExponents[index]=exponent;
        if(previousExponent==0){
            emptyCellCount--;
        }
        if(exponent==0){
            emptyCellCount++;
        }
        if(exponent>maximumExponentOnBoard){
            maximumExponentOnBoard=exponent;
        }else if(previousExponent==maximumExponentOnBoard){
            recomputeMaximumExponent();
        }
    }
    private void recomputeMaximumExponent(){
        byte largest=0;
        for(int index=0;index<16;index++){
            if(cellExponents[index]>largest){
                largest=cellExponents[index];
            }
        }
        maximumExponentOnBoard=largest;
    }
    private void recomputeEmptyCellCount(){
        int count=0;
        for(int index=0;index<16;index++){
            if(cellExponents[index]==0){
                count++;
            }
        }
        emptyCellCount=count;
    }
    /**
     * Packs the whole board into a single long that holds sixteen four bit cells.
     * @return the packed board
     */
    public long toPackedLong(){
        return PackedBoardUtility.pack(cellExponents);
    }
    /**
     * Unpacks a packed board into the cells and rebuilds both counters.
     * @param packed the packed board
     */
    public void fromPackedLong(long packed){
        PackedBoardUtility.unpack(packed,cellExponents);
        recomputeMaximumExponent();
        recomputeEmptyCellCount();
    }
    /**
     * Restores the board from a snapshot cell array and rebuilds both counters.
     * @param cells the sixteen cell exponents to restore
     */
    public void restoreFrom(byte[] cells){
        for(int index=0;index<16;index++){
            cellExponents[index]=cells[index];
        }
        recomputeMaximumExponent();
        recomputeEmptyCellCount();
    }
    /**
     * Restores the board cells from a complete snapshot.
     * @param state the snapshot to restore the cells from
     */
    public void restoreFrom(GameState state){
        restoreFrom(state.cells);
    }
    /**
     * Empties every cell of the board.
     */
    public void clearBoard(){
        for(int index=0;index<16;index++){
            cellExponents[index]=0;
        }
        emptyCellCount=16;
        maximumExponentOnBoard=0;
    }
    /**
     * Applies one move to the board by consulting the precomputed row tables.
     * @param direction the direction to move every tile towards
     * @return the number of points the move earned, or zero when the move was illegal
     */
    public int applyMove(Direction direction){
        long packedBefore=toPackedLong();
        long packedAfter=RowMoveTable.applyPacked(packedBefore,direction);
        if(packedAfter==packedBefore){
            return 0;
        }
        int scoreGained=RowMoveTable.scoreGained(packedBefore,packedAfter,direction);
        fromPackedLong(packedAfter);
        return scoreGained;
    }
    /**
     * Reports whether a move in the given direction would change the board.
     * @param direction the direction to test
     * @return true when at least one tile would slide or merge
     */
    public boolean canMove(Direction direction){
        long packed=toPackedLong();
        return RowMoveTable.applyPacked(packed,direction)!=packed;
    }
    /**
     * Reports whether no direction at all can change the board, which is the
     * real game over condition.
     * @return true when the game is over
     */
    public boolean isGameOver(){
        long packed=toPackedLong();
        return RowMoveTable.applyPacked(packed,Direction.LEFT)==packed
            &&RowMoveTable.applyPacked(packed,Direction.RIGHT)==packed
            &&RowMoveTable.applyPacked(packed,Direction.UP)==packed
            &&RowMoveTable.applyPacked(packed,Direction.DOWN)==packed;
    }
    /**
     * Returns how many cells are empty.
     * @return the empty cell count
     */
    public int countEmptyCells(){
        return emptyCellCount;
    }
    /**
     * Returns the largest exponent on the board.
     * @return the largest exponent, where 0 means the board is empty
     */
    public byte getMaximumExponentOnBoard(){
        return maximumExponentOnBoard;
    }
    /**
     * Creates an independent copy of this board.
     * @return a new board with the same cells
     */
    public Board copyBoard(){
        Board copy=new Board();
        copy.fromPackedLong(toPackedLong());
        return copy;
    }
    /**
     * Copies the sixteen cell exponents into a fresh array.
     * @return a fresh array holding a copy of the cells
     */
    public byte[] snapshotCells(){
        return cellExponents.clone();
    }
    /**
     * Places a randomly chosen tile into a randomly chosen empty cell, using the
     * standard ninety to ten split between a 2 tile and a 4 tile.
     * @return the flat index that received the tile, or negative one when the board is full
     */
    public int spawnRandomTile(){
        if(emptyCellCount==0){
            return -1;
        }
        int target=RandomUtility.randomInt(emptyCellCount);
        int seen=0;
        for(int index=0;index<16;index++){
            if(cellExponents[index]!=0){
                continue;
            }
            if(seen==target){
                byte exponent=RandomUtility.randomDouble()<0.9?(byte)1:(byte)2;
                cellExponents[index]=exponent;
                emptyCellCount--;
                if(exponent>maximumExponentOnBoard){
                    maximumExponentOnBoard=exponent;
                }
                return index;
            }
            seen++;
        }
        return -1;
    }
    /**
     * Applies one move, records where every individual tile travelled, spawns a
     * new tile, and returns the reusable description of all of it. The returned
     * object belongs to this board and stays valid until the next move.
     * @param direction the direction to move every tile towards
     * @return the description of the applied move
     */
    public MoveResult applyMoveWithTransitions(Direction direction){
        MoveResult result=reusableMoveResult;
        result.reset();
        long packedBefore=toPackedLong();
        long packedAfter=RowMoveTable.applyPacked(packedBefore,direction);
        if(packedAfter==packedBefore){
            return result;
        }
        result.boardChanged=true;
        result.scoreGained=RowMoveTable.scoreGained(packedBefore,packedAfter,direction);
        recordTransitions(packedBefore,direction,result);
        fromPackedLong(packedAfter);
        int spawnIndex=spawnRandomTile();
        if(spawnIndex>=0){
            int spawnRow=indexToRow(spawnIndex);
            int spawnColumn=indexToColumn(spawnIndex);
            result.addTransition(spawnRow,spawnColumn,spawnRow,spawnColumn,(byte)0,cellExponents[spawnIndex],false,true);
        }
        return result;
    }
    private void recordTransitions(long packedBefore,Direction direction,MoveResult result){
        boolean horizontal=direction.isHorizontal();
        boolean forward=direction==Direction.LEFT||direction==Direction.UP;
        for(int line=0;line<4;line++){
            int writePosition=forward?0:3;
            int previousWrittenExponent=-1;
            boolean previousCellMerged=false;
            int lastWriteTransitionIndex=-1;
            for(int step=0;step<4;step++){
                int sourcePosition=forward?step:3-step;
                int sourceRow=horizontal?line:sourcePosition;
                int sourceColumn=horizontal?sourcePosition:line;
                int sourceIndex=rowColumnToIndex(sourceRow,sourceColumn);
                int sourceExponent=(int)((packedBefore>>(sourceIndex*4))&0xFL);
                if(sourceExponent==0){
                    continue;
                }
                int writeRow=horizontal?line:writePosition;
                int writeColumn=horizontal?writePosition:line;
                if(previousWrittenExponent==sourceExponent&&!previousCellMerged){
                    int mergePosition=forward?writePosition-1:writePosition+1;
                    int mergeRow=horizontal?line:mergePosition;
                    int mergeColumn=horizontal?mergePosition:line;
                    result.mergeCount++;
                    if(lastWriteTransitionIndex>=0){
                        TileTransition absorbedTile=result.tileTransitions[lastWriteTransitionIndex];
                        absorbedTile.merged=true;
                        absorbedTile.newExponent=(byte)(sourceExponent+1);
                    }
                    result.addTransition(sourceRow,sourceColumn,mergeRow,mergeColumn,(byte)sourceExponent,(byte)(sourceExponent+1),true,false);
                    lastWriteTransitionIndex=result.tileTransitionCount-1;
                    previousCellMerged=true;
                }else{
                    result.addTransition(sourceRow,sourceColumn,writeRow,writeColumn,(byte)sourceExponent,(byte)sourceExponent,false,false);
                    lastWriteTransitionIndex=result.tileTransitionCount-1;
                    writePosition=forward?writePosition+1:writePosition-1;
                    previousWrittenExponent=sourceExponent;
                    previousCellMerged=false;
                }
            }
        }
    }
}
