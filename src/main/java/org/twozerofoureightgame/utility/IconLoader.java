package org.twozerofoureightgame.utility;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
/**
 * Loads the application icon from the resources that ship inside the JAR.
 * Several square sizes are packaged, from sixteen to five hundred and twelve
 * pixels, and this class picks the one that matches the display it is running
 * on, so the icon stays sharp on an ordinary monitor and on a high density
 * display alike. The icon itself is a miniature 2048 board, which remains
 * recognizable even when it is reduced to sixteen pixels in a title bar.
 * If the resources are missing for any reason the method returns null rather
 * than failing, because a missing icon must never stop the game from starting.
 */
public final class IconLoader{
    /** The icon sizes that are packaged, in pixels. */
    private static final int[] AVAILABLE_SIZES={16,32,48,64,128,256,512};
    /** The size, in pixels, of the icon in an ordinary window title bar. */
    private static final int TITLE_BAR_SIZE=32;
    /**
     * This class only exposes static helpers and is never instantiated.
     */
    private IconLoader(){
    }
    /**
     * Chooses a packaged size that suits the current display.
     * @return the edge length in pixels of the size to load
     */
    public static int preferredSizeForThisDisplay(){
        int resolution=Toolkit.getDefaultToolkit().getScreenResolution();
        int scale=resolution>=120?2:1;
        int wanted=TITLE_BAR_SIZE*scale;
        for(int index=0;index<AVAILABLE_SIZES.length;index++){
            if(AVAILABLE_SIZES[index]>=wanted){
                return AVAILABLE_SIZES[index];
            }
        }
        return AVAILABLE_SIZES[AVAILABLE_SIZES.length-1];
    }
    /**
     * Loads the application icon at a size suited to this display.
     * @return the icon image, or null when it cannot be loaded
     */
    public static Image loadApplicationIcon(){
        int size=preferredSizeForThisDisplay();
        Image image=loadIconOfSize(size);
        if(image==null){
            image=loadIconOfSize(256);
        }
        return image;
    }
    /**
     * Loads one specific packaged size of the icon.
     * @param size the edge length in pixels of the wanted icon
     * @return the icon image, or null when it cannot be loaded
     */
    public static Image loadIconOfSize(int size){
        String resource="/icons/favicon-"+size+".png";
        InputStream stream=IconLoader.class.getResourceAsStream(resource);
        if(stream==null){
            return null;
        }
        try{
            return ImageIO.read(stream);
        }catch(IOException exception){
            return null;
        }finally{
            try{
                stream.close();
            }catch(IOException exception){
                return null;
            }
        }
    }
}
