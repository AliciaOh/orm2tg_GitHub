/*
 * This code was generated automatically.
 * Do NOT edit this file, changes will be lost.
 * Instead, change and commit the underlying schema.
 */

package orm2tg.tg.schema;

import de.uni_koblenz.jgralab.EdgeDirection;

public interface FolderPart1 extends de.uni_koblenz.jgralab.Vertex {

	public static final de.uni_koblenz.jgralab.schema.VertexClass VC = orm2tg.tg.schema.UniquenessConstraint_nestedPrefRefScheme.instance().getGraphClass().getVertexClass("FolderPart1");

	public java.lang.String get_folderPart1Name();

	public void set_folderPart1Name(java.lang.String _folderPart1Name);

	public orm2tg.tg.schema.preferredIdentifiers.FolderPart1 get_preferredIdentifiers();

	public void set_preferredIdentifiers(orm2tg.tg.schema.preferredIdentifiers.FolderPart1 _preferredIdentifiers);

	/**
	 * @return the next orm2tg.tg.schema.FolderPart1 vertex in the global vertex sequence
	 */
	public orm2tg.tg.schema.FolderPart1 getNextFolderPart1();

	/**
	 * @return the first edge of class FolderPart1HasFolder at this vertex
	 */
	public orm2tg.tg.schema.FolderPart1HasFolder getFirstFolderPart1HasFolderIncidence();

	/**
	 * @return the first edge of class FolderPart1HasFolder at this vertex
	 * @param orientation the orientation of the edge
	 */
	public orm2tg.tg.schema.FolderPart1HasFolder getFirstFolderPart1HasFolderIncidence(EdgeDirection orientation);

	/**
	 * Returns an Iterable for all incidence edges of this vertex that are of type FolderPart1HasFolder or subtypes.
	 */
	public Iterable<orm2tg.tg.schema.FolderPart1HasFolder> getFolderPart1HasFolderIncidences();
	
	/**
	 * Returns an Iterable for all incidence edges of this vertex that are of type FolderPart1HasFolder.
	 * @param direction EdgeDirection.IN or EdgeDirection.OUT, only edges of this direction will be included in the Iterable
	 */
	public Iterable<orm2tg.tg.schema.FolderPart1HasFolder> getFolderPart1HasFolderIncidences(EdgeDirection direction);
}
