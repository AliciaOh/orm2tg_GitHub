package grumlconstraintgenerator;

import java.util.ArrayList;
import java.util.HashMap;

import de.uni_koblenz.jgralab.schema.Domain;
import de.uni_koblenz.jgralab.schema.Schema;
import grumlconstraintgenerator.GReQLConstraintVariable.VariableKind;
import ormschema.constraints.ValueConstraint;
import ormschema.structure.EntityObject;
import ormschema.structure.Role;
import ormschema.structure.RoleKind;
import ormschema.structure.RoleProxy;
import ormschema.structure.ValueRange;
import ormschema.structure.ValueType;
import utilities.ORMSchemaGraphUtilities.PlayerType;
import utilities.StringManipulations;

/**
 * 
 * This class is used to map ORM's value constraint to a grUML constraint.
 * 
 * @author Alicia Owen
 *
 */

public class ValueConstraintGenerator extends GrUMLConstraintGenerator {

	public ValueConstraintGenerator(HashMap<String, String> xmlIdTogrUMLNameMap, Schema tgSchema) {
		super(xmlIdTogrUMLNameMap, tgSchema);
	}

	@Override
	String createGReQLExpression() throws Exception {

		String expression = "forall %s @ %s";

		createGReQLConstraintVariables();
		variableList = createVariableDeclaration(definedVariables);

		// ---- value constraint on a Role
		if (((ValueConstraint) ormConstraint).get_valueConstrainedRole() != null
				&& ((ValueConstraint) ormConstraint).get_valueConstrainedRole().get_player() instanceof EntityObject) {
			predicate = "%s? %s : true";
			// this ValueConstraint applies to the reference mode of the player
			// of
			// the constrained role iff the constrained role is played
			// 1) need to make sure that the relation between the EntityObject
			// playing the constrained Role
			// and the other Fact participants exists
			String relationsOfConstrainedVariable = listRelationsOfVariable(definedVariables.get(0));

			// 2) formulate expression checking that the values of the
			// EntityObject's reference mode are within the defined ValueRange
			ValueType valueType = util
					.getPreferredIdentifier(((EntityObject) definedVariables.get(0).getInvolvedObjectType()), 0);
			String attributeAccess = attributeAccessString(valueType, definedVariables.get(0));
			String attributeName = xmlIdTogrUMLNameMap.get(valueType.get_xmlId());
			Domain domain = tgSchema.getAttributedElementClass(definedVariables.get(0).getGrUMLTypeName())
					.getAttribute(attributeName).getDomain();

			predicate = String.format(predicate, relationsOfConstrainedVariable,
					valueRangesToString(attributeAccess, domain));
		} else {
			StringBuilder sb = new StringBuilder();
			predicate = "%s";
			// ---- ValueConstraint on a ValueType OR value constraint on a
			// role played by a ValueType
			// need to know for which domain the value range is defined in
			// order to represent the values correctly within the GReQL
			// constraint
			assert definedVariables.size() > 0;

			ValueType rolePlayer = null;
			if (((ValueConstraint) ormConstraint).get_valueConstrainedRole() != null) {
				// ValueConstraint on role played by ValueType
				rolePlayer = (ValueType) ((ValueConstraint) ormConstraint).get_valueConstrainedRole().get_player();
			} else {
				// ValueConstraint on ValueType directly
				rolePlayer = ((ValueConstraint) ormConstraint).get_valueConstrainedValueType();
			}

			String grUMLName = xmlIdTogrUMLNameMap.get(rolePlayer.get_xmlId());

			Domain domain = tgSchema.getAttributedElementClass(definedVariables.get(0).getGrUMLTypeName())
					.getAttribute(grUMLName).getDomain();

			String delimiter = "";
			for (GReQLConstraintVariable variable : definedVariables) {
				String attributeAccess = attributeAccessString(rolePlayer, variable);

				sb.append(delimiter).append(valueRangesToString(attributeAccess, domain));
				delimiter = " and ";

			}
			predicate = String.format(predicate, sb.toString());

		}
		return String.format(expression, variableList, predicate);
	}

	/**
	 * This method receives a {@link GReQLConstraintVariable} as input and
	 * returns all its relations in form of a String (as they present themselves
	 * in the grUML schema, i.e. relations with ValueTypes become attributes).
	 * 
	 * @param variableOfInterest
	 *            - the {@link GReQLConstraintVariable} for which the relations
	 *            should be retrieved in form of a String
	 * @return the String representing the relations of the input variable with
	 *         other elements of the grUML schema (namely VertexClasses and
	 *         attributes)
	 */
	private String listRelationsOfVariable(GReQLConstraintVariable variableOfInterest) {
		StringBuilder sb = new StringBuilder();
		String delimiter = "";
		for (GReQLConstraintVariable variable : definedVariables) {

			if (variable != variableOfInterest) {

				sb.append(delimiter).append(formulateLink(variableOfInterest, variable, ""));
				delimiter = " and ";
			}

		}
		return sb.toString();
	}

	@Override
	void createGReQLConstraintVariables() {
		// value constraint on a role
		// - on the role of a ValueType
		// -> which is a reference mode for some EntityObject
		// -> which is in a relation with an EntityObject
		// - on a role which is played by an EntityObject
		if (((ValueConstraint) ormConstraint).get_valueConstrainedRole() != null) {
			Role valueConstrainedRole = ((ValueConstraint) ormConstraint).get_valueConstrainedRole();
			// on role of a value type
			// -> generate variables for all neighboring roles (since the
			// ValueType will be an attribute of all the neighbors)
			if (valueConstrainedRole.get_player() instanceof ValueType) {
				ArrayList<RoleKind> neighboringRoles = util.getOtherRoles(valueConstrainedRole,
						PlayerType.ENTITYOBJECT);
				for (RoleKind roleKind : neighboringRoles) {
					if (roleKind instanceof RoleProxy) {
						roleKind = ((RoleProxy) roleKind).get_linkedRole();
					}
					createVariable((Role) roleKind, VariableKind.VERTEXCLASS);
				}
			} else {
				// on a role played by an EntityObject
				// -> need variables for all other roles participating in this
				// relation, since I need to make sure they're all present
				// before checking that the EntityObject has values within the
				// defined ValueRange
				createVariable(valueConstrainedRole, VariableKind.VERTEXCLASS);
				ArrayList<RoleKind> neighboringRoles = util.getOtherRoles(valueConstrainedRole, PlayerType.ALL);
				for (RoleKind roleKind : neighboringRoles) {
					if (roleKind instanceof RoleProxy) {
						roleKind = ((RoleProxy) roleKind).get_linkedRole();
					}
					createVariable((Role) roleKind, VariableKind.VERTEXCLASS);
				}
			}

		} else {
			// value constraint on a ValueType
			assert ((ValueConstraint) ormConstraint).get_valueConstrainedValueType() instanceof ValueType;
			ValueType valueType = ((ValueConstraint) ormConstraint).get_valueConstrainedValueType();
			// need to retrieve all EntityObjects which have a relation
			// with this ValueType, since the mapping step will turn the
			// ValueType into attributes of the VertexClasses
			// representing these EntityObjects
			ArrayList<Role> entityObjectRoles = new ArrayList<>();
			for (Role role : valueType.get_playedRole()) {
				// for each fact the ValueType participates in, check whether
				// the
				// other roles are played by EntityObjects. If so, they will
				// receive
				// the ValueType as attribute during mapping.
				util.getOtherRoles(role, PlayerType.ENTITYOBJECT)
						.forEach(otherRole -> entityObjectRoles.add((Role) otherRole));

			}

			for (Role entityObjectRole : entityObjectRoles) {
				// create a variable for each EntityObject (from their roles)
				createVariable(entityObjectRole, VariableKind.VERTEXCLASS);
			}
		}

	}

	/**
	 * This method creates a String from the {@link ValueRange}s defined in an
	 * ORM {@link ValueConstraint}.
	 * 
	 * @param domain
	 *            - the {@link Domain} of the {@link ValueRange}s' values
	 * @return a string of the form [attribute] [operator] [upper/lower bound of
	 *         the {@link ValueRange}] (repeated as often as needed and linked
	 *         through "and")
	 * @throws Exception
	 */
	private String valueRangesToString(String attributeAccess, Domain domain) throws Exception {
		StringBuilder sb = new StringBuilder();

		String delimiter = "";
		for (ValueRange valueRange : ((ValueConstraint) ormConstraint).get_valueRange()) {

			String minValue = valueRange.get_minValue();
			String maxValue = valueRange.get_maxValue();

			String s = "%s %s %s";
			if (!maxValue.equals("") || !minValue.equals("")) {
				sb.append(delimiter);
				delimiter = " and ";

				String innerDelimiter = "";
				if (!minValue.equals("")) {
					minValue = adapatStringToDomain(minValue, domain);
					sb.append(String.format(s, attributeAccess, " >= ", minValue));
					innerDelimiter = " and ";
				}

				if (!maxValue.equals("")) {
					maxValue = adapatStringToDomain(maxValue, domain);
					sb.append(innerDelimiter).append(String.format(s, attributeAccess, " <= ", maxValue));
				}

			}
		}
		return sb.toString();
	}

	private String adapatStringToDomain(String someValue, Domain domain) throws Exception {
		String domainName = domain.getJavaAttributeImplementationTypeName(tgSchema.getPackagePrefix());

		String adjustedValue = someValue;
		switch (domainName) {
		case "int":
		case "java.lang.Double":
		case "java.lang.Long":
			break;
		case "java.lang.String":
			adjustedValue = "\"" + adjustedValue + "\"";
			break;
		case "boolean":
			if (representsBooleanValue(adjustedValue)) {
				adjustedValue = StringManipulations.firstCharToLowerCase(someValue);
				break;
			} else {
				throw new Exception(
						"The type of the provided value should be boolean, but the provided String does not match the following pattern ^([Tt]rue|[Ff]alse)$");
			}

		default:
			throw new Exception("The data type of the ValueType provided in ORM maps to the unexpected data type "
					+ domainName + ". Values from this domain cannot be mapped to grUML yet.");
		}
		return adjustedValue;
	}

	private boolean representsBooleanValue(String adjustedValue) {

		return adjustedValue.matches("^([Tt]rue|[Ff]alse)$");
	}
}
