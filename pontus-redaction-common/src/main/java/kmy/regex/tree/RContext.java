/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.tree;

public abstract class RContext
{
  public abstract Object evalRAlt( RAltNode regexAlt );
  public abstract Object evalRAny( RAnyNode regexAny );
  public abstract Object evalRBoundary( RBoundaryNode regexBoundary );
  public abstract Object evalRConst( RConstNode regexConst );
  public abstract Object evalRCharClass( RCharClassNode regexCharClass );
  public abstract Object evalRLookAhead( RLookAheadNode regexLookAhead );
  public abstract Object evalRPick( RPickNode regexPick );
  public abstract Object evalRRepeat( RRepeatNode regexRepeat );
  public abstract Object evalRSubst( RSubstNode regexSubst );
}
