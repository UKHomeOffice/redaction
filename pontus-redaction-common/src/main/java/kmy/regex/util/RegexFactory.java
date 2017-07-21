/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.util;

public abstract class RegexFactory
{
  protected abstract Regex createRegex( char[] arr, int off, int length,
				    boolean lowerCase, boolean patt );

  public Regex createRegex( String re )
  {
    char[] arr = re.toCharArray();
    return createRegex( arr, 0, arr.length, false, false );
  }  

  public Regex createRegex( String re, boolean ignoreCase )
  {
    if( ignoreCase )
      return new CaseInsensitiveRegex( re );
    else
      return createRegex( re );
  }  

  Regex createLowerCaseRegex( String re )
  {
    char[] arr = re.toCharArray();
    return createRegex( arr, 0, arr.length, true, false );
  }  

  public Regex createFilePattern( String re )
  {
    char[] arr = re.toCharArray();
    return createRegex( arr, 0, arr.length, false, true );
  }  
}
