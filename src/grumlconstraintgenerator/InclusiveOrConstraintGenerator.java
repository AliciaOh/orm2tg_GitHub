package grumlconstraintgenerator;

import java.util.ArrayList;
import java.util.HashMap;

import de.uni_koblenz.jgralab.schema.Schema;
import grumlconstraintgenerator.GReQLConstraintVariable.VariableKind;
import ormschema.constraints.RoleSequenceConstraint;
import ormschema.structure.EntityObject;
import ormschema.structure.ObjectType;
import ormschema.structure.Role;
import ormschema.structure.ValueType;

/**
 * This class is used to generate a GReQL constraint which expresses an ORM
 * disjunctive mandatory constraint.
 * 
 * @author Alicia Owen
 *
 */
public class InclusiveOrConstraintGenerator extends GrUMLConstraintGenerator {

	public InclusiveOrConstraintGenerator(HashMap<String, String> xmlIdTogrUMLNameMap, Schema tgSchema) {
		super(xmlIdTogrUMLNameMap, tgSchema);

	}

	@Override
	String createGReQLExpression() throws Exception {

		String expression = "forall %s @ %s";

		createGReQLConstraintVariables();
		variableList = createVariableDeclaration(definedVariables);

		StringBuilder sb = new StringBuilder();
		String delimiter = "";
		assert definedVariables.size() > 0;
		GReQLConstraintVariable previousVariable = definedVariables.get(0);
		GReQLConstraintVariable lastNonMuteVariable = definedVariables.get(0);

		boolean processingVertexClass = true;

		String predicateForVertexClassWrapper = "%s > 0";
		String predicateForVertexClass = "%s degree{%s}(%s)";
		StringBuilder predicateForVertexClassBuilder = new StringBuilder();

		String predicateForAttribute = "%s isDefined(%s)";
		StringBuilder predicateForAttributeBuilder = new StringBuilder();

		String innerDelimiter = "";
		// lists the edge names in string form, i.e. "edge1, edge2, edge3"
		StringBuilder edgeEnumeration = new StringBuilder();
		int count = 0;
		for (GReQLConstraintVariable var : definedVariables) {
			if (var.getInvolvedObjectType() instanceof ValueType) {
				// TODO this doesn't work correctly.
				String attributeAccess = attributeAccessString(
						(ValueType) ((Role) util.getNeighboringRole(var.getInvolvedRole())).get_player(),
						(EntityObject) var.getInvolvedObjectType());
				delimiter = predicateForAttributeBuilder.length() > 0 ? " or" : "";

				predicateForAttributeBuilder.append(String.format(predicateForAttribute, delimiter, attributeAccess));
			} else {
				// check whether the role next to the constrained role is played
				// by a ValueType
				// (if it is, use "predicateForAttribute")

				if (util.getFactArity(var.getInvolvedRole().get_fact()) == 2
						&& ((Role) util.getNeighboringRole(var.getInvolvedRole())).get_player() instanceof ValueType) {

					// append the corresponding strings for the data collected
					// this far
					String variableName = var.isMute ? lastNonMuteVariable.getName() : previousVariable.getName();
					if (processingVertexClass) {

						predicateForVertexClassBuilder.append(String.format(predicateForVertexClassWrapper, String
								.format(predicateForVertexClass, delimiter, edgeEnumeration.toString(), variableName)));
						// refresh the StringBuilder
						edgeEnumeration.setLength(0);
						innerDelimiter = "";

					} else {
						predicateForAttributeBuilder
								.append(String.format(predicateForAttribute, delimiter, variableName));
					}
					// now process the data at hand
					processingVertexClass = false;
					ValueType player = (ValueType) ((Role) util.getNeighboringRole(var.getInvolvedRole())).get_player();
					String attributeAccess = attributeAccessString(player, lastNonMuteVariable);
					delimiter = " or";
					predicateForAttributeBuilder
							.append(String.format(predicateForAttribute, delimiter, attributeAccess));
				} else {
					// the role next to the constrained role is played by an
					// entity type or objectification
					processingVertexClass = true;
					String grUMLFactName = xmlIdTogrUMLNameMap.get(var.getFact().get_xmlId());

					innerDelimiter = edgeEnumeration.length() > 0 ? ", " : "";
					// case 1 : this variable refers to constrained role which
					// is
					// played
					// by a different object type (compared to the previous
					// variable)
					if (var.getInvolvedObjectType() != previousVariable.getInvolvedObjectType()) {
						// add everything up to this point and start over
						// (format
						// "%s
						// degree{%s}(%s)")
						String variableName = var.isMute ? lastNonMuteVariable.getName() : previousVariable.getName();
						predicateForVertexClassBuilder.append(String.format(predicateForVertexClass, delimiter,
								edgeEnumeration.toString(), variableName));
						// refresh the StringBuilder
						edgeEnumeration.setLength(0);
						innerDelimiter = "";
						edgeEnumeration.append(innerDelimiter).append(grUMLFactName);
					} else {
						// case 2: this variable refers to a constrained role
						// which
						// is played by the same object type compared to the
						// (different!)
						// role in the previous variable
						edgeEnumeration.append(innerDelimiter).append(grUMLFactName);

					}

					// check whether the loop is about to end
					if (count == definedVariables.size() - 1) {
						String variableName = var.isMute ? lastNonMuteVariable.getName() : var.getName();
						// format "%s degree{%s}(%s)"
						predicateForVertexClassBuilder.append(
								String.format(predicateForVertexClass, delimiter, edgeEnumeration, variableName));

					}

				}

			}
			// make progress in the loop
			previousVariable = var;
			if (!var.isMute()) {
				lastNonMuteVariable = var;
			}
			++count;
		}

		sb.append(String.format(predicateForVertexClassWrapper, predicateForVertexClassBuilder.toString()));

		predicate = sb.toString() + predicateForAttributeBuilder.toString();

		return String.format(expression, variableList, predicate);

	}

	@Override
	void createGReQLConstraintVariables() {
		ArrayList<Role> involvedRoles = util.getConstrainedRoles((RoleSequenceConstraint) ormConstraint).get(0);

		assert involvedRoles.size() > 0;
		ObjectType previousPlayer = involvedRoles.get(0).get_player();
		int count = 0;
		for (Role role : involvedRoles) {
			// roles played by ValueTypes and roles played by the same player as
			// the previous one are set to mute

			if (role.get_player() instanceof ValueType) {
				// if the constrained role is played by a value type, then the
				// variables need to be generated from the neighboring entity
				// types or objectifiedtypes (because these need to be declared
				// in the constraint)
				createVariables((Role) util.getNeighboringRole(role), VariableKind.VERTEXCLASS, 1);
			} else if (previousPlayer == role.get_player() && count > 0) {
				createVariables(role, VariableKind.VERTEXCLASS, 1);
				definedVariables.get(definedVariables.size() - 1).setMute(true);
			} else {
				createVariables(role, VariableKind.VERTEXCLASS, 1);
			}
			// progress in loop
			previousPlayer = role.get_player();
			++count;
		}

	}

}
