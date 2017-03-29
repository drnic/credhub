package io.pivotal.security.jna.libcrypto;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

public class Rsa extends Structure {

  public int pad;
  public long version;
  public Pointer rsaMethod;
  public Pointer engine;
  public Pointer np;
  public Pointer ep;
  public Pointer dp;
  public Pointer pp;
  public Pointer qp;
  public Pointer dmp1;
  public Pointer dmq1;
  public Pointer iqmp;

  public Rsa(Pointer pp) {
    super(pp);
  }

  @Override
  protected List getFieldOrder() {
    return Arrays
        .asList("pad", "version", "rsaMethod", "engine", "np", "ep", "dp", "pp",
            "qp", "dmp1", "dmq1", "iqmp");
  }

  public static class ByReference extends Rsa implements Structure.ByReference {

    public ByReference(Pointer p) {
      super(p);
    }
  }
}
