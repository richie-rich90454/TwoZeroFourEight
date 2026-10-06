package org.twozerofoureightgame.ai;
/**
 * Converts between the flat sixteen byte board used by the game and the single
 * packed long used by the search, and supplies the handful of low level bit
 * operations the search needs. This is the only class in the project that is
 * allowed to think in bits; every method documents the arithmetic it performs.
 * The packing rule is simple: cell number n lives in bits four n through four n
 * plus three of the long, so cell zero occupies the least significant nibble and
 * cell fifteen occupies the most significant nibble. Sixteen cells times four
 * bits equals sixty four bits, which is exactly one long.
 */
public final class PackedBoardUtility{
    /** A mask that keeps only the low nibble of a value. */
    public static final long NIBBLE_MASK=0xFL;
    /**
     * This class only exposes static helpers and is never instantiated.
     */
    private PackedBoardUtility(){
    }
    /**
     * Packs sixteen exponents into one long. Worked example: a board whose first
     * three cells hold exponents one, zero and two produces a long whose low
     * nibbles are one, zero, two, and then zeros.
     * @param cellExponents the sixteen exponents, where 0 means empty
     * @return the packed board
     */
    public static long pack(byte[] cellExponents){
        long packed=0L;
        for(int index=0;index<16;index++){
            packed|=(long)(cellExponents[index]&0xF)<<(index*4);
        }
        return packed;
    }
    /**
     * Unpacks a long back into sixteen exponents.
     * @param packed the packed board
     * @param outputCells the array that receives the sixteen exponents
     */
    public static void unpack(long packed,byte[] outputCells){
        for(int index=0;index<16;index++){
            outputCells[index]=(byte)((packed>>>(index*4))&NIBBLE_MASK);
        }
    }
    /**
     * Reads a single cell out of a packed board.
     * @param packed the packed board
     * @param index the flat cell number between 0 and 15
     * @return the exponent stored in that cell
     */
    public static int nibbleAt(long packed,int index){
        return (int)((packed>>>(index*4))&NIBBLE_MASK);
    }
    /**
     * Writes a single cell into a packed board and returns the new board. The
     * incoming board is left untouched because longs are values.
     * @param packed the packed board to change
     * @param index the flat cell number between 0 and 15
     * @param value the exponent to store, between 0 and 15
     * @return the packed board with the cell replaced
     */
    public static long setNibbleAt(long packed,int index,int value){
        int shift=index*4;
        long cleared=packed&~(NIBBLE_MASK<<shift);
        return cleared|((long)(value&0xF)<<shift);
    }
    /**
     * Builds a sixteen bit mask whose bit n is set when cell n is empty. The
     * search walks this mask to enumerate the chance branch of the expectimax.
     * @param packed the packed board
     * @return the empty cell mask
     */
    public static int emptyNibbleMask(long packed){
        int mask=0;
        for(int index=0;index<16;index++){
            if(nibbleAt(packed,index)==0){
                mask|=1<<index;
            }
        }
        return mask;
    }
    /**
     * Counts the empty cells of a packed board by counting the set bits of the
     * empty cell mask, where each empty cell contributes exactly one bit.
     * @param packed the packed board
     * @return how many of the sixteen cells are empty
     */
    public static int countEmptyCells(long packed){
        return Integer.bitCount(emptyNibbleMask(packed));
    }
    /**
     * Finds the largest exponent on a packed board.
     * @param packed the packed board
     * @return the largest exponent, or 0 for an empty board
     */
    public static int maximumExponent(long packed){
        int largest=0;
        for(int index=0;index<16;index++){
            int value=nibbleAt(packed,index);
            if(value>largest){
                largest=value;
            }
        }
        return largest;
    }
    /**
     * Reports whether two equal tiles sit next to each other somewhere on the
     * board, either side by side in a row or on top of each other in a column.
     * @param packed the packed board
     * @return true when at least one merge is available somewhere
     */
    public static boolean hasMatchingNeighbors(long packed){
        for(int row=0;row<4;row++){
            for(int column=0;column<4;column++){
                int value=nibbleAt(packed,row*4+column);
                if(value==0){
                    continue;
                }
                if(column<3&&nibbleAt(packed,row*4+column+1)==value){
                    return true;
                }
                if(row<3&&nibbleAt(packed,(row+1)*4+column)==value){
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Swaps the rows and the columns of a packed board, so that what was a
     * column becomes a row and the other way around. Transposing twice returns
     * the original board. This is what lets one pair of horizontal row tables
     * serve all four directions. The method performs the six off diagonal
     * nibble swaps of a four by four grid: the pairs are cells one and four, two
     * and eight, three and twelve, six and nine, seven and thirteen, and eleven
     * and fourteen. Each swap isolates the difference between the two nibbles
     * with an exclusive or, then writes that difference into both places.
     * @param packed the packed board to transpose
     * @return the transposed packed board
     */
    public static long transpose(long packed){
        long result=packed;
        result=swapNibbles(result,1,4);
        result=swapNibbles(result,2,8);
        result=swapNibbles(result,3,12);
        result=swapNibbles(result,6,9);
        result=swapNibbles(result,7,13);
        result=swapNibbles(result,11,14);
        return result;
    }
    private static long swapNibbles(long packed,int firstIndex,int secondIndex){
        int firstShift=firstIndex*4;
        int secondShift=secondIndex*4;
        long difference=((packed>>>firstShift)^(packed>>>secondShift))&NIBBLE_MASK;
        return packed^((difference<<firstShift)|(difference<<secondShift));
    }
}
