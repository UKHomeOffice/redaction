/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.form;

import java.io.Writer;
import java.io.IOException;

public class StringSpan extends Span
{

  String str;

  public StringSpan( String s )
  {
    str = s;
  }

  public void print( Writer out, int[] argPtr, Object[] args )
  {
    try
      {
	out.write( str );
      }
    catch( IOException e )
      {
	throw new RuntimeException( "IOException: " + e.getMessage() );
      }
  }

}
