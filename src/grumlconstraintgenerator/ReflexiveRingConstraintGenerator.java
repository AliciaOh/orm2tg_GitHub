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
 * This class is used to map ORM's reflexive ring constraint to a grUML
 * constraint.
 * 
 * @author Alicia Owen
 *
 */
public class ReflexiveRingConstraintGenerator extends GrUMLConstraintGenerator {
	public ReflexiveRingConstraintGenerator(HashMap<String, String> xmlIdTogrUMLNameMap, Schema tgSchema) {
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

			predicate = "alpha(%s) -->{%s} alpha(%s) and omega(%s) -->{%s} omega(%s)";
			predicate = String.format(predicate, v1.getName(), v1.getGrUMLTypeName(), v1.getName(), v1.getName(),
					v1.getGrUMLTypeName(), v1.getName());
		} else {
			predicate = "not(%s) or (alpha(%s)-->{%s}<--{%s} alpha(%s) and alpha(%s)-->{%s}<--{%s} alpha(%s))";
			predicate = String.format(predicate, formulateLink(v1, v2, ""), v1.getName(), v1.getGrUMLTypeName(),
					v2.getGrUMLTypeName(), v1.getName(), v2.getName(), v1.getGrUMLTypeName(), v2.getGrUMLTypeName(),
					v2.getName());

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
