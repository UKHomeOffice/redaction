/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.interp;

public interface RInterpCommands
{
  final static byte ASSIGN     = (byte)0x01; /* ASSIGN var val */
  final static byte HARDASSIGN = (byte)0x02; /* HARDASSIGN var val */
  final static byte PICK       = (byte)0x03; /* PICK var */
  final static byte FORK       = (byte)0x04; /* FORK label */
  final static byte SKIP       = (byte)0x05; /* SKIP */
  final static byte SKIP_NON_NEWLINE   = (byte)0x06; /* SKIP */

  final static byte ASSERT_CLASS       = (byte)0x10; /* ASSERT_CLASS classID */
  final static byte ASSERT_CHAR        = (byte)0x11; /* ASSERT_CHAR count char char ... */
  final static byte ASSERT_RANGE       = (byte)0x12; /* ASSERT_RANGE count, char ... */
  final static byte ASSERT_CLASS_RANGE = (byte)0x13; /* ASSERT_CLASS_RANGE classID, count, char ... */
  final static byte ASSERT_VAR         = (byte)0x14; /* ASSERT_VAR var */
  final static byte ASSERT_EXT_VAR     = (byte)0x15; /* ASSERT_EXT_VAR extCell var */
  final static byte BOUNDARY           = (byte)0x16; /* BOUNDARY boundaryID */

  final static byte SHIFTTBL           = (byte)0x17; /* SHIFTTBL off cnt {chr shft} cnt {chr} */

  final static byte DECJUMP  = (byte)0x20; /* DECJUMP var label */
  final static byte DECFAIL  = (byte)0x21; /* DECFAIL var */

  final static byte JUMP = (byte)0x30; /* JUMP label */
  final static byte FAIL = (byte)0x31; /* FAIL */

  final static byte MFSTART      = (byte)0x40; /* MFSTART headDec, label */
  final static byte MFSTART_HEAD = (byte)0x41; /* MFSTART_HEAD minCount, headDec, label */
  final static byte MFEND        = (byte)0x42; /* MFEND */
  final static byte MFENDLIMIT   = (byte)0x43; /* MFENDLIMIT count */

  final static byte CHARLEFT    = (byte)0x50; /* CHARLEFT count */

  final static byte JUMP_RANGE     = (byte)0x60; /* JUMP_RANGE label count char ... */
  final static byte JUMP_CHAR      = (byte)0x61; /* JUMP_CHAR char ... */
  final static byte JUMP_MIN_LEFT  = (byte)0x62; /* JUMP_MIN_LEFT label count */
  final static byte JUMP_MAX_LEFT  = (byte)0x63; /* JUMP_MIN_LEFT label count */

  final static byte STOP = (byte)0x00; /* STOP */
}
