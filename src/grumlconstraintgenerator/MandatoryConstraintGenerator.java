package grumlconstraintgenerator;

import java.util.ArrayList;
import java.util.HashMap;

import de.uni_koblenz.jgralab.schema.Schema;
import grumlconstraintgenerator.GReQLConstraintVariable.VariableKind;
import ormschema.constraints.RoleSequenceConstraint;
import ormschema.structure.Role;
import ormschema.structure.RoleKind;
import ormschema.structure.RoleProxy;
import ormschema.structure.ValueType;

/**
 * 
 * This class is used to map ORM's mandatory constraint to a grUML constraint.
 * 
 * @author Alicia Owen
 *
 */
public class MandatoryConstraintGenerator extends GrUMLConstraintGenerator {

	public MandatoryConstraintGenerator(HashMap<String, String> xmlIdTogrUMLNameMap, Schema tgSchema) {
		super(xmlIdTogrUMLNameMap, tgSchema);
	}

	@Override
	String createGReQLExpression() {
		String expression = "forall %s @ %s";

		createGReQLConstraintVariables();
		variableList = createVariableDeclaration(definedVariables);

		// must be a mandatory constraint on a role played by an
		// EntityObject in a Fact in which a ValueType is the second role
		// player (all other mandatory constraints don't require a GReQL
		// constraint)
		predicate = String.format("isDefined(%s)", attributeAccessString(
				(ValueType) definedVariables.get(1).getInvolvedObjectType(), definedVariables.get(0)));

		return String.format(expression, variableList, predicate);
	}

	@Override
	void createGReQLConstraintVariables() {

		ArrayList<Role> involvedRoles = util.getConstrainedRoles((RoleSequenceConstraint) ormConstraint).get(0);
		// must be a mandatory constraint on
		// 1) an EntityObject's role
		// 2) the neighboring Role must be played by a ValueType (or no
		// Constraint would be necessary!)
		// 3) the Fact must be binary
		createVariables(involvedRoles.get(0), VariableKind.VERTEXCLASS, 1);

		// retrieve the role played by the neighboring ValueType and create a
		// variable from this role
		RoleKind neighboringRoleKind = util.getNeighboringRole(involvedRoles.get(0));

		if (neighboringRoleKind instanceof RoleProxy) {
			createVariables(((RoleProxy) neighboringRoleKind).get_linkedRole(), VariableKind.VERTEXCLASS, 1);
		} else {
			createVariables((Role) neighboringRoleKind, VariableKind.VERTEXCLASS, 1);
		}
	}

}
