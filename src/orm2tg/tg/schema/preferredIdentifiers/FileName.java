/*
 * This code was generated automatically.
 * Do NOT edit this file, changes will be lost.
 * Instead, change and commit the underlying schema.
 */

package orm2tg.tg.schema.preferredIdentifiers;

import de.uni_koblenz.jgralab.GraphIO;
import de.uni_koblenz.jgralab.exception.GraphIOException;
import de.uni_koblenz.jgralab.exception.NoSuchAttributeException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileName implements de.uni_koblenz.jgralab.Record {
	private static List<String> componentNames = new ArrayList<>(1);

	static {
		componentNames.add("fileNameCode");
		componentNames = Collections.unmodifiableList(componentNames);
	}

	@Override
	public List<String> getComponentNames() {
		return componentNames;
	}

	@Override
	public boolean hasComponent(String name) {
		return componentNames.contains(name);
	}

	@Override
public int size() {
	return 1;
}

	private final java.lang.String _fileNameCode;

	public FileName(java.lang.String _fileNameCode) {
		this._fileNameCode = _fileNameCode;
	}
	
	public FileName(java.util.Map<String, Object> componentValues) {
		assert componentValues.size() == 1;
		assert componentValues.containsKey("fileNameCode");
		_fileNameCode = (java.lang.String)componentValues.get("fileNameCode");
	}

	public FileName(GraphIO io) throws GraphIOException {
		io.match(de.uni_koblenz.jgralab.impl.TgLexer.Token.LBR);
		_fileNameCode = io.matchUtfString();
		io.match(de.uni_koblenz.jgralab.impl.TgLexer.Token.RBR);
	}

	public java.lang.String get_fileNameCode() {
		return _fileNameCode;
	}

	@Override
	public Object getComponent(String name) {
		if (name.equals("fileNameCode")) {
			return _fileNameCode;
		}
		throw new NoSuchAttributeException("preferredIdentifiers.FileName doesn't contain an attribute " + name);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String fileNameCodeString;
		if (_fileNameCode == null) fileNameCodeString = "null";
		else fileNameCodeString = String.valueOf(_fileNameCode);
		sb.append("[").append("fileNameCode").append("=").append(fileNameCodeString);
		return sb.append("]").toString();
	}

	@Override
	public void writeComponentValues(GraphIO io) throws IOException, GraphIOException {
		io.write("(");
		io.writeUtfString(_fileNameCode);
		io.write(")");
	}
	public org.pcollections.PMap<String, Object> toPMap() {
		org.pcollections.PMap<String, Object> m = de.uni_koblenz.jgralab.JGraLab.map();
		m = m.plus("fileNameCode", _fileNameCode);
		return m;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof FileName) {
			FileName rec = (FileName) o;
			if (!(_fileNameCode.equals(rec._fileNameCode))) {
				return false;
				}
			return true;
		}
		if (o instanceof de.uni_koblenz.jgralab.Record) {
			de.uni_koblenz.jgralab.Record rec = (de.uni_koblenz.jgralab.Record) o;
			if (rec.size() != 1) {
				return false;
			}
			try {
				if (!rec.getComponent("fileNameCode").equals(_fileNameCode)) {
					return false;
				}
				return true;
			} catch (NoSuchAttributeException e) {
				return false;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int h = 0;
		h += _fileNameCode.hashCode();
		return h;
	}
}
