package org.twozerofoureightgame.controller;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import org.twozerofoureightgame.model.Direction;
/**
 * Turns key presses into directions. The arrow keys and the letter keys W, A, S
 * and D both work, which covers the usual laptop arrow cluster and the usual
 * gaming keyboard without asking anybody to choose. Nothing else here decides
 * whether a move happens: the controller is the single place that decides that,
 * so the keyboard never has to know whether the game is paused, thinking, or
 * already animating.
 */
public final class KeyboardHandler extends KeyAdapter{
    private final GameController controller;
    /**
     * Creates a key listener bound to one controller.
     * @param controller the controller to report directions to
     */
    public KeyboardHandler(GameController controller){
        this.controller=controller;
    }
    @Override
    public void keyPressed(KeyEvent event){
        int keyCode=event.getKeyCode();
        Direction direction=null;
        if(keyCode==KeyEvent.VK_LEFT||keyCode==KeyEvent.VK_A){
            direction=Direction.LEFT;
        }else if(keyCode==KeyEvent.VK_RIGHT||keyCode==KeyEvent.VK_D){
            direction=Direction.RIGHT;
        }else if(keyCode==KeyEvent.VK_UP||keyCode==KeyEvent.VK_W){
            direction=Direction.UP;
        }else if(keyCode==KeyEvent.VK_DOWN||keyCode==KeyEvent.VK_S){
            direction=Direction.DOWN;
        }
        if(direction!=null){
            controller.handleDirectionInput(direction);
        }
    }
}
