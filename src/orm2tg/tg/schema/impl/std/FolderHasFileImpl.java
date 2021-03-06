/*
 * This code was generated automatically.
 * Do NOT edit this file, changes will be lost.
 * Instead, change and commit the underlying schema.
 */

package orm2tg.tg.schema.impl.std;

import de.uni_koblenz.jgralab.impl.std.EdgeImpl;

import de.uni_koblenz.jgralab.EdgeDirection;
import de.uni_koblenz.jgralab.GraphIO;
import de.uni_koblenz.jgralab.Vertex;
import de.uni_koblenz.jgralab.exception.GraphIOException;
import de.uni_koblenz.jgralab.exception.NoSuchAttributeException;

import orm2tg.tg.schema.impl.std.ReversedFolderHasFileImpl;

import java.io.IOException;

import orm2tg.tg.schema.File;
import orm2tg.tg.schema.Folder;
/**
 * FromVertexClass: Folder
 * FromRoleName : 
 * ToVertexClass: File
 * ToRoleName : 
 */

public class FolderHasFileImpl extends EdgeImpl implements de.uni_koblenz.jgralab.Edge, orm2tg.tg.schema.FolderHasFile {

	public FolderHasFileImpl(int id, de.uni_koblenz.jgralab.Graph g, Vertex alpha, Vertex omega) {
		super(id, g, alpha, omega);
		((de.uni_koblenz.jgralab.impl.InternalGraph) graph).addEdge(this, alpha, omega);
	}

	@Override
	public final de.uni_koblenz.jgralab.schema.EdgeClass getAttributedElementClass() {
		return orm2tg.tg.schema.FolderHasFile.EC;
	}

	@Override
	public final java.lang.Class<? extends de.uni_koblenz.jgralab.Edge> getSchemaClass() {
		return orm2tg.tg.schema.FolderHasFile.class;
	}

	public <T> T getAttribute(String attributeName) {
		throw new NoSuchAttributeException("FolderHasFile doesn't contain an attribute " + attributeName);
	}

	public <T> void setAttribute(String attributeName, T data) {
		throw new NoSuchAttributeException("FolderHasFile doesn't contain an attribute " + attributeName);
	}

	public void readAttributeValues(GraphIO io) throws GraphIOException {
	}

	public void readAttributeValueFromString(String attributeName, String value) throws GraphIOException {
		throw new NoSuchAttributeException("FolderHasFile doesn't contain an attribute " + attributeName);
	}

	public void writeAttributeValues(GraphIO io) throws GraphIOException, IOException {
	}

	public String writeAttributeValueToString(String attributeName) throws IOException, GraphIOException {
		throw new NoSuchAttributeException("FolderHasFile doesn't contain an attribute " + attributeName);
	}

	public orm2tg.tg.schema.FolderHasFile getNextFolderHasFileInGraph() {
		return (orm2tg.tg.schema.FolderHasFile)getNextEdge(orm2tg.tg.schema.FolderHasFile.EC);
	}

	public orm2tg.tg.schema.FolderHasFile getNextFolderHasFileIncidence() {
		return (orm2tg.tg.schema.FolderHasFile)getNextIncidence(orm2tg.tg.schema.FolderHasFile.EC);
	}

	public orm2tg.tg.schema.FolderHasFile getNextFolderHasFileIncidence(EdgeDirection orientation) {
		return (orm2tg.tg.schema.FolderHasFile)getNextIncidence(orm2tg.tg.schema.FolderHasFile.EC, orientation);
	}

	public de.uni_koblenz.jgralab.schema.AggregationKind getAggregationKind() {
		return de.uni_koblenz.jgralab.schema.AggregationKind.NONE;
	}

	@Override
	public de.uni_koblenz.jgralab.schema.AggregationKind getAlphaAggregationKind() {
		return de.uni_koblenz.jgralab.schema.AggregationKind.NONE;
	}

	@Override
	public de.uni_koblenz.jgralab.schema.AggregationKind getOmegaAggregationKind() {
		return de.uni_koblenz.jgralab.schema.AggregationKind.NONE;
	}

	protected de.uni_koblenz.jgralab.impl.ReversedEdgeBaseImpl createReversedEdge() {
		return new ReversedFolderHasFileImpl(this, graph);
	}
	public Folder getAlpha() {
		return (Folder) super.getAlpha();
	}
	public File getOmega() {
		return (File) super.getOmega();
	}
}
