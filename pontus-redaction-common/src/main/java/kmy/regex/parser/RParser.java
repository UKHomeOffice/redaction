/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.parser;

import java.io.*;
import java.util.*;

import kmy.regex.tree.*;
import kmy.regex.compiler.*;
import kmy.regex.interp.*;
import kmy.regex.util.Regex;
import kmy.regex.util.RegexRefiller;
import kmy.regex.jvm.*;

import kmy.jint.util.CompilerException;
import kmy.jint.constants.MiniErrorCodes;

/**
 *  Regular expression parser.
 *  <p>
 *  TODO:
 *  <ul>
 *  <li>possibly add syntax flags (perl/sed/POSIX style, etc.)
 *  </ul>
 */
public class RParser implements CharClassCodes, MiniErrorCodes
{

  private RNode head;
  private RNode tail;
  private int varCounter;
  private Hashtable refList;
  private Hashtable pickList;
  private int syntax;
  private boolean lowerCase;

  public final static int FILE_PATTERN_SYNTAX = 0x1000;

  public RParser()
  {
  }  

  public RParser( int syntax, boolean lowerCase )
  {
    this.syntax = syntax;
    this.lowerCase = lowerCase;
  }

  private void append( RNode node )
  {
    if( node == null )
      return;

    if( head == null )
      {
	head = node;
	tail = node;
      }
    else
      tail.tail = node;

    while( tail.tail != null )
      tail = tail.tail;
  }
      
  public RNode parse( String regex )
  {
    return parse( regex.toCharArray() );
  }

  public RNode parse( String regex, boolean noimplicit )
  {
    char[] arr = regex.toCharArray();
    return parse( arr, 0, arr.length, noimplicit );
  }

  public RNode parse( char[] regex )
  {
    return parse( regex, 0, regex.length, false );
  }

  public RNode parse( char[] regex, int offset, int length, boolean noimplicit )
  {
    int indexIn = offset + length;
    int index;
    if( (syntax & FILE_PATTERN_SYNTAX) == 0 )
      index = parseImpl( regex, offset, indexIn );
    else
      index = parseFilePattern( regex, offset, indexIn, false );
    if( index < indexIn )
      throw new CompilerException( ERR_R_EXTRABRACKET, index );
    RNode res = head;
    if( lowerCase )
      res.toLowerCase();
    refList = new Hashtable();
    pickList = new Hashtable();
    res.collectReferences( refList, pickList );
    res = res.markReferenced( refList, pickList, noimplicit );
    res.prepare( 0, 0 );
    head = null;
    tail = null;
    varCounter = 0;
    return res;
  }

  public String[] getRefList()
  {
    int size = refList.size();
    if( size == 0 )
      return null;
    Vector acc = new Vector();
    Enumeration refs = refList.elements();
    for( int i = 0 ; i < size ; i++ )
      {
	RSubstNode sn = (RSubstNode)refs.nextElement();
	if( sn.picked || Character.isDigit(sn.var.charAt(0)) )
	  continue;
	acc.addElement( sn.var );
      }
    if( acc.size() == 0 )
      return null;
    String[] res = new String[acc.size()];
    acc.copyInto(res);
    return res;
  }

  public String[] getPickList()
  {
    int size = pickList.size();
    if( size == 0 )
      return null;
    String[] res = new String[size];
    Enumeration picks = pickList.keys();
    for( int i = 0 ; i < size ; i++ )
      res[i] = (String)picks.nextElement();
    return res;
  }

  public int parseFilePattern( char[] regex, int index, int maxIndex, boolean inner )
  {
    // Loop through all characters and process them
    // through the big switch statement. Subpatterns (inside 
    // brackets) are processed recursively.
    for( int i = index ; i < maxIndex ; i++ )
      {
	char c = regex[i];
	int pos = i;
	switch( c )
	  {
	    case '*' :
	      append( new RRepeatNode( pos, (c=='*'?0:1),
				       Integer.MAX_VALUE, new RAnyNode(pos), false ) );
	      continue;
	    case '?' :
	      append( new RAnyNode( pos ) );
	      continue;
	    case '[' :
	      i = parseCharClass( regex, i, maxIndex );
	      append( charClassNode );
	      continue;
	    case '{' :
	      {
		int start = i;
		RNode saveHead = head;
		RNode saveTail = tail;
		head = null;
		tail = null;
		i = parseFilePattern( regex, i+1, maxIndex, true );
		if( i >= maxIndex || regex[i] != '}' )
		  throw new CompilerException( ERR_R_NOCURLBRACKET, start );
		RNode p = head;
		head = saveHead;
		tail = saveTail;
		append( p );
	      }
	      continue;
	    case '\\' :
	      {
		i = parseBackslash( regex, i, maxIndex, true );
		if( backslashNode != null )
		  append( backslashNode );
	      }
	      continue;
	    case ',' :
	      if( inner )
		{
		  RNode alt1 = head;
		  head = null;
		  tail = null;
		  i = parseFilePattern( regex, i+1, maxIndex, true )-1;
		  RNode r = head;
		  head = null;
		  tail = null;
		  append( new RAltNode( pos, alt1, r ) );
		}
	      else
		append( new RConstNode( pos, ',' ) );
	      continue;
	    case '}' :
	      if( inner )
		return i;
	      append( new RConstNode( pos, '}' ) );
	      continue;
	    default :
	      append( new RConstNode( pos, c ) );
	      continue;
	  }
      }
    return maxIndex;
  }

  public int parseImpl( char[] regex, int index, int maxIndex )
  {
    RNode prev = null;
    String vname = null;
    // Loop through all characters and process them
    // through the big switch statement. Subpatterns (inside 
    // brackets) are processed recursively.
    for( int i = index ; i < maxIndex ; i++ )
      {
	char c = regex[i];
	int pos = i;
	switch( c )
	  {
	    case '*' :
	    case '+' :
	    case '?' :
	      {
		if( prev == null )
		  {
		    Object[] addInfo = { "'" + c + "'" };
		    throw new CompilerException( ERR_R_BADSTART, index, addInfo );
		  }
		boolean greedy = i+1 >= maxIndex || regex[i+1] != '?';
		if( ! greedy )
		  i++;
		if( c == '?' )
		  if( greedy )
		    prev = new RAltNode( pos, prev, null );
		  else
		    prev = new RAltNode( pos, null, prev );
		else
		  prev = new RRepeatNode( pos, (c=='*'?0:1), Integer.MAX_VALUE, prev, greedy );
	      }
	      continue;
	    case '.' :
	      append( prev );
	      prev = new RAnyNode(pos);
	      continue;
	    case '{' :
	      {
		int j = ++i;
		int comma = -1;
		while( true )
		  {
		    if( i >= maxIndex )
		      throw new CompilerException( ERR_R_NOCURLBRACKET, (j-1) );
		    c = regex[i];
		    if( c == '}' )
		      break;
		    if( c == ',' )
		      comma = i;
		    i++;
		  }
		int min;
		int max;
		if( comma == -1 )
		  {
		    min = Integer.parseInt( new String( regex, j, i-j ) );
		    max = min;
		  }
		else
		  {
		    min = Integer.parseInt( new String( regex, j, comma-j ) );
		    if( comma == i-1 )
		      max = Integer.MAX_VALUE;
		    else
		      max = Integer.parseInt( new String( regex, comma+1, i-comma-1 ) );
		  }
		boolean greedy = i+1 >= maxIndex || regex[i+1] != '?';
		if( ! greedy )
		  i++;
		prev = new RRepeatNode( pos, min, max, prev, greedy );
	      }
	      break;
	    case ')' :
	      append( prev );
	      return i;
	    case '@' :
	      {
		int start = i;
		i = parseVariable( regex, i, maxIndex ) + 1;
		if( i >= maxIndex || regex[i] != '(' )
		  throw new CompilerException( ERR_R_NOVAREXPR, start );
		vname = varName;
	      }
	      // fall through
	    case '(' :
	      {
		int start = i;
		int lookAheadFlag = 0;
		append( prev );
		RNode saveHead = head;
		RNode saveTail = tail;
		head = null;
		tail = null;
		String var;
		if( vname != null )
		  {
		    var = vname;
		    vname = null;
		  }
		else
		  {
		    // see if extended construction '(?...)'
		    if( i+1 < maxIndex && regex[i+1] == '?' )
		      {
			// yes, this is an extended construction
			i += 2;
			if( i >= maxIndex )
			  throw new CompilerException( ERR_R_BQUNFINISHED, start );
			switch( regex[i] )
			  {
			    case ':' :
			      // non-saving grouping
			      var = "";
			      break;
			    case '=' :
			    case '!' :
			      var = "";
			      lookAheadFlag = regex[i];
			      break;
			    default :
			      throw new CompilerException( ERR_R_BQBAD, start );
			  }
		      }
		    else
		      var = Integer.toString( ++varCounter );
		  }
		i = parseImpl( regex, i+1, maxIndex );
		if( i >= maxIndex || regex[i] != ')' )
		  throw new CompilerException( ERR_R_NOBRACKET, start );
		if( lookAheadFlag != 0 )
		  {
		    prev = new RLookAheadNode( pos, head, lookAheadFlag == '=' );
		  }
		else
		  {
		    append( new RPickNode( pos, var, false ) );
		    prev = new RPickNode( pos, var, true );
		    prev.tail = head;
		  }
		head = saveHead;
		tail = saveTail;
	      }
	      continue;
	    case '^' :
	    case '<' :
	    case '>' :
	      append( prev );
	      prev = new RBoundaryNode( pos, c );
	      break;
	    case '|' :
	      {
		append( prev );
		RNode alt1 = head;
		head = null;
		tail = null;
		i = parseImpl( regex, i+1, maxIndex )-1;
		prev = new RAltNode( pos, alt1, head );
		head = null;
		tail = null;
	      }
	      continue;
            case '$' :
	      {
		append( prev );
		if( i+1 >= maxIndex || (c = regex[i+1]) == ')' || c == '|' )
		  {
		    prev = new RBoundaryNode( pos, '$' );
		    continue;
		  }
		i = parseVariable( regex, i, maxIndex );
		prev = new RSubstNode( pos, varName );
	      }
	      continue;
	    case '[' :
	      append( prev );
	      i = parseCharClass( regex, i, maxIndex );
	      prev = charClassNode;
	      continue;
	    case '\\' :
	      {
		i = parseBackslash( regex, i, maxIndex, false );
		if( backslashNode != null )
		  {
		    append( prev );
		    prev = backslashNode;
		  }
	      }
	      continue;
	    default :
	      {
		append( prev );
		prev = new RConstNode( pos, c );
	      }
	      continue;
	  }
      }
    append( prev );
    return maxIndex;
  }

  private String varName;

  private int parseVariable( char[] regex, int i, int maxIndex )
  {
    int start = ++i;
    if( i >= maxIndex )
      throw new CompilerException( ERR_R_NOVARNAME, (i-1) );
    char c = regex[i];
    if( c != '{' )
      {
	while( true )
	  {
	    if( i >= maxIndex )
	      break;
	    if( !Character.isLetterOrDigit( regex[i] ) && regex[i] != '_' )
	      break;
	    i++;
	  }
	varName = new String( regex, start, i-start );
      }
    else
      {
	do
	  {
	    c = regex[i++];
	    if( i >= maxIndex )
	      throw new CompilerException( ERR_R_NOCURLBRACKET, start );
	  }
	while( c != '}' );
	varName = new String( regex, start+1, i-start-2 );
      }
    return i - 1;
  }

  private RNode     charClassNode;
  
  private int parseCharClass( char[] regex, int i, int maxIndex )
  {
    boolean neg = false;
    int pos = i;
    char c;
    int j = ++i;
    if( j < maxIndex && regex[j] == '^' )
      {
	neg = true;
	j++;
      }
    do
      {
	if( ++i >= maxIndex )
	  throw new CompilerException( ERR_R_NOSQBRACKET, j );
	c = regex[i];
      }
    while( c != ']' );
    char[] buf = new char[2*(i-j)];
    char ri = 0;
    while( j < i )
      {
	c = regex[j];
	if( c == '\\' )
	  {
	    j = parseBackslash( regex, j, maxIndex, true ) + 1;
	    if( backslashTranslation < 0 )
	      continue;
	    c = (char)backslashTranslation;
	  }
	else
	  j++;
	buf[ri++] = c;
	if( regex[j] != '-' )
	  {
	    buf[ri++] = c;
	    continue;
	  }
	j++;
	while( true )
	  {
	    c = regex[j];
	    if( c == '\\' )
	      {
		j = parseBackslash( regex, j, maxIndex, true ) + 1;
		if( backslashTranslation < 0 )
		  continue;
		c = (char)backslashTranslation;
	      }
	    else
	      j++;
	    break;
	  }
	if( c == ']' )
	  {
	    c = buf[ri-1];
	    buf[ri++] = c;
	    buf[ri++] = '-';
	    buf[ri++] = '-';
	    break;
	  }
	buf[ri++] = c;
      }
    if( ri < buf.length )
      {
	char[] newbuf = new char[ri];
	System.arraycopy( buf, 0, newbuf, 0, ri );
	buf = newbuf;
      }
    charClassNode = new RCharClassNode( pos, neg, buf );
    return i;
  }

  private RNode     backslashNode;
  private int       backslashTranslation;

  private int parseBackslash( char[] regex, int i, int maxIndex, boolean inRange )
  {
    int pos = i;
    i++;
    if( i >= maxIndex )
      throw new CompilerException( ERR_R_STRAYBSLASH, i );
    char c = regex[i];
    backslashNode = null;
    backslashTranslation = -1;
    switch( c )
      {
        case '\n' :
	  return i;
        case 'c' :
	  if( ++i >= maxIndex )
	    throw new CompilerException( ERR_R_CTLUNFINISHED, i );
	  c = regex[i];
	  if( '@' > c || '_' < c )
	    throw new CompilerException( ERR_R_CTLINVALID, i );
	  c -= '@';
	  break;
        case 'n' :
	  c = '\n';
	  break;
	case 't' :
	  c = '\t';
	  break;
	case 'r' :
	  c = '\r';
	  break;
	case 'f' :
	  c = (char)0x0c;
	  break;
	case 'u' :
	case 'x' :
	  {
	    int ndigit = ( c == 'x' ? 2 : 4 );
	    if( i+ndigit >= maxIndex )
	      throw new CompilerException( ERR_R_HEXUNFINISHED, i );
	    String num = new String( regex, i+1, ndigit );
	    try
	      {
		c = (char)Integer.parseInt( num, 16 );
	      }
	    catch( NumberFormatException e )
	      {
		Object[] addInfo = { Integer.toString(ndigit), num };
		throw new CompilerException( ERR_R_HEXBADNUMBER, i );
	      }
	    i += ndigit;
	  }
	  break;
	case 'w' :
	case 'W' :
	case 's' :
	case 'S' :
	case 'i' :
	case 'I' :
	case 'd' :
	case 'D' :
	  if( !inRange )
	    {
	      backslashNode = new RCharClassNode( pos, CharSet.decode(c) );
	      return i;
	    }
	  break;
	case 'b' :
	case 'B' :
	case 'A' :
	case 'Z' :
	  if( !inRange )
	    {
	      backslashNode = new RBoundaryNode( pos, c );
	      return i;
	    }
	  else
	    if( c == 'b' )
	      c = (char)0x08; // backspace
	  break;
	default:
	  if( '0' <= c && c <= '9' )
	    {
	      if( c == '0' )
		i++; // backreference cannot start with 0
	      else
		{
		  if( !inRange )
		    {
		      // see if backreference
		      int first = i; // first digit character
		      while( ++i < maxIndex )
			{
			  char c1 = regex[i];
			  if( '0' > c1 || '9' < c1 )
			    break;
			}
		      String decimalString = new String( regex, first, i-first );
		      int decimal = Integer.parseInt( decimalString );
		      if( decimal <= varCounter )
			{
			  // yes, it is backreference
			  backslashNode = new RSubstNode( pos, decimalString );
			  return i-1;
			}
		      else
			i = first;
		    }
		  if( c > '7' )
		    {
		      if( !inRange )
			throw new CompilerException( ERR_R_BADBACKREF, i );
		      // \9 in character class would be just 9
		      break;
		    }
		}
	      c = 0;
	      int j = 0;
	      while( i < maxIndex && j < 3 )
		{
		  char chr = regex[i];
		  if( '0' <= chr && chr <= '7' )
		    {
		      c = (char)((c<<3)+(chr-'0'));
		      i++;
		    }
		  else
		    break;
		}
	      i--;
	    }
	  break;
      }
    if( inRange )
      backslashTranslation = c;
    else
      backslashNode = new RConstNode( pos, c );
    return i;
  }

}
