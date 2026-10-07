package org.twozerofoureightgame.controller;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.twozerofoureightgame.ai.ExpectimaxAI;
import org.twozerofoureightgame.ai.RowMoveTable;
import org.twozerofoureightgame.model.Board;
import org.twozerofoureightgame.model.Direction;
import org.twozerofoureightgame.model.GameState;
import org.twozerofoureightgame.model.MoveResult;
import org.twozerofoureightgame.io.ScreenshotUtility;
import org.twozerofoureightgame.io.StateExporter;
import org.twozerofoureightgame.io.StateImporter;
import org.twozerofoureightgame.utility.AnimationManager;
import org.twozerofoureightgame.view.GameFrame;
/**
 * The one place that decides anything. It owns the board, the score, the undo
 * snapshot, the artificial intelligence, and the window, and it is the only
 * class that is allowed to change a game in progress.
 * Three modes share one live board. A human plays every move. A human presses
 * Auto Play and the artificial intelligence takes over, always reading the board
 * as it stands at that moment, which is what makes handing over mid game
 * instant. A human asks for a hint and gets advice without the board changing.
 * A direction typed while the artificial intelligence is thinking is held in a
 * single slot and played as soon as autoplay is switched off, and a direction
 * typed while the board is animating is dropped, because the board is already
 * committed to a move.
 * After every move the controller checks for the win and for the real game over.
 * There is no third ending. A game is never abandoned because a search was
 * unsure: the only two ways out are a 2048 tile and a board with no legal move.
 */
public final class GameController implements AnimationManager.AnimationCompletionListener{
    /** How long the artificial intelligence may think about one move. */
    public static final int AI_TIME_BUDGET_MILLISECONDS=100;
    /** The shortest pause between two autoplay moves. */
    public static final int AUTOPLAY_MINIMUM_DELAY_MILLISECONDS=100;
    /** The longest pause between two autoplay moves. */
    public static final int AUTOPLAY_MAXIMUM_DELAY_MILLISECONDS=1000;
    /** The exponent of the tile that wins the game. */
    public static final int WINNING_EXPONENT=11;
    /** How long a step that finds the board busy waits before looking again. */
    private static final int AUTOPLAY_RECHECK_MILLISECONDS=25;
    private final Board board=new Board();
    private final ExpectimaxAI expectimaxAI=new ExpectimaxAI();
    private final GameFrame gameFrame;
    private final javax.swing.Timer autoplayTimer;
    private GameState previousState=null;
    private int score=0;
    private int moveCount=0;
    private int bestScore=0;
    private int boardGeneration=0;
    private boolean won=false;
    private boolean lost=false;
    private boolean autoPlayEnabled=false;
    private boolean autoPlayResumeOnContinue=false;
    private boolean paused=false;
    private boolean overlayVisible=false;
    private boolean aiThinking=false;
    private Direction queuedDirection=null;
    private Direction pendingAutoDirection=null;
    private SwingWorker<Direction,Void> activeAIWorker=null;
    private int autoPlayDelayMilliseconds=250;
    /**
     * Builds the window and starts the first game.
     */
    public GameController(){
        RowMoveTable.prepare();
        autoplayTimer=new javax.swing.Timer(autoPlayDelayMilliseconds,new java.awt.event.ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent event){
                runAutoPlayStep();
            }
        });
        autoplayTimer.setRepeats(false);
        gameFrame=new GameFrame(this);
        startNewGame();
    }
    /**
     * Accepts a direction from the keyboard, a swipe, or the autoplay timer.
     * @param direction the direction that was asked for
     */
    public void handleDirectionInput(Direction direction){
        if(direction==null||paused||lost||overlayVisible){
            return;
        }
        if(isBoardAnimating()){
            return;
        }
        if(autoPlayEnabled||aiThinking){
            queuedDirection=direction;
            return;
        }
        performMove(direction);
    }
    private void performMove(Direction direction){
        if(!board.canMove(direction)){
            return;
        }
        previousState=new GameState(board.snapshotCells(),score,moveCount,won,lost);
        MoveResult result=board.applyMoveWithTransitions(direction);
        score+=result.scoreGained;
        moveCount++;
        boardGeneration++;
        queuedDirection=null;
        if(score>bestScore){
            bestScore=score;
        }
        gameFrame.getHeaderPanel().updateScore(score,bestScore,moveCount);
        gameFrame.getBoardPanel().applyMoveResult(result);
        checkEndOfGame();
    }
    private void checkEndOfGame(){
        if(!won&&board.getMaximumExponentOnBoard()>=WINNING_EXPONENT){
            won=true;
            captureVictoryScreenshot();
            showOverlay("You win!",true);
            return;
        }
        if(board.isGameOver()){
            lost=true;
            showOverlay("Game over",false);
            return;
        }
    }
    /**
     * Saves a picture of the winning board by itself, before the end of game
     * overlay is put on screen. The picture is written silently, because the
     * overlay is about to cover the window and the player should not have to
     * dismiss a dialog to see where the file went.
     * @return the absolute path of the picture, or null when none could be written
     */
    private String captureVictoryScreenshot(){
        try{
            return ScreenshotUtility.capture(gameFrame);
        }catch(IOException exception){
            return null;
        }
    }
    private boolean isBoardAnimating(){
        return gameFrame.getBoardPanel().getAnimationManager().hasActiveAnimations();
    }
    /**
     * Puts the board back the way it was before the last move and forgets that
     * move, so the undo can only ever step back one move.
     */
    public void undoLastMove(){
        if(previousState==null){
            return;
        }
        cancelActiveAIWorker();
        board.restoreFrom(previousState.cells);
        score=previousState.score;
        moveCount=previousState.moveCount;
        won=previousState.won;
        lost=previousState.lost;
        previousState=null;
        queuedDirection=null;
        boardGeneration++;
        hideOverlay();
        expectimaxAI.resetTranspositionTable();
        gameFrame.getBoardPanel().rebuildFromBoard(board);
        refreshHeader();
        JOptionPane.showMessageDialog(gameFrame,"Move undone.");
    }
    /**
     * Throws the current game away and deals a fresh board with two tiles.
     */
    public void startNewGame(){
        autoplayTimer.stop();
        cancelActiveAIWorker();
        autoPlayEnabled=false;
        autoPlayResumeOnContinue=false;
        updateAnimationScale();
        paused=false;
        won=false;
        lost=false;
        score=0;
        moveCount=0;
        previousState=null;
        queuedDirection=null;
        boardGeneration++;
        board.clearBoard();
        board.spawnRandomTile();
        board.spawnRandomTile();
        expectimaxAI.resetTranspositionTable();
        hideOverlay();
        gameFrame.getBoardPanel().rebuildFromBoard(board);
        refreshHeader();
    }
    /**
     * Asks the artificial intelligence what the best move would be and reports
     * it without touching the board.
     */
    public void requestHint(){
        if(paused||lost||overlayVisible||aiThinking){
            return;
        }
        if(isBoardAnimating()){
            return;
        }
        aiThinking=true;
        refreshStatus();
        final byte[] cells=board.snapshotCells();
        activeAIWorker=new SwingWorker<Direction,Void>(){
            @Override
            protected Direction doInBackground(){
                return expectimaxAI.chooseBestMove(cells,AI_TIME_BUDGET_MILLISECONDS);
            }
            @Override
            protected void done(){
                aiThinking=false;
                refreshStatus();
                try{
                    Direction direction=get();
                    if(direction!=null){
                        JOptionPane.showMessageDialog(gameFrame,"Hint: move "+direction.name()+".");
                    }
                }catch(Exception exception){
                    refreshStatus();
                }
            }
        };
        activeAIWorker.execute();
    }
    /**
     * Switches autoplay on when it is off and off when it is on. Switching it off
     * hands the board straight back to the human, playing any direction that was
     * typed while the artificial intelligence was busy.
     */
    public void toggleAutoPlay(){
        if(overlayVisible||lost){
            return;
        }
        if(autoPlayEnabled){
            autoPlayEnabled=false;
            autoplayTimer.stop();
            cancelActiveAIWorker();
            updateAnimationScale();
            refreshStatus();
            if(queuedDirection!=null&&!isBoardAnimating()&&!paused){
                Direction direction=queuedDirection;
                queuedDirection=null;
                performMove(direction);
            }
            return;
        }
        autoPlayEnabled=true;
        pendingAutoDirection=null;
        updateAnimationScale();
        refreshStatus();
        if(!isBoardAnimating()&&!paused){
            scheduleAutoPlayStep();
        }
    }
    /**
     * Switches the pause on and off. Pausing stops autoplay and refuses every
     * direction, but never interrupts an animation that is already running.
     */
    public void togglePause(){
        if(overlayVisible||lost){
            return;
        }
        paused=!paused;
        if(paused){
            autoplayTimer.stop();
        }else if(autoPlayEnabled&&!isBoardAnimating()){
            scheduleAutoPlayStep();
        }
        refreshStatus();
    }
    /**
     * Schedules the next autoplay move so that the slider value is the true gap
     * between moves: the search always spends its full think budget after the
     * timer fires, so the timer waits for the slider value minus that budget.
     * Both delays are set because a restarted timer fires after its initial
     * delay, not its delay.
     */
    private void scheduleAutoPlayStep(){
        int gap=autoPlayDelayMilliseconds-AI_TIME_BUDGET_MILLISECONDS;
        if(gap<0){
            gap=0;
        }
        autoplayTimer.setInitialDelay(gap);
        autoplayTimer.setDelay(gap);
        autoplayTimer.restart();
    }
    /**
     * Looks again shortly when a step fires while the board is still moving or
     * the search is still running. This keeps the event thread from spinning on
     * a zero delay timer while it waits.
     */
    private void recheckAutoPlaySoon(){
        autoplayTimer.setInitialDelay(AUTOPLAY_RECHECK_MILLISECONDS);
        autoplayTimer.setDelay(AUTOPLAY_RECHECK_MILLISECONDS);
        autoplayTimer.restart();
    }
    /**
     * Returns the pause between two autoplay moves, which the speed slider sets.
     * @return the delay in milliseconds
     */
    public int getAutoPlayDelayMilliseconds(){
        return autoPlayDelayMilliseconds;
    }
    /**
     * Sets the pause between two autoplay moves. Values outside the slider range
     * are clamped, so the slider can never break the pacing.
     * @param delayMilliseconds the delay in milliseconds
     */
    public void setAutoPlayDelayMilliseconds(int delayMilliseconds){
        if(delayMilliseconds<AUTOPLAY_MINIMUM_DELAY_MILLISECONDS){
            delayMilliseconds=AUTOPLAY_MINIMUM_DELAY_MILLISECONDS;
        }
        if(delayMilliseconds>AUTOPLAY_MAXIMUM_DELAY_MILLISECONDS){
            delayMilliseconds=AUTOPLAY_MAXIMUM_DELAY_MILLISECONDS;
        }
        autoPlayDelayMilliseconds=delayMilliseconds;
        updateAnimationScale();
    }
    /**
     * Matches the animation pace to the autoplay state: while autoplay drives,
     * faster slider settings also shorten the slides so a 100 ms gap is truly
     * reachable; otherwise every move plays at full length.
     */
    private void updateAnimationScale(){
        double scale=1.0;
        if(autoPlayEnabled){
            scale=0.3+0.7*(autoPlayDelayMilliseconds-AUTOPLAY_MINIMUM_DELAY_MILLISECONDS)/(AUTOPLAY_MAXIMUM_DELAY_MILLISECONDS-AUTOPLAY_MINIMUM_DELAY_MILLISECONDS);
        }
        gameFrame.getBoardPanel().getAnimationManager().setAnimationScale(scale);
    }
    /**
     * Throws away any answer the artificial intelligence is still working on and
     * forgets any move already computed but not yet played, so that a decision
     * made about an old board can never be played on a new one.
     */
    private void cancelActiveAIWorker(){
        if(activeAIWorker!=null&&!activeAIWorker.isDone()){
            activeAIWorker.cancel(true);
        }
        activeAIWorker=null;
        pendingAutoDirection=null;
    }
    private void runAutoPlayStep(){
        if(!autoPlayEnabled||paused||overlayVisible||lost){
            return;
        }
        if(isBoardAnimating()||aiThinking){
            recheckAutoPlaySoon();
            return;
        }
        if(pendingAutoDirection!=null){
            Direction direction=pendingAutoDirection;
            pendingAutoDirection=null;
            performMove(direction);
            if(autoPlayEnabled&&!paused&&!overlayVisible&&!lost){
                launchAutoPlayWorker();
            }
            return;
        }
        launchAutoPlayWorker();
    }
    /**
     * Starts the search for the next autoplay move. The board model already holds
     * the finished move while its animation is still playing, so calling this
     * right after a move lets the search think during the animation instead of
     * after it. Only one search may run at a time.
     */
    private void launchAutoPlayWorker(){
        aiThinking=true;
        refreshStatus();
        final byte[] cells=board.snapshotCells();
        activeAIWorker=new SwingWorker<Direction,Void>(){
            @Override
            protected Direction doInBackground(){
                return expectimaxAI.chooseBestMove(cells,AI_TIME_BUDGET_MILLISECONDS);
            }
            @Override
            protected void done(){
                aiThinking=false;
                refreshStatus();
                try{
                    Direction direction=get();
                    if(direction!=null&&autoPlayEnabled&&!paused&&!overlayVisible&&!lost){
                        pendingAutoDirection=direction;
                        if(!isBoardAnimating()){
                            scheduleAutoPlayStep();
                        }
                    }
                }catch(Exception exception){
                    refreshStatus();
                }
            }
        };
        activeAIWorker.execute();
    }
    /**
     * Called by the animation manager once the board has stopped moving. A held
     * direction is played first; otherwise autoplay schedules its next move.
     */
    @Override
    public void onAllAnimationsFinished(){
        if(queuedDirection!=null&&!autoPlayEnabled&&!aiThinking&&!paused&&!overlayVisible&&!lost){
            Direction direction=queuedDirection;
            queuedDirection=null;
            performMove(direction);
            return;
        }
        if(autoPlayEnabled&&!paused&&!overlayVisible&&!lost){
            scheduleAutoPlayStep();
        }
    }
    /**
     * Handles the New Game button on the end of game overlay.
     */
    public void onOverlayNewGame(){
        startNewGame();
    }
    /**
     * Handles the Continue button on the win overlay, which hides the overlay and
     * lets the game carry on past 2048.
     */
    public void onOverlayContinue(){
        hideOverlay();
        if(autoPlayResumeOnContinue){
            autoPlayEnabled=true;
            scheduleAutoPlayStep();
        }
        refreshStatus();
    }
    private void showOverlay(String message,boolean allowContinue){
        overlayVisible=true;
        autoplayTimer.stop();
        autoPlayResumeOnContinue=autoPlayEnabled;
        autoPlayEnabled=false;
        pendingAutoDirection=null;
        gameFrame.getOverlayPanel().showMessage(message,allowContinue);
        gameFrame.showGlassPane();
        refreshStatus();
    }
    private void hideOverlay(){
        if(!overlayVisible&&!gameFrame.getOverlayPanel().hasMessage()){
            return;
        }
        overlayVisible=false;
        gameFrame.getOverlayPanel().hideOverlay();
        gameFrame.hideGlassPane();
    }
    /**
     * Writes the game to a text file chosen by the player.
     */
    public void exportGame(){
        JFileChooser chooser=new JFileChooser();
        chooser.setSelectedFile(new File("2048-save.txt"));
        if(chooser.showSaveDialog(gameFrame)!=JFileChooser.APPROVE_OPTION){
            return;
        }
        File file=chooser.getSelectedFile();
        if(!file.getName().endsWith(".txt")){
            file=new File(file.getAbsolutePath()+".txt");
        }
        try{
            StateExporter.export(new GameState(board.snapshotCells(),score,moveCount,won,lost),file);
            JOptionPane.showMessageDialog(gameFrame,"Game exported to "+file.getAbsolutePath()+".");
        }catch(IOException exception){
            JOptionPane.showMessageDialog(gameFrame,"Invalid save file.");
        }
    }
    /**
     * Reads a game from a text file chosen by the player.
     */
    public void importGame(){
        JFileChooser chooser=new JFileChooser();
        if(chooser.showOpenDialog(gameFrame)!=JFileChooser.APPROVE_OPTION){
            return;
        }
        File file=chooser.getSelectedFile();
        try{
            GameState state=StateImporter.importState(file);
            applyImportedState(state);
            JOptionPane.showMessageDialog(gameFrame,"Game imported from "+file.getAbsolutePath()+".");
        }catch(IOException exception){
            JOptionPane.showMessageDialog(gameFrame,"Invalid save file.");
        }
    }
    private void applyImportedState(GameState state){
        autoplayTimer.stop();
        cancelActiveAIWorker();
        autoPlayEnabled=false;
        paused=false;
        updateAnimationScale();
        board.restoreFrom(state.cells);
        score=state.score;
        moveCount=state.moveCount;
        won=state.won;
        lost=state.lost;
        previousState=null;
        queuedDirection=null;
        boardGeneration++;
        if(score>bestScore){
            bestScore=score;
        }
        hideOverlay();
        expectimaxAI.resetTranspositionTable();
        gameFrame.getBoardPanel().rebuildFromBoard(board);
        refreshHeader();
        if(board.isGameOver()){
            lost=true;
            showOverlay("Game over",false);
        }
    }
    /**
     * Saves a picture of the board as a PNG file.
     */
    public void takeScreenshot(){
        try{
            String path=ScreenshotUtility.capture(gameFrame);
            JOptionPane.showMessageDialog(gameFrame,"Screenshot saved to "+path+".");
        }catch(IOException exception){
            JOptionPane.showMessageDialog(gameFrame,"Invalid save file.");
        }
    }
    private void refreshHeader(){
        gameFrame.getHeaderPanel().updateScore(score,bestScore,moveCount);
        refreshStatus();
    }
    private void refreshStatus(){
        String status="Ready";
        if(lost){
            status="Game over";
        }else if(won&&overlayVisible){
            status="You win!";
        }else if(paused){
            status="Paused";
        }else if(aiThinking){
            status="Thinking…";
        }else if(autoPlayEnabled){
            status="Auto-playing";
        }
        gameFrame.getHeaderPanel().setStatus(status);
        gameFrame.getControlPanel().reflectModes(autoPlayEnabled,paused);
    }
}
