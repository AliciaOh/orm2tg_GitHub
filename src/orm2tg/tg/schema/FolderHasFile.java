/*
 * This code was generated automatically.
 * Do NOT edit this file, changes will be lost.
 * Instead, change and commit the underlying schema.
 */

package orm2tg.tg.schema;

import de.uni_koblenz.jgralab.EdgeDirection;

import orm2tg.tg.schema.File;
import orm2tg.tg.schema.Folder;
/**
 * FromVertexClass: Folder
 * FromRoleName : 
 * ToVertexClass: File
 * ToRoleName : 
 */

public interface FolderHasFile extends de.uni_koblenz.jgralab.Edge {

	public static final de.uni_koblenz.jgralab.schema.EdgeClass EC = orm2tg.tg.schema.UniquenessConstraint_nestedPrefRefScheme.instance().getGraphClass().getEdgeClass("FolderHasFile");

	/**
	 * @return the next orm2tg.tg.schema.FolderHasFile edge in the global edge sequence
	 */
	public orm2tg.tg.schema.FolderHasFile getNextFolderHasFileInGraph();

	/**
	 * @return the next edge of class orm2tg.tg.schema.FolderHasFile at the "this" vertex
	 */
	public orm2tg.tg.schema.FolderHasFile getNextFolderHasFileIncidence();

	/**
	 * @return the next edge of class orm2tg.tg.schema.FolderHasFile at the "this" vertex
	 * @param orientation the orientation of the edge
	 */
	public orm2tg.tg.schema.FolderHasFile getNextFolderHasFileIncidence(EdgeDirection orientation);
	public Folder getAlpha();
	public File getOmega();
}
