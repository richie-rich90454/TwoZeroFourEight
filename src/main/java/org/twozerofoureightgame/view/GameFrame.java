package org.twozerofoureightgame.view;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.twozerofoureightgame.controller.GameController;
import org.twozerofoureightgame.controller.GestureHandler;
import org.twozerofoureightgame.controller.KeyboardHandler;
import org.twozerofoureightgame.utility.IconLoader;
/**
 * The window itself. It has a fixed size of six hundred by seven hundred pixels,
 * it cannot be resized, and it stacks three pieces from top to bottom: the
 * header, the board, and the controls. The end of game overlay is mounted on the
 * glass pane, which is the layer Swing reserves for exactly this: an overlay
 * that covers the whole window and takes every mouse click until it is dismissed.
 */
public final class GameFrame extends JFrame{
    /** The width of the window in pixels. */
    public static final int WINDOW_WIDTH=600;
    /** The height of the window in pixels. */
    public static final int WINDOW_HEIGHT=700;
    private final HeaderPanel headerPanel;
    private final BoardPanel boardPanel;
    private final ControlPanel controlPanel;
    private final OverlayPanel overlayPanel;
    /**
     * Builds the window and connects every part of the interface to the
     * controller: the buttons, the keyboard, the swipe gesture, and the moment
     * the board comes to rest.
     * @param controller the controller that owns the game
     */
    public GameFrame(GameController controller){
        super("2048 — Expectimax");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(IconLoader.loadApplicationIcon());
        setSize(WINDOW_WIDTH,WINDOW_HEIGHT);
        setResizable(false);
        headerPanel=new HeaderPanel();
        boardPanel=new BoardPanel();
        controlPanel=new ControlPanel(controller);
        overlayPanel=new OverlayPanel();
        getContentPane().setBackground(Theme.WINDOW_BACKGROUND);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(headerPanel,BorderLayout.NORTH);
        JPanel boardWrapper=new JPanel(new BorderLayout());
        boardWrapper.setBackground(Theme.WINDOW_BACKGROUND);
        boardWrapper.add(boardPanel,BorderLayout.CENTER);
        getContentPane().add(boardWrapper,BorderLayout.CENTER);
        getContentPane().add(controlPanel,BorderLayout.SOUTH);
        JPanel glassPanel=(JPanel)getGlassPane();
        glassPanel.setLayout(new BorderLayout());
        glassPanel.add(overlayPanel,BorderLayout.CENTER);
        overlayPanel.getNewGameButton().addActionListener(new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                controller.onOverlayNewGame();
            }
        });
        overlayPanel.getContinueButton().addActionListener(new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                controller.onOverlayContinue();
            }
        });
        glassPanel.setVisible(false);
        boardPanel.getAnimationManager().setCompletionListener(controller);
        addKeyListener(new KeyboardHandler(controller));
        GestureHandler gestureHandler=new GestureHandler(controller);
        boardPanel.addMouseListener(gestureHandler);
        boardPanel.addMouseMotionListener(gestureHandler);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    /**
     * Returns the header showing the score, the best score, the moves, and the
     * status.
     * @return the header panel
     */
    public HeaderPanel getHeaderPanel(){
        return headerPanel;
    }
    /**
     * Returns the board.
     * @return the board panel
     */
    public BoardPanel getBoardPanel(){
        return boardPanel;
    }
    /**
     * Returns the button strip.
     * @return the control panel
     */
    public ControlPanel getControlPanel(){
        return controlPanel;
    }
    /**
     * Returns the end of game overlay.
     * @return the overlay panel
     */
    public OverlayPanel getOverlayPanel(){
        return overlayPanel;
    }
    /**
     * Shows the dimmed overlay across the whole window.
     */
    public void showGlassPane(){
        getGlassPane().setVisible(true);
    }
    /**
     * Hides the overlay so that the board can be played again.
     */
    public void hideGlassPane(){
        getGlassPane().setVisible(false);
    }
}
