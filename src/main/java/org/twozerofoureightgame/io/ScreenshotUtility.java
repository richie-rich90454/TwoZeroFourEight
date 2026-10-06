package org.twozerofoureightgame.io;
import java.awt.AWTError;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
/**
 * Writes a picture of the game to disk as a PNG file.
 * When the window is on screen the picture is taken straight from the display,
 * so it shows the window exactly as the player sees it, title bar and all. When
 * there is no display to read, or the window is not showing, the components are
 * painted into an off screen image instead, which yields the whole window
 * without the operating system frame. Either way the file name carries the date
 * and the time, so several pictures can be kept without renaming them.
 * The image is created only when a picture is actually taken, so an ordinary
 * game never pays for it, and it is discarded as soon as it has been written.
 */
public final class ScreenshotUtility{
    /** How long the window is given to come to the front before it is read. */
    private static final long SETTLE_DELAY_MILLISECONDS=140L;
    /**
     * This class only exposes static helpers and is never instantiated.
     */
    private ScreenshotUtility(){
    }
    /**
     * Captures the whole window as it appears on screen.
     * @param frame the window to capture
     * @return the absolute path of the picture that was written
     * @throws IOException when the picture cannot be created or written
     */
    public static String capture(JFrame frame)throws IOException{
        BufferedImage image=captureFromDisplay(frame);
        if(image==null){
            image=paintWindow(frame);
        }
        return write(image);
    }
    /**
     * Captures a single component by painting it, for callers that have no
     * window to read from the display.
     * @param component the component to capture
     * @return the absolute path of the picture that was written
     * @throws IOException when the picture cannot be created or written
     */
    public static String capture(JComponent component)throws IOException{
        return write(paintComponent(component));
    }
    private static BufferedImage captureFromDisplay(JFrame frame){
        if(frame==null||!frame.isShowing()){
            return null;
        }
        try{
            Robot robot=new Robot();
            frame.toFront();
            robot.waitForIdle();
            Thread.sleep(SETTLE_DELAY_MILLISECONDS);
            Rectangle bounds=frame.getBounds();
            if(bounds.width<=0||bounds.height<=0){
                return null;
            }
            return robot.createScreenCapture(bounds);
        }catch(Exception exception){
            return null;
        }catch(AWTError error){
            return null;
        }
    }
    private static BufferedImage paintWindow(JFrame frame){
        JComponent content=frame==null?null:(JComponent)frame.getContentPane();
        if(content==null){
            return new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);
        }
        return paintComponent(content);
    }
    private static BufferedImage paintComponent(JComponent component){
        int width=Math.max(1,component.getWidth());
        int height=Math.max(1,component.getHeight());
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics();
        component.paint(graphics);
        graphics.dispose();
        return image;
    }
    private static String write(BufferedImage image)throws IOException{
        String stamp=new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File file=new File("2048-screenshot-"+stamp+".png");
        ImageIO.write(image,"png",file);
        return file.getAbsolutePath();
    }
}
