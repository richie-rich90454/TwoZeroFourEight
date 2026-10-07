package org.twozerofoureightgame.view;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import org.twozerofoureightgame.model.Board;
import org.twozerofoureightgame.model.MoveResult;
import org.twozerofoureightgame.model.TileTransition;
import org.twozerofoureightgame.utility.AnimationManager;
/**
 * Paints the board and turns a move into animation. The panel owns one tile view
 * per cell, a pool of spare tile views to reuse, and the animation manager that
 * drives every slide, merge settle, spawn fade, and score float from a single
 * timer.
 * A move is animated by following the transitions the board recorded. Each tile
 * that travels is lifted out of the grid, added to the flying set, and handed to
 * the manager with the centres it should travel between. Two tiles that merge
 * both fly to the same cell: the first one waits for its partner, and when the
 * second arrives the manager is told to settle the first and recycle the second.
 * While the board is moving, input is refused, so the grid and the tiles always
 * describe the same game.
 */
public final class BoardPanel extends JPanel{
    /** The largest cell the board will draw, in pixels. */
    public static final int MAXIMUM_CELL_SIZE=100;
    /** The smallest cell the board will shrink to, in pixels. */
    public static final int MINIMUM_CELL_SIZE=48;
    /** The width and height of one cell in pixels. */
    public static final int CELL_SIZE=94;
    /** The gap between two neighbouring cells in pixels. */
    public static final int CELL_GAP=12;
    /** The gap between the outer cells and the edge of the board. */
    public static final int BOARD_MARGIN=18;
    /** The text of the score float, or null when nothing is floating. */
    public String floatingScoreText=null;
    /** The horizontal centre of the score float. */
    public float floatingScorePixelX=0f;
    /** The vertical centre of the score float. */
    public float floatingScorePixelY=0f;
    /** How opaque the score float is. */
    public float floatingScoreAlpha=0f;
    private final TileView[][] tileViews=new TileView[4][4];
    private final TileView[] recycledTileViews=new TileView[16];
    private final TileView[] mergePartnerViews=new TileView[16];
    private int recycledTileViewCount=0;
    private int boardOffsetX=0;
    private int boardOffsetY=0;
    private int currentCellSize=CELL_SIZE;
    private final AnimationManager animationManager;
    /**
     * Creates an empty board panel and its animation manager.
     */
    public BoardPanel(){
        setOpaque(false);
        animationManager=new AnimationManager(this,tileViews);
    }
    /**
     * Returns the manager that drives the animations, which the controller
     * consults before accepting another move.
     * @return the animation manager
     */
    public AnimationManager getAnimationManager(){
        return animationManager;
    }
    @Override
    public void doLayout(){
        super.doLayout();
        int fixed=BOARD_MARGIN*2+CELL_GAP*5;
        int available=Math.min(getWidth(),getHeight())-fixed;
        int cell=available/4;
        if(cell<MINIMUM_CELL_SIZE){
            cell=MINIMUM_CELL_SIZE;
        }
        if(cell>MAXIMUM_CELL_SIZE){
            cell=MAXIMUM_CELL_SIZE;
        }
        currentCellSize=cell;
        int boardExtent=fixed+cell*4;
        boardOffsetX=(getWidth()-boardExtent)/2;
        boardOffsetY=(getHeight()-boardExtent)/2;
    }
    /**
     * Returns the size of one cell, which shrinks to fit whatever room the
     * window actually gives the board. Nothing can be clipped this way.
     * @return the current cell size in pixels
     */
    public int cellSize(){
        return currentCellSize;
    }
    /**
     * Returns the left edge of one column, including the board offset.
     * @param column the column counted from the left
     * @return the left edge in panel pixels
     */
    public int cellLeft(int column){
        return boardOffsetX+BOARD_MARGIN+CELL_GAP+column*(currentCellSize+CELL_GAP);
    }
    /**
     * Returns the top edge of one row, including the board offset.
     * @param row the row counted from the top
     * @return the top edge in panel pixels
     */
    public int cellTop(int row){
        return boardOffsetY+BOARD_MARGIN+CELL_GAP+row*(currentCellSize+CELL_GAP);
    }
    /**
     * Returns the horizontal centre of one column.
     * @param column the column counted from the left
     * @return the horizontal centre in panel pixels
     */
    public float cellCenterX(int column){
        return cellLeft(column)+currentCellSize*0.5f;
    }
    /**
     * Returns the vertical centre of one row.
     * @param row the row counted from the top
     * @return the vertical centre in panel pixels
     */
    public float cellCenterY(int row){
        return cellTop(row)+currentCellSize*0.5f;
    }
    /**
     * Rebuilds every tile from the model, with no animation at all. This is what
     * a new game and an undo need.
     * @param board the model board to mirror
     */
    public void rebuildFromBoard(Board board){
        animationManager.cancelAll();
        doLayout();
        for(int row=0;row<4;row++){
            for(int column=0;column<4;column++){
                TileView existing=tileViews[row][column];
                if(existing!=null){
                    tileViews[row][column]=null;
                    recycleTileView(existing);
                }
            }
        }
        for(int row=0;row<4;row++){
            for(int column=0;column<4;column++){
                byte exponent=board.getExponentAt(row,column);
                if(exponent==0){
                    continue;
                }
                TileView view=obtainTileView();
                view.exponent=exponent;
                view.row=row;
                view.column=column;
                view.scaleFactor=1f;
                view.alphaValue=1f;
                view.pixelCenterX=cellCenterX(column);
                view.pixelCenterY=cellCenterY(row);
                tileViews[row][column]=view;
            }
        }
        repaint();
    }
    /**
     * Animates one move.
     * @param result the description of the move the board just applied
     */
    public void applyMoveResult(MoveResult result){
        for(int index=0;index<mergePartnerViews.length;index++){
            mergePartnerViews[index]=null;
        }
        for(int index=0;index<result.tileTransitionCount;index++){
            TileTransition transition=result.tileTransitions[index];
            if(transition.isNewSpawn){
                TileView spawnedView=obtainTileView();
                spawnedView.exponent=transition.newExponent;
                spawnedView.row=transition.toRow;
                spawnedView.column=transition.toColumn;
                spawnedView.scaleFactor=1f;
                spawnedView.alphaValue=0f;
                spawnedView.pixelCenterX=cellCenterX(transition.toColumn);
                spawnedView.pixelCenterY=cellCenterY(transition.toRow);
                tileViews[transition.toRow][transition.toColumn]=spawnedView;
                animationManager.addMovingView(spawnedView);
                animationManager.startSpawnFade(spawnedView,transition.toRow,transition.toColumn);
                continue;
            }
            TileView view=tileViews[transition.fromRow][transition.fromColumn];
            if(view==null){
                continue;
            }
            tileViews[transition.fromRow][transition.fromColumn]=null;
            view.row=transition.toRow;
            view.column=transition.toColumn;
            animationManager.addMovingView(view);
            animationManager.startSlide(view,transition.fromRow,transition.fromColumn,transition.toRow,transition.toColumn,cellCenterX(transition.fromColumn),cellCenterY(transition.fromRow),cellCenterX(transition.toColumn),cellCenterY(transition.toRow),!transition.merged);
            if(transition.merged){
                int destinationIndex=transition.toRow*4+transition.toColumn;
                TileView partner=mergePartnerViews[destinationIndex];
                if(partner==null){
                    mergePartnerViews[destinationIndex]=view;
                }else{
                    mergePartnerViews[destinationIndex]=null;
                    animationManager.startMergePop(partner,view,transition.toRow,transition.toColumn,transition.newExponent);
                }
            }
        }
        if(result.scoreGained>0){
            animationManager.startScoreFloat("+"+result.scoreGained,boardOffsetX+BOARD_MARGIN+CELL_GAP+2*(currentCellSize+CELL_GAP)+currentCellSize*0.5f,cellTop(0)+currentCellSize*0.6f);
        }
    }
    /**
     * Returns a tile view to the pool of spares so that the next tile does not
     * need a new object.
     * @param view the tile view to keep for later, which may be null
     */
    public void recycleTileView(TileView view){
        if(view==null||recycledTileViewCount>=recycledTileViews.length){
            return;
        }
        recycledTileViews[recycledTileViewCount++]=view;
    }
    private TileView obtainTileView(){
        if(recycledTileViewCount>0){
            return recycledTileViews[--recycledTileViewCount];
        }
        return new TileView();
    }
    @Override
    protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);
        Graphics2D graphics2D=(Graphics2D)graphics;
        graphics2D.setRenderingHints(Theme.ANTIALIASED_RENDERING_HINTS);
        graphics2D.setColor(Theme.BOARD_BACKGROUND);
        int boardExtent=BOARD_MARGIN*2+CELL_GAP*5+currentCellSize*4;
        graphics2D.fillRoundRect(boardOffsetX,boardOffsetY,boardExtent,boardExtent,Theme.ARC_RADIUS*2,Theme.ARC_RADIUS*2);
        graphics2D.setColor(Theme.EMPTY_CELL_COLOR);
        for(int row=0;row<4;row++){
            for(int column=0;column<4;column++){
                graphics2D.fillRoundRect(cellLeft(column),cellTop(row),currentCellSize,currentCellSize,Theme.ARC_RADIUS*2,Theme.ARC_RADIUS*2);
            }
        }
        for(int row=0;row<4;row++){
            for(int column=0;column<4;column++){
                TileView view=tileViews[row][column];
                if(view!=null){
                    view.draw(graphics2D,currentCellSize);
                }
            }
        }
        TileView[] movingViews=animationManager.getMovingViews();
        int movingViewCount=animationManager.getMovingViewCount();
        for(int index=0;index<movingViewCount;index++){
            movingViews[index].draw(graphics2D,currentCellSize);
        }
        if(floatingScoreText!=null&&floatingScoreAlpha>0f){
            Composite previousComposite=graphics2D.getComposite();
            graphics2D.setComposite(Theme.alphaComposite(floatingScoreAlpha));
            graphics2D.setColor(Theme.LIGHT_TEXT_COLOR);
            graphics2D.setFont(Theme.VALUE_FONT);
            FontMetrics metrics=graphics2D.getFontMetrics();
            int textWidth=metrics.stringWidth(floatingScoreText);
            graphics2D.drawString(floatingScoreText,(int)floatingScorePixelX-textWidth/2,(int)floatingScorePixelY);
            graphics2D.setComposite(previousComposite);
        }
    }
}
