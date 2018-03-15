package eca;

import java.util.HashMap;
import java.util.Map;

import de.uni_koblenz.jgralab.Record;
import de.uni_koblenz.jgralab.Vertex;
import de.uni_koblenz.jgralab.eca.Condition;
import de.uni_koblenz.jgralab.eca.events.ChangeAttributeEvent;
import de.uni_koblenz.jgralab.eca.events.Event;
import de.uni_koblenz.jgralab.schema.AttributedElementClass;
import de.uni_koblenz.jgralab.schema.VertexClass;

/**
 * 
 * This class defines a condition which is evaluated if an attribute value
 * changes. The condition checks whether a change made to a record containing
 * preferred identifiers for vertex classes is valid, i.e. whether the new value
 * of the record component is unique over all instances of the vertex class in
 * question.
 * 
 * @author Alicia Owen
 *
 */

public class PreferredIdentifierAttributeChangedCondition<AEC extends AttributedElementClass<AEC, ?>>
		implements Condition<AEC> {

	@Override
	public boolean evaluate(Event<AEC> event) {
		assert event instanceof ChangeAttributeEvent;
		// check that the attribute which is modified receives a value which
		// is unique for the instances of the VertexClass to which it
		// belongs
		Vertex v = (Vertex) event.getElement();
		VertexClass vc = v.getAttributedElementClass();

		Record originalRecord = (Record) ((ChangeAttributeEvent<?>) event).getOldValue();
		Record updatedRecord = (Record) ((ChangeAttributeEvent<?>) event).getNewValue();

		Map<String, Object> changedComponent = getChangedComponent(originalRecord, updatedRecord);

		if (changedComponent == null) {
			// if the Record returns to a state where it wasn't set yet, it is
			// "unique"
			System.out.println("The record was set to null (TRUE)");
			return true;
		}

		System.out.println("Testing Condition ... (cause: " + ((ChangeAttributeEvent<?>) event).getAttributeName()
				+ " in " + ((ChangeAttributeEvent<?>) event).getElement().toString() + " is changing)");
		for (Vertex vertex : event.getGraph().vertices(vc)) {
			if (vertex != v) {

				Record otherRecord = (Record) vertex.getAttribute(((ChangeAttributeEvent<?>) event).getAttributeName());
				if (otherRecord == null) {
					continue;
				}
				if (isKeyValueInOtherRecord(changedComponent, otherRecord)) {
					System.out
							.println("Found the same Value in another Vertex of this VertexClass (uniqueness = FALSE)");
					return false;
				}

			}
			System.out.println("The updated value is unique (uniqueness = TRUE)");
			return true;
		}
		System.out.println("There is no other attributes of this kind (uniqueness = TRUE)");
		return true;

	}

	private boolean isKeyValueInOtherRecord(Map<String, Object> changedComponent, Record otherRecord) {
		assert changedComponent.size() == 1;

		String key = (String) changedComponent.keySet().toArray()[0];

		if (otherRecord.getComponent(key).equals(changedComponent.get(key))) {
			return true;
		}
		return false;
	}

	private Map<String, Object> getChangedComponent(Record recordBeforeChange, Record recordAfterChange) {

		if (recordAfterChange == null) {
			// record is reverted to a state where it wasn't set yet
			return null;
		}

		Map<String, Object> changedComponent = new HashMap<String, Object>();

		Map<String, Object> changedKeyValue = recordAfterChange.toPMap();

		if (recordBeforeChange == null) {
			// this is the initial generation of the Record
			for (String key : changedKeyValue.keySet()) {
				changedComponent.put(key, changedKeyValue.get(key));
			}

			return changedComponent;
		}

		Map<String, Object> originalKeyValue = recordBeforeChange.toPMap();

		for (String key : originalKeyValue.keySet()) {

			if (!originalKeyValue.get(key).equals(changedKeyValue.get(key))) {
				changedComponent.put(key, changedKeyValue.get(key));
			}

		}
		return changedComponent;
	}

}
