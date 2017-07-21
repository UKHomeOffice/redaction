/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.form;

import java.math.*;
import java.io.Writer;

public class HexNumberFormatterSpan extends NumberFormatterSpan
{
  boolean uppercase;

  public HexNumberFormatterSpan( int min, int max, int alignment,
			 char fillChar, int overflowChar, boolean upper )
  {
    super( min, max, alignment, fillChar, overflowChar, null );
    uppercase = upper;
  }

  public HexNumberFormatterSpan( int min, int alignment, boolean upper )
  {
    super( min, alignment );
    uppercase = upper;
  }

  public void printf( Writer out, int v )
  {
    String s = Integer.toHexString(v);
    if( uppercase )
      s = s.toUpperCase();
    printf( out, s.toCharArray() );
  }

  public void printf( Writer out, char v )
  {
    String s = Integer.toHexString(v);
    printf( out, s.toCharArray() );
  }

  public void printf( Writer out, long v )
  {
    String s = Long.toHexString(v);
    if( uppercase )
      s = s.toUpperCase();
    printf( out, s.toCharArray() );
  }

  public void printf( Writer out, BigInteger v )
  {
    String s = v.toString( 16 );
    if( uppercase )
      s = s.toUpperCase();
    printf( out, s.toCharArray() );
  }
  
}
