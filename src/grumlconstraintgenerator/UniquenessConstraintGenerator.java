package grumlconstraintgenerator;

import java.util.ArrayList;
import java.util.HashMap;

import de.uni_koblenz.jgralab.schema.Schema;
import grumlconstraintgenerator.GReQLConstraintVariable.VariableKind;
import ormschema.constraints.RoleSequenceConstraint;
import ormschema.constraints.UniquenessConstraint;
import ormschema.structure.EntityObject;
import ormschema.structure.Role;

/**
 * 
 * This class is used to map ORM's uniqueness constraint to a grUML constraint.
 * 
 * @author Alicia Owen
 *
 */

public class UniquenessConstraintGenerator extends GrUMLConstraintGenerator {

	public UniquenessConstraintGenerator(HashMap<String, String> xmlIdTogrUMLNameMap, Schema tgSchema) {
		super(xmlIdTogrUMLNameMap, tgSchema);
	}

	@Override
	String createGReQLExpression() {
		String expression = "forall %s @ %s";
		int factArity = util.getFactArity(util.getFact((RoleSequenceConstraint) ormConstraint));
		createGReQLConstraintVariables();
		variableList = createVariableDeclaration(definedVariables);

		GReQLConstraintVariable v1 = definedVariables.get(0);
		GReQLConstraintVariable v2 = definedVariables.get(1);

		if (!((UniquenessConstraint) ormConstraint).is_isInterpredicateConstraint() && factArity == 2) {
			// n:m constraint on binary fact type
			predicate = "%s <> %s ? alpha(%s) <> alpha(%s) or omega(%s) <> omega(%s) : true";
			predicate = String.format(predicate, v1.getName(), v2.getName(), v1.getName(), v2.getName(), v1.getName(),
					v2.getName());

		} else if (!((UniquenessConstraint) ormConstraint).is_isInterpredicateConstraint() && factArity > 2) {
			// n:m on ternary or higher arity fact type
			ArrayList<Role> involvedRoles = util.getConstrainedRoles(((UniquenessConstraint) ormConstraint)).get(0);
			Role objectifiedFactRole = getObjectifiedFactRole(involvedRoles.get(0));
			// create an additional variable for the nested GReQL constraint
			createVariables(objectifiedFactRole, VariableKind.VERTEXCLASS, 1);

			String predicate = "count(%s) <= 1";
			String innerExpression = "from %s: V{%s} with %s report %s end";

			int counter = 0;
			StringBuilder sb = new StringBuilder();
			while (counter < involvedRoles.size() - 1) {

				sb.append(formulateLink(definedVariables.get(counter), definedVariables.get(counter + 1),
						definedVariables.get(involvedRoles.size()), "", false, true));
				++counter;
			}
			sb.append(definedVariables.get(counter).getName());

			innerExpression = String.format(innerExpression, definedVariables.get(involvedRoles.size()).getName(),
					definedVariables.get(involvedRoles.size()).getGrUMLTypeName(), sb.toString(),
					definedVariables.get(counter).getName());

			this.predicate = String.format(predicate, innerExpression);

		} else if (((UniquenessConstraint) ormConstraint).is_isInterpredicateConstraint()) {
			// TODO external uniqueness constraint

		}

		return String.format(expression, variableList, predicate);
	}

	@Override
	void createGReQLConstraintVariables() {

		ArrayList<Role> involvedRoles = util.getConstrainedRoles((RoleSequenceConstraint) ormConstraint).get(0);

		if (!((UniquenessConstraint) ormConstraint).is_isInterpredicateConstraint()) {
			boolean condition1 = util.getFactArity(util.getFact((UniquenessConstraint) ormConstraint)) == 2;
			boolean condition2 = involvedRoles.get(0).get_player() instanceof EntityObject;
			boolean condition3 = involvedRoles.get(1).get_player() instanceof EntityObject;

			if (condition1 && condition2 && condition3) {

				// binary fact containing only EntityTypes:
				// need two variables representing the same edge (it doesn't
				// matter which of the two roles is selected)

				createVariables(involvedRoles.get(0), VariableKind.EDGECLASS, 2);

			} else {

				// a ternary or higher arity fact containing only
				// EntityTypes:

				for (Role role : involvedRoles) {
					createVariables(role, VariableKind.VERTEXCLASS, 1);
				}
			}

		} else {
			for (Role role : involvedRoles) {
				createVariables(role, VariableKind.VERTEXCLASS, 1);
			}

		}

	}

	/**
	 * @param involvedRoles
	 * @return
	 */
	private Role getObjectifiedFactRole(Role neighboringRole) {
		Role objectifiedFactRole;
		if (neighboringRole.get_roleProxy() != null) {
			objectifiedFactRole = (Role) util.getNeighboringRole(neighboringRole.get_roleProxy());
		} else {
			objectifiedFactRole = (Role) util.getNeighboringRole(neighboringRole);
		}
		return objectifiedFactRole;

	}

}
