package org.twozerofoureightgame.utility;
import org.twozerofoureightgame.view.BoardPanel;
import org.twozerofoureightgame.view.TileView;
/**
 * Drives every animation in the game from one timer that ticks sixteen
 * milliseconds at a time. The manager owns a fixed pool of animation objects
 * and a fixed array of the tiles currently in flight, so a move starts four
 * animations without allocating a single object. The timer only runs while
 * something is moving and stops itself the moment the last animation finishes,
 * which keeps the processor idle when the player is thinking.
 * The four animation kinds are a slide, a merge swell, a spawn fade, and a
 * score float. They are told apart by an identifier, and the identifier selects
 * one of four concrete update methods, so no callback object is ever created.
 */
public final class AnimationManager{
    /** Identifies a tile sliding from one cell to another. */
    public static final int SLIDE_IDENTIFIER=1;
    /** Identifies the swell that follows a merge. */
    public static final int MERGE_POP_IDENTIFIER=2;
    /** Identifies a newly spawned tile fading in. */
    public static final int SPAWN_FADE_IDENTIFIER=3;
    /** Identifies the score text rising and fading away. */
    public static final int SCORE_FLOAT_IDENTIFIER=4;
    /** How long a slide takes. */
    public static final long SLIDE_DURATION_NANOS=150000000L;
    /** How long a merge swell takes. */
    public static final long MERGE_POP_DURATION_NANOS=150000000L;
    /** How long a spawn fade takes. */
    public static final long SPAWN_FADE_DURATION_NANOS=140000000L;
    /** How long the score float takes. */
    public static final long SCORE_FLOAT_DURATION_NANOS=550000000L;
    /** The largest number of tiles that can be in flight at once. */
    public static final int MAXIMUM_MOVING_VIEWS=16;
    private static final int ANIMATION_POOL_SIZE=64;
    private static final int TICK_INTERVAL_MILLISECONDS=16;
    private static final float MERGE_POP_SCALE=1.26f;
    private static final float SPAWN_START_SCALE=0.72f;
    private static final float SCORE_FLOAT_RISE_PIXELS=44f;
    private final Animation[] animationPool=new Animation[ANIMATION_POOL_SIZE];
    private final TileView[] movingViews=new TileView[MAXIMUM_MOVING_VIEWS];
    private final BoardPanel boardPanel;
    private final TileView[][] tileViews;
    private final javax.swing.Timer timer;
    private int movingViewCount=0;
    private AnimationCompletionListener completionListener;
    /**
     * Implemented by whoever needs to know that the board has stopped moving,
     * which in this game is the controller driving autoplay.
     */
    public interface AnimationCompletionListener{
        /**
         * Called on the event thread once the last running animation has finished.
         */
        void onAllAnimationsFinished();
    }
    /**
     * Creates a manager bound to one board panel and to the grid of tile views
     * that panel paints.
     * @param boardPanel the panel to repaint on every tick
     * @param tileViews the grid of settled tile views, indexed by row then column
     */
    public AnimationManager(BoardPanel boardPanel,TileView[][] tileViews){
        this.boardPanel=boardPanel;
        this.tileViews=tileViews;
        for(int index=0;index<ANIMATION_POOL_SIZE;index++){
            animationPool[index]=new Animation();
        }
        timer=new javax.swing.Timer(TICK_INTERVAL_MILLISECONDS,new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                tick();
            }
        });
        timer.setRepeats(true);
    }
    /**
     * Registers the object that wants to hear about the board coming to rest.
     * @param listener the listener to notify, or null to stop notifying
     */
    public void setCompletionListener(AnimationCompletionListener listener){
        completionListener=listener;
    }
    /**
     * Reports whether a tile is still travelling, which is what the controller
     * checks before accepting another move. The score float is deliberately left
     * out: it hangs around for half a second after the last tile has landed, and
     * swallowing a key press for that long would make the game feel stuck.
     * @return true while at least one tile animation is active
     */
    public boolean hasActiveAnimations(){
        for(int index=0;index<ANIMATION_POOL_SIZE;index++){
            Animation animation=animationPool[index];
            if(animation.active&&animation.identifier!=SCORE_FLOAT_IDENTIFIER){
                return true;
            }
        }
        return false;
    }
    /**
     * Returns how many tiles are in flight, which the board panel needs in order
     * to paint them on top of the settled tiles.
     * @return the number of tiles currently moving
     */
    public int getMovingViewCount(){
        return movingViewCount;
    }
    /**
     * Returns the tiles currently in flight.
     * @return the array of moving tiles, whose first entries are meaningful
     */
    public TileView[] getMovingViews(){
        return movingViews;
    }
    /**
     * Starts a slide. The tile is added to the flying set, so it is painted on
     * top of the board until it lands.
     * @param view the tile that is travelling
     * @param fromRow the row the tile leaves
     * @param fromColumn the column the tile leaves
     * @param toRow the row the tile arrives at
     * @param toColumn the column the tile arrives at
     * @param fromPixelX the horizontal centre the tile starts at
     * @param fromPixelY the vertical centre the tile starts at
     * @param toPixelX the horizontal centre the tile ends at
     * @param toPixelY the vertical centre the tile ends at
     * @param settlesOnCompletion true when the tile should drop into the grid when it lands
     */
    public void startSlide(TileView view,int fromRow,int fromColumn,int toRow,int toColumn,float fromPixelX,float fromPixelY,float toPixelX,float toPixelY,boolean settlesOnCompletion){
        Animation animation=acquire();
        if(animation==null){
            return;
        }
        animation.identifier=SLIDE_IDENTIFIER;
        animation.view=view;
        animation.consumedView=null;
        animation.sourceRow=fromRow;
        animation.sourceColumn=fromColumn;
        animation.targetRow=toRow;
        animation.targetColumn=toColumn;
        animation.fromPixelX=fromPixelX;
        animation.fromPixelY=fromPixelY;
        animation.toPixelX=toPixelX;
        animation.toPixelY=toPixelY;
        animation.settlesOnCompletion=settlesOnCompletion;
        animation.startDelayNanos=0L;
        animation.durationNanos=SLIDE_DURATION_NANOS;
        start(animation);
    }
    /**
     * Starts the swell that plays after two tiles have slid together. The tile
     * that survives keeps its place in the grid; the tile that was consumed goes
     * back into the pool of reusable views.
     * @param survivorView the tile that remains
     * @param consumedView the tile that merged away
     * @param row the destination row
     * @param column the destination column
     * @param newExponent the exponent revealed by the merge
     */
    public void startMergePop(TileView survivorView,TileView consumedView,int row,int column,byte newExponent){
        Animation animation=acquire();
        if(animation==null){
            return;
        }
        animation.identifier=MERGE_POP_IDENTIFIER;
        animation.view=survivorView;
        animation.consumedView=consumedView;
        animation.targetRow=row;
        animation.targetColumn=column;
        animation.newExponent=newExponent;
        animation.settlesOnCompletion=true;
        animation.startDelayNanos=SLIDE_DURATION_NANOS;
        animation.durationNanos=MERGE_POP_DURATION_NANOS;
        start(animation);
    }
    /**
     * Starts the fade that reveals a newly spawned tile.
     * @param view the spawned tile
     * @param row the row it occupies
     * @param column the column it occupies
     */
    public void startSpawnFade(TileView view,int row,int column){
        Animation animation=acquire();
        if(animation==null){
            return;
        }
        animation.identifier=SPAWN_FADE_IDENTIFIER;
        animation.view=view;
        animation.consumedView=null;
        animation.targetRow=row;
        animation.targetColumn=column;
        animation.settlesOnCompletion=true;
        animation.startDelayNanos=SLIDE_DURATION_NANOS;
        animation.durationNanos=SPAWN_FADE_DURATION_NANOS;
        start(animation);
    }
    /**
     * Starts the text that rises from the board showing the points just earned.
     * @param text the text to show, for example plus forty
     * @param pixelX the horizontal centre to rise from
     * @param pixelY the vertical centre to rise from
     */
    public void startScoreFloat(String text,float pixelX,float pixelY){
        Animation animation=acquire();
        if(animation==null){
            return;
        }
        animation.identifier=SCORE_FLOAT_IDENTIFIER;
        animation.view=null;
        animation.consumedView=null;
        animation.floatText=text;
        animation.floatStartPixelX=pixelX;
        animation.floatStartPixelY=pixelY;
        animation.startDelayNanos=0L;
        animation.durationNanos=SCORE_FLOAT_DURATION_NANOS;
        start(animation);
    }
    /**
     * Puts a tile into the flying set so that it is painted above the board.
     * @param view the tile that is now in flight
     */
    public void addMovingView(TileView view){
        if(movingViewCount>=MAXIMUM_MOVING_VIEWS){
            return;
        }
        movingViews[movingViewCount++]=view;
    }
    /**
     * Drops every animation and every flying tile, which is what a new game or an
     * undo needs before it rebuilds the board from scratch.
     */
    public void cancelAll(){
        for(int index=0;index<ANIMATION_POOL_SIZE;index++){
            animationPool[index].active=false;
        }
        movingViewCount=0;
        boardPanel.floatingScoreText=null;
        boardPanel.floatingScoreAlpha=0f;
        timer.stop();
    }
    private Animation acquire(){
        for(int index=0;index<ANIMATION_POOL_SIZE;index++){
            if(!animationPool[index].active){
                return animationPool[index];
            }
        }
        return null;
    }
    private void start(Animation animation){
        animation.startTimeNanos=System.nanoTime();
        animation.active=true;
        if(!timer.isRunning()){
            timer.start();
        }
    }
    private void tick(){
        long now=System.nanoTime();
        int activeCount=0;
        for(int index=0;index<ANIMATION_POOL_SIZE;index++){
            Animation animation=animationPool[index];
            if(!animation.active){
                continue;
            }
            long elapsed=now-animation.startTimeNanos-animation.startDelayNanos;
            if(elapsed<0){
                activeCount++;
                continue;
            }
            if(elapsed>=animation.durationNanos){
                animation.active=false;
                applyAnimation(animation,1f);
                finishAnimation(animation);
                continue;
            }
            activeCount++;
            applyAnimation(animation,(float)((double)elapsed/(double)animation.durationNanos));
        }
        if(activeCount==0){
            timer.stop();
            if(completionListener!=null){
                completionListener.onAllAnimationsFinished();
            }
        }
        boardPanel.repaint();
    }
    private void applyAnimation(Animation animation,float progress){
        if(animation.identifier==SLIDE_IDENTIFIER){
            applySlide(animation,progress);
        }else if(animation.identifier==MERGE_POP_IDENTIFIER){
            applyMergePop(animation,progress);
        }else if(animation.identifier==SPAWN_FADE_IDENTIFIER){
            applySpawnFade(animation,progress);
        }else{
            applyScoreFloat(animation,progress);
        }
    }
    private void applySlide(Animation animation,float progress){
        TileView view=animation.view;
        if(view==null){
            return;
        }
        float eased=Easing.easeOutCubic(progress);
        view.pixelCenterX=animation.fromPixelX+(animation.toPixelX-animation.fromPixelX)*eased;
        view.pixelCenterY=animation.fromPixelY+(animation.toPixelY-animation.fromPixelY)*eased;
    }
    private void applyMergePop(Animation animation,float progress){
        TileView view=animation.view;
        if(view==null){
            return;
        }
        view.exponent=animation.newExponent;
        float rise;
        if(progress<0.5f){
            rise=Easing.easeOutBack(progress*2f);
        }else{
            rise=1f;
        }
        float settle=progress<0.5f?0f:Easing.easeInOutQuad((progress-0.5f)*2f);
        view.scaleFactor=1f+(MERGE_POP_SCALE-1f)*(rise-settle);
    }
    private void applySpawnFade(Animation animation,float progress){
        TileView view=animation.view;
        if(view==null){
            return;
        }
        view.alphaValue=Easing.easeOutCubic(progress);
        view.scaleFactor=SPAWN_START_SCALE+(1f-SPAWN_START_SCALE)*Easing.easeOutBack(progress);
    }
    private void applyScoreFloat(Animation animation,float progress){
        float eased=Easing.linear(progress);
        boardPanel.floatingScoreText=animation.floatText;
        boardPanel.floatingScorePixelX=animation.floatStartPixelX;
        boardPanel.floatingScorePixelY=animation.floatStartPixelY-SCORE_FLOAT_RISE_PIXELS*eased;
        boardPanel.floatingScoreAlpha=1f-eased;
    }
    private void finishAnimation(Animation animation){
        if(animation.identifier==SCORE_FLOAT_IDENTIFIER){
            boardPanel.floatingScoreText=null;
            boardPanel.floatingScoreAlpha=0f;
            return;
        }
        if(animation.identifier==MERGE_POP_IDENTIFIER){
            settleView(animation.view);
            removeMovingView(animation.consumedView);
            boardPanel.recycleTileView(animation.consumedView);
            return;
        }
        if(animation.settlesOnCompletion){
            settleView(animation.view);
            return;
        }
        removeMovingView(animation.view);
    }
    private void settleView(TileView view){
        if(view==null){
            return;
        }
        tileViews[view.row][view.column]=view;
        removeMovingView(view);
    }
    private void removeMovingView(TileView view){
        if(view==null){
            return;
        }
        for(int index=0;index<movingViewCount;index++){
            if(movingViews[index]==view){
                movingViews[index]=movingViews[movingViewCount-1];
                movingViews[movingViewCount-1]=null;
                movingViewCount--;
                return;
            }
        }
    }
}
