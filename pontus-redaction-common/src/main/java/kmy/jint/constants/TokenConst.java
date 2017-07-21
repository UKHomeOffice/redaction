/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.jint.constants;

public interface TokenConst
{
  final public static int TOKEN_EOF      = 0x10001;
  final public static int TOKEN_WORD     = 0x10002;
  final public static int TOKEN_V_INT    = 0x10003;
  final public static int TOKEN_V_LONG   = 0x10004;
  final public static int TOKEN_V_FLOAT  = 0x10010;
  final public static int TOKEN_V_DOUBLE = 0x10011;
  final public static int TOKEN_NONE     = 0x10020;
  final public static int TOKEN_V_STRING = '"';
  final public static int TOKEN_V_CHAR   = '\'';

  final public static int TOKEN_V_REGEX  = '`';  // m`re`
  final public static int TOKEN_V_SUBST  = 'a';  // s`re`form`
  final public static int TOKEN_V_TRANS  = 't';  // t`rg`rg`
  final public static int TOKEN_V_PATT   = 'F';  // p`patt`
  final public static int TOKEN_V_FORM   = 'f';  // f`form` - printf-style format string
  final public static int TOKEN_V_BOUND  = 'b';  // b`form` - format string with embedded variables

  final public static int TOKEN_GE     = 'G'; // >=
  final public static int TOKEN_GG     = 'g'; // >>
  final public static int TOKEN_GGG    = 'Q'; // >>>
  final public static int TOKEN_GGE    = 'k'; // >>=
  final public static int TOKEN_GGGE   = 'q'; // >>>=
  final public static int TOKEN_LE     = 'L'; // <=
  final public static int TOKEN_LL     = 'l'; // <<
  final public static int TOKEN_LLE    = 'K'; // <<=
  final public static int TOKEN_PP     = 'p'; // ++
  final public static int TOKEN_PE     = 'P'; // +=
  final public static int TOKEN_SE     = 'S'; // /=
  final public static int TOKEN_EE     = 'e'; // ==
  final public static int TOKEN_AE     = 'A'; // *=
  final public static int TOKEN_MM     = 'm'; // --
  final public static int TOKEN_ME     = 'M'; // -=
  final public static int TOKEN_NE     = 'N'; // !=
  final public static int TOKEN_PCE    = 'C'; // %=
  final public static int TOKEN_OR     = 'O'; // ||
  final public static int TOKEN_ORE    = 'o'; // |=
  final public static int TOKEN_AND    = 'D'; // &&
  final public static int TOKEN_ANDE   = 'd'; // &=
  final public static int TOKEN_XE     = 'X'; // ^=
  final public static int TOKEN_SS     = 's'; // ::
  final public static int TOKEN_TE     = 'T'; // ~=

  final public static int UNARY_MINUS  = 'U'; // -

  final public static int TOKEN_ABSTRACT     = -1;
  final public static int TOKEN_BOOLEAN      = -2;
  final public static int TOKEN_BREAK        = -3;
  final public static int TOKEN_BYTE         = -4;
  final public static int TOKEN_BYVALUE      = -5;
  final public static int TOKEN_CASE         = -6;
  final public static int TOKEN_CATCH        = -7;
  final public static int TOKEN_CHAR         = -8;
  final public static int TOKEN_CLASS        = -9;
  final public static int TOKEN_CONST        = -10;
  final public static int TOKEN_CONTINUE     = -11;
  final public static int TOKEN_DEFAULT      = -12;
  final public static int TOKEN_DO           = -13;
  final public static int TOKEN_DOUBLE       = -14;
  final public static int TOKEN_ELSE         = -15;
  final public static int TOKEN_EXTENDS      = -16;
  final public static int TOKEN_FALSE        = -17;
  final public static int TOKEN_FINAL        = -18;
  final public static int TOKEN_FINALLY      = -19;
  final public static int TOKEN_FLOAT        = -20;
  final public static int TOKEN_FOR          = -21;
  final public static int TOKEN_GOTO         = -22;
  final public static int TOKEN_IF           = -23;
  final public static int TOKEN_IMPLEMENTS   = -24;
  final public static int TOKEN_IMPORT       = -25;
  final public static int TOKEN_INSTANCEOF   = -26;
  final public static int TOKEN_INT          = -27;
  final public static int TOKEN_INTERFACE    = -28;
  final public static int TOKEN_LONG         = -29;
  final public static int TOKEN_NATIVE       = -30;
  final public static int TOKEN_NEW          = -31;
  final public static int TOKEN_NULL         = -32;
  final public static int TOKEN_PACKAGE      = -33;
  final public static int TOKEN_PRIVATE      = -34;
  final public static int TOKEN_PROTECTED    = -35;
  final public static int TOKEN_PUBLIC       = -36;
  final public static int TOKEN_RETURN       = -37;
  final public static int TOKEN_SHORT        = -38;
  final public static int TOKEN_STATIC       = -39;
  final public static int TOKEN_SUPER        = -40;
  final public static int TOKEN_SWITCH       = -41;
  final public static int TOKEN_SYNCHRONIZED = -42;
  final public static int TOKEN_THIS         = -43;
  final public static int TOKEN_THREADSAFE   = -44;
  final public static int TOKEN_THROW        = -45;
  final public static int TOKEN_THROWS       = -46;
  final public static int TOKEN_TRANSIENT    = -47;
  final public static int TOKEN_TRUE         = -48;
  final public static int TOKEN_TRY          = -49;
  final public static int TOKEN_VOID         = -50;
  final public static int TOKEN_VOLATILE     = -51;
  final public static int TOKEN_WHILE        = -52;

  final public static int TOKEN_WHERE        = -53; // unofficial

  final public static int TOK_OPT_GLOBAL      = 0x00000001; // g
  final public static int TOK_OPT_IGNORE      = 0x00000002; // i
  final public static int TOK_OPT_SINGLELINE  = 0x00000004; // s
  final public static int TOK_OPT_MULTILINE   = 0x00000008; // m

  final public static int TOK_OPT_BUFFERONLY  = 0x00010000; // B
  final public static int TOK_OPT_DECLARE     = 0x00020000; // D
  final public static int TOK_OPT_OFFLINE     = 0x00040000; // O
  final public static int TOK_OPT_RETAINALL   = 0x00080000; // R
  final public static int TOK_OPT_STANDALONE  = 0x00100000; // S

}
