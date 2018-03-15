package eca;

import java.util.Set;

import de.uni_koblenz.jgralab.eca.Condition;
import de.uni_koblenz.jgralab.eca.events.ChangeAttributeEvent;
import de.uni_koblenz.jgralab.eca.events.Event;
import de.uni_koblenz.jgralab.graphvalidator.ConstraintViolation;
import de.uni_koblenz.jgralab.graphvalidator.GraphValidator;
import de.uni_koblenz.jgralab.schema.AttributedElementClass;

/**
 * 
 * This class defines a condition which is evaluated if an attribute value
 * changes. The condition checks whether the TGraph is valid by using JGraLab's
 * GraphValidator.
 * 
 * @author Alicia Owen
 *
 */

public class GraphValidityCondition<AEC extends AttributedElementClass<AEC, ?>> implements Condition<AEC> {

	@Override
	public boolean evaluate(Event<AEC> event) {
		assert event instanceof ChangeAttributeEvent;

		GraphValidator gv = new GraphValidator(event.getGraph());
		Set<ConstraintViolation> violations = gv.validate();
		if (violations.size() > 0) {
			System.out.println("The graph is invalid, please correct the following errors before proceeding!");
		}
		for (ConstraintViolation violation : gv.validate()) {
			System.out.println(violation.toString());
		}
		return violations.size() == 0 ? true : false;
	}
}
