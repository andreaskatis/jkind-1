package jkind.api.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jkind.api.results.JKindResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Kind2XmlParseThread extends Thread {
	private final InputStream xmlStream;
	private Throwable throwable;
	private SAXParser parser;
	private DefaultHandler handler;

	public Kind2XmlParseThread(InputStream xmlStream, JKindResult result)
			throws ParserConfigurationException, SAXException {
		super("Xml Parse");
		this.xmlStream = xmlStream;
		this.parser = SAXParserFactory.newInstance().newSAXParser();
		this.handler = new Kind2XmlHandler(result);
	}

	@Override
	public void run() {
		/*
		 * The SAX parser buffers its input which conflicts with the way we are
		 * streaming data from the XML file as it is written. This results in
		 * data in the XML not being acted upon until more content is written to
		 * the XML file which causes the buffer to fill. Instead, we read the
		 * XML file ourselves and give relevant pieces of it to the SAX parser
		 * as they are ready.
		 * 
		 * The downside is we assume the <Property ...> and </Property> tags are
		 * on their own lines. Same for <Counterexample> and </Counterexample>
		 */

		try (LineInputStream lines = new LineInputStream(xmlStream)) {
			StringBuilder buffer = null;
			String line;
			while ((line = lines.readLine()) != null) {
				if (line.contains("</Property>")) {
					buffer.append(line);
					parsePropety(buffer.toString());
					buffer = null;
				} else if (line.contains("</Counterexample>")) {
					buffer.append(line);
					parseCounterexample(buffer.toString());
					buffer = null;
				} else if (line.contains("<Property") || line.contains("<Counterexample")) {
					buffer = new StringBuilder();
					buffer.append(line);
				} else if (buffer != null) {
					buffer.append(line);
				}
			}
		} catch (Throwable t) {
			throwable = t;
		}
	}

	public void parsePropety(String propertyXml) throws SAXException, IOException {
		parser.parse(new InputSource(new StringReader(propertyXml)), handler);
	}

	public void parseCounterexample(String counterexampleXml) throws SAXException, IOException {
		parser.parse(new InputSource(new StringReader(counterexampleXml)), handler);
	}

	public Throwable getThrowable() {
		return throwable;
	}
}
