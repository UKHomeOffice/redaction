/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.jvm;

public abstract class RLocalAllocator
{
  public abstract int alloc();
  public abstract void free( int local );
}
