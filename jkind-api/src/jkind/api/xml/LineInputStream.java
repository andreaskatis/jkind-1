package jkind.api.xml;

import java.io.IOException;
import java.io.InputStream;

public class LineInputStream implements AutoCloseable {
	private final InputStream source;

	public LineInputStream(InputStream source) {
		this.source = source;
	}

	public String readLine() throws IOException {
		StringBuilder buffer = new StringBuilder();
		
		int c;
		while ((c = source.read()) != -1) {
			buffer.append((char) c);
			if (c == '\n') {
				return buffer.toString();
			}
		}
		
		if (buffer.length() == 0) {
			source.close();
			return null;
		} else {
			return buffer.toString();
		}
	}

	@Override
	public void close() throws Exception {
		source.close();
	}
}
