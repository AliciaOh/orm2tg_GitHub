/*
 * This code was generated automatically.
 * Do NOT edit this file, changes will be lost.
 * Instead, change and commit the underlying schema.
 */

package orm2tg.tg.schema;

import de.uni_koblenz.jgralab.GraphIO;
import de.uni_koblenz.jgralab.exception.GraphIOException;

import de.uni_koblenz.jgralab.schema.impl.SchemaImpl;

import de.uni_koblenz.jgralab.schema.EdgeClass;
import de.uni_koblenz.jgralab.schema.GraphClass;
import de.uni_koblenz.jgralab.schema.RecordDomain;
import de.uni_koblenz.jgralab.schema.VertexClass;

import java.lang.ref.WeakReference;

/**
 * The schema UniquenessConstraint_nestedPrefRefScheme is implemented following the singleton pattern.
 * To get the instance, use the static method <code>instance()</code>.
 */
public class UniquenessConstraint_nestedPrefRefScheme extends SchemaImpl {

	/**
	 * reference to the singleton instance
	 */
	static WeakReference<UniquenessConstraint_nestedPrefRefScheme> theInstance = new WeakReference<>(null);
	
	/**
	 * @return the singleton instance of UniquenessConstraint_nestedPrefRefScheme
	 */
	public static synchronized UniquenessConstraint_nestedPrefRefScheme instance() {
		UniquenessConstraint_nestedPrefRefScheme s = theInstance.get();
		if (s != null) {
			return s;
		}
		s = new UniquenessConstraint_nestedPrefRefScheme();
		theInstance = new WeakReference<>(s);
		return s;
	}
	
	/**
	 * Creates a UniquenessConstraint_nestedPrefRefScheme and builds its schema classes.
	 * This constructor is private. Use the <code>instance()</code> method
	 * to access the schema.
	 */
	private UniquenessConstraint_nestedPrefRefScheme() {
		super("UniquenessConstraint_nestedPrefRefScheme", "orm2tg.tg.schema");

		{
			RecordDomain dom = createRecordDomain("preferredIdentifiers.File");
			dom.addComponent("fileNameCode", getDomain("String"));
			dom.addComponent("folderPart1Name", getDomain("String"));
			dom.addComponent("folderPart2ID", getDomain("String"));
		}

		{
			RecordDomain dom = createRecordDomain("preferredIdentifiers.FileName");
			dom.addComponent("fileNameCode", getDomain("String"));
		}

		{
			RecordDomain dom = createRecordDomain("preferredIdentifiers.Folder");
			dom.addComponent("folderPart1Name", getDomain("String"));
			dom.addComponent("folderPart2ID", getDomain("String"));
		}

		{
			RecordDomain dom = createRecordDomain("preferredIdentifiers.FolderPart1");
			dom.addComponent("folderPart1Name", getDomain("String"));
		}

		{
			RecordDomain dom = createRecordDomain("preferredIdentifiers.FolderPart2");
			dom.addComponent("folderPart2ID", getDomain("String"));
		}

		{
			GraphClass gc = createGraphClass("UniquenessConstraint_nestedPrefRefSchemeGraph");

			VertexClass vc_File = gc.createVertexClass("File");
			vc_File.setAbstract(false);
			vc_File.createAttribute("fileNameCode", getDomain("String"), null);
			vc_File.createAttribute("folderPart1Name", getDomain("String"), null);
			vc_File.createAttribute("folderPart2ID", getDomain("Integer"), null);
			vc_File.createAttribute("preferredIdentifiers", getDomain("preferredIdentifiers.File"), null);

			VertexClass vc_FileName = gc.createVertexClass("FileName");
			vc_FileName.setAbstract(false);
			vc_FileName.createAttribute("fileNameCode", getDomain("String"), null);
			vc_FileName.createAttribute("preferredIdentifiers", getDomain("preferredIdentifiers.FileName"), null);

			VertexClass vc_Folder = gc.createVertexClass("Folder");
			vc_Folder.setAbstract(false);
			vc_Folder.createAttribute("folderPart1Name", getDomain("String"), null);
			vc_Folder.createAttribute("folderPart2ID", getDomain("Integer"), null);
			vc_Folder.createAttribute("preferredIdentifiers", getDomain("preferredIdentifiers.Folder"), null);

			VertexClass vc_FolderPart1 = gc.createVertexClass("FolderPart1");
			vc_FolderPart1.setAbstract(false);
			vc_FolderPart1.createAttribute("folderPart1Name", getDomain("String"), null);
			vc_FolderPart1.createAttribute("preferredIdentifiers", getDomain("preferredIdentifiers.FolderPart1"), null);

			VertexClass vc_FolderPart2 = gc.createVertexClass("FolderPart2");
			vc_FolderPart2.setAbstract(false);
			vc_FolderPart2.createAttribute("folderPart2ID", getDomain("Integer"), null);
			vc_FolderPart2.createAttribute("preferredIdentifiers", getDomain("preferredIdentifiers.FolderPart2"), null);

			EdgeClass ec_FileNameHasFile = gc.createEdgeClass("FileNameHasFile",
				vc_FileName, 1, 1, "", de.uni_koblenz.jgralab.schema.AggregationKind.NONE,
				vc_File, 1, 2147483647, "", de.uni_koblenz.jgralab.schema.AggregationKind.NONE);
			ec_FileNameHasFile.setAbstract(false);

			EdgeClass ec_FolderHasFile = gc.createEdgeClass("FolderHasFile",
				vc_Folder, 0, 1, "", de.uni_koblenz.jgralab.schema.AggregationKind.NONE,
				vc_File, 1, 2147483647, "", de.uni_koblenz.jgralab.schema.AggregationKind.NONE);
			ec_FolderHasFile.setAbstract(false);

			EdgeClass ec_FolderPart1HasFolder = gc.createEdgeClass("FolderPart1HasFolder",
				vc_FolderPart1, 1, 1, "", de.uni_koblenz.jgralab.schema.AggregationKind.NONE,
				vc_Folder, 1, 2147483647, "", de.uni_koblenz.jgralab.schema.AggregationKind.NONE);
			ec_FolderPart1HasFolder.setAbstract(false);

			EdgeClass ec_FolderPart2Hasfolder = gc.createEdgeClass("FolderPart2Hasfolder",
				vc_FolderPart2, 0, 1, "", de.uni_koblenz.jgralab.schema.AggregationKind.NONE,
				vc_Folder, 1, 2147483647, "", de.uni_koblenz.jgralab.schema.AggregationKind.NONE);
			ec_FolderPart2Hasfolder.setAbstract(false);
		}

		finish();
	}

	@Override
	public de.uni_koblenz.jgralab.GraphFactory createDefaultGraphFactory(de.uni_koblenz.jgralab.ImplementationType implementationType) {
		switch(implementationType) {
			case GENERIC:
				return new de.uni_koblenz.jgralab.impl.generic.GenericGraphFactoryImpl(this);
			case STANDARD:
				return new orm2tg.tg.schema.impl.std.UniquenessConstraint_nestedPrefRefSchemeGraphFactoryImpl();
		}
		throw new UnsupportedOperationException("No " + implementationType + " support compiled.");
	}

	/**
	 * Creates a new UniquenessConstraint_nestedPrefRefSchemeGraph graph.
	*/
	public UniquenessConstraint_nestedPrefRefSchemeGraph createUniquenessConstraint_nestedPrefRefSchemeGraph(de.uni_koblenz.jgralab.ImplementationType implType) {
		return createUniquenessConstraint_nestedPrefRefSchemeGraph(implType, null, 100, 100);
	}

	/**
	 * Creates a new UniquenessConstraint_nestedPrefRefSchemeGraph graph with initial vertex and edge counts <code>vMax</code>, <code>eMax</code>.
	 *
	 * @param vMax initial vertex count
	 * @param eMax initial edge count
	*/
	public UniquenessConstraint_nestedPrefRefSchemeGraph createUniquenessConstraint_nestedPrefRefSchemeGraph(de.uni_koblenz.jgralab.ImplementationType implType, String id, int vMax, int eMax) {
		de.uni_koblenz.jgralab.GraphFactory factory = createDefaultGraphFactory(implType);
		return factory.createGraph(getGraphClass(), id, vMax, eMax);
	}

	/**
	 * Creates a new UniquenessConstraint_nestedPrefRefSchemeGraph graph.
	*/
	public UniquenessConstraint_nestedPrefRefSchemeGraph createUniquenessConstraint_nestedPrefRefSchemeGraph(de.uni_koblenz.jgralab.GraphFactory factory) {
		return factory.createGraph(getGraphClass(), null, 100, 100);
	}

	/**
	 * Creates a new UniquenessConstraint_nestedPrefRefSchemeGraph graph.
	*/
	public UniquenessConstraint_nestedPrefRefSchemeGraph createUniquenessConstraint_nestedPrefRefSchemeGraph(de.uni_koblenz.jgralab.GraphFactory factory, String id, int vMax, int eMax) {
		return factory.createGraph(getGraphClass(), id, vMax, eMax);
	}

	public UniquenessConstraint_nestedPrefRefSchemeGraph loadUniquenessConstraint_nestedPrefRefSchemeGraph(String filename) throws GraphIOException {
		de.uni_koblenz.jgralab.GraphFactory factory = createDefaultGraphFactory(de.uni_koblenz.jgralab.ImplementationType.STANDARD);
		return loadUniquenessConstraint_nestedPrefRefSchemeGraph(filename, factory, null);
	}

	public UniquenessConstraint_nestedPrefRefSchemeGraph loadUniquenessConstraint_nestedPrefRefSchemeGraph(String filename, de.uni_koblenz.jgralab.ProgressFunction pf) throws GraphIOException {
		de.uni_koblenz.jgralab.GraphFactory factory = createDefaultGraphFactory(de.uni_koblenz.jgralab.ImplementationType.STANDARD);
		return loadUniquenessConstraint_nestedPrefRefSchemeGraph(filename, factory, pf);
	}

	public UniquenessConstraint_nestedPrefRefSchemeGraph loadUniquenessConstraint_nestedPrefRefSchemeGraph(String filename, de.uni_koblenz.jgralab.ImplementationType implType) throws GraphIOException {
		de.uni_koblenz.jgralab.GraphFactory factory = createDefaultGraphFactory(implType);
		return loadUniquenessConstraint_nestedPrefRefSchemeGraph(filename, factory, null);
	}

	
	public UniquenessConstraint_nestedPrefRefSchemeGraph loadUniquenessConstraint_nestedPrefRefSchemeGraph(String filename, de.uni_koblenz.jgralab.ImplementationType implType, de.uni_koblenz.jgralab.ProgressFunction pf) throws GraphIOException {
		de.uni_koblenz.jgralab.GraphFactory factory = createDefaultGraphFactory(implType);
		return loadUniquenessConstraint_nestedPrefRefSchemeGraph(filename, factory, pf);
	}

	public UniquenessConstraint_nestedPrefRefSchemeGraph loadUniquenessConstraint_nestedPrefRefSchemeGraph(String filename, de.uni_koblenz.jgralab.GraphFactory factory) throws GraphIOException {
		return GraphIO.loadGraphFromFile(filename, factory, null);
	}

	public UniquenessConstraint_nestedPrefRefSchemeGraph loadUniquenessConstraint_nestedPrefRefSchemeGraph(String filename, de.uni_koblenz.jgralab.GraphFactory factory, de.uni_koblenz.jgralab.ProgressFunction pf) throws GraphIOException {
		return GraphIO.loadGraphFromFile(filename, factory, pf);
	}
	
	@Override
	public boolean reopen() {
		throw new UnsupportedOperationException("Cannot reopen a compiled Schema.");
	}
}