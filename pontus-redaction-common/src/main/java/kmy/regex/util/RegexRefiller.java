/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.util;

public abstract class RegexRefiller
{
  /**
   *  Read more characters into regex's buffer and hand (possibly new) buffer
   *  and character range to the Regex. Character range start must remain the
   *  same. It also can change regex's refiller (setting it to null if buffer
   *  cannot be refilled).
   *  @returns new value for buffer boundary
   */
  public abstract int refill( Regex regex, int boundary );
}
