/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.tools;

import java.io.*;
import kmy.regex.util.Regex;

public class Dir
{
  public static void main( String[] args )
  {
    Regex filter = Regex.createFilePattern( args[0] );
    File f = new File( "." );
    String[] list = f.list( filter );
    for(int i = 0 ; i < list.length ; i++ )
      System.out.println( list[i] );
  }
}

