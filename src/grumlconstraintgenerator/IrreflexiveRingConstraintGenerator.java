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
 * This class is used to map ORM's irreflexive ring constraint to a grUML
 * constraint.
 * 
 * @author Alicia Owen
 *
 */
public class IrreflexiveRingConstraintGenerator extends GrUMLConstraintGenerator {

	public IrreflexiveRingConstraintGenerator(HashMap<String, String> xmlIdTogrUMLNameMap, Schema tgSchema) {
		super(xmlIdTogrUMLNameMap, tgSchema);
		// TODO Auto-generated constructor stub
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

			predicate = "omega(%s) <> alpha(%s)";
			predicate = String.format(predicate, v1.getName(), v1.getName());
		} else {
			predicate = "not(alpha(%s) = alpha(%s) and omega(%s) = omega(%s))";
			predicate = String.format(predicate, v1.getName(), v2.getName(), v1.getName(), v2.getName());

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
			createVariables(role, VariableKind.EDGECLASS, 1);
		}

	}
}
