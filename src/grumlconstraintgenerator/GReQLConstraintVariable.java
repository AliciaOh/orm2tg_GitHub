package grumlconstraintgenerator;

import de.uni_koblenz.jgralab.schema.EdgeClass;
import de.uni_koblenz.jgralab.schema.VertexClass;
import ormschema.structure.EntityObject;
import ormschema.structure.Fact;
import ormschema.structure.ObjectType;
import ormschema.structure.Role;
import ormschema.structure.ValueType;

/**
 * 
 * This class is used to represent a variable that can be used within GReQL
 * expressions.
 * 
 * @author Alicia Owen
 *
 */

public class GReQLConstraintVariable {

	protected String variableName;
	/**
	 * The grUML name of the represented Fact or ObjectType
	 */
	protected String grUMLTypeName;
	/**
	 * The player of the role from which this {@link GReQLConstraintVariable} is
	 * generated
	 */
	protected ObjectType involvedObjectType;
	/**
	 * The role from which this {@link GReQLConstraintVariable} is generated
	 */
	protected Role involvedRole;

	/**
	 * An integer which stored the information at which position within the fact
	 * the role from which this {@link GReQLConstraintVariable} is generated
	 * lies
	 */
	protected int positionInFact;
	/**
	 * indicates whether the variable represents a grUML {@link VertexClass} or
	 * a grUML {@link EdgeClass}
	 */
	VariableKind variableKind;
	/**
	 * This boolean indicates whether the variable is to be included in the
	 * variable declaration or not
	 */
	boolean isMute;

	public GReQLConstraintVariable(String name, String grUMLName, Role involvedRole, ObjectType involvedObjectType,
			VariableKind variableKind, int positionInFact, boolean isMute) {
		super();
		this.variableName = name;
		this.grUMLTypeName = grUMLName;
		this.involvedRole = involvedRole;
		this.involvedObjectType = involvedObjectType;
		this.variableKind = variableKind;
		this.positionInFact = positionInFact;
		this.isMute = isMute;

	}

	/**
	 * This method returns the type declaration for this variable, i.e. "E([some
	 * edge class])" if the variable represents and EdgeClass and V([some vertex
	 * class]) if it represents a VertexClass.
	 * 
	 * @return the string with the type declaration for this
	 *         {@link GReQLConstraintVariable}
	 */
	public String declareType() {
		if (variableKind == VariableKind.VERTEXCLASS) {
			return vc();
		} else {
			assert variableKind == VariableKind.EDGECLASS;
			return ec();
		}
	}

	/**
	 * This method returns the {@link Fact} in which the {@link Role}
	 * "involvedRole" represented in the {@link GReQLConstraintVariable}
	 * participates.
	 * 
	 * @return the {@link Fact} linked to this {@link GReQLConstraintVariable}
	 */
	public Fact getFact() {

		Fact fact = null;
		if (involvedRole.get_roleProxy() != null) {
			fact = involvedRole.get_roleProxy().get_fact();
		} else {
			fact = involvedRole.get_fact();
		}

		return fact;

	}

	/**
	 * 
	 * @return the grUMLTypeName, which is the grUML name of the
	 *         {@link ObjectType} or {@link Fact} represented by this
	 *         {@link GReQLConstraintVariable}.
	 */
	public String getGrUMLTypeName() {
		return grUMLTypeName;
	}

	/**
	 * @return the involvedRole
	 */
	public Role getInvolvedRole() {
		return involvedRole;
	}

	/**
	 * This method returns the object type which plays the role from which this
	 * {@link GReQLConstraintVariable} was generated.
	 * 
	 * @return the involvedObjectType
	 */
	public ObjectType getInvolvedObjectType() {
		return involvedObjectType;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return variableName;
	}

	public int getPositionInFact() {
		return positionInFact;
	}

	public VariableKind getVariableKind() {
		return variableKind;
	}

	/**
	 * @return the isMute
	 */
	public boolean isMute() {
		return isMute;
	}

	/**
	 * 
	 * @return the type declaration for a {@link GReQLConstraintVariable}
	 *         representing an {@link EdgeClass}
	 */
	private String ec() {

		String s = "E{%s}";
		return String.format(s, grUMLTypeName);
	}

	/**
	 * 
	 * @return the type declaration for a {@link GReQLConstraintVariable}
	 *         representing a {@link VertexClass}
	 */
	private String vc() {

		String s = "V{%s}";
		return String.format(s, grUMLTypeName);
	}

	/**
	 * This method returns a String which represents the ObjectType for which
	 * this {@link GReQLConstraintVariable} is defined. If the ObjectType is an
	 * {@link EntityObject} but the {@link GReQLConstraintVariable} represents
	 * an EdgeClass, it will return alpha([variableName]) or
	 * omega([variableName]) depending on the position of the involved Role in
	 * the Fact. If it is a ValueType, it returns the name this ValueType is
	 * given during mapping.
	 * 
	 * @return
	 */
	public String printObjectType() {
		if (variableKind == VariableKind.EDGECLASS && positionInFact == 0
				&& involvedObjectType instanceof EntityObject) {
			String s = "alpha(%s)";
			return String.format(s, variableName);
		} else if (variableKind == VariableKind.EDGECLASS && positionInFact == 1
				&& involvedObjectType instanceof EntityObject) {
			String s = "omega(%s)";
			return String.format(s, variableName);
		} else if (involvedObjectType instanceof ValueType) {
			return grUMLTypeName;
		} else {
			return variableName;
		}
	}

	/**
	 * @param isMute
	 *            the isMute to set
	 */
	public void setMute(boolean isMute) {
		this.isMute = isMute;
	}

	public enum VariableKind {
		VERTEXCLASS, EDGECLASS;
	}
}
