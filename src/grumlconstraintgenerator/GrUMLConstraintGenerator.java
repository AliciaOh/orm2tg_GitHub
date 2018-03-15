package grumlconstraintgenerator;

import java.util.ArrayList;
import java.util.HashMap;

import de.uni_koblenz.jgralab.schema.AttributedElementClass;
import de.uni_koblenz.jgralab.schema.Schema;
import de.uni_koblenz.jgralab.schema.VertexClass;
import de.uni_koblenz.jgralab.schema.impl.ConstraintImpl;
import grumlconstraintgenerator.GReQLConstraintVariable.VariableKind;
import ormschema.constraints.Constraint;
import ormschema.structure.EntityObject;
import ormschema.structure.Fact;
import ormschema.structure.ObjectType;
import ormschema.structure.ObjectifiedType;
import ormschema.structure.Role;
import ormschema.structure.RoleKind;
import ormschema.structure.RoleProxy;
import ormschema.structure.ValueType;
import utilities.ORMSchemaGraphUtilities;
import utilities.StringManipulations;

/**
 * 
 * This class defines methods and variables required to map ORM's constraints to
 * grUML constraints.
 * 
 * @author Alicia Owen
 *
 */

public abstract class GrUMLConstraintGenerator {

	protected String variableList;
	protected String predicate;

	protected ORMSchemaGraphUtilities util;
	/**
	 * A map that holds {@link EntityObject}s as keys and {@link VertexClass}es
	 * as values. Required to retrieve {@link VertexClass}es when processing
	 * {@link Fact}s from the ORM schema graph.
	 */
	protected HashMap<String, String> xmlIdTogrUMLNameMap;

	/**
	 * This list holds all variables defined for the current GReQL constraint
	 */
	protected ArrayList<GReQLConstraintVariable> definedVariables;

	/**
	 * The ORM constraint that should be formulated using GReQL
	 */
	protected Constraint ormConstraint;

	protected Schema tgSchema;

	public GrUMLConstraintGenerator(HashMap<String, String> xmlIdTogrUMLNameMap, Schema tgSchema) {
		this.xmlIdTogrUMLNameMap = xmlIdTogrUMLNameMap;
		this.tgSchema = tgSchema;
	}

	public ConstraintImpl generateGReQLConstraint(Constraint ormConstraint) {
		this.ormConstraint = ormConstraint;
		util = new ORMSchemaGraphUtilities();

		initializeLists();
		String message = createGReQLMessage();
		if (!message.equals("")) {
			System.out.println(message);
		}
		try {
			String expression = createGReQLExpression();
			if (!expression.equals("")) {
				System.out.println(expression);
			}
		} catch (Exception e) {
			System.out.println("An error ocurred: " + e.getMessage());

		}

		String offendingElements = createGReQLOffendingElements();
		if (!offendingElements.equals("")) {
			System.out.println(offendingElements);
		}

		ConstraintImpl constraint = new ConstraintImpl(message, predicate, offendingElements);
		return constraint;
	}

	/**
	 * This method creates the GReQL message string that is returned if a GReQL
	 * constraint is not met
	 * 
	 * @return
	 */
	String createGReQLMessage() {
		GrUMLConstraintMessageGenerator messageGen = new GrUMLConstraintMessageGenerator(ormConstraint);
		return messageGen.createMessage();
	}

	abstract String createGReQLExpression() throws Exception;

	/**
	 * This method returns the String needed in a GReQL constraint to retrieve
	 * offending elements. This method will report the set of all variables
	 * defined in the GReQL constraint.
	 * 
	 * @return the String needed to retrieve the
	 *         {@link AttributedElementClass}es that are violating the GReQL
	 *         constraint.
	 */
	protected String createGReQLOffendingElements() {
		String offendingElements = "from %s with not(%s) reportSet %s end";
		String enumerationOfVariableNames = listVariableNames();

		return String.format(offendingElements, variableList, predicate, enumerationOfVariableNames);
	}

	abstract void createGReQLConstraintVariables();

	protected void initializeLists() {
		definedVariables = new ArrayList<>();
	}

	protected void createVariable(Role role, VariableKind variableKind) {
		createVariables(role, variableKind, 1);
	}

	/**
	 * This method is used to create {@link GReQLConstraintVariable}s from the
	 * input role.
	 * 
	 * @param role
	 *            - the {@link Role} from which the
	 *            {@link GReQLConstraintVariable} is generated
	 * @param variableKind
	 *            - the kind of variable that should be created (an EdgeClass
	 *            oder VertexClass)
	 * @param count
	 *            - the number of variables that should be generated for the
	 *            input role
	 */
	protected void createVariables(Role role, VariableKind variableKind, int count) {
		Fact fact;

		fact = getGrUMLFact(role);

		String typeName;
		ObjectType rolePlayer = util.getPlayer(role);

		if (variableKind == VariableKind.VERTEXCLASS || role.get_player() instanceof ValueType) {
			// if the variable will represent a VertexClass (isVertexClass ==
			// true) or if the variable stands for a ValueType, retrieve the
			// grUML name of the Role player
			typeName = xmlIdTogrUMLNameMap.get(rolePlayer.get_xmlId());
		} else {
			// if the variable represents an EdgeClass, retrieve the grUML name
			// of the Fact the Role is involved in
			typeName = xmlIdTogrUMLNameMap.get(fact.get_xmlId());
		}

		int positionInFact = util.getPositionOfRoleInFact(role, fact);

		boolean isMute = false;
		String variableName = "";
		// if the role player is a ValueType, don't add this variable to the
		// variable declaration
		// if the variable stands for an EdgeClass and a variable representing
		// this EdgeClass already exists, don't add this variable to the
		// variable declaration
		if (role.get_player() instanceof ValueType
				|| (variableKind != VariableKind.VERTEXCLASS && variableOfTypeExists(typeName))) {
			isMute = true;
		}

		int i = 0;
		while (i < count) {
			// don't bother creating a unique variable name if the variable is
			// mute!
			if (!isMute) {
				variableName = createVariableName(typeName);
			}
			definedVariables.add(new GReQLConstraintVariable(variableName, typeName, role, rolePlayer, variableKind,
					positionInFact, isMute));
			++i;
		}
	}

	/**
	 * Given a {@link Role}, this method will return the {@link Fact} which is
	 * represented in the grUML schema. One {@link Role} may participate in at
	 * most two {@link Fact}s:
	 * <ol>
	 * <li>in any Fact</li>
	 * <li>if the Fact in 1) is ternary or higher in arity, this Fact is
	 * objectified and ORM defines a {@link RoleProxy} for each of the
	 * {@link Role}s in the original {@link Fact}. These {@link RoleProxy}s
	 * participate in binary Facts with an {@link ObjectifiedType} which
	 * represents the (ternary or higher arity) {@link Fact}.</li>
	 * </ol>
	 * Since grUML also only represents binary relations, this method will
	 * always return the {@link Fact} the {@link RoleProxy} is involved in, if
	 * the input {@link Role} has a {@link RoleProxy}.
	 * 
	 * @param role
	 *            - a {@link Role} from an ORM {@link Fact}
	 * @return the {@link Fact} that is represented in the grUML schema (if the
	 *         input Role participated in a ternary or higher arity fact, this
	 *         will return the implied fact!)
	 */
	private Fact getGrUMLFact(Role role) {
		Fact fact;
		if (role.get_roleProxy() != null) {
			fact = role.get_roleProxy().get_fact();
		} else {
			fact = role.get_fact();
		}
		return fact;
	}

	protected boolean variableOfTypeExists(String typeName) {
		for (GReQLConstraintVariable variable : definedVariables) {
			if (variable.getGrUMLTypeName().equals(typeName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A method that creates the variable declaration string for the given
	 * VertexClass/EdgeClass type
	 * 
	 * @param typeName
	 *            - the type of EdgeClass/VertexClass for which variables are
	 *            defined
	 * @param variableCount
	 *            - the number of variables that need defining for this type
	 * @param isVertexClass
	 *            - true, if the variables are defined as instances of a
	 *            VertexClass. False if they're defined as instanced of an
	 *            EdgeClass.
	 * @return the String with the definition of the required number of
	 *         variables of the requested type.
	 */
	protected String createVariableDeclaration(ArrayList<GReQLConstraintVariable> variables) {
		StringBuilder sb = new StringBuilder();

		int i = 0;
		while (i < variables.size() && variables.get(i).isMute()) {
			++i;
		}

		if (i < variables.size()) {
			GReQLConstraintVariable previousVariable = variables.get(i);

			String delimiter = "";

			int counter = i;
			for (GReQLConstraintVariable variable : variables) {
				if (variable.isMute()) {
					// don't print variables that represent ValueTypes in the
					// variable declaration
					// check whether this is the last variable!
					if (counter == variables.size() - 1) {
						sb.append(": " + previousVariable.declareType());
					}
					++counter;
					continue;
				}
				if (previousVariable.getGrUMLTypeName().equals(variable.getGrUMLTypeName())) {
					sb.append(delimiter).append(variable.getName());
				} else {
					String variableTypeDeclaration = ": " + previousVariable.declareType();
					sb.append(variableTypeDeclaration).append(delimiter).append(variable.getName());
				}
				delimiter = ", ";

				if (!variable.isMute) {
					previousVariable = variable;
				}

				if (counter == variables.size() - 1) {

					sb.append(": " + variable.declareType());
				}
				++counter;

			}
			return sb.toString();
		} else {
			return "";
		}
	}

	/**
	 * A method to retrieve the possibly transformed name of the ORM
	 * {@link ObjectType}. The method uses the unique XML ID of the ORM Element
	 * to retrieve the mapped name.
	 * 
	 * @param objectType
	 *            - the {@link ObjectType} whose name may have been changed
	 *            during the mapping process
	 * @return the grUML name of the input {@link ObjectType}
	 */
	protected String getGrUMLName(String xmlID) {

		if (xmlIdTogrUMLNameMap.get(xmlID) != null) {
			return xmlIdTogrUMLNameMap.get(xmlID);
		} else {
			return "";
		}
	}

	protected ArrayList<Role> sortToMatchFactReading(ArrayList<Role> listOfRoles) {
		assert util.allRolesFromSingleFact(listOfRoles);
		assert listOfRoles.size() > 0;

		Fact fact = listOfRoles.get(0).get_fact();
		ArrayList<Role> orderedRoles = new ArrayList<>();

		// iterate over the ordered RoleKinds of the fact
		// if they match one of the constrainedRoles, add this role (it will be
		// a Role, since none of the constraints apply to RoleProxys!) to the
		// ordered list
		for (RoleKind roleKind : fact.get_factRole()) {
			if (listOfRoles.contains(roleKind)) {
				orderedRoles.add((Role) roleKind);
			}
		}

		return orderedRoles;
	}

	/**
	 * This method checks whether the input String "name" already exists amongst
	 * the previously defined variables.
	 * 
	 * @param name
	 * @return
	 */
	protected boolean containedInVariableNames(String name) {

		if (definedVariables.size() == 0) {
			return false;
		}

		for (GReQLConstraintVariable variable : definedVariables) {
			if (variable.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method formulates the link between two
	 * {@link GReQLConstraintVariable}s representing {@link ObjectType}s. The
	 * link refers to the way the grUML VertexClasses or attributes relate to
	 * each other. So for an input of two variables representing VertexClasses
	 * in grUML, the result will be of the form [variable name 1] -->{[name of
	 * EdgeClass connecting VertexClasses]} [variable name 2]. If variable 2
	 * represented an attribute of the VertexClass represented by variable 1,
	 * then it would return: [variable 1 name].[name of attribute represented by
	 * variable 2] If the input variables represent an EdgeClass, the variables
	 * still refers to a specific role of the relation (i.e. one end of the
	 * EdgeClass). Depending to the position within the EdgeClass, this method
	 * will insert alpha([variable role player]) for the origin of the EdgeClass
	 * and omega([variable role player]) for the destination of the EdgeClass.
	 * 
	 * @param sourceVariable
	 *            -
	 * @param targetVariable
	 *            -
	 * @return
	 */
	protected String formulateLink(GReQLConstraintVariable sourceVariable, GReQLConstraintVariable targetVariable,
			GReQLConstraintVariable objectifiedFact, String quantifier, boolean muteSource, boolean muteTarget) {
		assert sourceVariable != null && targetVariable != null
				? sourceVariable.getInvolvedRole().get_fact() == targetVariable.getInvolvedRole().get_fact() : true;
		boolean muteSourceVar = muteSource;
		boolean muteTargetVar = muteTarget;

		if (sourceVariable == null) {
			muteSourceVar = true;
		}
		if (targetVariable == null) {
			muteTargetVar = true;
		}

		if (sourceVariable != null && targetVariable != null
				&& (sourceVariable.getInvolvedObjectType() instanceof ValueType
						|| targetVariable.getInvolvedObjectType() instanceof ValueType)) {
			return createAttributeAccess(sourceVariable, targetVariable, muteSourceVar, muteTargetVar);
		}

		if (sourceVariable != null && targetVariable != null) {
			if (sourceVariable.getInvolvedRole().get_roleProxy() != null
					&& targetVariable.getInvolvedRole().get_roleProxy() != null) {
				return createIndirectLink(sourceVariable, targetVariable, objectifiedFact, quantifier, muteSourceVar,
						muteTargetVar);
			}
		}
		return createDirectLink(sourceVariable, targetVariable, quantifier, muteSourceVar, muteTargetVar);

	}

	private String createIndirectLink(GReQLConstraintVariable sourceVariable, GReQLConstraintVariable targetVariable,
			GReQLConstraintVariable objectifiedFact, String quantifier, boolean muteSourceVar, boolean muteTargetVar) {

		String pathViaObjectification = "%s %s-->{%s} %s <--{%s}%s%s %s";
		String v1 = muteSourceVar ? "" : sourceVariable.printObjectType();
		String v2 = muteTargetVar ? "" : targetVariable.printObjectType();

		String edgeClassFromSourceName = xmlIdTogrUMLNameMap.get(sourceVariable.getFact().get_xmlId());
		String edgeClassFromTargetName = xmlIdTogrUMLNameMap.get(targetVariable.getFact().get_xmlId());

		String leftBracket = quantifier.equals("") ? "" : "(";
		String rightBracket = quantifier.equals("") ? "" : ")";
		return String.format(pathViaObjectification, v1, leftBracket, edgeClassFromSourceName,
				objectifiedFact == null ? "" : objectifiedFact.getName(), edgeClassFromTargetName, rightBracket,
				quantifier, v2);

	}

	/**
	 * @param sourceVariable
	 * @param targetVariable
	 * @param muteSourceVar
	 * @param muteTargetVar
	 */
	private String createAttributeAccess(GReQLConstraintVariable sourceVariable, GReQLConstraintVariable targetVariable,
			boolean muteSourceVar, boolean muteTargetVar) {
		if (!(muteSourceVar || muteTargetVar)) {
			if (sourceVariable.getInvolvedObjectType() instanceof ValueType) {
				return attributeAccessString((ValueType) sourceVariable.getInvolvedObjectType(), targetVariable);
			} else {
				assert targetVariable.getInvolvedObjectType() instanceof ValueType;
				return attributeAccessString((ValueType) targetVariable.getInvolvedObjectType(), sourceVariable);
			}
		} else {
			return "";
		}
	}

	/**
	 * @param sourceVariable
	 * @param targetVariable
	 * @param quantifier
	 * @param muteSourceVar
	 * @param muteTargetVar
	 * @return
	 */
	private String createDirectLink(GReQLConstraintVariable sourceVariable, GReQLConstraintVariable targetVariable,
			String quantifier, boolean muteSourceVar, boolean muteTargetVar) {

		String edgeDeclaration = "";
		String edgeClassName = sourceVariable == null ? xmlIdTogrUMLNameMap.get(targetVariable.getFact().get_xmlId())
				: xmlIdTogrUMLNameMap.get(sourceVariable.getFact().get_xmlId());

		StringBuilder sourceVar = new StringBuilder();
		StringBuilder targetVar = new StringBuilder();

		if (sourceVariable != null && sourceVariable.getPositionInFact() == 0) {
			assert targetVariable != null ? targetVariable.getPositionInFact() == 1
					|| targetVariable.getInvolvedObjectType() == sourceVariable.getInvolvedObjectType() : true;
			edgeDeclaration = "%s -->{%s}%s %s";
			if (sourceVariable.getVariableKind() != VariableKind.VERTEXCLASS) {
				sourceVar = muteSourceVar ? sourceVar.append("")
						: sourceVar.append(" alpha(").append(sourceVariable.getName()).append(")");
				targetVar = muteTargetVar ? targetVar.append("")
						: targetVar.append(" omega(").append(targetVariable.getName()).append(")");
				edgeDeclaration = String.format(edgeDeclaration, sourceVar, edgeClassName, quantifier, targetVar);

			} else {
				sourceVar = muteSourceVar ? sourceVar.append("") : sourceVar.append(sourceVariable.getName());
				targetVar = muteTargetVar ? targetVar.append("") : targetVar.append(targetVariable.getName());

				edgeDeclaration = String.format(edgeDeclaration, sourceVar, edgeClassName, quantifier, targetVar);
			}

		} else {
			assert sourceVariable != null ? sourceVariable.getPositionInFact() == 1 : true;
			assert targetVariable != null ? targetVariable.getPositionInFact() == 0 : true;

			edgeDeclaration = "%s <--{%s}%s %s";
			if (sourceVariable != null && sourceVariable.getVariableKind() != VariableKind.VERTEXCLASS) {
				sourceVar = muteSourceVar ? sourceVar.append("")
						: sourceVar.append(" omega(").append(sourceVariable.getName()).append(")");
				targetVar = muteTargetVar ? targetVar.append("")
						: targetVar.append(" alpha(").append(targetVariable.getName()).append(")");
				edgeDeclaration = String.format(edgeDeclaration, sourceVar, edgeClassName, quantifier, targetVar);

			} else {
				sourceVar = muteSourceVar ? sourceVar.append("") : sourceVar.append(sourceVariable.getName());
				targetVar = muteTargetVar ? targetVar.append("") : targetVar.append(targetVariable.getName());

				edgeDeclaration = String.format(edgeDeclaration, sourceVar, edgeClassName, quantifier, targetVar);
			}
		}
		return edgeDeclaration;
	}

	protected String formulateLink(GReQLConstraintVariable sourceVariable, GReQLConstraintVariable targetVariable,
			String quantifier) {
		return formulateLink(sourceVariable, targetVariable, null, quantifier, false, false);
	}

	protected Object formulateReverseLink(GReQLConstraintVariable sourceVariable,
			GReQLConstraintVariable targetVariable, String quantifier) {
		return formulateLink(targetVariable, sourceVariable, null, quantifier, false, false);
	}

	protected String formulateEdgeFrom(GReQLConstraintVariable variable, String quantifier, boolean muteSource) {
		return formulateLink(variable, null, null, quantifier, muteSource, true);
	}

	protected String formulateEdgeFrom(GReQLConstraintVariable variable, String quantifier) {
		return formulateLink(variable, null, null, quantifier, false, true);
	}

	protected String formulateEdgeTo(GReQLConstraintVariable variable, String quantifier, boolean muteTarget) {
		return formulateLink(null, variable, null, quantifier, true, muteTarget);
	}

	protected String formulateEdgeTo(GReQLConstraintVariable variable, String quantifier) {
		return formulateLink(null, variable, null, quantifier, true, false);
	}

	/**
	 * A method to create a variable name for a given EdgeClass or VertexClass
	 * instance. The variable name is a lower-case version of the first letter
	 * of "typeName" and has a suffix which counts the variables of this type.
	 * 
	 * @param typeName
	 *            - the name of the VertexClass/EdgeClass instance for which
	 *            variables should be defined
	 * 
	 * @return the variable name as String
	 */
	protected String createVariableName(String typeName) {

		int suffixCounter = 1;
		while (containedInVariableNames(
				StringManipulations.firstCharToLowerCase(typeName.substring(0, 1)) + (suffixCounter))) {
			++suffixCounter;
		}
		return StringManipulations.firstCharToLowerCase(typeName.substring(0, 1)) + (suffixCounter);
	}

	/**
	 * This method returns a String granting access to the attribute generated
	 * from the input {@link ValueType} which belongs to the vertex class
	 * generated from the input {@link EntityObject}.
	 * 
	 * @param valueType
	 * @param entityObject
	 * @return
	 */
	protected String attributeAccessString(ValueType valueType, EntityObject entityObject) {
		assert hasRelation(valueType, entityObject);

		StringBuilder sb = new StringBuilder();

		String attributeGrUMLName = xmlIdTogrUMLNameMap.get(valueType.get_xmlId());
		String record = "";

		if (util.isPreferredIdentifierOf(valueType, entityObject)) {
			record = "preferredIdentifiers.";
		}

		String s = "%s.%s";
		s = String.format(s, entityObject.get_name(), sb.append(record).append(attributeGrUMLName));
		return s;
	}

	protected String attributeAccessString(ValueType valueType, GReQLConstraintVariable variable) {
		assert variable.getInvolvedObjectType() instanceof EntityObject;

		return attributeAccessString(valueType, (EntityObject) variable.getInvolvedObjectType());
	}

	private boolean hasRelation(ValueType valueType, EntityObject entityObject) {
		for (ValueType vt : entityObject.get_preferredIdentifierValueType()) {
			if (vt == valueType) {
				return true;
			}
		}
		for (Role role : entityObject.get_playedRole()) {
			for (RoleKind factRoleKind : role.get_fact().get_factRole()) {
				if (util.getPlayer(factRoleKind) == valueType) {
					return true;
				}
			}
		}

		return false;
	}

	protected String omega(GReQLConstraintVariable variable) {
		String s = "omega(%s)";
		return String.format(s, variable.getName());
	}

	protected String alpha(GReQLConstraintVariable variable) {
		String s = "alpha(%s)";
		return String.format(s, variable.getName());
	}

	/**
	 * This method is used to generate a String representing the list of defined
	 * variables within a GReQL constraint. {@link GReQLConstraintVariable}s
	 * flagged as mute, i.e. that don't appear in the GReQL constraint, are
	 * ignored. This list of variables is required for the GReQL constraint to
	 * be able to return offending elements (it will return the offending
	 * elements contained in this String).
	 * 
	 * @return the String containing the names of the variables defined in a
	 *         GReQL constraint.
	 */
	protected String listVariableNames() {
		StringBuilder sb = new StringBuilder();
		String delimiter = "";

		for (GReQLConstraintVariable var : definedVariables) {
			if (var.isMute) {
				continue;
			}
			sb.append(delimiter).append(var.getName());
			delimiter = ", ";
		}
		return sb.toString();

	}
}
