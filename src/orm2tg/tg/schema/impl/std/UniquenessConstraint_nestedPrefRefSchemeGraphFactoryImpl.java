/*
 * This code was generated automatically.
 * Do NOT edit this file, changes will be lost.
 * Instead, change and commit the underlying schema.
 */

package orm2tg.tg.schema.impl.std;

import de.uni_koblenz.jgralab.ImplementationType;
import de.uni_koblenz.jgralab.impl.GraphFactoryImpl;
import orm2tg.tg.schema.UniquenessConstraint_nestedPrefRefSchemeGraphFactory;

public class UniquenessConstraint_nestedPrefRefSchemeGraphFactoryImpl extends GraphFactoryImpl
		implements UniquenessConstraint_nestedPrefRefSchemeGraphFactory {

	public UniquenessConstraint_nestedPrefRefSchemeGraphFactoryImpl() {
		super(orm2tg.tg.schema.UniquenessConstraint_nestedPrefRefScheme.instance(), ImplementationType.STANDARD);
		createMaps();
		setGraphImplementationClass(orm2tg.tg.schema.UniquenessConstraint_nestedPrefRefSchemeGraph.GC,
				orm2tg.tg.schema.impl.std.UniquenessConstraint_nestedPrefRefSchemeGraphImpl.class);
		setVertexImplementationClass(orm2tg.tg.schema.File.VC, orm2tg.tg.schema.impl.std.FileImpl.class);
		setVertexImplementationClass(orm2tg.tg.schema.FileName.VC, orm2tg.tg.schema.impl.std.FileNameImpl.class);
		setVertexImplementationClass(orm2tg.tg.schema.Folder.VC, orm2tg.tg.schema.impl.std.FolderImpl.class);
		setVertexImplementationClass(orm2tg.tg.schema.FolderPart1.VC, orm2tg.tg.schema.impl.std.FolderPart1Impl.class);
		setVertexImplementationClass(orm2tg.tg.schema.FolderPart2.VC, orm2tg.tg.schema.impl.std.FolderPart2Impl.class);
		setEdgeImplementationClass(orm2tg.tg.schema.FileNameHasFile.EC,
				orm2tg.tg.schema.impl.std.FileNameHasFileImpl.class);
		setEdgeImplementationClass(orm2tg.tg.schema.FolderHasFile.EC,
				orm2tg.tg.schema.impl.std.FolderHasFileImpl.class);
		setEdgeImplementationClass(orm2tg.tg.schema.FolderPart1HasFolder.EC,
				orm2tg.tg.schema.impl.std.FolderPart1HasFolderImpl.class);
		setEdgeImplementationClass(orm2tg.tg.schema.FolderPart2Hasfolder.EC,
				orm2tg.tg.schema.impl.std.FolderPart2HasfolderImpl.class);
	}

}
