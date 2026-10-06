package org.twozerofoureightgame.utility;
/**
 * The single source of random numbers for the whole program: tile spawns, tile
 * values, autoplay delays, and the small random choice that breaks ties between
 * equally good moves. The generator is seeded by the running system, so every
 * session plays differently, and the seeding method exists so that tests can
 * force a known sequence. Sharing one generator also means one random decision
 * can never be made twice by two different parts of the program at the same
 * moment.
 */
public final class RandomUtility{
    private static java.util.Random random=new java.util.Random();
    /**
     * This class only exposes static helpers and is never instantiated.
     */
    private RandomUtility(){
    }
    /**
     * Restarts the generator from a known seed. The tests use this so that a
     * fixed board always produces the same move.
     * @param seed the seed to start from
     */
    public static void setSeed(long seed){
        random=new java.util.Random(seed);
    }
    /**
     * Returns a random whole number.
     * @param bound the exclusive upper bound, which must be positive
     * @return a random number from zero up to one less than the bound
     */
    public static int randomInt(int bound){
        return random.nextInt(bound);
    }
    /**
     * Returns a random fractional number.
     * @return a random number from zero up to but not including one
     */
    public static double randomDouble(){
        return random.nextDouble();
    }
}
