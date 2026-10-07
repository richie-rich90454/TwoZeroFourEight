package org.twozerofoureightgame.ai;
/**
 * A cache of positions already visited by the search, built from three parallel
 * primitive arrays instead of a hash map so that a lookup costs one multiply,
 * one shift, and three array reads with no pointer chasing and no garbage. Each
 * slot stores the full sixty four bit board as its key, so a collision between
 * two different boards is detected by comparing keys rather than by trusting the
 * hash. A slot also remembers how deep its value was computed, whether the value
 * came from a player node or from a chance node, and whether it is occupied at
 * all, packed into one byte.
 * The node kind is part of the identity of an entry, and this is not a detail.
 * The same board can be reached two ways: by a move, where it is a chance node
 * whose value is the average over the tiles that might appear, and by a tile
 * appearing, where it is a player node whose value is the best of the moves.
 * Those two values are different numbers, so an entry stored for one of them
 * must never be handed back for the other. Caching them under the same key made
 * the search return moves that were not the moves it had just evaluated.
 */
public final class TranspositionTable{
    /** Returned by a lookup that found nothing, far outside any real score. */
    public static final int NOT_FOUND=Integer.MIN_VALUE;
    /** The flag bit that marks a value computed at a player node. */
    public static final int FLAG_PLAYER_NODE=0x01;
    /** The flag bit that marks a value computed at a chance node. */
    public static final int FLAG_CHANCE_NODE=0x02;
    /** How many slots the table holds. */
    public static final int CAPACITY=1<<17;
    private static final int INDEX_MASK=CAPACITY-1;
    private static final int OCCUPIED_BIT=0x80;
    private static final int DEPTH_SHIFT=1;
    private static final int DEPTH_MASK=0x0F;
    private static final int FLAG_MASK=0x03;
    private static final long HASH_MULTIPLIER=0x9E3779B97F4A7C15L;
    private final long[] keys=new long[CAPACITY];
    private final int[] values=new int[CAPACITY];
    private final byte[] meta=new byte[CAPACITY];
    /**
     * Creates an empty table. The three arrays together occupy about one and a
     * half megabytes.
     */
    public TranspositionTable(){
    }
    private static int indexForKey(long key){
        long mixed=key*HASH_MULTIPLIER;
        return (int)((mixed^(mixed>>>32))&INDEX_MASK);
    }
    /**
     * Looks for a cached value for a board.
     * @param key the packed board
     * @param requiredDepth the depth the caller still needs to search
     * @param flag the node kind, one of {@link #FLAG_PLAYER_NODE} or {@link #FLAG_CHANCE_NODE}
     * @return the cached value, or {@link #NOT_FOUND} when there is none usable
     */
    public int lookup(long key,int requiredDepth,int flag){
        int index=indexForKey(key);
        if((meta[index]&OCCUPIED_BIT)==0){
            return NOT_FOUND;
        }
        if(keys[index]!=key){
            return NOT_FOUND;
        }
        int storedFlags=meta[index]&FLAG_MASK;
        if(storedFlags!=(flag&FLAG_MASK)){
            return NOT_FOUND;
        }
        int storedDepth=(meta[index]>>>DEPTH_SHIFT)&DEPTH_MASK;
        if(storedDepth<requiredDepth){
            return NOT_FOUND;
        }
        return values[index];
    }
    /**
     * Stores a value for a board. A slot that already holds a value of the same
     * node kind computed deeper than this one is left untouched, because a
     * deeper value is the more trustworthy one.
     * @param key the packed board
     * @param depth the number of levels still searched below this board
     * @param value the value that was computed
     * @param flag the node kind, one of {@link #FLAG_PLAYER_NODE} or {@link #FLAG_CHANCE_NODE}
     */
    public void store(long key,int depth,int value,int flag){
        int index=indexForKey(key);
        int storedFlags=meta[index]&FLAG_MASK;
        int storedDepth=(meta[index]>>>DEPTH_SHIFT)&DEPTH_MASK;
        boolean occupied=(meta[index]&OCCUPIED_BIT)!=0;
        boolean sameKind=occupied&&storedFlags==(flag&FLAG_MASK);
        if(sameKind&&storedDepth>depth){
            return;
        }
        keys[index]=key;
        values[index]=value;
        meta[index]=(byte)(OCCUPIED_BIT|((depth&DEPTH_MASK)<<DEPTH_SHIFT)|(flag&FLAG_MASK));
    }
    /**
     * Empties the table. The three arrays are refilled with zeroes, which takes
     * a few milliseconds and happens only when a new game starts.
     */
    public void clear(){
        java.util.Arrays.fill(keys,0L);
        java.util.Arrays.fill(values,0);
        java.util.Arrays.fill(meta,(byte)0);
    }
    /**
     * Returns how many slots the table holds, which is useful when reading the
     * memory budget in the documentation.
     * @return the capacity in slots
     */
    public int capacity(){
        return CAPACITY;
    }
}
