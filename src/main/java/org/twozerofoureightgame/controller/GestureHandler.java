package org.twozerofoureightgame.controller;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import org.twozerofoureightgame.model.Direction;
/**
 * Turns a drag into a direction, which is how the game is played with a mouse
 * and, because a finger drag produces the same events, with a touchscreen. The
 * direction is decided as soon as the drag has travelled twenty five pixels
 * along one axis, so the move fires while the finger is still moving rather than
 * only when it is lifted. Once one direction has been sent, the rest of that drag
 * is ignored until the button is released, so a long diagonal swipe never sends
 * two moves at once.
 */
public final class GestureHandler extends MouseAdapter implements java.awt.event.MouseMotionListener{
    /** How far the drag must travel before it counts as a swipe. */
    public static final int SWIPE_THRESHOLD_PIXELS=25;
    private final GameController controller;
    private int pressPixelX=0;
    private int pressPixelY=0;
    private boolean gestureConsumed=false;
    /**
     * Creates a gesture listener bound to one controller.
     * @param controller the controller to report directions to
     */
    public GestureHandler(GameController controller){
        this.controller=controller;
    }
    @Override
    public void mousePressed(MouseEvent event){
        pressPixelX=event.getX();
        pressPixelY=event.getY();
        gestureConsumed=false;
    }
    @Override
    public void mouseDragged(MouseEvent event){
        if(gestureConsumed){
            return;
        }
        int deltaX=event.getX()-pressPixelX;
        int deltaY=event.getY()-pressPixelY;
        int horizontalDistance=Math.abs(deltaX);
        int verticalDistance=Math.abs(deltaY);
        if(horizontalDistance<SWIPE_THRESHOLD_PIXELS&&verticalDistance<SWIPE_THRESHOLD_PIXELS){
            return;
        }
        gestureConsumed=true;
        if(horizontalDistance>=verticalDistance){
            controller.handleDirectionInput(deltaX>0?Direction.RIGHT:Direction.LEFT);
        }else{
            controller.handleDirectionInput(deltaY>0?Direction.DOWN:Direction.UP);
        }
    }
    @Override
    public void mouseReleased(MouseEvent event){
        pressPixelX=0;
        pressPixelY=0;
        gestureConsumed=false;
    }
    @Override
    public void mouseMoved(MouseEvent event){
    }
}
