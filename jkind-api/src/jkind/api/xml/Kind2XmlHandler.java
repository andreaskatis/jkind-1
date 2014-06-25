package jkind.api.xml;

import jkind.JKindException;
import jkind.api.results.JKindResult;
import jkind.api.results.PropertyResult;
import jkind.lustre.values.Value;
import jkind.results.Counterexample;
import jkind.results.InvalidProperty;
import jkind.results.Property;
import jkind.results.Signal;
import jkind.results.UnknownProperty;
import jkind.results.ValidProperty;
import jkind.util.Util;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class Kind2XmlHandler extends DefaultHandler {
	private final JKindResult result;

	private Counterexample cex;
	private Signal<Value> signal;

	private String propertyName;
	private double runtime;
	private String answer;
	private int k;

	private String type;
	private int time;

	private boolean readRuntime = false;
	private boolean readAnswer = false;
	private boolean readK = false;
	private boolean readValue = false;

	public Kind2XmlHandler(JKindResult result) {
		this.result = result;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if (qName.equals("Property")) {
			propertyName = attributes.getValue("name");
			runtime = 0;
			answer = null;
			k = 0;
			cex = null;
		} else if (qName.equals("Runtime")) {
			readRuntime = true;
		} else if (qName.equals("Answer")) {
			readAnswer = true;
		} else if (qName.equals("K")) {
			readK = true;
		} else if (qName.equals("Counterexample")) {
			cex = new Counterexample(k);
		} else if (qName.equals("Signal")) {
			signal = new Signal<>(attributes.getValue("name"));
			type = attributes.getValue("type");
			if (type.contains("subrange")) {
				type = "int";
			}
			cex.addSignal(signal);
		} else if (qName.equals("Value")) {
			readValue = true;
			time = Integer.parseInt(attributes.getValue("time"));
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if (qName.equals("Property")) {
			Property prop;
			switch (answer) {
			case "valid":
				prop = new ValidProperty(propertyName, k, runtime, null);
				break;

			case "invalid":
				prop = new InvalidProperty(propertyName, cex, runtime);
				break;

			case "unknown":
				prop = new UnknownProperty(propertyName, cex);
				break;

			default:
				throw new JKindException("Unknown property answer in XML file: " + answer);
			}

			PropertyResult pr = result.getPropertyResult(propertyName);
			if (pr == null) {
				pr = result.addProperty(propertyName);
				if (pr == null) {
					return;
				}
			}
			pr.setProperty(prop);
		}
	}

	@Override
	public void characters(char ch[], int start, int length) {
		if (readRuntime) {
			runtime = Double.parseDouble(new String(ch, start, length));
			readRuntime = false;
		} else if (readAnswer) {
			answer = new String(ch, start, length);
			readAnswer = false;
		} else if (readK) {
			// Kind2 uses 0 indexing with k, so we increment here
			k = Integer.parseInt(new String(ch, start, length)) + 1;
			readK = false;
		} else if (readValue) {
			String str = new String(ch, start, length);
			if (!str.trim().isEmpty()) {
				signal.putValue(time, Util.parseValue(type, new String(ch, start, length)));
			}
			readValue = false;
		}
	}
}
