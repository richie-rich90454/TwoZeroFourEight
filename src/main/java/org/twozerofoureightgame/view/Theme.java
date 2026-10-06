package org.twozerofoureightgame.view;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
/**
 * Every colour, font, radius, and rendering setting used by the interface, all
 * created once when the class is loaded. Painting code looks values up here
 * instead of building new objects, which is what keeps a repaint free of
 * allocation.
 * The look is deliberately plain: a warm off white page, a flat sand coloured
 * board, flat squares of colour for the tiles, and nothing else. There are no
 * gradients, no shadows, no glows, and no highlights. Every tile value has its
 * own colour so that the board can be read at a glance without reading a single
 * number, and the colours run from pale sand for the smallest tile through
 * orange, then gold, and finally to near black for the largest.
 */
public final class Theme{
    /** The colour behind everything, a warm off white. */
    public static final Color WINDOW_BACKGROUND=new Color(0xFAF8F2);
    /** The colour of the board itself. */
    public static final Color BOARD_BACKGROUND=new Color(0xBBADA0);
    /** The colour of the sixteen empty cells. */
    public static final Color EMPTY_CELL_COLOR=new Color(0xCDC1B4);
    /** The colour used for buttons. */
    public static final Color ACCENT_COLOR=new Color(0x8F7A66);
    /** The colour of the button captions. */
    public static final Color BUTTON_TEXT_COLOR=new Color(0xF9F6F2);
    /** The colour of text drawn on a pale tile. */
    public static final Color DARK_TEXT_COLOR=new Color(0x776E65);
    /** The colour of text drawn on a dark tile. */
    public static final Color LIGHT_TEXT_COLOR=new Color(0xF9F6F2);
    /** The colour of the heading and the header numbers. */
    public static final Color HEADING_COLOR=new Color(0x776E65);
    /** The colour of the dimmed wash behind an overlay message. */
    public static final Color OVERLAY_COLOR=new Color(0x8F7A66);
    /** The corner radius of every rounded rectangle, in pixels. */
    public static final int ARC_RADIUS=6;
    /** Anti aliased shapes, text, and strokes, reused on every repaint. */
    public static final RenderingHints ANTIALIASED_RENDERING_HINTS=buildRenderingHints();
    /** The font of the heading drawn inside the header. */
    public static final Font TITLE_FONT=new Font("SansSerif",Font.BOLD,26);
    /** The font of the small captions above the header numbers. */
    public static final Font CAPTION_FONT=new Font("SansSerif",Font.BOLD,13);
    /** The font of the header numbers and the status text. */
    public static final Font VALUE_FONT=new Font("SansSerif",Font.BOLD,19);
    /** The font of a tile showing one digit, such as the tile worth 8. */
    public static final Font TILE_FONT_ONE_DIGIT=new Font("SansSerif",Font.BOLD,38);
    /** The font of a tile showing two digits. */
    public static final Font TILE_FONT_TWO_DIGITS=new Font("SansSerif",Font.BOLD,32);
    /** The font of a tile showing three digits. */
    public static final Font TILE_FONT_THREE_DIGITS=new Font("SansSerif",Font.BOLD,26);
    /** The font of a tile showing four or more digits. */
    public static final Font TILE_FONT_FOUR_DIGITS=new Font("SansSerif",Font.BOLD,20);
    /** The font of the button captions. */
    public static final Font BUTTON_FONT=new Font("SansSerif",Font.BOLD,13);
    /** The font of the large overlay message. */
    public static final Font OVERLAY_FONT=new Font("SansSerif",Font.BOLD,40);
    /** The largest exponent whose tile is pale enough for dark text. */
    public static final int DARK_TEXT_LIMIT_EXPONENT=2;
    private static final Color[] TILE_PALETTE=new Color[16];
    private static final String[] TILE_TEXTS=new String[16];
    private static final AlphaComposite[] ALPHA_COMPOSITES=new AlphaComposite[101];
    private static final int[][] TILE_COLOUR_SOURCE={
        {0xEE,0xE4,0xDA},
        {0xED,0xE0,0xC8},
        {0xF2,0xB1,0x79},
        {0xF5,0x95,0x63},
        {0xF6,0x7C,0x5F},
        {0xF6,0x5E,0x3B},
        {0xED,0xCF,0x72},
        {0xED,0xCC,0x61},
        {0xED,0xC8,0x50},
        {0xED,0xC5,0x3F},
        {0xED,0xC2,0x2E},
        {0x3C,0x3A,0x32},
        {0x33,0x31,0x2B},
        {0x2B,0x29,0x24},
        {0x24,0x22,0x1E},
        {0x1E,0x1C,0x19}
    };
    static{
        for(int exponent=0;exponent<16;exponent++){
            int sourceIndex=exponent<=1?0:exponent-1;
            TILE_PALETTE[exponent]=new Color(TILE_COLOUR_SOURCE[sourceIndex][0],TILE_COLOUR_SOURCE[sourceIndex][1],TILE_COLOUR_SOURCE[sourceIndex][2]);
            TILE_TEXTS[exponent]=Integer.toString(1<<exponent);
        }
        for(int step=0;step<=100;step++){
            ALPHA_COMPOSITES[step]=AlphaComposite.getInstance(AlphaComposite.SRC_OVER,step/100f);
        }
    }
    /**
     * This class only exposes static helpers and is never instantiated.
     */
    private Theme(){
    }
    private static RenderingHints buildRenderingHints(){
        RenderingHints hints=new RenderingHints((RenderingHints.Key)null,null);
        hints.put(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        hints.put(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
        hints.put(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        return hints;
    }
    /**
     * Returns the fill colour of a tile.
     * @param exponent the tile exponent
     * @return the tile colour
     */
    public static Color tileColor(byte exponent){
        return TILE_PALETTE[exponent&0xF];
    }
    /**
     * Returns the text drawn on a tile.
     * @param exponent the tile exponent
     * @return the tile value as text, for example "2048"
     */
    public static String tileText(byte exponent){
        return TILE_TEXTS[exponent&0xF];
    }
    /**
     * Returns the readable text colour for a tile.
     * @param exponent the tile exponent
     * @return dark text on the two palest tiles and light text on the rest
     */
    public static Color tileTextColor(byte exponent){
        return (exponent&0xF)<=DARK_TEXT_LIMIT_EXPONENT?DARK_TEXT_COLOR:LIGHT_TEXT_COLOR;
    }
    /**
     * Returns the font that fits a tile value of the given size.
     * @param exponent the tile exponent
     * @return the largest font that still fits inside the cell
     */
    public static Font tileFont(byte exponent){
        int digitCount=Theme.tileText(exponent).length();
        if(digitCount<=1){
            return TILE_FONT_ONE_DIGIT;
        }
        if(digitCount==2){
            return TILE_FONT_TWO_DIGITS;
        }
        if(digitCount==3){
            return TILE_FONT_THREE_DIGITS;
        }
        return TILE_FONT_FOUR_DIGITS;
    }
    /**
     * Returns a translucent version of the source rule from the prebuilt table,
     * so that painting never creates a composite rule of its own.
     * @param alpha the transparency from zero for invisible to one for solid
     * @return the matching composite rule
     */
    public static AlphaComposite alphaComposite(float alpha){
        int step=(int)(alpha*100f+0.5f);
        if(step<0){
            step=0;
        }
        if(step>100){
            step=100;
        }
        return ALPHA_COMPOSITES[step];
    }
}
