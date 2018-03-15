package generateinstances;

import de.uni_koblenz.jgralab.Vertex;
import de.uni_koblenz.jgralab.eca.Action;
import de.uni_koblenz.jgralab.eca.ECARule;
import de.uni_koblenz.jgralab.eca.ECARuleManager;
import de.uni_koblenz.jgralab.eca.PrintAction;
import de.uni_koblenz.jgralab.eca.events.ChangeAttributeEventDescription;
import de.uni_koblenz.jgralab.eca.events.EventDescription;
import de.uni_koblenz.jgralab.schema.EdgeClass;
import de.uni_koblenz.jgralab.schema.VertexClass;
import eca.PreferredIdentifierAttributeChangedAction;
import orm2tg.tg.schema.File;
import orm2tg.tg.schema.FileNameHasFile;
import orm2tg.tg.schema.impl.std.UniquenessConstraint_nestedPrefRefSchemeGraphImpl;

/**
 * This class is intended to show how ECA rules could be added to a TGraph's
 * vertex classes. The ECAs are added to every vertex class which is added to
 * the graph or, if a graph is loaded, to every vertex class within this graph.
 * This class specializes the class which implements the GraphImpl interface for
 * a specific type of TGraph.
 * 
 * @author Alicia Owen
 *
 */
public class SpecializedGraphClass extends UniquenessConstraint_nestedPrefRefSchemeGraphImpl {

	ECARuleManager ecaRuleManager;

	public SpecializedGraphClass(String id, int vmax, int emax) {
		super(id, vmax, emax);
		this.ecaRuleManager = new ECARuleManager(this);
		this.addGraphChangeListener(ecaRuleManager);
	}

	@Override
	public void loadingCompleted() {

		for (Vertex v : this.vertices()) {
			ecaRuleManager.getRules();
			// TODO insert the correct values for "nextVC" and "connectingEC":

			addPreferredIdentifierAttributeChangedECA(v.getAttributedElementClass(), "preferredIdentifiers", File.VC,
					FileNameHasFile.EC);

		}

	}

	@Override
	public void fireAfterCreateVertex(Vertex v) {
		// TODO insert the correct values for "nextVC" and "connectingEC":

		addPreferredIdentifierAttributeChangedECA(v.getAttributedElementClass(), "preferredIdentifiers", File.VC,
				FileNameHasFile.EC);

		super.fireAfterCreateVertex(v);
	}

	private void addPreferredIdentifierAttributeChangedECA(VertexClass vc, String attributeName, VertexClass nextVC,
			EdgeClass connectingEC) {

		EventDescription<VertexClass> before_event = new ChangeAttributeEventDescription<>(
				EventDescription.EventTime.BEFORE, vc, attributeName);
		Action<VertexClass> before_action = new PrintAction<>(
				"The preferred identifier of VC \"" + vc.getQualifiedName() + "\" is about to be changed.");
		ECARule<VertexClass> before_rule = new ECARule<>(before_event, before_action);

		if (!ecaRuleManager.getRules().contains(before_rule)) {
			ecaRuleManager.addECARule(before_rule);
		}

		EventDescription<VertexClass> after_event = new ChangeAttributeEventDescription<>(
				EventDescription.EventTime.AFTER, vc, attributeName);
		Action<VertexClass> after_act = new PreferredIdentifierAttributeChangedAction(nextVC, connectingEC);
		ECARule<VertexClass> after_rule = new ECARule<>(after_event, after_act);

		if (!ecaRuleManager.getRules().contains(after_rule)) {
			ecaRuleManager.addECARule(after_rule);
		}

	}

}
