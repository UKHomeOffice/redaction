/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.jint.constants;

public interface DefinitionConst
{
  public static final int ACC_PUBLIC         = 0x0001;
  public static final int ACC_PRIVATE        = 0x0002;
  public static final int ACC_PROTECTED      = 0x0004;
  public static final int ACC_STATIC         = 0x0008;
  public static final int ACC_FINAL          = 0x0010;
  public static final int ACC_SUPER          = 0x0020; // for classes: new invokespecial semantics
  public static final int ACC_SYNCHRONIZED   = 0x0020; // for methods
  public static final int ACC_VOLATILE       = 0x0040;
  public static final int ACC_TRANSIENT      = 0x0080;
  public static final int ACC_NATIVE         = 0x0100;
  public static final int ACC_INTERFACE      = 0x0200;
  public static final int ACC_ABSTRACT       = 0x0400;

  public final static int PUBLIC_FLAG          = ACC_PUBLIC;
  public final static int PRIVATE_FLAG         = ACC_PRIVATE;
  public final static int ABSTRACT_FLAG        = ACC_ABSTRACT;
  public final static int FINAL_FLAG           = ACC_FINAL;
  public final static int INTERFACE_FLAG       = ACC_INTERFACE;
  public final static int PROTECTED_FLAG       = ACC_PROTECTED;
  public final static int STATIC_FLAG          = ACC_STATIC;
  public final static int SYNCHRONIZED_FLAG    = ACC_SYNCHRONIZED;
  public final static int NATIVE_FLAG          = ACC_NATIVE;
  public final static int VOLATILE_FLAG        = ACC_VOLATILE;
  public final static int TRANSIENT_FLAG       = ACC_TRANSIENT;

  public final static int STRICT_FLAG          = 0x00010000;
  public final static int EXPLICIT_FLAG        = 0x00020000;
  public final static int WHERE_FLAG           = 0x00040000;

  public final static int MUST_BE_CLASS_FLAG   = 0x00100000;
  public final static int SELF_USED_FLAG       = 0x00200000;
  public final static int METHOD_SCOPE_FLAG    = 0x00400000;
  public final static int WORLD_FLAG           = 0x00800000;

  public final static int CONST_FLAG           = 0x01000000;
  public final static int SCRIPT_STYLE_FLAG    = 0x02000000;
  public final static int PACKAGE_PRIVATE_FLAG = 0x04000000;

  public final static int ACC_MASK             = 0x0000FFFF;

}
