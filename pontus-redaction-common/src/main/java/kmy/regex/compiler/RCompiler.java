/*
    Copyright (c) Peter Sorotokin, 1998-2000
    See file "License.html" for terms of usage and
    redistribution.   
*/
package kmy.regex.compiler;

import kmy.regex.tree.*;

/**
 * This class compiles regex tree (RNode) into regex instructions. Regex
 * instructions are represented by calls to corresponding methods
 * of RMachine class.
 */
public class RCompiler extends RContext {

    /**
     * Do not generate a check that we have enough
     * characters if we need minimum MIN_CHAR_LEFT chars.
     */
    static final int MIN_CHAR_LEFT = 0;

    static final int CONDJUMP_COMPLEXITY = 6;

    RMachine gen;

    boolean charStarHead = false;

    public RCompiler() {
        this(new RDebugMachine());
    }

    public RCompiler(RMachine gen) {
        this.gen = gen;
    }

    private Object evalTail(RNode regex) {
        regex = regex.tail;
        if (regex != null)
            regex.eval(this);
        return null;
    }

    public void genShiftTable(RConstNode regexConst) {
        StringBuffer sb = new StringBuffer();
        while (true) {
            sb.append(regexConst.c);
            if (regexConst.tail instanceof RConstNode)
                regexConst = (RConstNode) regexConst.tail;
            else
                break;
        }
        char[] charArr = sb.toString().toCharArray();
        int nUnique = 0;
        char[] sorted = new char[charArr.length];
        System.arraycopy(charArr, 0, sorted, 0, charArr.length);
        for (int i = 0; i < sorted.length; i++) {
            char c = sorted[i];
            for (int j = i + 1; j < sorted.length; j++)
                if (c > sorted[j]) {
                    sorted[i] = sorted[j];
                    sorted[j] = c;
                    c = sorted[i];
                }
            if (i == 0 || c != sorted[i - 1])
                nUnique++;
        }
        char[] chars;
        if (nUnique == sorted.length)
            chars = sorted;
        else {
            chars = new char[nUnique];
            int j = 0;
            int i = 0;
            for (; j < nUnique; i++) {
                char c = sorted[i];
                if (i == 0 || c != sorted[i - 1])
                    chars[j++] = c;
            }
        }
        int[] indexes = new int[nUnique];
        for (int i = 0; i < nUnique; i++) {
            char c = chars[i];
            int j;
            for (j = charArr.length - 1; j > 0; j--)
                if (charArr[j] == c)
                    break;
            indexes[i] = charArr.length - 1 - j;
        }
        gen.shiftTable(true, charArr.length - 1, chars, indexes);
    }

    public Object evalRConst(RConstNode regexConst) {
        gen.tellPosition(regexConst.position);
        StringBuffer sb = new StringBuffer();
        while (true) {
            sb.append(regexConst.c);
            if (regexConst.tail instanceof RConstNode)
                regexConst = (RConstNode) regexConst.tail;
            else
                break;
        }
        char[] charArr = sb.toString().toCharArray();
        gen.assertFn(charArr);
        return evalTail(regexConst);
    }

    public Object evalRCharClass(RCharClassNode regexCharClass) {
        gen.tellPosition(regexCharClass.position);
        CharSet set = regexCharClass.charClass;
        gen.assertFn(set.charClass, set.ranges);
        return evalTail(regexCharClass);
    }

    public Object evalRBoundary(RBoundaryNode regexBoundary) {
        gen.tellPosition(regexBoundary.position);
        gen.boundary(regexBoundary.boundaryClass);
        return evalTail(regexBoundary);
    }

    public Object evalRAny(RAnyNode regexAny) {
        gen.tellPosition(regexAny.position);
        gen.skip();
        return evalTail(regexAny);
    }

    public Object evalRLookAhead(RLookAheadNode regexLookAhead) {
        gen.tellPosition(regexLookAhead.position);
        RNode body = regexLookAhead.body;
        boolean positive = regexLookAhead.positive;
        if (body == null) {
            if (!positive)
                gen.fail();
        } else {
            RLabel cont = gen.newLabel();
            RVariable cond = gen.newTmpVar(positive ? 0 : 1);
            gen.fork(cont);
            body.eval(this);
            gen.hardAssign(cond, (positive ? 1 : 0));
            gen.fail();
            gen.mark(cont);
            gen.decfail(cond);
            gen.forget(cond);
        }
        return evalTail(regexLookAhead);
    }

    public Object evalRAlt(RAltNode regexAlt) {
        gen.tellPosition(regexAlt.position);
        RNode alt1 = regexAlt.alt1;
        RLabel other = gen.newLabel();
        if (alt1 == null) {
            gen.fork(other);
        } else {
            if ((gen.getExtensions() & gen.EXT_CONDJUMP) != 0) {
                if (alt1.minLeft > regexAlt.minLeft)
                    gen.condJump(alt1.minLeft, alt1.maxLeft, other);
                if (alt1.prefix.isSingleChar())
                    gen.condJump(alt1.prefix.ranges[0], other);
            }
            gen.fork(other);
            alt1.eval(this);
        }
        RLabel cont = gen.newLabel();
        gen.jump(cont);
        gen.mark(other);
        RNode alt2 = regexAlt.alt2;
        if (alt2 != null) {
            if (alt2.minLeft > regexAlt.minLeft && (gen.getExtensions() & gen.EXT_CONDJUMP) != 0)
                gen.condJump(alt2.minLeft, alt2.maxLeft, null);
            alt2.eval(this);
        }
        gen.mark(cont);
        return evalTail(regexAlt);
    }

    public Object evalRRepeat(RRepeatNode regexRepeat) {
        gen.tellPosition(regexRepeat.position);
        RNode body = regexRepeat.body;
        int min = regexRepeat.min;
        int max = regexRepeat.max;
        boolean doingCharStarHead = charStarHead;
        charStarHead = false;
        if (min > 0) {
            RVariable count = null;
            RLabel repeat = null;
            if (min > 1) {
                count = gen.newTmpVar(min);
                repeat = gen.newLabel();
                gen.mark(repeat);
            }
            body.eval(this); // TODO: might need to use subroutines!
            if (min > 1) {
                gen.decjump(count, repeat);
                gen.forget(count);
            }
        }
        if (max != min) {
            if (max != Integer.MAX_VALUE)
                max -= min;
            int minBodyLength = body.minTotalLength(body);
            int minCharLeftAfter = regexRepeat.minLeft - min * minBodyLength;
            if (regexRepeat.greedy) {
                int maxBodyLength = body.maxTotalLength(body);
                if (minBodyLength == maxBodyLength &&
                        (gen.getExtensions() & gen.EXT_MULTIFORK) != 0 &&
                        !body.hasPicks() && !body.hasForks()) {
                    gen.mfStart(minBodyLength, (doingCharStarHead ? min : -1));
                    body.eval(this);
                    if (minCharLeftAfter > MIN_CHAR_LEFT && (gen.getExtensions() & gen.EXT_CONDJUMP) != 0)
                        gen.condJump(minCharLeftAfter, Integer.MAX_VALUE, null);
                    gen.mfEnd(max);
                } else {
                    RLabel other = gen.newLabel();
                    RVariable count = null;
                    if (max != Integer.MAX_VALUE)
                        count = gen.newTmpVar(max);
                    RLabel start = gen.newLabel();
                    gen.mark(start);
                    if (regexRepeat.tail != null && regexRepeat.tail.prefix.isSingleChar() &&
                            (gen.getExtensions() & gen.EXT_CONDJUMP) != 0) {
                        RLabel skipFork = gen.newLabel();
                        gen.condJump(regexRepeat.tail.prefix.ranges[0], skipFork);
                        gen.fork(other);
                        gen.mark(skipFork);
                    } else
                        gen.fork(other);
                    body.eval(this);
                    if (minCharLeftAfter > MIN_CHAR_LEFT && (gen.getExtensions() & gen.EXT_CONDJUMP) != 0)
                        gen.condJump(minCharLeftAfter, Integer.MAX_VALUE, null);
                    if (count != null) {
                        gen.decjump(count, start);
                        gen.forget(count);
                    } else
                        gen.jump(start);
                    gen.mark(other);
                }
            } else {
                RLabel other = gen.newLabel();
                RLabel start = gen.newLabel();
                RVariable count = null;
                if (max != Integer.MAX_VALUE)
                    count = gen.newTmpVar(max);
                gen.jump(other);
                gen.mark(start);
                if (count != null)
                    gen.decfail(count);
                body.eval(this);
                if (minCharLeftAfter > MIN_CHAR_LEFT && (gen.getExtensions() & gen.EXT_CONDJUMP) != 0)
                    gen.condJump(minCharLeftAfter, Integer.MAX_VALUE, null);
                gen.mark(other);
                gen.fork(start);
            }
            evalTail(regexRepeat);
        }
        return null;
    }

    public Object evalRPick(RPickNode regexPick) {
        gen.tellPosition(regexPick.position);
        RVariable var = gen.newVar(regexPick.name, regexPick.begin);
        gen.pick(var);
        return evalTail(regexPick);
    }

    public Object evalRSubst(RSubstNode regexSubst) {
        gen.tellPosition(regexSubst.position);
        gen.assertFn(regexSubst.var, regexSubst.picked);
        if (regexSubst.tail != null &&
                (gen.getExtensions() & gen.EXT_CONDJUMP) != 0 &&
                regexSubst.tail.minLeft != 0)
            gen.condJump(regexSubst.tail.minLeft, regexSubst.tail.maxLeft, null);
        return evalTail(regexSubst);
    }

    public void compile(RNode node, String name) {
        gen.tellName(name);
        charStarHead = false;
        RConstNode beginShiftTable = null;
        int hints = 0;
        if (node.isStartAnchored())
            hints |= gen.HINT_START_ANCHORED;
        if (node.isEndAnchored())
            hints |= gen.HINT_END_ANCHORED;
        if ((hints & gen.HINT_START_ANCHORED) == 0) {
            RNode p = node;
            while ((p instanceof RPickNode && !((RPickNode) p).referenced) ||
                    p instanceof RBoundaryNode)
                p = p.tail;
            if (p instanceof RRepeatNode) {
                RNode body = ((RRepeatNode) p).body;
                if (body.tail == null &&
                        (body instanceof RCharClassNode ||
                                body instanceof RConstNode || body instanceof RAnyNode) &&
                        body.minLength == 1 && body.maxLength == 1) {
                    charStarHead = true;
                    hints |= gen.HINT_CHAR_STAR_HEAD;
                }
            }
            if (p instanceof RConstNode && p.tail instanceof RConstNode)
                beginShiftTable = (RConstNode) p;
        }
        if ((gen.getExtensions() & gen.EXT_HINT) != 0)
            gen.hint(hints, node.minLeft, node.maxLeft);
        gen.init();
        if (beginShiftTable != null && ((gen.getExtensions() & gen.EXT_SHIFTTBL) != 0))
            genShiftTable(beginShiftTable);
        node.eval(this);
        gen.finish();
    }

}
