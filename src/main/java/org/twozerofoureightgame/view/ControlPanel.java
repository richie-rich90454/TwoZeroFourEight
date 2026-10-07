package org.twozerofoureightgame.view;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.twozerofoureightgame.controller.GameController;
/**
 * The strip below the board holding the eight buttons: new game, undo, hint,
 * auto play, pause, export, import, and screenshot, plus the autoplay speed
 * slider. Every button is flat, blue,
 * and built once here, so pressing one costs nothing beyond the action it runs.
 * The buttons and the slider do not ask for keyboard focus, which keeps the arrow keys free for
 * moving tiles.
 */
public final class ControlPanel extends JPanel{
    private final JButton newGameButton=new JButton("New Game");
    private final JButton undoButton=new JButton("Undo");
    private final JButton hintButton=new JButton("Hint");
    private final JButton autoPlayButton=new JButton("Auto Play");
    private final JButton pauseButton=new JButton("Pause");
    private final JButton exportButton=new JButton("Export");
    private final JButton importButton=new JButton("Import");
    private final JButton screenshotButton=new JButton("Screenshot");
    /**
     * Creates the buttons and wires each of them to the controller.
     * @param controller the controller that carries out the requested action
     */
    public ControlPanel(final GameController controller){
        setOpaque(true);
        setBackground(Theme.WINDOW_BACKGROUND);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(6,10,10,10));
        JPanel buttonGrid=new JPanel(new GridLayout(2,4,8,4));
        buttonGrid.setOpaque(false);
        styleButton(newGameButton);
        styleButton(undoButton);
        styleButton(hintButton);
        styleButton(autoPlayButton);
        styleButton(pauseButton);
        styleButton(exportButton);
        styleButton(importButton);
        styleButton(screenshotButton);
        newGameButton.addActionListener(new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                controller.startNewGame();
            }
        });
        undoButton.addActionListener(new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                controller.undoLastMove();
            }
        });
        hintButton.addActionListener(new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                controller.requestHint();
            }
        });
        autoPlayButton.addActionListener(new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                controller.toggleAutoPlay();
            }
        });
        pauseButton.addActionListener(new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                controller.togglePause();
            }
        });
        exportButton.addActionListener(new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                controller.exportGame();
            }
        });
        importButton.addActionListener(new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                controller.importGame();
            }
        });
        screenshotButton.addActionListener(new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                controller.takeScreenshot();
            }
        });
        buttonGrid.add(newGameButton);
        buttonGrid.add(undoButton);
        buttonGrid.add(hintButton);
        buttonGrid.add(autoPlayButton);
        buttonGrid.add(pauseButton);
        buttonGrid.add(exportButton);
        buttonGrid.add(importButton);
        buttonGrid.add(screenshotButton);
        add(buttonGrid,BorderLayout.CENTER);
        final JLabel speedLabel=new JLabel();
        speedLabel.setFont(Theme.CAPTION_FONT);
        speedLabel.setForeground(Theme.ACCENT_COLOR);
        final JSlider speedSlider=new JSlider(JSlider.HORIZONTAL,GameController.AUTOPLAY_MINIMUM_DELAY_MILLISECONDS,GameController.AUTOPLAY_MAXIMUM_DELAY_MILLISECONDS,controller.getAutoPlayDelayMilliseconds());
        speedSlider.setOpaque(false);
        speedSlider.setFocusable(false);
        speedSlider.setMajorTickSpacing(300);
        speedSlider.setMinorTickSpacing(100);
        speedSlider.setPaintTicks(true);
        speedSlider.setForeground(Theme.ACCENT_COLOR);
        speedSlider.setUI(new ThemedSliderUI(speedSlider));
        updateSpeedLabel(speedLabel,speedSlider.getValue());
        speedSlider.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent event){
                int value=speedSlider.getValue();
                controller.setAutoPlayDelayMilliseconds(value);
                updateSpeedLabel(speedLabel,value);
            }
        });
        JPanel speedPanel=new JPanel(new BorderLayout(8,0));
        speedPanel.setOpaque(false);
        speedPanel.setBorder(BorderFactory.createEmptyBorder(6,2,0,2));
        speedPanel.add(speedLabel,BorderLayout.WEST);
        speedPanel.add(speedSlider,BorderLayout.CENTER);
        add(speedPanel,BorderLayout.SOUTH);
    }
    private static void updateSpeedLabel(JLabel speedLabel,int delayMilliseconds){
        speedLabel.setText("AI speed: "+delayMilliseconds+" ms");
    }
    /**
     * Paints the speed slider in the board colors, a sand track with a brown
     * fill up to the thumb and a round brown thumb, so the gray default control
     * never shows. Ticks and text follow the slider foreground.
     */
    private static final class ThemedSliderUI extends javax.swing.plaf.basic.BasicSliderUI{
        ThemedSliderUI(JSlider slider){
            super(slider);
        }
        @Override
        protected java.awt.Dimension getThumbSize(){
            return new java.awt.Dimension(18,18);
        }
        @Override
        public void paintTrack(java.awt.Graphics graphics){
            java.awt.Graphics2D track=(java.awt.Graphics2D)graphics.create();
            track.setRenderingHints(Theme.ANTIALIASED_RENDERING_HINTS);
            int middle=trackRect.y+trackRect.height/2;
            int height=6;
            track.setColor(Theme.EMPTY_CELL_COLOR);
            track.fillRoundRect(trackRect.x,middle-height/2,trackRect.width,height,height,height);
            int fill=thumbRect.x+thumbRect.width/2-trackRect.x;
            track.setColor(Theme.ACCENT_COLOR);
            track.fillRoundRect(trackRect.x,middle-height/2,Math.max(height,fill),height,height,height);
            track.dispose();
        }
        @Override
        public void paintThumb(java.awt.Graphics graphics){
            java.awt.Graphics2D thumb=(java.awt.Graphics2D)graphics.create();
            thumb.setRenderingHints(Theme.ANTIALIASED_RENDERING_HINTS);
            thumb.setColor(Theme.ACCENT_COLOR);
            thumb.fillOval(thumbRect.x,thumbRect.y,thumbRect.width,thumbRect.height);
            thumb.setColor(Theme.BUTTON_TEXT_COLOR);
            thumb.drawOval(thumbRect.x,thumbRect.y,thumbRect.width-1,thumbRect.height-1);
            thumb.dispose();
        }
    }
    private void styleButton(JButton button){
        button.setFont(Theme.BUTTON_FONT);
        button.setBackground(Theme.ACCENT_COLOR);
        button.setForeground(Theme.BUTTON_TEXT_COLOR);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createEmptyBorder(7,6,7,6));
        button.setMargin(new java.awt.Insets(0,0,0,0));
    }

    /**
     * Tints the buttons that are currently meaningful, so that the active mode of
     * the game is obvious without reading the status word.
     * @param autoPlayActive true while the artificial intelligence is driving
     * @param paused true while the game is paused
     */
    public void reflectModes(boolean autoPlayActive,boolean paused){
        autoPlayButton.setBackground(autoPlayActive?Theme.HEADING_COLOR:Theme.ACCENT_COLOR);
        pauseButton.setBackground(paused?Theme.HEADING_COLOR:Theme.ACCENT_COLOR);
    }
}
