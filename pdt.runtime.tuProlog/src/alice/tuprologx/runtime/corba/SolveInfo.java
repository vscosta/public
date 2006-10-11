package alice.tuprologx.runtime.corba;


/**
* org/alice/tuprologx/runtime/corba/SolveInfo.java
* Generated by the IDL-to-Java compiler (portable), version "3.0"
* from org/alice/tuprologx/runtime/corba/Prolog.idl
* venerd� 28 dicembre 2001 12.37.09 GMT+01:00
*/

public final class SolveInfo implements org.omg.CORBA.portable.IDLEntity
{
  public boolean success = false;
  public String solution = null;
  public boolean halt = false;
  public int haltCode = (int)0;

  public SolveInfo ()
  {
  } // ctor

  public SolveInfo (boolean _success, String _solution, boolean _halt, int _haltCode)
  {
    success = _success;
    solution = _solution;
    halt = _halt;
    haltCode = _haltCode;
  } // ctor

} // class SolveInfo
