package generateinstances;

import de.uni_koblenz.jgralab.eca.ECARuleManager;
import de.uni_koblenz.jgralab.exception.GraphIOException;
import orm2tg.tg.schema.File;
import orm2tg.tg.schema.FileName;
import orm2tg.tg.schema.FileNameHasFile;
import orm2tg.tg.schema.Folder;
import orm2tg.tg.schema.FolderHasFile;
import orm2tg.tg.schema.FolderPart1;
import orm2tg.tg.schema.FolderPart1HasFolder;
import orm2tg.tg.schema.FolderPart2;
import orm2tg.tg.schema.FolderPart2Hasfolder;
import orm2tg.tg.schema.UniquenessConstraint_nestedPrefRefScheme;
import orm2tg.tg.schema.UniquenessConstraint_nestedPrefRefSchemeGraph;
import orm2tg.tg.schema.UniquenessConstraint_nestedPrefRefSchemeGraphFactory;
import orm2tg.tg.schema.impl.std.UniquenessConstraint_nestedPrefRefSchemeGraphFactoryImpl;

/**
 * 
 * This class is supposed to show how it is possible to automatically add ECAs
 * to a TGraph by adjusting the graph implementation class of the graph factory
 * for a specific type of TGraph.
 * 
 * @author Alicia Owen
 *
 */
public class ECATest_SpecializedGC {

	private ECARuleManager ecaRuleManager;
	UniquenessConstraint_nestedPrefRefSchemeGraph graph;

	public void createECARuleManager() {
		ecaRuleManager = new ECARuleManager(graph);
		graph.addGraphChangeListener(ecaRuleManager);
	}

	/**
	 * Create the Graph for testing
	 * 
	 * @throws GraphIOException
	 */
	public void initGraph() throws GraphIOException {

		// in analogy with
		// AnnotatedOsmGraph.java
		UniquenessConstraint_nestedPrefRefSchemeGraphFactory f = new UniquenessConstraint_nestedPrefRefSchemeGraphFactoryImpl();
		f.setGraphImplementationClass(UniquenessConstraint_nestedPrefRefSchemeGraph.GC, SpecializedGraphClass.class);
		this.graph = UniquenessConstraint_nestedPrefRefScheme.instance()
				.createUniquenessConstraint_nestedPrefRefSchemeGraph(f);

		createECARuleManager();

	}

	public static void main(String[] args) throws GraphIOException {
		ECATest_SpecializedGC teca = new ECATest_SpecializedGC();
		teca.initGraph();
		teca.updateGraphStandard();

	}

	private void updateGraphStandard() {
		// create File VC (RE: fileNameCode, folderPart1Name, folderPart2ID)
		File file1 = graph.createFile();

		// create File VC (RE: fileNameCode, folderPart1Name, folderPart2ID)
		File file2 = graph.createFile();

		// create Folder VC (RE: folderPart1Name, folderPart2ID)
		Folder folder = graph.createFolder();
		graph.createEdge(FolderHasFile.EC, folder, file1);

		// create FileName VC (RE: fileNameCode)
		FileName fileName = graph.createFileName();
		graph.createEdge(FileNameHasFile.EC, fileName, file1);

		// create FileName VC (with the same preferred ID)
		FileName fileName_duplicate = graph.createFileName();
		graph.createEdge(FileNameHasFile.EC, fileName_duplicate, file2);

		// create FolderPart1 VC (RE: folderPart1Name)
		FolderPart1 fp1 = graph.createFolderPart1();
		graph.createEdge(FolderPart1HasFolder.EC, fp1, folder);

		// create FolderPart2 VC (RE: folderPart2ID)
		FolderPart2 fp2 = graph.createFolderPart2();
		graph.createEdge(FolderPart2Hasfolder.EC, fp2, folder);

		// initialize "preferredIdentifiers" attributes (i.e. generate new
		// Records)
		file1.set_preferredIdentifiers(new orm2tg.tg.schema.preferredIdentifiers.File("", "", ""));
		folder.set_preferredIdentifiers(new orm2tg.tg.schema.preferredIdentifiers.Folder("", ""));
		fileName.set_preferredIdentifiers(new orm2tg.tg.schema.preferredIdentifiers.FileName("file_name"));
		fp1.set_preferredIdentifiers(new orm2tg.tg.schema.preferredIdentifiers.FolderPart1("fp1_name"));
		fp2.set_preferredIdentifiers(new orm2tg.tg.schema.preferredIdentifiers.FolderPart2("fp2_name"));

		// this change should not happen
		fileName_duplicate.set_preferredIdentifiers(new orm2tg.tg.schema.preferredIdentifiers.FileName("file_name"));

		//
	}

}
