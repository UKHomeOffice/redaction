/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.tree;

public class CharSet implements CharClassCodes
{
  public int charClass;
  public char[] ranges;

  static char[] smallRange = { (char)0, (char)0x7F };
  static char[] spaceRange = { ' ', ' ', '\t', '\t', '\r', '\r', '\n', '\n' };
  static char[] identRange  = { '0', '9', 'A', 'Z', 'a', 'z', '_', '_' };
  static char[] wordRange  = { 'A', 'Z', 'a', 'z', '_', '_' };
  static char[] digitRange  = { '0', '9' };

  public static CharSet FULL_CHARSET = new CharSet( smallRange, CLASS_ALL );

  public static CharSet IDENT_CHARSET     = new CharSet( identRange, CLASS_LETTER );
  public static CharSet NONIDENT_CHARSET  = IDENT_CHARSET.negate();
  public static CharSet SPACE_CHARSET     = new CharSet( spaceRange, CLASS_DISABLED );
  public static CharSet NONSPACE_CHARSET  = SPACE_CHARSET.negate();
  public static CharSet WORD_CHARSET      = new CharSet( wordRange, CLASS_LETTER );
  public static CharSet NONWORD_CHARSET   = WORD_CHARSET.negate();
  public static CharSet DIGIT_CHARSET     = new CharSet( digitRange, CLASS_DISABLED );
  public static CharSet NONDIGIT_CHARSET  = DIGIT_CHARSET.negate();

  public CharSet( char[] ranges, int charClass )
  {
    try
      {
	this.charClass = charClass;
	this.ranges = normalize( ranges );
      }
    catch( RuntimeException e )
      {
	e.printStackTrace();
	throw e;
      }
  }

  public CharSet( char[] ranges )
  {
    this( ranges, CLASS_DISABLED );
  }

  public CharSet( char c )
  {
    ranges = new char[2];
    ranges[0] = c;
    ranges[1] = c;
    charClass = CLASS_DISABLED;
  }

  public CharSet( int charClass )
  {
    this( null, charClass );
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append( "CharSet[" );
    if( (charClass & CLASS_UPPERCASE) != 0 )
      sb.append( "upper," );
    if( (charClass & CLASS_LOWERCASE) != 0 )
      sb.append( "lower," );
    if( (charClass & CLASS_NONLETTER) != 0 )
      sb.append( "non," );
    if( ranges != null )
      {
	for( int i = 0 ; i < ranges.length ; i += 2 )
	  {
	    char cf = ranges[i];
	    char cl = ranges[i+1];
	    if( cf > ' ' && cf <= '~' )
	      sb.append( cf );
	    else
	      sb.append( "\\u" + Integer.toHexString(cf) );
	    if( cf != cl )
	      {
		sb.append( '-' );
		if( cl > ' ' && cl <= '~' )
		  sb.append( cl );
		else
		  sb.append( "\\u" + Integer.toHexString(cl) );
	      }
	  }
      }
    sb.append( "]" );
    return sb.toString();
  }

  public CharSet negate()
  {
    try
      {
	char[] in = ranges;
	char[] res;
	int resClass;
	char maxChar;
	if( charClass == CLASS_DISABLED )
	  {
	    maxChar = (char)0xFFFF;
	    resClass = CLASS_DISABLED;
	  }
	else
	  {
	    maxChar = (char)0x7F;
	    resClass = (~charClass)&CLASS_ALL;
          }
	if( in != null && in.length > 0 )
	  {
	    if( in[in.length-1] > maxChar )
	      throw new RuntimeException( "Negation of this set is not supported:" + this );
	    int size = in.length + (in[0]==0?-1:1) + (in[in.length-1]==maxChar?-1:1);
	    if( size == 0 )
	      res = null;
	    else
	      {
		if( (size&1) != 0 )
		  throw new RuntimeException( "Assertion failed: odd size: " + size );
		res = new char[size];
		int i = 0;
		int j = 0;
		if( in[i] != 0 )
		  res[j++] = 0;
		else
		  i++;
		while( i < in.length )
		  if( (i&1) == 0 )
		    {
		      // start of original range - end of new range
		      res[j++] = (char)(in[i++]-1);
		    }
		  else
		    {
		      // end of original range - start of new range
		      if( in[i] != maxChar )
			res[j++] = (char)(in[i++]+1);
		    }
		if( j < size )
		  res[j++] = maxChar;
		if( j != size )
		  throw new RuntimeException( "Internal error: assertion failed for: " + this );
	      }
	  }
	else
	  {
	    res = new char[2];
	    res[0] = 0;
	    res[1] = (char)maxChar;
	  }
	return new CharSet( res, resClass );
      }
    catch( RuntimeException e )
      {
	e.printStackTrace();
	throw e;
      }
  }

  public CharSet merge( CharSet other )
  {
    return merge( this, other );
  }

  public CharSet toLowerCase()
  {
    if( ranges == null )
      return this;
    
    char[] rng = new char[ranges.length];

    // the following might not work very well!!!
    for( int i = 0 ; i < rng.length ; i++ )
      rng[i] = Character.toLowerCase( ranges[i] );

    return new CharSet( rng, charClass );
  }

  public int complexity()
  {
    return (ranges == null ? 0 : ranges.length ) + 
      ( charClass == CLASS_ALL || charClass == CLASS_NONE || charClass == CLASS_DISABLED ? 0 : 10 );
  }

  public boolean isSingleChar()
  {
    return (charClass == CLASS_NONE || charClass == CLASS_DISABLED ) && ranges.length == 2 &&
      ranges[0] == ranges[1];
  }

  //------ merging and normalizing

  public static char[] normalize( char[] range )
  {
    if( range == null )
      return null;

    // check if it is OK
    int k;
    for( k = 0 ; k < range.length ; k += 2 )
      {
	if( range[k] > range[k+1] )
	  throw new IllegalArgumentException( "Inversed range" );
	if( k+2 < range.length && range[k+1] >= range[k+2]-1 )
	  break;
      }
    if( k == range.length )
      return range;

    char[] r = new char[range.length];
    System.arraycopy( range, 0, r, 0, r.length );

    // sort it first
    for( int i = 0 ; i < r.length ; i += 2 )
      for( int j = i+2 ; j < r.length ; j += 2 )
	if( r[i] > r[j] )
	  {
	    char tmp = r[i];
	    r[i] = r[j];
	    r[j] = tmp;
	    tmp = r[i+1];
	    r[i+1] = r[j+1];
	    r[j+1] = tmp;
	  }

    // remove overlaps
    int j = 0;
    int i = 0;
    while( i < r.length )
      {
	r[j++] = r[i++];
	char c = r[i++]; // end of this span
	while( i < r.length && c+1 >= r[i] )
	  {
	    i++;
	    c = ( c > r[i] ? c : r[i] ); // end of the next span
	    i++;
	  }
	r[j++] = c;
	//System.out.println( "Range: " + (int)r[j-2] + "-" + (int)r[j-1] );
      }

    if( j != r.length )
      {
	char[] newRange = new char[j];
	System.arraycopy( r, 0, newRange, 0, j );
	r = newRange;
      }

    return r;    
  } 

  public static CharSet merge( CharSet set1, CharSet set2 )
  {
    int charClass = set1.charClass | set2.charClass;
    if( charClass != CLASS_DISABLED )
      charClass &= ~CLASS_DISABLED;
    char[] ranges = merge( set1.ranges, set2.ranges );
    CharSet m = new CharSet( ranges, charClass );
    //System.out.println( "Merged " + set1 + " " + set2 + " -> " + m );
    return m;
  }

  public static char[] merge( char[] range1, char[] range2 )
  {
    if( range1 == null )
      return range2;
    if( range2 == null )
      return range1;

    char[] range = new char[ range1.length + range2.length ];

    int i = 0;
    int i1 = 0;
    int i2 = 0;

    while( i1 < range1.length && i2 < range2.length )
      {
	int b;
	int e;
	char b1 = range1[i1];
	char e1 = range1[i1+1];
	char b2 = range2[i2];
	char e2 = range2[i2+1];
	if( b1 < b2 )
	  {
	    b = b1;
	    i1 += 2;
	    if( e1 < b2 )
	      e = e1;
	    else
	      {
		e = ( e1 > e2 ? e1 : e2 );
		i2 += 2;
	      }
	  }
	else 
	  {
	    // b1 >= b2
	    b = b2;
	    i2 += 2;
	    if( e2 < b1 )
	      e = e2;
	    else
	      {
		e = ( e1 > e2 ? e1 : e2 );
		i1 += 2;
	      }
	  }
	range[i++] = (char)b;
	range[i++] = (char)e;
      }

    if( i1 < range1.length )
      {
	System.arraycopy( range1, i1, range, i, range1.length-i1 );
	i += (range1.length-i1);
      }
    else
      if( i2 < range2.length )
	{
	  System.arraycopy( range2, i2, range, i, range2.length-i2 );
	  i += (range2.length-i2);
	}

    if( i != range.length )
      {
	char[] newRange = new char[i];
	System.arraycopy( range, 0, newRange, 0, i );
	range = newRange;
      }

    return range;
  }

  public static CharSet decode( char c )
  {
    switch( c )
      {
        case 'I' :
	  return NONIDENT_CHARSET;
        case 'i' :
	  return IDENT_CHARSET;
        case 'S' :
	  return NONSPACE_CHARSET;
        case 's' :
	  return SPACE_CHARSET;
        case 'w' :
	  return WORD_CHARSET;
        case 'W' :
	  return NONWORD_CHARSET;
        case 'd' :
	  return DIGIT_CHARSET;
        case 'D' :
	  return NONDIGIT_CHARSET;
	default :
	  throw new IllegalArgumentException( "Unknown char class code: " + c );
      }
  }
}
