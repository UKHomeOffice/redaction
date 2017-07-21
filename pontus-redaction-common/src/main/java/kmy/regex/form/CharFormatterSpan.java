/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.form;

import java.io.Writer;
import java.io.IOException;

import kmy.jint.lang.CharString;

public class CharFormatterSpan extends FormatterSpan
{

  public CharFormatterSpan( int min, int max, int alignment,
			 char fillChar, int overflowChar )
  {
    super( min, max, alignment, fillChar, overflowChar );
  }

  public CharFormatterSpan( int min, int alignment )
  {
    super( min, alignment );
  }

  public void printf( Writer out, int v )
  {
    super.printf( out, (char)v );
  }

  public void printf( Writer out, long v )
  {
    super.printf( out, (char)v );
  }

  public void printf( Writer out, double v )
  {
    super.printf( out, (char)v );
  }

  public void printf( Writer out, float v )
  {
    super.printf( out, (char)v );
  }

  public void printf( Writer out, CharString cs )
  {
    printf( out, cs.toString() );
  }

  public void printf( Writer out, String s )
  {
    super.printf( out, (char)(new Double(s)).doubleValue() );
  }

  protected void printfString( Writer out, String s )
  {
    super.printf( out, s );
  }

  public void printf( Writer out, Object obj )
  {
    if( obj instanceof Number )
      super.printf( out, obj );
    else
      printf( out, obj.toString() );
  }

}

