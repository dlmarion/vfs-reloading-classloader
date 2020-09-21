package org.apache.accumulo.classloader.vfs.context;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.accumulo.classloader.vfs.context.ReloadingVFSContextClassLoaderFactory.Context;
import org.apache.accumulo.classloader.vfs.context.ReloadingVFSContextClassLoaderFactory.ContextConfig;
import org.apache.accumulo.classloader.vfs.context.ReloadingVFSContextClassLoaderFactory.Contexts;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.Gson;


public class ReloadingVFSContextClassLoaderFactoryTest {
  
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  
  private static final Contexts c = new Contexts();

  @BeforeClass
  public static void setup() throws Exception {
    ContextConfig cc1 = new ContextConfig();
    cc1.setClassPath("file:///tmp/foo");
    cc1.setPostDelegate(true);
    cc1.setMonitorIntervalMs(30000);
    Context c1 = new Context();
    c1.setName("cx1");
    c1.setConfig(cc1);

    ContextConfig cc2 = new ContextConfig();
    cc2.setClassPath("file:///tmp/bar");
    cc2.setPostDelegate(false);
    cc2.setMonitorIntervalMs(30000);
    Context c2 = new Context();
    c2.setName("cx2");
    c2.setConfig(cc2);
    
    List<Context> list = new ArrayList<>();
    list.add(c1);
    list.add(c2);
    c.setContexts(list);
  }
  
  @Test
  public void testDeSer() throws Exception {
    Gson g = new Gson().newBuilder().setPrettyPrinting().create();
    String contexts = g.toJson(c);
    System.out.println(contexts);
    
    Gson g2 = new Gson();
    Contexts actual = g2.fromJson(contexts, Contexts.class);
    
    Assert.assertEquals(c, actual);
    
  }
  
  @Test
  public void testCreation() throws Exception {
    File f = temp.newFile();
    f.deleteOnExit();
    Gson g = new Gson();
    String contexts = g.toJson(c);
    try (BufferedWriter writer = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8, StandardOpenOption.WRITE)) {
      writer.write(contexts);
    }
    ReloadingVFSContextClassLoaderFactory cl = new ReloadingVFSContextClassLoaderFactory() {
      @Override
      protected String getConfigFileLocation() {
        return f.getAbsolutePath();
      }
    };
    cl.initialize(null);
    try {
      cl.getClassLoader("c1");
      Assert.fail("Expected illegal argument exception");
    } catch (IllegalArgumentException e) {
      // works
    }
    cl.getClassLoader("cx1");
    cl.getClassLoader("cx2");
    
  }
  
}
