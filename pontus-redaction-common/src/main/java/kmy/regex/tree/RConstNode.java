/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.tree;

public class RConstNode extends RNode
{

  public char c;

  public RConstNode( int pos, char c )
  {
    super( pos );
    this.c = c;
    minLength = 1;
    maxLength = 1;
    prefix = new CharSet( c );
  }

  public void toLowerCase()
  {
    char c1 = Character.toLowerCase( c );
    if( c1 != c )
      {
	c = c1;
	prefix = new CharSet( c );
      }
    super.toLowerCase();
  }

  public Object eval( RContext context )
  {
    return context.evalRConst( this );
  }
}
