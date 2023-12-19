package test.kotlin.c64.disk;

import c64.disk.BinaryDiskReader;
import junit.framework.TestCase;

import java.io.FileInputStream;

/**
 * Tests binary conversion methods.
 *
 * @author Daniel Schulte 2001-2018
 */
public class TestBinaryOps extends TestCase
{

    public TestBinaryOps(String method)
    {
        super(method);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testByteConversion()
    {
        try
        {
            FileInputStream fis = new FileInputStream("test-src/testdisk/byteconv.bin");
            assertNotNull(fis);
            byte[] buffer = new byte[1024];
            short[] convertedBuffer = new short[1024];
            int len = fis.read(buffer);

            for (int i = 0; i < len; i++)
            {
                convertedBuffer[i] = BinaryDiskReader.Companion.convertByte(buffer[i]);
            }

            System.out.println("--> testing byte conversion");
            assertEquals(0, convertedBuffer[0]);
            assertEquals(16, convertedBuffer[1]);
            assertEquals(32, convertedBuffer[2]);
            assertEquals(48, convertedBuffer[3]);
            assertEquals(64, convertedBuffer[4]);
            assertEquals(80, convertedBuffer[5]);
            assertEquals(96, convertedBuffer[6]);
            assertEquals(112, convertedBuffer[7]);
            assertEquals(128, convertedBuffer[8]);
            assertEquals(144, convertedBuffer[9]);
            assertEquals(160, convertedBuffer[10]);
            assertEquals(176, convertedBuffer[11]);
            assertEquals(192, convertedBuffer[12]);
            assertEquals(208, convertedBuffer[13]);
            assertEquals(224, convertedBuffer[14]);
            assertEquals(240, convertedBuffer[15]);
            assertEquals(255, convertedBuffer[16]);
            System.out.println("byte conversion test done <--\n");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            fail(ex.toString());
        }
    }
}
