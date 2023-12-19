package test.kotlin.c64.disk;

import c64.disk.*;
import junit.framework.TestCase;

/**
 * Tests for disk 'pool of radiance side 1'
 *
 * @author Daniel Schulte 2001-2018
 */
public class TestDisk_POR extends TestCase
{

    public TestDisk_POR(String method)
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

    public void testDiskInit()
    {
        try
        {
            IDisk disk = DiskFactory.INSTANCE.getDisk("test-src/testdisk/pool1.d64");
            assertNotNull(disk);
            assertEquals(D64Disk.class.getName(), disk.getClass().getName());

            int count = 0;
            FileChain fileChain = disk.getFileChain(18, 0);
            while (fileChain.hasNextSector() && count < 19)
            {
                final DiskSector diskSector = fileChain.nextSector();
                assertEquals(18, diskSector.getTrackSector().getTrack());
                // count number of sectors on track 18 --> not more than 19 sectors allowed!
                count++;
            }

            assertEquals(disk.getDirectory().getDiskName(), "DISK A SIDE 1");
            assertEquals(disk.getDirectory().getDiskID(), "S1 ");
            assertEquals(disk.getDirectory().getDosType(), "2A");

            count = 0;
            DirEntryChain dirEntryChain = disk.getDirectory().getDirEntryChain();
            while (dirEntryChain.hasNextDirEntry())
            {
                System.out.println(dirEntryChain.nextDirEntry().toString());
                count++;
            }

            assertEquals(100, count);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            fail(ex.toString());
        }
    }
}
