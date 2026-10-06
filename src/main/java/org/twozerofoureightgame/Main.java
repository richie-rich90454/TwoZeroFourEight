package org.twozerofoureightgame;
import javax.swing.SwingUtilities;
import org.twozerofoureightgame.controller.GameController;
/**
 * The starting point of the program. It does one thing: it hands the whole job to
 * the Swing event thread, because every window, every repaint, and every key
 * press belongs there. Building the controller builds the window and deals the
 * first board.
 */
public final class Main{
    /**
     * This class is never instantiated; it only holds the entry point.
     */
    private Main(){
    }
    /**
     * Starts the game.
     * @param arguments any command line arguments, which this game does not use
     */
    public static void main(String[] arguments){
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                new GameController();
            }
        });
    }
}
