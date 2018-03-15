package grumlconstraintgenerator;

import java.util.ArrayList;
import java.util.HashMap;

import de.uni_koblenz.jgralab.schema.Schema;
import grumlconstraintgenerator.GReQLConstraintVariable.VariableKind;
import ormschema.constraints.RoleSequenceConstraint;
import ormschema.structure.Fact;
import ormschema.structure.Role;

/**
 * 
 * This class is used to map ORM's transitive ring constraint to a grUML
 * constraint.
 * 
 * @author Alicia Owen
 *
 */

public class TransitiveRingConstraintGenerator extends GrUMLConstraintGenerator {
	public TransitiveRingConstraintGenerator(HashMap<String, String> xmlIdTogrUMLNameMap, Schema tgSchema) {
		super(xmlIdTogrUMLNameMap, tgSchema);
	}

	@Override
	String createGReQLExpression() {
		String expression = "forall %s @ %s";

		createGReQLConstraintVariables();
		variableList = createVariableDeclaration(definedVariables);

		Fact fact = util.getFact((RoleSequenceConstraint) ormConstraint);
		GReQLConstraintVariable v1 = definedVariables.get(0);
		GReQLConstraintVariable v2 = definedVariables.get(1);
		if (util.getFactArity(fact) == 2) {

			predicate = "%s? %s : true";
			predicate = String.format(predicate, formulateLink(v1, v2, "^2"), formulateLink(v1, v2, ""));
		} else {
			predicate = "%s? %s : true";
			predicate = String.format(predicate, formulateLink(v1, v2, "^2"), formulateLink(v1, v2, ""));

		}

		return String.format(expression, variableList, predicate);
	}

	@Override
	void createGReQLConstraintVariables() {

		// get the roles involved in the constraint
		assert util.getRoleSequenceCount((RoleSequenceConstraint) ormConstraint) == 1;
		ArrayList<Role> involvedRoles = util.getConstrainedRoles((RoleSequenceConstraint) ormConstraint).get(0);

		involvedRoles = sortToMatchFactReading(involvedRoles);
		for (Role role : involvedRoles) {
			createVariables(role, VariableKind.VERTEXCLASS, 1);
		}

	}
}
