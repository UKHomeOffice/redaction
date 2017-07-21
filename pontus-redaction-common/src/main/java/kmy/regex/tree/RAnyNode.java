/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.tree;

public class RAnyNode extends RNode
{

  public RAnyNode( int pos )
  {
    super( pos );
    minLength = 1;
    maxLength = 1;
  }  

  public Object eval( RContext context )
  {
    return context.evalRAny( this );
  }
}
