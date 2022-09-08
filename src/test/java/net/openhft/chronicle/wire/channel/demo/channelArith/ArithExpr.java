package software.chronicle.demo.wire.channelArith;

import net.openhft.chronicle.wire.Marshallable;

public class ArithExpr implements Marshallable {
    private int op1;
    private int op2;
    private Op operator;

    private int result = 0;

    public ArithExpr ( int op1, Op operator, int op2 ) {
        this.op1 = op1;
        this.op2 = op2;
        this.operator = operator;
    }

    public ArithExpr calculate () {
        switch ( operator ) {
            case PLUS:
                result = op1 + op2;
                break;
            case MINUS:
                result = op1 - op2;
                break;
            case TIMES:
                result = op1 * op2;
        }
        return this;
    }

    public String toString () {
        return op1 + " " +  operator + " " + op2+ " => " + result;
    }
}
