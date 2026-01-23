package xyz.doejack.filenet;

import java.io.IOException;

public class main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage:");
            System.out.println("  1. Decode & Reveal: java main <path_to_blob>");
            System.out.println("  2. Encrypt New Pass: java main <path_to_blob> <NEW_PASSWORD>");
            return;
        }

        String blobPath = args[0];
        String newPassword = (args.length > 1) ? args[1] : null;

        try {
            GCDConfigImpl processor = new GCDConfigImpl(blobPath);
            processor.decode(); 
            processor.hackAndReveal(newPassword);
            processor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}