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
 * This class is used to map ORM's acyclic ring constraint to a grUML
 * constraint.
 * 
 * @author Alicia Owen
 *
 */
public class AcyclicRingConstraintGenerator extends GrUMLConstraintGenerator {

	public AcyclicRingConstraintGenerator(HashMap<String, String> xmlIdTogrUMLNameMap, Schema tgSchema) {
		super(xmlIdTogrUMLNameMap, tgSchema);
	}

	@Override
	void createGReQLConstraintVariables() {

		// get the roles involved in the constraint
		assert util.getRoleSequenceCount((RoleSequenceConstraint) ormConstraint) == 1;
		ArrayList<Role> involvedRoles = util.getConstrainedRoles((RoleSequenceConstraint) ormConstraint).get(0);

		involvedRoles = sortToMatchFactReading(involvedRoles);
		Fact fact = util.getFact((RoleSequenceConstraint) ormConstraint);
		if (util.getFactArity(fact) == 2) {
			createVariables(involvedRoles.get(0), VariableKind.VERTEXCLASS, 1);
		} else {
			for (Role role : involvedRoles) {
				createVariables(role, VariableKind.VERTEXCLASS, 1);
			}
		}

	}

	@Override
	String createGReQLExpression() {
		String expression = "forall %s @ %s";

		createGReQLConstraintVariables();
		variableList = createVariableDeclaration(definedVariables);

		Fact fact = util.getFact((RoleSequenceConstraint) ormConstraint);
		if (util.getFactArity(fact) == 2) {
			predicate = "not(%s %s)";
			predicate = String.format(predicate, formulateEdgeFrom(definedVariables.get(0), "+"),
					definedVariables.get(0).getName());
		} else {
			predicate = "not(%s(%s %s)+ %s)";
			predicate = String.format(predicate, definedVariables.get(0).getName(),
					formulateEdgeFrom(definedVariables.get(0), "", true),
					formulateEdgeTo(definedVariables.get(1), "", true), definedVariables.get(1).getName());

		}

		return String.format(expression, variableList, predicate);
	}

}
