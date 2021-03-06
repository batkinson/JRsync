package com.github.batkinson.jrsync;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static com.github.batkinson.jrsync.TestUtils.computeBlocks;
import static com.github.batkinson.jrsync.TestUtils.randomAccess;
import static com.github.batkinson.jrsync.TestUtils.testFile;
import static com.github.batkinson.jrsync.TestUtils.toHex;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BlockDescTest {

    private static final Logger log = LoggerFactory.getLogger(BlockDescTest.class);

    private static final String MD5 = "MD5";

    private RandomAccessFile input;

    @Before
    public void setup() throws URISyntaxException, FileNotFoundException {
        input = randomAccess(testFile("file1.txt"));
    }

    @After
    public void teardown() throws IOException {
        input.close();
        input = null;
    }

    @Test
    public void describe() throws IOException, NoSuchAlgorithmException {
        long fileLength = input.length();
        for (int blockSize = 1; blockSize <= fileLength; blockSize++) {
            int readBytes = 0;
            RollingChecksum checksum = new RollingChecksum(blockSize);
            MessageDigest digest = MessageDigest.getInstance(MD5);
            byte[] block = new byte[blockSize];
            List<BlockDesc> descs = computeBlocks(input, blockSize, MD5);
            for (BlockDesc d : descs) {
                input.seek(d.blockIndex * blockSize);
                input.readFully(block);
                readBytes += blockSize;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("'%s' %d: %s %s%n", new String(block), d.blockIndex, d.weakChecksum, toHex(d.cryptoHash)));
                }
                checksum.reset();
                checksum.update(block);
                assertEquals(checksum.getValue(), d.weakChecksum);
                assertArrayEquals(digest.digest(block), d.cryptoHash);
            }
            assertEquals(readBytes, descs.size() * blockSize);
            assertEquals(fileLength % blockSize, fileLength - readBytes);
        }
    }

    @Test
    public void getters() {
        byte[] bs = {1, 2, 3};
        BlockDesc bd = new BlockDesc(1, 2, bs);
        assertEquals(1, bd.getBlockIndex());
        assertEquals(2, bd.getWeakChecksum());
        assertArrayEquals(bs, bd.getCryptoHash());
    }
}
