/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.jint.constants;

public interface MiniErrorCodes
{
  public static final int ERR_MASK_MODULE      = 0x00000F00;

  public static final int ERR_MASK_LEXER       = 0x00000100;
  public static final int ERR_MASK_PARSER      = 0x00000200;
  public static final int ERR_MASK_TYPER       = 0x00000300;
  public static final int ERR_MASK_CLASSTABLE  = 0x00000400;
  public static final int ERR_MASK_COMPILER    = 0x00000500;
  public static final int ERR_MASK_GENERATOR   = 0x00000600;
  public static final int ERR_MASK_FORMAT      = 0x00000700;
  public static final int ERR_MASK_REGEX       = 0x00000800;

  public static final int ERR_MASK_SEVERITY    = 0x0000F000;

  public static final int ERR_MASK_VERBOSE     = 0x00001000;
  public static final int ERR_MASK_INFO        = 0x00002000;
  public static final int ERR_MASK_WARNING     = 0x00003000;
  public static final int ERR_MASK_ERROR       = 0x00004000;
  public static final int ERR_MASK_FATAL       = 0x00005000;
  public static final int ERR_MASK_INTERNAL    = 0x00006000;

  // regex codes

  int ERR_R_GENERIC       = ERR_MASK_REGEX      | ERR_MASK_INTERNAL;

  int ERR_R_NOVARNAME     = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x001;
  int ERR_R_NOCURLBRACKET = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x002;
  int ERR_R_BADSTART      = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x003;
  int ERR_R_NOVAREXPR     = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x004;
  int ERR_R_BQUNFINISHED  = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x005;
  int ERR_R_BQBAD         = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x006;
  int ERR_R_NOBRACKET     = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x007;
  int ERR_R_EXTRABRACKET  = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x008;
  int ERR_R_NOSQBRACKET   = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x009;
  int ERR_R_STRAYBSLASH   = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x00A;
  int ERR_R_CTLUNFINISHED = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x00B;
  int ERR_R_CTLINVALID    = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x00C;
  int ERR_R_HEXUNFINISHED = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x00D;
  int ERR_R_HEXBADNUMBER  = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x00E;
  int ERR_R_BADBACKREF    = ERR_MASK_REGEX      | ERR_MASK_ERROR    | 0x00F;

  int ERR_R_INTERNAL      = ERR_MASK_REGEX      | ERR_MASK_INTERNAL | 0x001;

  // formatter codes

  int ERR_F_GENERIC       = ERR_MASK_FORMAT     | ERR_MASK_INTERNAL;
  int ERR_F_NOVARNAME     = ERR_MASK_FORMAT     | ERR_MASK_ERROR    | 0x001;
  int ERR_F_NOCURLBRACKET = ERR_MASK_FORMAT     | ERR_MASK_ERROR    | 0x002;
  int ERR_F_BADEND        = ERR_MASK_FORMAT     | ERR_MASK_ERROR    | 0x003;
  int ERR_F_BADFORMATCHAR = ERR_MASK_FORMAT     | ERR_MASK_ERROR    | 0x004;
  int ERR_F_INCOMPLETE    = ERR_MASK_FORMAT     | ERR_MASK_ERROR    | 0x005;
  int ERR_F_NOTWITHJAVA   = ERR_MASK_FORMAT     | ERR_MASK_ERROR    | 0x006;

}
