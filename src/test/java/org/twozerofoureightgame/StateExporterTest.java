package org.twozerofoureightgame;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.twozerofoureightgame.io.StateExporter;
import org.twozerofoureightgame.io.StateImporter;
import org.twozerofoureightgame.model.GameState;
/**
 * Reads like the specification of a save file. A written file must contain the
 * version, the score, the moves, both flags, and sixteen tile values. Reading it
 * back must give exactly the snapshot that went out. A file with the wrong
 * version, the wrong number of tiles, or a value that is not a power of two must
 * be refused rather than half loaded.
 */
class StateExporterTest{
    @TempDir
    File temporaryFolder;
    private static GameState sampleState(){
        byte[] cells=new byte[16];
        int[] tileValues={2,0,4,0, 0,0,0,8, 16,0,0,0, 0,0,0,0};
        for(int index=0;index<16;index++){
            if(tileValues[index]!=0){
                cells[index]=(byte)Integer.numberOfTrailingZeros(tileValues[index]);
            }
        }
        return new GameState(cells,24680,137,true,false);
    }
    @Test
    void aWrittenFileHoldsEveryLineTheFormatPromises()throws IOException{
        File file=new File(temporaryFolder,"save.txt");
        StateExporter.export(sampleState(),file);
        java.util.List<String> lines=Files.readAllLines(file.toPath(),StandardCharsets.UTF_8);
        assertEquals(6,lines.size());
        assertEquals("version=1",lines.get(0));
        assertEquals("score=24680",lines.get(1));
        assertEquals("moves=137",lines.get(2));
        assertEquals("won=true",lines.get(3));
        assertEquals("lost=false",lines.get(4));
        assertTrue(lines.get(5).startsWith("board=2,0,4,0,"));
        assertEquals(16,lines.get(5).substring("board=".length()).split(",").length);
    }
    @Test
    void aWrittenFileComesBackExactlyAsItWentOut()throws IOException{
        GameState original=sampleState();
        File file=new File(temporaryFolder,"round-trip.txt");
        StateExporter.export(original,file);
        GameState restored=StateImporter.importState(file);
        assertArrayEquals(original.cells,restored.cells);
        assertEquals(original.score,restored.score);
        assertEquals(original.moveCount,restored.moveCount);
        assertEquals(original.won,restored.won);
        assertEquals(original.lost,restored.lost);
    }
    @Test
    void aFileFromAFutureVersionIsRefused()throws IOException{
        File file=new File(temporaryFolder,"future.txt");
        Files.write(file.toPath(),java.util.Arrays.asList("version=99","score=0","moves=0","won=false","lost=false","board=0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0"),StandardCharsets.UTF_8);
        assertThrows(IOException.class,()->StateImporter.importState(file));
    }
    @Test
    void aFileWithTooFewTilesIsRefused()throws IOException{
        File file=new File(temporaryFolder,"short.txt");
        Files.write(file.toPath(),java.util.Arrays.asList("version=1","score=0","moves=0","won=false","lost=false","board=2,0,4"),StandardCharsets.UTF_8);
        assertThrows(IOException.class,()->StateImporter.importState(file));
    }
    @Test
    void aFileWithAValueThatIsNotAPowerOfTwoIsRefused()throws IOException{
        File file=new File(temporaryFolder,"odd.txt");
        Files.write(file.toPath(),java.util.Arrays.asList("version=1","score=0","moves=0","won=false","lost=false","board=2,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0"),StandardCharsets.UTF_8);
        assertThrows(IOException.class,()->StateImporter.importState(file));
    }
    @Test
    void aFileWithNoBoardLineAtAllIsRefused()throws IOException{
        File file=new File(temporaryFolder,"empty.txt");
        Files.write(file.toPath(),java.util.Arrays.asList("version=1","score=0","moves=0","won=false","lost=false"),StandardCharsets.UTF_8);
        assertThrows(IOException.class,()->StateImporter.importState(file));
    }
}
