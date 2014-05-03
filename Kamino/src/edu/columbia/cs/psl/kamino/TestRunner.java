package edu.columbia.cs.psl.kamino;

import java.io.File;
import java.io.FileOutputStream;

import edu.columbia.cs.psl.kamino.org.objectweb.asm.ClassReader;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.ClassWriter;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.util.CheckClassAdapter;

public class TestRunner {
    public static void main(String[] args) {
        try {
            ClassReader cr = new ClassReader(Constants.TOMCAT_VERSION);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cr.accept(new CheckClassAdapter(new ControlFlowLoggingClassVisitor(cw)), ClassReader.EXPAND_FRAMES);
            File instDir = new File("inst");
            if (!instDir.exists()) {
                instDir.mkdir();
            }
            FileOutputStream fos = new FileOutputStream("inst/"+Constants.TOMCAT_VERSION+".class");
            fos.write(cw.toByteArray());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
