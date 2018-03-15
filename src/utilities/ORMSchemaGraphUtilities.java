package utilities;

import java.util.ArrayList;
import java.util.Iterator;

import ormschema.constraints.Constraint;
import ormschema.constraints.RoleSequenceConstraint;
import ormschema.constraints.SetComparisonConstraint;
import ormschema.constraints.SetComparisonConstraintKind;
import ormschema.constraints.UniquenessConstraint;
import ormschema.structure.EntityObject;
import ormschema.structure.EntityType;
import ormschema.structure.Fact;
import ormschema.structure.FactKind;
import ormschema.structure.ObjectType;
import ormschema.structure.ObjectifiedType;
import ormschema.structure.Role;
import ormschema.structure.RoleKind;
import ormschema.structure.RoleProxy;
import ormschema.structure.RoleSequence;
import ormschema.structure.ValueType;

public final class ORMSchemaGraphUtilities {
	public enum PlayerType {
		VALUETYPE, ENTITYOBJECT, ENTITYTYPE, OBJECTIFIEDTYPE, ALL;
	}

	/**
	 * A method that, for an input {@link RoleKind}, retrieves the
	 * {@link RoleKind} adjacent to it. This method only works for binary
	 * relations!
	 * 
	 * @param role
	 *            - the {@link RoleKind} whose neighbor is retrieved
	 * @return the neighboring {@link RoleKind}
	 */
	public RoleKind getNeighboringRole(RoleKind role) {
		assert getFactArity(role.get_fact()) == 2;
		for (RoleKind otherRole : role.get_fact().get_factRole()) {
			if (otherRole != role) {
				return otherRole;
			}
		}
		return null;
	}

	/**
	 * This method returns a list of {@link RoleKind}s participating in the same
	 * {@link Fact} as the input {@link RoleKind} is. The {@link RoleKind}s
	 * returned by this method can be further specified to be played by specific
	 * {@link ObjectType}s only.
	 * 
	 * @param role
	 *            - the {@link RoleKind} which participates in a {@link Fact}
	 *            which contains further {@link RoleKind}
	 * @param type
	 *            - can be used to specify what kinds of players the returned
	 *            {@link RoleKind}s should have
	 * @return a list of {@link RoleKind}s that participate in the same
	 *         {@link Fact} as the input {@link Role} does (possibly filtered
	 *         for a specific type of player)
	 */
	public ArrayList<RoleKind> getOtherRoles(RoleKind role, PlayerType type) {
		ArrayList<RoleKind> otherRoleKinds = new ArrayList<>();
		for (RoleKind otherRole : role.get_fact().get_factRole()) {
			if (otherRole != role) {

				switch (type) {
				case ALL:
					otherRoleKinds.add(otherRole);
					break;
				case ENTITYOBJECT:
					if (getPlayer(otherRole) instanceof ObjectType) {
						otherRoleKinds.add(otherRole);
					}
					;
					;
					break;
				case ENTITYTYPE:
					if (getPlayer(otherRole) instanceof EntityType) {
						otherRoleKinds.add(otherRole);
					}
					;
					;
					break;
				case OBJECTIFIEDTYPE:
					if (getPlayer(otherRole) instanceof ObjectifiedType) {
						otherRoleKinds.add(otherRole);
					}
					;
					;
					break;
				case VALUETYPE:
					if (getPlayer(otherRole) instanceof ValueType) {
						otherRoleKinds.add(otherRole);
					}
					;
					;
					break;
				}

			}
		}
		return otherRoleKinds;
	}

	/**
	 * A method to find out whether the {@link Fact} at hand has a spanning
	 * {@link UniquenessConstraint} or not.
	 * 
	 * @param fact
	 *            - the fact that may or may not have a spanning
	 *            {@link UniquenessConstraint}
	 * @return true, if the input {@link Fact} has a spanning
	 *         {@link UniquenessConstraint}. False if not.
	 */
	public boolean hasSpanningUC(Fact fact) {
		for (RoleKind role : fact.get_factRole()) {
			if (role instanceof Role) {
				for (RoleSequence rs : ((Role) role).get_roleSequenceContainer()) {
					if (rs.get_associatedConstraint() instanceof UniquenessConstraint
							&& !rs.get_associatedConstraint().is_isInterpredicateConstraint()) {
						if (getRoleSequenceSize(rs) == getFactArity(fact)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * A method that returns the size of the input {@link RoleSequence}.
	 * 
	 * @param roleSequence
	 *            - the {@link RoleSequence} whose size is returned
	 * @return the size of the input {@link RoleSequence}
	 */
	public int getRoleSequenceSize(RoleSequence roleSequence) {
		int counter = 0;

		Iterator<Role> roleSequenceIterator = roleSequence.get_role().iterator();
		while (roleSequenceIterator.hasNext()) {
			++counter;
			roleSequenceIterator.next();
		}
		return counter;
	}

	/**
	 * A method that counts the number of {@link RoleKind}s within a
	 * {@link Fact} and returns this number. The number of roles participating
	 * in a fact is the arity of the fact.
	 * 
	 * @param fact
	 *            - the {@link Fact} for which the arity is determined
	 * @return the arity of the input {@link Fact}
	 */
	public int getFactArity(Fact fact) {
		int factArity = 0;

		Iterator<RoleKind> factRoleIterator = fact.get_factRole().iterator();
		while (factRoleIterator.hasNext()) {
			factRoleIterator.next();
			++factArity;
		}

		return factArity;
	}

	/**
	 * A method to identify whether a {@link Constraint} is an interpredicate
	 * constraint or intrapredicate constraint. For this purpose it checks
	 * whether the Roles participating in the Constraint originate from
	 * different Facts (then it's an interpredicate constraint) or from only one
	 * Fact (then it's an intrapredicate/internal constraint).
	 *
	 * @param constraint
	 *            - the {@link Constraint} that may be an interpredicate
	 *            constraint or not
	 * @return - true if the input {@link Constraint} is an interpredicate
	 *         (between predicates/facts) constraint, false if not.
	 */
	public boolean isInterpredicateConstraint(Constraint constraint) {
		String exampleFactName = null;

		if (constraint instanceof RoleSequenceConstraint) {

			for (RoleSequence roleSequence : ((RoleSequenceConstraint) constraint).get_roleSequence()) {
				for (Role role : roleSequence.get_role()) {
					if (exampleFactName == null) {
						exampleFactName = role.get_fact().get_name();
						continue;
					}
					if (!exampleFactName.equals(role.get_fact().get_name())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * A method to check whether the input {@link Fact} has exactly two
	 * {@link RoleKind}s or not.
	 * 
	 * @param objectifiedFact
	 *            - the {@link Fact} that may or may not contain two
	 *            {@link RoleKind}s
	 * @return true, if the {@linkplain} Fact} has 2 {@link RoleKind}s, false if
	 *         not.
	 */
	public boolean isBinaryFact(Fact objectifiedFact) {
		return getFactArity(objectifiedFact) == 2 ? true : false;
	}

	/**
	 * This method returns the player of the input {@link RoleKind}. If the
	 * input is a {@link RoleProxy}, this method retrieves the {@link Role} for
	 * which it is a proxy and returns the player of that {@link Role}.
	 * 
	 * @param currentRole
	 *            - the {@link RoleKind} for which the player should be
	 *            retrieved
	 * @return - the {@link ObjectifiedType} that plays the input
	 *         {@link RoleKind}
	 */
	public ObjectType getPlayer(RoleKind currentRole) {
		if (currentRole instanceof Role) {
			return ((Role) currentRole).get_player();
		} else if (currentRole instanceof RoleProxy) {
			return ((RoleProxy) currentRole).get_linkedRole().get_player();
		}
		return null;
	}

	public int getRoleCount(RoleSequence roleSequence) {
		int count = 0;
		Iterator<Role> iterator = roleSequence.get_role().iterator();
		while (iterator.hasNext()) {
			iterator.next();
			++count;
		}

		return count;
	}

	/**
	 * A method to retrieve the {@link Role}s contained in the
	 * {@link RoleSequence}s that define the input
	 * {@link RoleSequenceConstraint}.
	 * 
	 * @param constraint
	 *            - the {@link RoleSequenceConstraint} from which the players of
	 *            the constrained {@link Role} should be extracted
	 * @return An array list which, for each {@link RoleSequence} contained in
	 *         the {@link RoleSequenceConstraint}, contains an array list of
	 *         {@link Role}s
	 */
	public ArrayList<ArrayList<Role>> getConstrainedRoles(RoleSequenceConstraint constraint) {
		ArrayList<ArrayList<Role>> involvedRoles = new ArrayList<>();
		for (RoleSequence rs : constraint.get_roleSequence()) {
			// the role sequences may be identical or subtypes of the role
			// player could participate
			ArrayList<Role> listOfRoles = new ArrayList<>();
			rs.get_role().forEach(role -> listOfRoles.add(role));
			involvedRoles.add(listOfRoles);
		}
		return involvedRoles;
	}

	public RoleSequence getRoleSequence(RoleSequenceConstraint ormConstraint, int n) {
		assert n < getRoleSequenceCount(ormConstraint);
		int count = 0;
		for (RoleSequence rs : ormConstraint.get_roleSequence()) {
			if (count == n) {
				return rs;
			}
			++count;
		}
		return null;
	}

	public Role getRole(RoleSequence roleSequence, int n) {
		assert n < getRoleCount(roleSequence);
		int count = 0;
		for (Role role : roleSequence.get_role()) {
			if (count == n) {
				return role;
			}
			++count;
		}
		return null;
	}

	/**
	 * A method that counts the number of {@link RoleSequence}s in a
	 * {@link RoleSequenceConstraint}.
	 * 
	 * @param constraint
	 *            - the {@link RoleSequenceConstraint} with an unknown number of
	 *            {@link RoleSequence}s
	 * @return the number of {@link RoleSequence}s in the input
	 *         {@link RoleSequenceConstraint}
	 */
	public int getRoleSequenceCount(RoleSequenceConstraint constraint) {
		int count = 0;
		Iterator<RoleSequence> iterator = constraint.get_roleSequence().iterator();
		while (iterator.hasNext()) {
			iterator.next();
			++count;
		}

		return count;
	}

	public boolean isLastRoleInReadingOrder(Role role, Fact fact) {
		Iterator<RoleKind> iterator = fact.get_factRole().iterator();
		while (iterator.hasNext()) {
			RoleKind nextRole = iterator.next();
			if (nextRole == role && !iterator.hasNext()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method returns whether a {@link RoleSequenceConstraint} applies to a
	 * single {@link Role} or multiple Roles.
	 * 
	 * @param ormConstraint
	 *            - the {@link RoleSequenceConstraint} which may apply to
	 *            multiple {@link Role}s or not
	 * @return true if the {@link RoleSequenceConstraint} applies to one
	 *         {@link Role}, false if not.
	 */
	public boolean isSingleRoleConstrained(RoleSequenceConstraint ormConstraint) {
		int count = 0;
		for (RoleSequence rs : ormConstraint.get_roleSequence()) {

			Iterator<Role> iterator = rs.get_role().iterator();
			while (iterator.hasNext()) {
				iterator.next();
				count++;
			}
			break;
		}
		return count == 1 ? true : false;
	}

	/**
	 * A method that checks whether the input {@link ObjectType} "ot" is the
	 * supertype of the second input {@link ObjectType} "potentialSubtype"
	 * 
	 * @param ot
	 *            - an {@link ObjectType} that might be the supertype of the
	 *            second parameter
	 * @param potentialSubtype
	 *            - an {@link ObjectType} that might be the subtype of the
	 *            second parameter
	 * @return true if "ot" is a supertype of "potentialSubtype". False if not.
	 */
	public boolean isSupertypeOfObjectType(ObjectType ot, ObjectType potentialSubtype) {
		boolean isSupertype = false;

		for (ObjectType supertype : potentialSubtype.get_supertype()) {
			if (supertype == ot) {
				return true;
			} else {
				isSupertype = isSupertypeOfObjectType(ot, supertype);
				return isSupertype;
			}
		}
		return false;
	}

	public boolean allRolesFromSingleFact(ArrayList<Role> listOfRoles) {
		assert listOfRoles.size() > 0;
		Fact fact = listOfRoles.get(0).get_fact();

		for (Role role : listOfRoles) {
			if (role.get_fact() != fact) {
				return false;
			}
		}
		return true;
	}

	/**
	 * This method retrieves the first {@link Fact} linked to a
	 * {@link RoleSequenceConstraint} with just one RoleSequence. It DOES NOT
	 * work for the following {@link SetComparisonConstraint}s: equality,
	 * exclusion, exclusive-or and subset constraints.
	 * 
	 * @param constraint
	 *            - a {@link RoleSequenceConstraint} that only has one
	 *            RoleSequence element
	 */
	public Fact getFact(RoleSequenceConstraint constraint) {
		assert getRoleSequenceCount(constraint) == 1;

		RoleSequence roleSequence = getRoleSequence(constraint, 0);
		return getRole(roleSequence, 0).get_fact();

	}

	/**
	 * 
	 * @param roleSequenceConstraint
	 * @return a list of {@link ObjectType}s affected by the input
	 *         {@link RoleSequenceConstraint}
	 */
	public ArrayList<ObjectType> getConstrainedPlayers(RoleSequenceConstraint roleSequenceConstraint) {
		ArrayList<ObjectType> listOfConstrainedPlayers = new ArrayList<>();
		ArrayList<ArrayList<Role>> involvedRoles = getConstrainedRoles(roleSequenceConstraint);
		for (ArrayList<Role> listOfRoles : involvedRoles) {
			listOfRoles.forEach(role -> listOfConstrainedPlayers.add(role.get_player()));
		}
		return listOfConstrainedPlayers;
	}

	public int getConstrainedRoleCount(RoleSequenceConstraint roleSequenceConstraint) {
		return getConstrainedPlayers(roleSequenceConstraint).size();
	}

	public ArrayList<Integer> getMultiplicity(RoleKind roleKind) {
		ArrayList<Integer> minMaxValues = new ArrayList<>();
		minMaxValues.add(roleKind.get_multiplicityLowerBound());
		minMaxValues.add(roleKind.get_multiplicityUpperBound());
		return minMaxValues;
	}

	/**
	 * A method that checks whether a {@link Fact} contains {@link ValueType}s
	 * or not
	 * 
	 * @param fact
	 *            - the {@link Fact} that may or may not contain a
	 *            {@link ValueType}
	 * @return true if the {@link Fact} contains at least one {@link ValueType},
	 *         false if not.
	 */
	public boolean containsValueType(Fact fact) {
		for (RoleKind role : fact.get_factRole()) {

			if (getPlayer(role) instanceof ValueType) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method checks whether the input {@link ValueType} is a preferred
	 * identifier of the input {@link EntityObject}
	 * 
	 * @param valueType
	 *            - the {@link ValueType} which might be a preferred identifier
	 *            of the input {@link ObjectType}
	 * @param entityObject
	 *            - the {@link EntityObject} which might have the input
	 *            {@link ValueType} as preferred identifier
	 * @return true if the {@link ValueType} is a preferred identifier of the
	 *         input {@link EntityObject}
	 */
	public boolean isPreferredIdentifierOf(ValueType valueType, EntityObject entityObject) {
		for (ValueType vt : entityObject.get_preferredIdentifierValueType()) {
			if (vt == valueType) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method returns the role played by the input {@link ObjectType} in
	 * the input {@link Fact}
	 * 
	 * @param objectType
	 * @param fact
	 * @return the role played by the input {@link ObjectType} in the input
	 *         {@link Fact} or null, of the object type doesn't play a role in
	 *         the fact
	 */
	public Role getRoleInFact(ObjectType objectType, Fact fact) {
		for (RoleKind roleKind : fact.get_factRole()) {
			if (roleKind instanceof Role && ((Role) roleKind).get_player() == objectType) {
				return (Role) roleKind;
			}
		}
		return null;
	}

	/**
	 * This method returns the number of preferred identifiers an
	 * {@link EntityObject} has
	 * 
	 * @param et
	 *            - an {@link EntityObject}
	 * @return the number of preferred identifiers the entity object has
	 */
	public int getPreferredIdentifierCount(EntityObject et) {
		int counter = 0;
		Iterator<ValueType> iterator = et.get_preferredIdentifierValueType().iterator();
		while (iterator.hasNext()) {
			++counter;
			iterator.next();
		}
		return counter;
	}

	/**
	 * This method retrieves the i-th preferred identifier of an
	 * {@link EntityObject}
	 * 
	 * @param et
	 * @param i
	 * @return the i-th preferred identifier of the input entity object or null,
	 *         if it doesn't exist
	 */
	public ValueType getPreferredIdentifier(EntityObject et, int i) {
		int counter = 0;
		Iterator<ValueType> iterator = et.get_preferredIdentifierValueType().iterator();
		while (iterator.hasNext()) {
			if (counter == i) {
				return iterator.next();
			}
			++counter;
		}
		return null;

	}

	/**
	 * This method retrieves the position of a {@link Role} in a {@link Fact}.
	 * 
	 * @param role
	 * @param fact
	 * @return
	 */
	public int getPositionOfRoleInFact(Role role, Fact fact) {
		int positionInFact = 0;
		if (constainsRole(fact, role)) {
			RoleKind targetRole = role;
			if (role.get_roleProxy() != null) {
				targetRole = role.get_roleProxy();
			}
			for (RoleKind roleKind : fact.get_factRole()) {

				if (targetRole == roleKind) {
					break;
				}
				++positionInFact;
			}
		} else {
			positionInFact = -1;
		}

		return positionInFact;
	}

	/**
	 * This method checks whether a {@link Fact} contains the input {@link Role}
	 * 
	 * @param fact
	 *            the fact that may or may not contain the input role
	 * @param role
	 *            the role that may or may not be included in the input fact
	 * @return true, if the role is part of the fact. False if not.
	 */
	private boolean constainsRole(Fact fact, Role role) {

		RoleKind targetRole = role;
		if (role.get_roleProxy() != null) {
			targetRole = role.get_roleProxy();
		}
		for (RoleKind roleKind : fact.get_factRole()) {
			if (roleKind == targetRole) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method retrieves the supertype from a subtype constraint.
	 * 
	 * @param constraint
	 *            a {@link SetComparisonConstraint} that applies to a subtype
	 *            definition in ORM
	 * @return the {@link ObjectType} which is the supertype within the
	 *         constrained subtype relations
	 */
	public ObjectType retrieveSupertype(SetComparisonConstraint constraint) {
		assert isSubtypeConstraint(constraint);

		// subtype constraints always apply to the role of the supertype, so
		// simply return the player of any of the constrained roles

		return getRole(getRoleSequence(constraint, 0), 0).get_player();

	}

	/**
	 * This method checks whether a given {@link SetComparisonConstraint} refers
	 * to a subtype definition.
	 * 
	 * @param constraint
	 *            - the {@link SetComparisonConstraint} that might constrain a
	 *            subtype relation
	 * @return true of the input constraint refers to a subtype relation. False
	 *         if not.
	 */
	public boolean isSubtypeConstraint(SetComparisonConstraint constraint) {

		if (constraint.get_type() == SetComparisonConstraintKind.INCLUSIVEOR) {
			// if the first constrained role belongs to a SUBTYPEFACT, all other
			// roles will too
			return getFact(constraint).get_factKind() == FactKind.SUBTYPEFACT;
		} else {
			// other set comparison constraints have multiple RoleSequences -
			// can't use getFact
			RoleSequence roleSequence = getRoleSequence(constraint, 0);
			return getRole(roleSequence, 0).get_fact().get_factKind() == FactKind.SUBTYPEFACT;
		}
	}

	/**
	 * This method returns a list of {@link ObjectType}s which are subtypes of
	 * the supertype whose roles are constrained by the input
	 * {@link SetComparisonConstraint} "constraint"
	 * 
	 * @param constraint
	 *            - a {@link SetComparisonConstraint} that applies to subtype
	 *            relations
	 * @return - the subtypes from the constrained subtype definitions
	 */
	public ArrayList<ObjectType> getSubtypes(SetComparisonConstraint constraint) {
		ArrayList<ObjectType> subtypes = new ArrayList<>();

		RoleSequence roleSequence = getRoleSequence(constraint, 0);

		for (Role role : roleSequence.get_role()) {
			// subtype relations can't contain RoleProxys, so casting is safe
			Role neighboringRole = (Role) getNeighboringRole(role);
			subtypes.add(neighboringRole.get_player());
		}

		return subtypes;
	}

}
