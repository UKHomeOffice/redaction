/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.jint.gen;

interface JVMCodes
{
  final static int nop = 0x00;

  final static int aconst_null = 0x01;

  final static int iconst_m1 = 0x02;
  final static int iconst_0 = 0x03;
  final static int iconst_1 = 0x04;
  final static int iconst_2 = 0x05;
  final static int iconst_3 = 0x06;
  final static int iconst_4 = 0x07;
  final static int iconst_5 = 0x08;

  final static int lconst_0 = 0x09;
  final static int lconst_1 = 0x0A;

  final static int fconst_0 = 0x0B;
  final static int fconst_1 = 0x0C;
  final static int fconst_2 = 0x0D;

  final static int dconst_0 = 0x0E;
  final static int dconst_1 = 0x0F;

  final static int bipush = 0x10;
  final static int sipush = 0x11;

  final static int ldc    = 0x12;
  final static int ldc_w  = 0x13;
  final static int ldc2_w = 0x14;

  final static int iload = 0x15;
  final static int lload = 0x16;
  final static int fload = 0x17;
  final static int dload = 0x18;
  final static int aload = 0x19;

  final static int iload_0 = 0x1A;
  final static int iload_1 = 0x1B;
  final static int iload_2 = 0x1C;
  final static int iload_3 = 0x1D;

  final static int lload_0 = 0x1E;
  final static int lload_1 = 0x1F;
  final static int lload_2 = 0x20;
  final static int lload_3 = 0x21;

  final static int fload_0 = 0x22;
  final static int fload_1 = 0x23;
  final static int fload_2 = 0x24;
  final static int fload_3 = 0x25;

  final static int dload_0 = 0x26;
  final static int dload_1 = 0x27;
  final static int dload_2 = 0x28;
  final static int dload_3 = 0x29;

  final static int aload_0 = 0x2A;
  final static int aload_1 = 0x2B;
  final static int aload_2 = 0x2C;
  final static int aload_3 = 0x2D;

  final static int iaload = 0x2E;
  final static int laload = 0x2F;
  final static int faload = 0x30;
  final static int daload = 0x31;
  final static int aaload = 0x32;
  final static int baload = 0x33;
  final static int caload = 0x34;
  final static int saload = 0x35;

  final static int istore = 0x36;
  final static int lstore = 0x37;
  final static int fstore = 0x38;
  final static int dstore = 0x39;
  final static int astore = 0x3A;

  final static int istore_0 = 0x3B;
  final static int istore_1 = 0x3C;
  final static int istore_2 = 0x3D;
  final static int istore_3 = 0x3E;

  final static int lstore_0 = 0x3F;
  final static int lstore_1 = 0x40;
  final static int lstore_2 = 0x41;
  final static int lstore_3 = 0x42;

  final static int fstore_0 = 0x43;
  final static int fstore_1 = 0x44;
  final static int fstore_2 = 0x45;
  final static int fstore_3 = 0x46;

  final static int dstore_0 = 0x47;
  final static int dstore_1 = 0x48;
  final static int dstore_2 = 0x49;
  final static int dstore_3 = 0x4A;

  final static int astore_0 = 0x4B;
  final static int astore_1 = 0x4C;
  final static int astore_2 = 0x4D;
  final static int astore_3 = 0x4E;

  final static int iastore = 0x4F;
  final static int lastore = 0x50;
  final static int fastore = 0x51;
  final static int dastore = 0x52;
  final static int aastore = 0x53;
  final static int bastore = 0x54;
  final static int castore = 0x55;
  final static int sastore = 0x56;

  final static int pop  = 0x57;
  final static int pop2 = 0x58;

  final static int dup     = 0x59;
  final static int dup_x1  = 0x5A;
  final static int dup_x2  = 0x5B;
  final static int dup2    = 0x5C;
  final static int dup2_x1 = 0x5D;
  final static int dup2_x2 = 0x5E;

  final static int swap = 0x5F;

  final static int iadd = 0x60;
  final static int ladd = 0x61;
  final static int fadd = 0x62;
  final static int dadd = 0x63;
  final static int isub = 0x64;
  final static int lsub = 0x65;
  final static int fsub = 0x66;
  final static int dsub = 0x67;
  final static int imul = 0x68;
  final static int lmul = 0x69;
  final static int fmul = 0x6A;
  final static int dmul = 0x6B;
  final static int idiv = 0x6C;
  final static int ldiv = 0x6D;
  final static int fdiv = 0x6E;
  final static int ddiv = 0x6F;
  final static int irem = 0x70;
  final static int lrem = 0x71;
  final static int frem = 0x72;
  final static int drem = 0x73;

  final static int ineg = 0x74;
  final static int lneg = 0x75;
  final static int fneg = 0x76;
  final static int dneg = 0x77;

  final static int ishl  = 0x78;
  final static int lshl  = 0x79;
  final static int ishr  = 0x7A;
  final static int lshr  = 0x7B;
  final static int iushr = 0x7C;
  final static int lushr = 0x7D;
  final static int iand  = 0x7E;
  final static int land  = 0x7F;
  final static int ior   = 0x80;
  final static int lor   = 0x81;
  final static int ixor  = 0x82;
  final static int lxor  = 0x83;

  final static int iinc = 0x84;

  final static int i2l = 0x85;
  final static int i2f = 0x86;
  final static int i2d = 0x87;
  final static int l2i = 0x88;
  final static int l2f = 0x89;
  final static int l2d = 0x8A;
  final static int f2i = 0x8B;
  final static int f2l = 0x8C;
  final static int f2d = 0x8D;
  final static int d2i = 0x8E;
  final static int d2l = 0x8F;
  final static int d2f = 0x90;
  final static int i2b = 0x91;
  final static int i2c = 0x92;
  final static int i2s = 0x93;

  final static int lcmp  = 0x94;
  final static int fcmpl = 0x95;
  final static int fcmpg = 0x96;
  final static int dcmpl = 0x97;
  final static int dcmpg = 0x98;

  final static int ifeq = 0x99;
  final static int ifne = 0x9A;
  final static int iflt = 0x9B;
  final static int ifge = 0x9C;
  final static int ifgt = 0x9D;
  final static int ifle = 0x9E;

  final static int if_icmpeq = 0x9F;
  final static int if_icmpne = 0xA0;
  final static int if_icmplt = 0xA1;
  final static int if_icmpge = 0xA2;
  final static int if_icmpgt = 0xA3;
  final static int if_icmple = 0xA4;

  final static int if_acmpeq = 0xA5;
  final static int if_acmpne = 0xA6;

  final static int _goto = 0xA7;
  final static int jsr   = 0xA8;
  final static int ret   = 0xA9;

  final static int tableswitch  = 0xAA;
  final static int lookupswitch = 0xAB;

  final static int ireturn = 0xAC;
  final static int lreturn = 0xAD;
  final static int freturn = 0xAE;
  final static int dreturn = 0xAF;
  final static int areturn = 0xB0;
  final static int vreturn = 0xB1;

  final static int getstatic = 0xB2;
  final static int putstatic = 0xB3;
  final static int getfield  = 0xB4;
  final static int putfield  = 0xB5;

  final static int invokevirtual   = 0xB6;
  final static int invokespecial   = 0xB7;
  final static int invokestatic    = 0xB8;
  final static int invokeinterface = 0xB9;

  final static int newobject = 0xBB;
  final static int newarray  = 0xBC;
  final static int anewarray = 0xBD;

  final static int arraylength = 0xBE;

  final static int athrow = 0xBF;

  final static int checkcast = 0xC0;

  final static int instnceof = 0xC1;

  final static int monitorenter = 0xC2;
  final static int monitorexit = 0xC3;

  final static int wide = 0xC4; 

  final static int multianewarray = 0xC5;

  final static int ifnull    = 0xC6;
  final static int ifnonnull = 0xC7;

  final static int goto_w = 0xC8;
  final static int jsr_w  = 0xC9;

  //---- constant pool entry codes

  final static byte TAG_UTF8               = (byte)1;
  final static byte TAG_Integer            = (byte)3;
  final static byte TAG_Float              = (byte)4;
  final static byte TAG_Long               = (byte)5;
  final static byte TAG_Double             = (byte)6;
  final static byte TAG_Class              = (byte)7;
  final static byte TAG_String             = (byte)8;
  final static byte TAG_FieldRef           = (byte)9;
  final static byte TAG_MethodRef          = (byte)10;
  final static byte TAG_InterfaceMethodRef = (byte)11;
  final static byte TAG_NameAndType        = (byte)12;


}
