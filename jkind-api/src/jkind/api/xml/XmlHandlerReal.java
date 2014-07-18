package jkind.api.xml;

import java.util.ArrayList;
import java.util.List;

import jkind.JKindException;
import jkind.api.results.JKindResultRealizability;
import jkind.api.results.RealizabilityResult;
import jkind.interval.IntEndpoint;
import jkind.interval.Interval;
import jkind.interval.NumericEndpoint;
import jkind.interval.NumericInterval;
import jkind.interval.RealEndpoint;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.results.Counterexample;
import jkind.results.InvalidRealizability;
import jkind.results.Realizability;
import jkind.results.Signal;
import jkind.results.ValidRealizability;
import jkind.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class XmlHandlerReal extends DefaultHandler {
	private final JKindResultRealizability resultreal;
	
	private Counterexample cex;
	private Signal<Value> signal;

	private String realizabilityName;
	private double runtime;
	private String answer;
	private int k;
	private List<String> invariants;

	private String type;
	private int time;

	private boolean readRuntime = false;
	private boolean readAnswer = false;
	private boolean readK = false;
	private boolean readValue = false;
	private boolean readInvariant = false;



	public XmlHandlerReal(JKindResultRealizability result) {
		this.resultreal = result;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if (qName.equals("Realizability")) {
			realizabilityName = attributes.getValue("name");
			runtime = 0;
			invariants = new ArrayList<>();
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
		} else if (qName.equals("Interval")) {
			Interval interval = readInterval(attributes.getValue("low"),
					attributes.getValue("high"));
			signal.putValue(time, interval);
		} else if (qName.equals("Invariant")) {
			readInvariant = true;
		}
	}

	private Interval readInterval(String low, String high) {
		NumericEndpoint lowEnd;
		NumericEndpoint highEnd;

		switch (type) {
		case "int":
			lowEnd = readIntEndpoint(low);
			highEnd = readIntEndpoint(high);
			break;

		case "real":
			lowEnd = readRealEndpoint(low);
			highEnd = readRealEndpoint(high);
			break;

		default:
			throw new JKindException("Unknown interval type in XML file: " + type);
		}
		
		return new NumericInterval(lowEnd, highEnd);
	}

	private IntEndpoint readIntEndpoint(String text) {
		switch (text) {
		case "inf":
			return IntEndpoint.POSITIVE_INFINITY;
		case "-inf":
			return IntEndpoint.NEGATIVE_INFINITY;
		default:
			IntegerValue iv = (IntegerValue) Util.parseValue("int", text);
			return new IntEndpoint(iv.value);
		}
	}

	private RealEndpoint readRealEndpoint(String text) {
		switch (text) {
		case "inf":
			return RealEndpoint.POSITIVE_INFINITY;
		case "-inf":
			return RealEndpoint.NEGATIVE_INFINITY;
		default:
			RealValue rv = (RealValue) Util.parseValue("real", text);
			return new RealEndpoint(rv.value);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if (qName.equals("Realizability")) {
			Realizability real;
			switch (answer) {
			case "valid":
				real = new ValidRealizability(realizabilityName, k, runtime, invariants);
				break;

			case "falsifiable":
				real = new InvalidRealizability(realizabilityName, cex, runtime);
				break;

			default:
				throw new JKindException("Unknown realizability answer in XML file: " + answer);
			}

			RealizabilityResult re = resultreal.getRealizabilityResult();
			if (re == null) {
				re = resultreal.addRealizability(realizabilityName);
				if (re == null) {
					return;
				}
			}
			re.setRealizability(real);
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
			k = Integer.parseInt(new String(ch, start, length));
			readK = false;
		} else if (readValue) {
			String str = new String(ch, start, length);
			if (!str.trim().isEmpty()) {
				signal.putValue(time, Util.parseValue(type, new String(ch, start, length)));
			}
			readValue = false;
		} else if (readInvariant) {
			invariants.add(new String(ch, start, length));
			readInvariant = false;
		}
	}
}
