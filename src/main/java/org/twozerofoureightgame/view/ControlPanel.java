package org.twozerofoureightgame.view;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.twozerofoureightgame.controller.GameController;
/**
 * The strip below the board holding the eight buttons: new game, undo, hint,
 * auto play, pause, export, import, and screenshot. Every button is flat, blue,
 * and built once here, so pressing one costs nothing beyond the action it runs.
 * The buttons do not ask for keyboard focus, which keeps the arrow keys free for
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
        setLayout(new GridLayout(2,4,8,4));
        setBorder(BorderFactory.createEmptyBorder(6,10,10,10));
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
        add(newGameButton);
        add(undoButton);
        add(hintButton);
        add(autoPlayButton);
        add(pauseButton);
        add(exportButton);
        add(importButton);
        add(screenshotButton);
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
