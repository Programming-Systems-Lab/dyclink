package edu.columbia.cs.psl.kamino;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class Logger {
    private OutputStreamWriter writer;
    public File outputFile;

    public Logger(File outputFile) {
        this.outputFile = outputFile;
        try {
            this.writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(this.outputFile)));
        } catch (FileNotFoundException e) {
            closeBuffer();
            e.printStackTrace();
        }
    }

    public void record(String output) {
        try {
            this.writer.write(output);
            this.writer.flush();
        } catch (IOException e) {
            closeBuffer();
            e.printStackTrace();
        }
    }

    public void closeBuffer() {
        try {
            this.writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
