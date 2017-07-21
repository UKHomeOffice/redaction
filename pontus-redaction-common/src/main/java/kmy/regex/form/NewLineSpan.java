/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.form;

import java.io.Writer;
import java.io.IOException;

public class NewLineSpan extends Span
{

  static char[] newline = { '\n' };

  static
  {
    try
      {
	newline = System.getProperty( "line.separator" ).toCharArray();
      }
    catch( Exception e )
      {
	e.printStackTrace();
      }
  }

  public void print( Writer out, int[] argPtr, Object[] args )
  {
    try
      {
	out.write( newline );
      }
    catch( IOException e )
      {
	throw new RuntimeException( "IOException: " + e.getMessage() );
      }
  }

}
