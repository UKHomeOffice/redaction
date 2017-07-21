/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.util;

import java.util.Enumeration;

public class CaseInsensitiveRegex extends ProxyRegex
{

  private int matchOff;

  private char[] cMatch;

  private int[] handleToExtHandle;
  private int[] extOff;
  private Object[] extBuf;

  public CaseInsensitiveRegex( String regex )
  {
    this( createLowerCaseRegex( regex ) );
  }

  public CaseInsensitiveRegex( Regex lowerCaseRegex )
  {
    super( lowerCaseRegex );
    int maxHandle = -1;
    int maxExtHandle = -1;
    Enumeration vars = variables();
    while( vars.hasMoreElements() )
      {
	String name = (String)vars.nextElement();
	int h = getVariableHandle( name, true );
	if( h > maxHandle )
	  maxHandle = h;
	h = getVariableHandle( name, false );
	if( h > maxHandle )
	  maxHandle = h;
	h = getExtVariableHandle( name );
	if( h > maxExtHandle )
	  maxExtHandle = h;
      }
    if( maxHandle >= 0 )
      {
	handleToExtHandle = new int[maxHandle+1];
	if( maxExtHandle >= 0 )
	  {
	    extOff = new int[maxExtHandle+1];
	    extBuf = new Object[maxExtHandle+1];
	  }
	vars = variables();
	while( vars.hasMoreElements() )
	  {
	    String name = (String)vars.nextElement();
	    int eh = getExtVariableHandle( name );
	    int h = getVariableHandle( name, true );
	    handleToExtHandle[h] = eh;
	    h = getVariableHandle( name, false );
	    handleToExtHandle[h] = eh;
	  }
      }
  }

  public void init( char[] arr, int off, int len )
  {
    init( -1, arr, off, len );
  }

  private void init( int extHandle, char[] arr, int off, int len )
  {
    int i = off;
    int last = off + len;
    char c;
    while( i < last && ( !Character.isLetter( c = arr[i] ) ||
			Character.isLowerCase( c ) ) )
      i++;
    if( extHandle < 0 )
      cMatch = arr;
    else
      extBuf[extHandle] = arr;
    if( i == last )
      if( extHandle < 0 )
	{
	  matchOff = 0;
	  super.init( arr, off, len );
	}
      else
	{
	  extOff[extHandle] = 0;
	  super.setExtVariableBuffer( extHandle, arr );
	}
    else
      {
	char[] buf = new char[len];
	int j = i - off;
	System.arraycopy( arr, off, buf, 0, j );
	while( i < last )
	  buf[j++] = Character.toLowerCase( arr[i++] );
	if( extHandle < 0 )
	  {
	    matchOff = off;
	    super.init( buf, 0, buf.length );
	  }
	else
	  {
	    extOff[extHandle] = 0;
	    super.setExtVariableBuffer( extHandle, buf );
	  }
      }
  }

  public int getIndex( int handle )
  {
    int eh = handleToExtHandle[handle];
    if( eh < 0 )
      return super.getIndex( handle ) + matchOff;
    else
      return super.getIndex( handle ) + extOff[eh];
  }

  public void setIndex( int handle, int index )
  {
    int eh = handleToExtHandle[handle];
    if( eh < 0 )
      super.setIndex( handle, index - matchOff );
    else
      super.setIndex( handle, index - extOff[eh] );
  }

  public int getMatchStart()
  {
    return super.getMatchStart() + matchOff;
  }

  public int getMatchEnd()
  {
    return super.getMatchEnd() + matchOff;
  }

  public char[] getCharBuffer( int extHandle )
  {
    if( extHandle < 0 )
      return cMatch;
    return (char[])extBuf[extHandle];
  }

  public void setExtVariableBuffer( int extHandle, char[] arr )
  {
    init( extHandle, arr, 0, arr.length );
  }

  public String toString()
  {
    return super.toString()+"i";
  }
}
