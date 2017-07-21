/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.jvm;

import kmy.jint.gen.*;
import java.io.IOException;

abstract public class RJavaClassCustomizer
{
  public void customInitAction( CodeGenerator gen ) throws IOException
  {
  }

  public void customConstructorAction( CodeGenerator gen ) throws IOException
  {
  }

  public void customSuccessAction( CodeGenerator gen, int v_buf, int v_cells )
    throws IOException
  {
  }

  public void customMembers( CodeGenerator gen ) throws IOException
  {
  }

  public String customConstructorSignature()
  {
    return "()V";
  }

}
