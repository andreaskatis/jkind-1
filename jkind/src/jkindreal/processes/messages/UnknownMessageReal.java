package jkindreal.processes.messages;

import java.util.List;

public class UnknownMessageReal extends MessageReal {
	final public List<String> unknown;

	public UnknownMessageReal(List<String> unknown) {
		this.unknown = safeCopy(unknown);
	}
}
