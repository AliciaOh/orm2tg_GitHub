/*
 * This code was generated automatically.
 * Do NOT edit this file, changes will be lost.
 * Instead, change and commit the underlying schema.
 */

package orm2tg.tg.schema.impl.std;

import de.uni_koblenz.jgralab.impl.std.ReversedEdgeImpl;
import de.uni_koblenz.jgralab.impl.std.EdgeImpl;

import de.uni_koblenz.jgralab.EdgeDirection;
import de.uni_koblenz.jgralab.Graph;
import de.uni_koblenz.jgralab.GraphIO;
import de.uni_koblenz.jgralab.exception.GraphIOException;

import java.io.IOException;

import orm2tg.tg.schema.File;
import orm2tg.tg.schema.Folder;

public class ReversedFolderHasFileImpl extends ReversedEdgeImpl implements de.uni_koblenz.jgralab.Edge, orm2tg.tg.schema.FolderHasFile {

	ReversedFolderHasFileImpl(EdgeImpl e, Graph g) {
		super(e, g);
	}

	@Override
	public final de.uni_koblenz.jgralab.schema.EdgeClass getAttributedElementClass() {
		return getNormalEdge().getAttributedElementClass();
	}

	public void readAttributeValues(GraphIO io) throws GraphIOException {
		throw new GraphIOException("Can not call readAttributeValues for reversed Edges.");
	}

	public void readAttributeValueFromString(String attributeName, String value) throws GraphIOException {
		throw new GraphIOException("Can not call readAttributeValuesFromString for reversed Edges.");
	}

	public void writeAttributeValues(GraphIO io) throws GraphIOException, IOException {
		throw new GraphIOException("Can not call writeAttributeValues for reversed Edges.");
	}

	public String writeAttributeValueToString(String _attributeName) throws IOException, GraphIOException {
		throw new GraphIOException("Can not call writeAttributeValueToString for reversed Edges.");
	}

	public orm2tg.tg.schema.FolderHasFile getNextFolderHasFileInGraph() {
		return ((orm2tg.tg.schema.FolderHasFile)normalEdge).getNextFolderHasFileInGraph();
	}

	public orm2tg.tg.schema.FolderHasFile getNextFolderHasFileIncidence() {
		return (orm2tg.tg.schema.FolderHasFile)getNextIncidence(orm2tg.tg.schema.FolderHasFile.EC);
	}

	public orm2tg.tg.schema.FolderHasFile getNextFolderHasFileIncidence(EdgeDirection orientation) {
		return (orm2tg.tg.schema.FolderHasFile)getNextIncidence(orm2tg.tg.schema.FolderHasFile.EC, orientation);
	}
	public Folder getAlpha() {
		return (Folder) super.getAlpha();
	}
	public File getOmega() {
		return (File) super.getOmega();
	}
}
