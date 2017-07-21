/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.tree;

public class RCharClassNode extends RNode
{

  public CharSet charClass;

  public RCharClassNode( int pos, CharSet set )
  {
    super( pos );
    this.charClass = set;
    prefix = set;
    minLength = 1;
    maxLength = 1;
  }

  public RCharClassNode( int pos, boolean neg, char[] ranges )
  {
    super( pos );
    charClass = new CharSet( ranges );
    if( neg )
      charClass = charClass.negate();
    prefix = charClass;
    minLength = 1;
    maxLength = 1;
  }

  public void toLowerCase()
  {
    charClass = charClass.toLowerCase();
    super.toLowerCase();
  }

  public Object eval( RContext context )
  {
    return context.evalRCharClass( this );
  }
}
