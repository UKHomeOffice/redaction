/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.tree;

import java.util.Dictionary;

public class RRepeatNode extends RNode
{
  public RNode body;
  public int min;
  public int max;
  public boolean greedy;

  public RRepeatNode( int pos, RNode body )
  {
    this( pos, 0, Integer.MAX_VALUE, body, true );
  }

  public RRepeatNode( int pos, int min, int max, RNode body, boolean greedy )
  {
    super( pos );
    this.min = min;
    this.max = max;
    this.body = body;
    this.greedy = greedy;
  }

  public int getNCells()
  {
    return super.getNCells() + body.getNCells() + (min>1?1:0) + (max<Integer.MAX_VALUE&&max!=min?1:0);
  }

  public void prepare( int addMaxLeft, int addMinLeft )
  {
    if( tail != null )
      {
	tail.prepare( addMaxLeft, addMinLeft );
	addMaxLeft = tail.maxLeft;
	addMinLeft = tail.minLeft;
      }

    body.prepare( 0, 0 ); // this will only calculate lengths

    int minl = minTotalLength( body );
    minLength = min*minl;
    int maxl = maxTotalLength( body );
    if( maxl < Short.MAX_VALUE && max < Short.MAX_VALUE )
      maxLength = maxl*max;
    else
      maxLength = Integer.MAX_VALUE;

    if( addMaxLeft < Integer.MAX_VALUE && maxLength < Integer.MAX_VALUE &&
       addMaxLeft + maxLength > 0 )
      body.prepare( addMaxLeft + maxLength - maxl, addMinLeft + minLength - minl );
    else
      body.prepare( Integer.MAX_VALUE, addMinLeft + ( min>1 ? (min-1)*minl : 0 ) );

    finishPrepare( addMaxLeft, addMinLeft );
  }

  public CharSet findPrefix( CharSet tailPrefix )
  {
    if( tail != null )
      tailPrefix = tail.findPrefix( tailPrefix );
    prefix = body.findPrefix( tailPrefix ).merge( tailPrefix );
    CharSet set = body.findPrefix( prefix );
    if( min > 0 )
      prefix = set;
    return prefix;
  }

  public void toLowerCase()
  {
    super.toLowerCase();
    body.toLowerCase();
  }

  public boolean hasPicks()
  {
    if( body.hasPicks() )
      return true;
    return super.hasPicks();
  }
  
  public boolean hasForks()
  {
    return true;
  }
  
  public void collectReferences( Dictionary refList, Dictionary pickList )
  {
    body.collectReferences( refList, pickList );
    super.collectReferences( refList, pickList );
  }

  public RNode markReferenced( Dictionary refList, Dictionary pickList, boolean collapse )
  {
    body = body.markReferenced( refList, pickList, collapse );
    return super.markReferenced( refList, pickList, collapse );
  }

  public Object eval( RContext context )
  {
    return context.evalRRepeat( this );
  }
}





