package xyz.doejack.filenet;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.util.zip.InflaterInputStream;

public class GCDConfigImpl implements GCDConfig {
    /**
     * This class represents IBM FileNet(c) GCD Configuration and may convert it from binary to old
     * xml style
     *
     * @author mod3x@yandex.ru
     */
    private FileInputStream fileInputStream;
    private FileOutputStream fileOutputStream;
    private SequenceInputStream sequenceInputStream;
    private InflaterInputStream inflaterInputStream;
    final byte[] array = new byte[16];

    GCDConfigImpl(String name) throws IOException {
        try {
            this.fileInputStream = new FileInputStream(name);
            this.fileOutputStream = new FileOutputStream(name + ".gcd.out");
            fileInputStream.read(array);
            this.sequenceInputStream = new SequenceInputStream(new ByteArrayInputStream(array), this.fileInputStream);
            this.inflaterInputStream = new InflaterInputStream(sequenceInputStream);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Decode BLOB configuration to raw xml
     */
    @Override
    public void decode() {
        try {
            if (!isDecoded()) {
                for (int i = this.inflaterInputStream.read(); i != -1; i =
                        this.inflaterInputStream.read()) {
                    this.fileOutputStream.write(i);
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * CLose all streams used by this class
     */
    @Override
    public void close() {
        try {
            this.fileInputStream.close();
            this.fileOutputStream.close();
            this.inflaterInputStream.close();
            this.sequenceInputStream.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private Boolean isDecoded() throws NullPointerException, IndexOutOfBoundsException {
        try {
            if (this.array[0] == 60 || this.array[1] == 63 || this.array[2] == 120) {
                return true;
            }
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
}
