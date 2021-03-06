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

import orm2tg.tg.schema.impl.std.ReversedFolderPart2HasfolderImpl;

import java.io.IOException;

import orm2tg.tg.schema.Folder;
import orm2tg.tg.schema.FolderPart2;
/**
 * FromVertexClass: FolderPart2
 * FromRoleName : 
 * ToVertexClass: Folder
 * ToRoleName : 
 */

public class FolderPart2HasfolderImpl extends EdgeImpl implements de.uni_koblenz.jgralab.Edge, orm2tg.tg.schema.FolderPart2Hasfolder {

	public FolderPart2HasfolderImpl(int id, de.uni_koblenz.jgralab.Graph g, Vertex alpha, Vertex omega) {
		super(id, g, alpha, omega);
		((de.uni_koblenz.jgralab.impl.InternalGraph) graph).addEdge(this, alpha, omega);
	}

	@Override
	public final de.uni_koblenz.jgralab.schema.EdgeClass getAttributedElementClass() {
		return orm2tg.tg.schema.FolderPart2Hasfolder.EC;
	}

	@Override
	public final java.lang.Class<? extends de.uni_koblenz.jgralab.Edge> getSchemaClass() {
		return orm2tg.tg.schema.FolderPart2Hasfolder.class;
	}

	public <T> T getAttribute(String attributeName) {
		throw new NoSuchAttributeException("FolderPart2Hasfolder doesn't contain an attribute " + attributeName);
	}

	public <T> void setAttribute(String attributeName, T data) {
		throw new NoSuchAttributeException("FolderPart2Hasfolder doesn't contain an attribute " + attributeName);
	}

	public void readAttributeValues(GraphIO io) throws GraphIOException {
	}

	public void readAttributeValueFromString(String attributeName, String value) throws GraphIOException {
		throw new NoSuchAttributeException("FolderPart2Hasfolder doesn't contain an attribute " + attributeName);
	}

	public void writeAttributeValues(GraphIO io) throws GraphIOException, IOException {
	}

	public String writeAttributeValueToString(String attributeName) throws IOException, GraphIOException {
		throw new NoSuchAttributeException("FolderPart2Hasfolder doesn't contain an attribute " + attributeName);
	}

	public orm2tg.tg.schema.FolderPart2Hasfolder getNextFolderPart2HasfolderInGraph() {
		return (orm2tg.tg.schema.FolderPart2Hasfolder)getNextEdge(orm2tg.tg.schema.FolderPart2Hasfolder.EC);
	}

	public orm2tg.tg.schema.FolderPart2Hasfolder getNextFolderPart2HasfolderIncidence() {
		return (orm2tg.tg.schema.FolderPart2Hasfolder)getNextIncidence(orm2tg.tg.schema.FolderPart2Hasfolder.EC);
	}

	public orm2tg.tg.schema.FolderPart2Hasfolder getNextFolderPart2HasfolderIncidence(EdgeDirection orientation) {
		return (orm2tg.tg.schema.FolderPart2Hasfolder)getNextIncidence(orm2tg.tg.schema.FolderPart2Hasfolder.EC, orientation);
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
		return new ReversedFolderPart2HasfolderImpl(this, graph);
	}
	public FolderPart2 getAlpha() {
		return (FolderPart2) super.getAlpha();
	}
	public Folder getOmega() {
		return (Folder) super.getOmega();
	}
}
