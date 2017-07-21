/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.util;

import java.io.Writer;
import java.io.StringWriter;
import java.io.Serializable;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Vector;
import java.util.Enumeration;

import kmy.jint.lang.ArgList;
import kmy.jint.lang.CharString;

public class Replacer
{
  int flags;
  Regex      regex;
  Formatter  form;
  int[][]    subst;

  public static int GLOBAL_FLAG = 0x00000001; // same as TokenConst.TOK_OPT_GLOBAL !!!

  public Replacer( String patt, String form, int flags )
  {
    this.regex = Regex.createRegex( patt );
    this.form = new Formatter( form, true );
    this.flags = flags;
    init();
  }

  public Replacer( Regex patt, Formatter form, int flags )
  {
    this.regex = patt;
    this.form = form;
    this.flags = flags;
    init();
  }

  public Replacer( Regex patt, String form, int flags )
  {
    this.regex = patt;
    this.form = new Formatter( form, true );
    this.flags = flags;
    init();
  }

  private void init()
  {
    String[] vars = form.getVariables();
    if( vars != null )
      {
	subst = new int[vars.length][];
	for( int i = 0 ; i < vars.length ; i++ )
	  {
	    String var = vars[i];
	    // System.out.println("Variable: '" + var + "'" );
	    int bh = regex.getVariableHandle( var, true );
	    if( bh < 0 || regex.getExtVariableHandle( var ) >= 0 )
	      {
		//throw new RuntimeException( "Variable '" + var + "' not found in regex" );
	      }
	    else
	      {
		int eh = regex.getVariableHandle( var, false );
		int[] ref = { bh, eh };
		subst[i] = ref;
	      }
	  }
      }
  }

  public Replacer cloneReplacer()
  {
    try
      {
	Replacer rep = (Replacer)super.clone();
	rep.regex = regex.cloneRegex();
	return rep;
      }
    catch( CloneNotSupportedException e )
      {
	throw new RuntimeException( "Internal error: " + e );
      }
  }

  public Object clone()
  {
    return cloneReplacer();
  }

  static class Substitution
  {
    int           begin;
    int           end;
    Object[]      args;
  }

  public String process( String s )
  {
    char[] arr = s.toCharArray();
    int first = 0;
    int last = arr.length;
    String r = process( arr, first, last, null );
    if( r == null )
      return s;
    return r;
  }
  
  public CharString process( CharString s )
  {
    String r = process( s.buf, s.first, s.last, null );
    if( r == null )
      return s;
    return new CharString( r );
  }
  
  public char[] process( char[] arr )
  {
    String r = process( arr, 0, arr.length, null );
    if( r == null )
      return arr;
    return r.toCharArray();
  }
  
  public Object process( Object s )
  {
    if( s instanceof String )
      return process( (String)s );
    else if( s instanceof CharString )
      return process( (CharString)s );
    else
      return process( s.toString() );
  }
  
  public String process( char[] arr, int first, int last, Object[] extVars )
  {
    boolean global = ((flags & GLOBAL_FLAG) != 0 );
    regex.init( arr, first, last );
    Vector substList = null;
    int k = 0;
    while( regex.search() )
      {
	if( substList == null )
	  substList = new Vector();
	Substitution substitution = new Substitution();
	if( subst != null )
	  {
	    Object[] args = new Object[subst.length];
	    for( int i = 0 ; i < args.length ; i++ )
	      {
		int[] handles = subst[i];
		if( handles == null )
		  {
		    if( extVars == null )
		      throw new IllegalArgumentException( 
			"Need value for external variable " + form.getVariables()[i] );
		    args[i] = extVars[k++];
		  }
		else
		  {
		    int begin = regex.getIndex( handles[0] );
		    int end = regex.getIndex( handles[1] );
		    args[i] = new CharString( arr, begin, end-begin );
		  }
	      }
	    substitution.args = args;
	  }
	substitution.begin = regex.getMatchStart();
	substitution.end = regex.getMatchEnd();
	substList.addElement( substitution );
	if( ! global )
	  break;
      }
    if( substList == null )
      return null;
    StringWriter buf = new StringWriter();
    Enumeration sbt = substList.elements();
    do
      {
	Substitution substitution = (Substitution)sbt.nextElement();
	int begin = substitution.begin;
	if( begin > first )
	  buf.write( arr, first, begin-first );
	form.printf( buf, new ArgList( substitution.args ) );
	first = substitution.end;	
      }
    while( sbt.hasMoreElements() );
    if( last > first )
      buf.write( arr, first, last-first );
    return buf.toString();
  }

  public Regex getRegex()
  {
    return regex;
  }

  public static void main( String[] args )
  {
    Replacer rep = new Replacer( args[0], args[1], GLOBAL_FLAG );
    BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
    String s;
    try
      {
	while( ( s = in.readLine() ) != null )
	  {
	    System.out.println( rep.process( s ) );
	  }
      }
    catch( Exception e )
      {
	e.printStackTrace();
      }
  }

}

