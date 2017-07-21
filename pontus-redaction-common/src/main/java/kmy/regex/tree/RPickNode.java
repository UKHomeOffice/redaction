/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.tree;

import java.util.Dictionary;

public class RPickNode extends RNode
{

  public String   name;
  public boolean  begin;
  public boolean  referenced;

  public RPickNode( int pos, String name, boolean begin )
  {
    super( pos );
    this.name = name;
    this.begin = begin;
    minLength = 0;
    maxLength = 0;
  }  

  public int getNCells()
  {
    return super.getNCells() + 1;
  }

  public CharSet findPrefix( CharSet tailPrefix )
  {
    if( tail == null )
      prefix = tailPrefix;
    else
      {
	tail.findPrefix( tailPrefix );
	prefix = tail.prefix;
      }
    return prefix;
  }

  public boolean isStartAnchored()
  {
    if( tail == null )
      return false;
    return tail.isStartAnchored();
  }

  public boolean hasPicks()
  {
    if( name != null )
      return true;
    return super.hasPicks();
  }
  
  public void collectReferences( Dictionary refList, Dictionary pickList )
  {
    if( name.length() > 0 )
      pickList.put( name, this );
    super.collectReferences( refList, pickList );
  }

  public RNode markReferenced( Dictionary refList, Dictionary pickList,
			     boolean collapse )
  {
    referenced = refList.get( name ) != null;
    if( name.length() == 0 || (!referenced && collapse && Character.isDigit(name.charAt(0))) )
      if( tail == null )
	return null;
      else
	return tail.markReferenced( refList, pickList, collapse );
    return super.markReferenced( refList, pickList, collapse );
  }

  public Object eval( RContext context )
  {
    return context.evalRPick( this );
  }
}
