/*
 * This code was generated automatically.
 * Do NOT edit this file, changes will be lost.
 * Instead, change and commit the underlying schema.
 */

package orm2tg.tg.schema;

import de.uni_koblenz.jgralab.EdgeDirection;

import orm2tg.tg.schema.Folder;
import orm2tg.tg.schema.FolderPart2;
/**
 * FromVertexClass: FolderPart2
 * FromRoleName : 
 * ToVertexClass: Folder
 * ToRoleName : 
 */

public interface FolderPart2Hasfolder extends de.uni_koblenz.jgralab.Edge {

	public static final de.uni_koblenz.jgralab.schema.EdgeClass EC = orm2tg.tg.schema.UniquenessConstraint_nestedPrefRefScheme.instance().getGraphClass().getEdgeClass("FolderPart2Hasfolder");

	/**
	 * @return the next orm2tg.tg.schema.FolderPart2Hasfolder edge in the global edge sequence
	 */
	public orm2tg.tg.schema.FolderPart2Hasfolder getNextFolderPart2HasfolderInGraph();

	/**
	 * @return the next edge of class orm2tg.tg.schema.FolderPart2Hasfolder at the "this" vertex
	 */
	public orm2tg.tg.schema.FolderPart2Hasfolder getNextFolderPart2HasfolderIncidence();

	/**
	 * @return the next edge of class orm2tg.tg.schema.FolderPart2Hasfolder at the "this" vertex
	 * @param orientation the orientation of the edge
	 */
	public orm2tg.tg.schema.FolderPart2Hasfolder getNextFolderPart2HasfolderIncidence(EdgeDirection orientation);
	public FolderPart2 getAlpha();
	public Folder getOmega();
}