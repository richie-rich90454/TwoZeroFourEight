package org.twozerofoureightgame.view;
import java.awt.BorderLayout;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.twozerofoureightgame.utility.Easing;
/**
 * The dimmed wash that covers the game when it ends, carrying one large message
 * and the buttons that answer it. Three things can put it on screen: reaching
 * the 2048 tile, running out of legal moves, and the reachability probe giving
 * up. The message fades in over a fifth of a second rather than appearing
 * instantly, and the Continue button is only offered for a win, because a lost
 * game has nothing to continue and the probe is only ever a guess.
 */
public final class OverlayPanel extends JPanel{
    /** How long the wash takes to fade in. */
    public static final long FADE_DURATION_NANOS=200000000L;
    private static final int TICK_INTERVAL_MILLISECONDS=16;
    private final JButton newGameButton=new JButton("New Game");
    private final JButton continueButton=new JButton("Continue");
    private String message="";
    private float fadeAlpha=0f;
    private long animationStartNanos=0L;
    private final javax.swing.Timer fadeTimer;
    /**
     * Creates the overlay with its button row.
     */
    public OverlayPanel(){
        setOpaque(false);
        setLayout(new BorderLayout());
        JPanel messageSpacer=new JPanel(new BorderLayout());
        messageSpacer.setOpaque(false);
        messageSpacer.setBorder(BorderFactory.createEmptyBorder(0,0,24,0));
        add(messageSpacer,BorderLayout.CENTER);
        JPanel buttonPanel=new JPanel();
        buttonPanel.setOpaque(false);
        styleButton(newGameButton);
        styleButton(continueButton);
        buttonPanel.add(newGameButton);
        buttonPanel.add(continueButton);
        add(buttonPanel,BorderLayout.SOUTH);
        fadeTimer=new javax.swing.Timer(TICK_INTERVAL_MILLISECONDS,new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                tickFade();
            }
        });
        fadeTimer.setRepeats(true);
    }
    private void styleButton(JButton button){
        button.setFont(Theme.BUTTON_FONT);
        button.setBackground(Theme.ACCENT_COLOR);
        button.setForeground(Theme.BUTTON_TEXT_COLOR);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(9,16,9,16));
    }
    /**
     * Shows a message and starts the fade.
     * @param message the message to show
     * @param allowContinue true to offer the Continue button, which only makes
     *                      sense after reaching the 2048 tile
     */
    public void showMessage(String message,boolean allowContinue){
        this.message=message;
        continueButton.setVisible(allowContinue);
        fadeAlpha=0f;
        animationStartNanos=System.nanoTime();
        if(!fadeTimer.isRunning()){
            fadeTimer.start();
        }
    }
    /**
     * Clears the message and stops the fade.
     */
    public void hideOverlay(){
        fadeTimer.stop();
        fadeAlpha=0f;
        message="";
        continueButton.setVisible(false);
    }
    /**
     * Reports whether a message is currently being shown.
     * @return true when a message is on screen
     */
    public boolean hasMessage(){
        return message.length()>0;
    }
    private void tickFade(){
        long elapsed=System.nanoTime()-animationStartNanos;
        if(elapsed>=FADE_DURATION_NANOS){
            fadeAlpha=1f;
            fadeTimer.stop();
            repaint();
            return;
        }
        fadeAlpha=Easing.easeOutCubic((float)((double)elapsed/(double)FADE_DURATION_NANOS));
        repaint();
    }
    @Override
    protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);
        if(message.length()==0||fadeAlpha<=0f){
            return;
        }
        Graphics2D graphics2D=(Graphics2D)graphics;
        graphics2D.setRenderingHints(Theme.ANTIALIASED_RENDERING_HINTS);
        Composite previousComposite=graphics2D.getComposite();
        graphics2D.setComposite(Theme.alphaComposite(0.86f*fadeAlpha));
        graphics2D.setColor(Theme.OVERLAY_COLOR);
        graphics2D.fillRect(0,0,getWidth(),getHeight());
        graphics2D.setComposite(Theme.alphaComposite(fadeAlpha));
        graphics2D.setColor(Theme.BUTTON_TEXT_COLOR);
        graphics2D.setFont(Theme.OVERLAY_FONT);
        FontMetrics metrics=graphics2D.getFontMetrics();
        int textWidth=metrics.stringWidth(message);
        graphics2D.drawString(message,(getWidth()-textWidth)/2,getHeight()/2);
        graphics2D.setComposite(previousComposite);
    }
    /**
     * Returns the button that starts a new game from the overlay.
     * @return the new game button
     */
    public JButton getNewGameButton(){
        return newGameButton;
    }
    /**
     * Returns the button that dismisses a win so that playing can continue.
     * @return the continue button
     */
    public JButton getContinueButton(){
        return continueButton;
    }
}
