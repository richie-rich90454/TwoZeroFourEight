package org.twozerofoureightgame.view;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
/**
 * The visual stand-in for one tile. Every field is a primitive, so a tile view
 * costs a header and seven small fields and nothing else. The animations move
 * the view by writing new pixel centres into it, changing its scale during a
 * merge, and fading its transparency, which means the painting code never has to
 * know what stage of a move it is in. Drawing creates no objects: the rounded
 * rectangle comes from the built in drawing methods, the text comes from the
 * prebuilt table in {@link Theme}, and the composite rule comes from a prebuilt
 * table there as well.
 */
public final class TileView{
    /** The exponent of the tile, where 0 means the cell is empty. */
    public byte exponent;
    /** The row this tile belongs to once it has settled. */
    public int row;
    /** The column this tile belongs to once it has settled. */
    public int column;
    /** The horizontal centre of the tile, in panel pixels. */
    public float pixelCenterX;
    /** The vertical centre of the tile, in panel pixels. */
    public float pixelCenterY;
    /** How large the tile is drawn relative to a full cell. */
    public float scaleFactor=1f;
    /** How opaque the tile is, from zero for invisible to one for solid. */
    public float alphaValue=1f;
    /**
     * Creates a blank tile view that the board panel will position and fill in.
     */
    public TileView(){
    }
    /**
     * Draws this tile as a rounded square with its value centred inside.
     * @param graphics the destination, already set up with anti aliasing
     * @param cellSize the width and height of one board cell in pixels
     */
    public void draw(Graphics2D graphics,float cellSize){
        int halfSize=(int)(cellSize*0.5f*scaleFactor);
        int left=(int)pixelCenterX-halfSize;
        int top=(int)pixelCenterY-halfSize;
        int size=halfSize*2;
        Composite previousComposite=graphics.getComposite();
        graphics.setComposite(Theme.alphaComposite(alphaValue));
        graphics.setColor(Theme.tileColor(exponent));
        graphics.fillRoundRect(left,top,size,size,Theme.ARC_RADIUS*2,Theme.ARC_RADIUS*2);
        graphics.setColor(Theme.tileTextColor(exponent));
        graphics.setFont(Theme.tileFont(exponent));
        FontMetrics metrics=graphics.getFontMetrics();
        String text=Theme.tileText(exponent);
        int textWidth=metrics.stringWidth(text);
        int baseline=(int)pixelCenterY+(metrics.getAscent()-metrics.getDescent())/2;
        graphics.drawString(text,(int)pixelCenterX-textWidth/2,baseline);
        graphics.setComposite(previousComposite);
    }
}
