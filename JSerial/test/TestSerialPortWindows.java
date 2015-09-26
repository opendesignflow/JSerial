import dk.thibaut.serial.SerialConfig;
import dk.thibaut.serial.SerialException;
import dk.thibaut.serial.SerialPort;

import dk.thibaut.serial.enums.BaudRate;
import dk.thibaut.serial.enums.DataBits;
import dk.thibaut.serial.enums.Parity;
import dk.thibaut.serial.enums.StopBits;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/*
 * Because JSerial rely a lot on the native implementations, I prefer
 * to have real unit tests, with native API calls. For this to work
 * you need to have a virtual port utility installed on you system.
 *
 * I recommend the free (but limited) Free Virtual Serial Port by HDD
 * Software (http://freevirtualserialports.com/).
 *
 * Once your software is configured, configure both PORT_READ and PORT_WRITE
 * constants to virtual port names. Test will write data on PORT_WRITE
 * and read it from PORT_READ to test different functions and errors.
 */

public class TestSerialPortWindows {

    private static final String PORT_READ = "COM2";
    private static final String PORT_WRITE = "COM4";
    private static final SerialConfig DEFAULT_CONFIG = new SerialConfig(
        BaudRate.B115200, Parity.NONE, StopBits.ONE, DataBits.D8);

    private SerialPort portRead;
    private SerialPort portWrite;

    @Before
    public void setUp() throws IOException {
        portRead = SerialPort.open(PORT_READ);
        portRead.setConfig(DEFAULT_CONFIG);
        portRead.setTimeout(SerialPort.TIMEOUT_INFINITE);
        portWrite = SerialPort.open(PORT_WRITE);
        portWrite.setConfig(DEFAULT_CONFIG);
    }

    @After
    public void tearDown() throws IOException {
        portRead.close();
        portWrite.close();
    }

    @Test(expected = SerialException.class)
    public void testOpenFails() throws IOException {
        SerialPort.open("COM254");
    }

    @Test
    public void testGetSetConfig() throws IOException {
        /* Sets some strange configuration on the port. */
        SerialConfig config = new SerialConfig(BaudRate.B256000,
            Parity.EVEN, StopBits.ONE_HALF, DataBits.D7);
        portRead.setConfig(config);
        portRead.close();
        /* Re-opens the port, and check that this is the current
         * configuration, which means it was applied correctly. */
        portRead = SerialPort.open(PORT_READ);
        config = portRead.getConfig();
        assertEquals(config.BaudRate, BaudRate.B256000);
        assertEquals(config.Parity, Parity.EVEN);
        assertEquals(config.StopBits, StopBits.ONE_HALF);
        assertEquals(config.DataBits, DataBits.D7);
    }

    @Test
    public void testReadWrite() throws IOException {
        ByteBuffer toWrite = ByteBuffer.allocateDirect(50);
        for (byte b = 0; b < toWrite.capacity(); b++)
            toWrite.put(b);
        toWrite.clear();
        portWrite.getChannel().write(toWrite);
        ByteBuffer toRead = ByteBuffer.allocateDirect(50);
        portRead.getChannel().read(toRead);
        assertEquals(toRead.get(0), 0);
        assertEquals(toRead.get(25), 25);
        assertEquals(toRead.get(49), 49);
    }

    @Test
    public void testTimeoutValue() throws IOException, InterruptedException {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(5);
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    portRead.getChannel().read(buffer);
                } catch (IOException err) {
                    err.printStackTrace();
                }
            }
        });
        portRead.setTimeout(110);
        thread.start();
        thread.join(100);
        assertTrue(thread.isAlive());
        thread.join(20);
        assertFalse(thread.isAlive());
    }

}