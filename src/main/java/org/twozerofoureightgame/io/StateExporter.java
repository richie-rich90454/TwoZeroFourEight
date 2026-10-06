package org.twozerofoureightgame.io;
import java.util.ArrayList;
import java.util.List;
import org.twozerofoureightgame.model.GameState;
/**
 * Writes a board to a small plain text file that a person can read and edit.
 * Each line is a name followed by an equals sign and a value. The tile values
 * are written out as the numbers a player sees, such as 2 and 2048, rather than
 * as exponents, so that a saved file makes sense to somebody who has never seen
 * the source code.
 */
public final class StateExporter{
    /** The version number written into every file and checked on the way back in. */
    public static final int FORMAT_VERSION=1;
    /**
     * This class only exposes static helpers and is never instantiated.
     */
    private StateExporter(){
    }
    /**
     * Writes one snapshot to disk.
     * @param state the snapshot to write
     * @param file the destination file
     * @throws java.io.IOException when the file cannot be written
     */
    public static void export(GameState state,java.io.File file)throws java.io.IOException{
        List<String> lines=new ArrayList<String>();
        lines.add("version="+FORMAT_VERSION);
        lines.add("score="+state.score);
        lines.add("moves="+state.moveCount);
        lines.add("won="+state.won);
        lines.add("lost="+state.lost);
        StringBuilder boardLine=new StringBuilder("board=");
        for(int index=0;index<16;index++){
            if(index>0){
                boardLine.append(',');
            }
            boardLine.append(state.cells[index]==0?0:1<<state.cells[index]);
        }
        lines.add(boardLine.toString());
        java.nio.file.Files.write(file.toPath(),lines,java.nio.charset.StandardCharsets.UTF_8);
    }
}
