package org.twozerofoureightgame.io;
import java.util.List;
import org.twozerofoureightgame.model.GameState;
/**
 * Reads back a file written by {@link StateExporter} and turns it into a
 * snapshot. Every value is checked before it is trusted: the version number must
 * match, the board line must hold exactly sixteen entries, and each entry must
 * be zero or a power of two that fits in a cell. Anything else raises an
 * exception so that the controller can show the player a single clear message
 * instead of loading a broken board.
 */
public final class StateImporter{
    /**
     * This class only exposes static helpers and is never instantiated.
     */
    private StateImporter(){
    }
    /**
     * Reads one snapshot from disk.
     * @param file the file to read
     * @return the snapshot the file describes
     * @throws java.io.IOException when the file is missing, unreadable, or malformed
     */
    public static GameState importState(java.io.File file)throws java.io.IOException{
        List<String> lines=java.nio.file.Files.readAllLines(file.toPath(),java.nio.charset.StandardCharsets.UTF_8);
        int version=-1;
        int score=0;
        int moveCount=0;
        boolean won=false;
        boolean lost=false;
        int[] tileValues=null;
        for(int index=0;index<lines.size();index++){
            String line=lines.get(index).trim();
            if(line.startsWith("version=")){
                version=parseInt(line.substring("version=".length()));
            }else if(line.startsWith("score=")){
                score=parseInt(line.substring("score=".length()));
            }else if(line.startsWith("moves=")){
                moveCount=parseInt(line.substring("moves=".length()));
            }else if(line.startsWith("won=")){
                won=parseBoolean(line.substring("won=".length()));
            }else if(line.startsWith("lost=")){
                lost=parseBoolean(line.substring("lost=".length()));
            }else if(line.startsWith("board=")){
                tileValues=parseBoard(line.substring("board=".length()));
            }
        }
        if(version!=StateExporter.FORMAT_VERSION){
            throw new java.io.IOException("Unsupported save file version.");
        }
        if(tileValues==null||tileValues.length!=16){
            throw new java.io.IOException("Invalid save file.");
        }
        byte[] cells=new byte[16];
        for(int index=0;index<16;index++){
            if(tileValues[index]==0){
                cells[index]=0;
                continue;
            }
            int exponent=0;
            while(exponent<16&&(1<<exponent)!=tileValues[index]){
                exponent++;
            }
            if(exponent==16){
                throw new java.io.IOException("Invalid save file.");
            }
            cells[index]=(byte)exponent;
        }
        return new GameState(cells,score,moveCount,won,lost);
    }
    private static int parseInt(String text)throws java.io.IOException{
        try{
            return Integer.parseInt(text.trim());
        }catch(NumberFormatException exception){
            throw new java.io.IOException("Invalid save file.");
        }
    }
    private static boolean parseBoolean(String text)throws java.io.IOException{
        String trimmed=text.trim();
        if(trimmed.equals("true")){
            return true;
        }
        if(trimmed.equals("false")){
            return false;
        }
        throw new java.io.IOException("Invalid save file.");
    }
    private static int[] parseBoard(String text)throws java.io.IOException{
        String[] pieces=text.split(",");
        if(pieces.length!=16){
            throw new java.io.IOException("Invalid save file.");
        }
        int[] tileValues=new int[16];
        for(int index=0;index<16;index++){
            tileValues[index]=parseInt(pieces[index]);
        }
        return tileValues;
    }
}
