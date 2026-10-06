package org.twozerofoureightgame.view;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.twozerofoureightgame.utility.Easing;
/**
 * The strip above the board showing the score, the best score of this session,
 * the number of moves played, and the current status of the game. The score is
 * not written straight into the label: it counts up to its new value over a
 * quarter of a second so that the player can see how much a move was worth. The
 * timer that drives that count stops itself the moment the number arrives, so an
 * idle game costs nothing.
 */
public final class HeaderPanel extends JPanel{
    /** How long the score takes to travel to its new value. */
    public static final long SCORE_ANIMATION_NANOS=250000000L;
    private static final int TICK_INTERVAL_MILLISECONDS=16;
    private final JLabel scoreValueLabel=new JLabel("0");
    private final JLabel bestValueLabel=new JLabel("0");
    private final JLabel movesValueLabel=new JLabel("0");
    private final JLabel statusValueLabel=new JLabel("Ready");
    private final JLabel titleLabel=new JLabel("2048 — Expectimax");
    private int displayedScore=0;
    private int startingScore=0;
    private int targetScore=0;
    private long animationStartNanos=0L;
    private final javax.swing.Timer scoreTimer;
    /**
     * Creates the header with every label in place.
     */
    public HeaderPanel(){
        setOpaque(true);
        setBackground(Theme.WINDOW_BACKGROUND);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8,18,2,18));
        titleLabel.setFont(Theme.TITLE_FONT);
        titleLabel.setForeground(Theme.HEADING_COLOR);
        JPanel titlePanel=new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel,BorderLayout.WEST);
        add(titlePanel,BorderLayout.NORTH);
        JPanel readoutPanel=new JPanel(new GridLayout(1,4,12,0));
        readoutPanel.setOpaque(false);
        readoutPanel.add(createReadout("Score",scoreValueLabel));
        readoutPanel.add(createReadout("Best",bestValueLabel));
        readoutPanel.add(createReadout("Moves",movesValueLabel));
        readoutPanel.add(createReadout("Status",statusValueLabel));
        add(readoutPanel,BorderLayout.CENTER);
        scoreTimer=new javax.swing.Timer(TICK_INTERVAL_MILLISECONDS,new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                tickScoreAnimation();
            }
        });
        scoreTimer.setRepeats(true);
    }
    private JPanel createReadout(String caption,JLabel valueLabel){
        JPanel readout=new JPanel(new BorderLayout());
        readout.setOpaque(false);
        readout.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        JLabel captionLabel=new JLabel(caption);
        captionLabel.setFont(Theme.CAPTION_FONT);
        captionLabel.setForeground(Theme.ACCENT_COLOR);
        valueLabel.setFont(Theme.VALUE_FONT);
        valueLabel.setForeground(Theme.HEADING_COLOR);
        readout.add(captionLabel,BorderLayout.NORTH);
        readout.add(valueLabel,BorderLayout.CENTER);
        return readout;
    }
    /**
     * Sets the numbers shown in the header. The score eases to its new value;
     * the other two appear at once.
     * @param score the current score
     * @param bestScore the highest score seen in this session
     * @param moveCount the number of moves played
     */
    public void updateScore(int score,int bestScore,int moveCount){
        bestValueLabel.setText(Integer.toString(bestScore));
        movesValueLabel.setText(Integer.toString(moveCount));
        if(score!=targetScore){
            startingScore=displayedScore;
            targetScore=score;
            animationStartNanos=System.nanoTime();
            if(!scoreTimer.isRunning()){
                scoreTimer.start();
            }
        }
    }
    /**
     * Sets the single status word that describes what the game is doing.
     * @param status the status text to show
     */
    public void setStatus(String status){
        statusValueLabel.setText(status);
    }
    private void tickScoreAnimation(){
        long elapsed=System.nanoTime()-animationStartNanos;
        if(elapsed>=SCORE_ANIMATION_NANOS){
            displayedScore=targetScore;
            scoreValueLabel.setText(Integer.toString(displayedScore));
            scoreTimer.stop();
            return;
        }
        float progress=Easing.easeOutCubic((float)((double)elapsed/(double)SCORE_ANIMATION_NANOS));
        displayedScore=startingScore+(int)((targetScore-startingScore)*progress);
        scoreValueLabel.setText(Integer.toString(displayedScore));
    }

}
