package org.apache.accumulo.classloader.vfs.context;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.accumulo.classloader.vfs.ReloadingVFSClassLoader;
import org.apache.accumulo.core.spi.common.ClassLoaderFactory;

import com.google.gson.Gson;

public class ReloadingVFSContextClassLoaderFactory implements ClassLoaderFactory {
  
  public static class Contexts {
    List<Context> contexts;

    public List<Context> getContexts() {
      return contexts;
    }

    public void setContexts(List<Context> contexts) {
      this.contexts = contexts;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((contexts == null) ? 0 : contexts.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Contexts other = (Contexts) obj;
      if (contexts == null) {
        if (other.contexts != null)
          return false;
      } else if (!contexts.equals(other.contexts))
        return false;
      return true;
    }
  }
  
  public static class Context {
    private String name;
    private ContextConfig config;
    public String getName() {
      return name;
    }
    public void setName(String name) {
      this.name = name;
    }
    public ContextConfig getConfig() {
      return config;
    }
    public void setConfig(ContextConfig config) {
      this.config = config;
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((config == null) ? 0 : config.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Context other = (Context) obj;
      if (config == null) {
        if (other.config != null)
          return false;
      } else if (!config.equals(other.config))
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      return true;
    }
  }
  
  public static class ContextConfig {
    private String classPath;
    private boolean postDelegate;
    private long monitorIntervalMs;
    public String getClassPath() {
      return classPath;
    }
    public void setClassPath(String classPath) {
      this.classPath = ReloadingVFSClassLoader.replaceEnvVars(classPath, System.getenv());
    }
    public boolean getPostDelegate() {
      return postDelegate;
    }
    public void setPostDelegate(boolean postDelegate) {
      this.postDelegate = postDelegate;
    }
    public long getMonitorIntervalMs() {
      return monitorIntervalMs;
    }
    public void setMonitorIntervalMs(long monitorIntervalMs) {
      this.monitorIntervalMs = monitorIntervalMs;
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((classPath == null) ? 0 : classPath.hashCode());
      result = prime * result + (int) (monitorIntervalMs ^ (monitorIntervalMs >>> 32));
      result = prime * result + (postDelegate ? 1231 : 1237);
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ContextConfig other = (ContextConfig) obj;
      if (classPath == null) {
        if (other.classPath != null)
          return false;
      } else if (!classPath.equals(other.classPath))
        return false;
      if (monitorIntervalMs != other.monitorIntervalMs)
        return false;
      if (postDelegate != other.postDelegate)
        return false;
      return true;
    }
  }
  
  public static final String CONFIG_LOCATION = "vfs.context.class.loader.config";
  private static final Map<String, ReloadingVFSClassLoader> CONTEXTS = new HashMap<>();
  
  protected String getConfigFileLocation() {
    String loc = System.getenv(CONFIG_LOCATION);
    if (null == loc || loc.isBlank()) {
      throw new RuntimeException(CONFIG_LOCATION + " system property must be set to use ReloadingVFSContextClassLoaderFactory");
    }
    return loc;
  }
  
  @Override
  public void initialize(ClassLoaderFactoryConfiguration conf) throws Exception {
    // Properties
    File f = new File(getConfigFileLocation());
    if (!f.canRead()) {
      throw new RuntimeException("Unable to read configuration file: " + f.getAbsolutePath());
    }
    Gson g = new Gson();
    Contexts con = g.fromJson(Files.newBufferedReader(f.toPath()), Contexts.class);

    con.getContexts().forEach(c -> {
      CONTEXTS.put(c.getName(), new ReloadingVFSClassLoader(ReloadingVFSContextClassLoaderFactory.class.getClassLoader()) {
        @Override
        protected String getClassPath() {
          return c.getConfig().getClassPath();
        }
        @Override
        protected boolean isPreDelegationModel() {
          return !(c.getConfig().getPostDelegate());
        }
        @Override
        protected long getMonitorInterval() {
          return c.getConfig().getMonitorIntervalMs();
        }
      });
    });
  }

  @Override
  public ClassLoader getClassLoader(String contextName) throws IllegalArgumentException {
    if (!CONTEXTS.containsKey(contextName)) {
      throw new IllegalArgumentException("ReloadingVFSContextClassLoaderFactory not configured for context: " + contextName);
    }
    return CONTEXTS.get(contextName);
  }

}