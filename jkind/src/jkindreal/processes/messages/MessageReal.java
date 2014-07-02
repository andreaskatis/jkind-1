package jkindreal.processes.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class MessageReal {
	protected <T> List<T> safeCopy(List<T> list) {
		return Collections.unmodifiableList(new ArrayList<>(list));
	}
}
