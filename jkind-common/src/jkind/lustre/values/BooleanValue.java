package jkind.lustre.values;

import jkind.lustre.BinaryOp;
import jkind.lustre.UnaryOp;

/**
 * A boolean signal value
 */
public class BooleanValue extends Value {
	final public boolean value;

	private BooleanValue(boolean value) {
		this.value = value;
	}

	final public static BooleanValue TRUE = new BooleanValue(true);
	final public static BooleanValue FALSE = new BooleanValue(false);

	public static BooleanValue fromBoolean(boolean value) {
		return value ? TRUE : FALSE;
	}

	@Override
	public Value applyBinaryOp(BinaryOp op, Value right) {
		if (!(right instanceof BooleanValue)) {
			return null;
		}
		boolean other = ((BooleanValue) right).value;

		switch (op) {
		case EQUAL:
			return fromBoolean(value == other);
		case NOTEQUAL:
			return fromBoolean(value != other);
		case OR:
			return fromBoolean(value || other);
		case AND:
			return fromBoolean(value && other);
		case XOR:
			return fromBoolean(value != other);
		case IMPLIES:
			return fromBoolean(!value || other);
		default:
			return null;
		}
	}

	@Override
	public Value applyUnaryOp(UnaryOp op) {
		switch (op) {
		case NOT:
			return fromBoolean(!value);
		default:
			return null;
		}
	}
	
	@Override
	public String toString() {
		return Boolean.toString(value);
	}

	@Override
	public int hashCode() {
		return Boolean.valueOf(value).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BooleanValue) {
			BooleanValue other = (BooleanValue) obj;
			return (value == other.value);
		}
		return false;
	}
}
