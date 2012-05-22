package natlab.tame.builtin.shapeprop.ast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import natlab.tame.builtin.shapeprop.ShapePropMatch;
import natlab.tame.valueanalysis.value.Args;
import natlab.tame.valueanalysis.value.Value;

public class SPMult extends SPAbstractMatchExpr
{
	static boolean Debug = true;
	SPAbstractMatchExpr sp;
	public SPMult (SPAbstractMatchExpr sp)
	{
		this.sp = sp;
		//System.out.println("*");
	}
	
	public ShapePropMatch match(boolean isPatternSide, ShapePropMatch previousMatchResult, List<? extends Value<?>> argValues)
	{
		while(argValues.size()>(previousMatchResult.getNumMatched()))
		{
			if (Debug) System.out.println("inside star loop "+previousMatchResult.getNumMatched());
			if (Debug) System.out.println(previousMatchResult.getNumMatched());
			if (Debug) System.out.println("index doesn't point null, keep matching!");
			previousMatchResult = sp.match(isPatternSide, previousMatchResult, argValues);
		}
		
		return previousMatchResult;
	}
	
	public String toString()
	{
		return sp.toString()+"*";
	}
}