package xyz.doejack.filenet;

import java.io.IOException;

public class main {
    public static void main(String[] args) {
        if (args.length > 1 || args.length <= 0) {
            System.out.println("Caller must have only one argument");
            System.out.println("With path to blob file");
        } else {
            try {
                GCDConfigImpl GCDConfig = new GCDConfigImpl(args[0]);
                GCDConfig.decode();
                GCDConfig.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
