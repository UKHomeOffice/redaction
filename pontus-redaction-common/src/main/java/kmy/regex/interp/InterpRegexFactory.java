/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.interp;

import kmy.regex.util.Regex;
import kmy.regex.util.RegexFactory;
import kmy.regex.tree.RNode;
import kmy.regex.parser.RParser;
import kmy.regex.compiler.RMachine;
import kmy.regex.compiler.RCompiler;
import kmy.regex.interp.RInterpMachine;

import kmy.jint.util.CompilerException;

public class InterpRegexFactory extends RegexFactory
{

  public InterpRegexFactory()
  {
  }

  protected Regex createRegex( char[] arr, int off, int len, 
			   boolean lowerCase, boolean filePattern )
  {
    RParser parser = new RParser( 
		(filePattern ? RParser.FILE_PATTERN_SYNTAX : 0), lowerCase );
    RNode regexNode = parser.parse( arr, off, len, false );
    RMachine machine = new RInterpMachine();
    RCompiler comp = new RCompiler( machine );
    String name;
    if( len < 28 )
      name = new String( arr, off, len );
    else
      name = new String( arr, off, 28 );
    comp.compile( regexNode, name );
    return machine.makeRegex();
  }
}
