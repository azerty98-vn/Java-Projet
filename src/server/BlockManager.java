package server;
/* code complet pour BlockManager ici */
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * BlockManager handles reading specific blocks from a file.
 */
public class BlockManager {

    private final File file;
    private final int blockSize;
    private final int totalBlocks;

    public BlockManager(File file, int blockSize) {
        this.file = file;
        this.blockSize = blockSize;
        this.totalBlocks = (int) Math.ceil((double) file.length() / blockSize);
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public byte[] readBlock(int blockId) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long start = (long) blockId * blockSize;
            raf.seek(start);
            int size = (int) Math.min(blockSize, file.length() - start);
            byte[] buffer = new byte[size];
            raf.readFully(buffer);
            return buffer;
        }
    }
}
