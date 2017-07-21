/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.tree;

public interface CharClassCodes
{
  // char classes work only for range 0x0080-0xFFFF,
  // ASCII is always done by ranges
  public static final int CLASS_DISABLED     = 0x8;
  public static final int CLASS_NONE         = 0;
  public static final int CLASS_ALL          = 0x7;
  public static final int CLASS_LETTER       = 0x3;
  public static final int CLASS_LOWERCASE    = 0x1;
  public static final int CLASS_UPPERCASE    = 0x2;
  public static final int CLASS_NONLETTER    = 0x4;
  public static final int CLASS_NONLOWERCASE = 0x6;
  public static final int CLASS_NONUPPERCASE = 0x5;
}
