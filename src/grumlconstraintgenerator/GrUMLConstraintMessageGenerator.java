package grumlconstraintgenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ormschema.constraints.CardinalityConstraint;
import ormschema.constraints.Constraint;
import ormschema.constraints.FrequencyConstraint;
import ormschema.constraints.MandatoryConstraint;
import ormschema.constraints.RingConstraint;
import ormschema.constraints.RingConstraintKind;
import ormschema.constraints.RoleSequenceConstraint;
import ormschema.constraints.SetComparisonConstraint;
import ormschema.constraints.SetComparisonConstraintKind;
import ormschema.constraints.UniquenessConstraint;
import ormschema.constraints.ValueConstraint;
import ormschema.structure.Fact;
import ormschema.structure.FactKind;
import ormschema.structure.ObjectType;
import ormschema.structure.Range;
import ormschema.structure.Role;
import ormschema.structure.RoleKind;
import ormschema.structure.RoleProxy;
import ormschema.structure.RoleSequence;
import ormschema.structure.ValueRange;
import utilities.ORMSchemaGraphUtilities;

/**
 * 
 * This class is used to generate verbalizations for ORM constraints.
 * 
 * @author Alicia Owen
 *
 */
public class GrUMLConstraintMessageGenerator {

	Constraint ormConstraint;

	private ORMSchemaGraphUtilities util;

	public GrUMLConstraintMessageGenerator(Constraint ormConstraint) {
		this.ormConstraint = ormConstraint;
	}

	public String createMessage() {

		util = new ORMSchemaGraphUtilities();
		StringBuffer sb = new StringBuffer();
		if (ormConstraint instanceof RoleSequenceConstraint) {
			sb.append(defineVariables((RoleSequenceConstraint) ormConstraint));
			sb.append(writeFacts(ormConstraint));
		} else {
			sb.append(writeFacts(ormConstraint));
		}
		return sb.toString();
	}

	private String defineVariables(RoleSequenceConstraint ormConstraint) {
		StringBuffer sb = new StringBuffer();
		String prefix = "";
		String variableDefinition = "";
		String suffix = "";

		if (ormConstraint instanceof SetComparisonConstraint) {

			if (((SetComparisonConstraint) ormConstraint).get_type().name().equals("EQUALITY")) {
				ArrayList<ArrayList<Role>> involvedRoles = util.getConstrainedRoles(ormConstraint);
				prefix = "For each ";
				variableDefinition = printInvolvedObjectTypes(involvedRoles, true, " and ");
				if (involvedRoles.size() > 2) {
					suffix = ", all or none of the following hold: ";
				} else {
					suffix = ", ";
				}

			} else if (((SetComparisonConstraint) ormConstraint).get_type().name().equals("EXCLUSION")) {
				ArrayList<ArrayList<Role>> involvedRoles = util.getConstrainedRoles(ormConstraint);
				prefix = "For each ";
				variableDefinition = printInvolvedObjectTypes(involvedRoles, true, " and ");
				suffix = ", at most one of the following holds: ";

			} else if (((SetComparisonConstraint) ormConstraint).get_type().name().equals("EXCLUSIVEOR")) {
				ArrayList<ArrayList<Role>> involvedRoles = util.getConstrainedRoles(ormConstraint);
				prefix = "For each ";
				variableDefinition = printInvolvedObjectTypes(involvedRoles, true, " and ");
				suffix = ", exactly one of the following holds: ";

			} else if (((SetComparisonConstraint) ormConstraint).get_type().name().equals("INCLUSIVEOR")) {
				ArrayList<ArrayList<Role>> involvedRoles = util.getConstrainedRoles(ormConstraint);
				prefix = "For each ";
				variableDefinition = printInvolvedObjectTypes(involvedRoles, true, " and ");
				suffix = ", ";

			} else if (((SetComparisonConstraint) ormConstraint).get_type().name().equals("SUBSET")) {
				ArrayList<ArrayList<Role>> involvedRoles = util.getConstrainedRoles(ormConstraint);
				if (util.isSingleRoleConstrained(ormConstraint)) {
					prefix = "For each ";
					variableDefinition = printInvolvedObjectTypes(involvedRoles, true, " and ");
					suffix = ", ";
				} else {
					prefix = "If ";
				}

			}
		} else if (ormConstraint instanceof UniquenessConstraint) {
			if (((UniquenessConstraint) ormConstraint).is_isInterpredicateConstraint()) {
				ArrayList<ArrayList<Role>> involvedRoles = util.getConstrainedRoles(ormConstraint);
				prefix = "For each ";
				variableDefinition = printInvolvedObjectTypes(involvedRoles, true, " and ");
				suffix = ", ";
			}
		}
		sb.append(prefix + variableDefinition + suffix);
		return sb.toString();
	}

	private String writeFacts(Constraint ormConstraint) {
		StringBuffer sb = new StringBuffer();
		String prefix = "";
		String factSeparator = "";
		String suffix = "";
		String facts = "";

		if (ormConstraint instanceof SetComparisonConstraint) {

			if (((SetComparisonConstraint) ormConstraint).get_type().name().equals("EQUALITY")) {
				if (util.getRoleSequenceCount((RoleSequenceConstraint) ormConstraint) == 2) {
					factSeparator = " if and only if ";
				} else {
					factSeparator = "; ";
				}
				suffix = ". [ORM Equality Constraint]";

			} else if (((SetComparisonConstraint) ormConstraint).get_type().name().equals("EXCLUSION")) {
				factSeparator = "; ";
				suffix = ". [ORM Exclusion Constraint]";

			} else if (((SetComparisonConstraint) ormConstraint).get_type().name().equals("EXCLUSIVEOR")) {
				factSeparator = "; ";
				suffix = ". [ORM Exclusive Or Constraint]";

			} else if (((SetComparisonConstraint) ormConstraint).get_type().name().equals("INCLUSIVEOR")) {
				factSeparator = " or ";
				suffix = ". [ORM Inclusive Or Constraint]";
			} else if (((SetComparisonConstraint) ormConstraint).get_type().name().equals("SUBSET")) {
				if (util.isSingleRoleConstrained((RoleSequenceConstraint) ormConstraint)) {
					prefix = "if ";
				}
				factSeparator = " then ";
				suffix = ". [ORM Subset Constraint]";

			}
			facts = writeFacts((RoleSequenceConstraint) ormConstraint, factSeparator, "that ", "some ");
		} else if (ormConstraint instanceof CardinalityConstraint) {
			ArrayList<Range> listOfRanges = new ArrayList<>();
			((CardinalityConstraint) ormConstraint).get_cardinalityRange().forEach(range -> listOfRanges.add(range));
			if (((CardinalityConstraint) ormConstraint).is_isRoleConstraint()) {
				prefix = "For each population of \"";
				facts = verbalizeFactWithOneConstrainedRole(
						((CardinalityConstraint) ormConstraint).get_cardinalityConstrainedRole(), "", "");

				suffix = "\", the number of "
						+ ((CardinalityConstraint) ormConstraint).get_cardinalityConstrainedRole().get_player()
								.get_name()
						+ " instances is " + verbalizeRanges(listOfRanges) + ". [ORM Cardinality Constraint on a role]";

			} else {
				prefix = "Each population of "
						+ ((CardinalityConstraint) ormConstraint).get_cardinalityConstrainedObjectType().get_name()
						+ " contains " + verbalizeRanges(listOfRanges)
						+ " instances.  [ORM Cardinality Constraint on object type]";
			}

		} else if (ormConstraint instanceof FrequencyConstraint) {

			if (!((FrequencyConstraint) ormConstraint).is_isInterpredicateConstraint()) {
				String combination = "";
				assert util.getRoleSequenceCount((FrequencyConstraint) ormConstraint) == 1;
				RoleSequence roleSequence = ((FrequencyConstraint) ormConstraint).get_roleSequence().iterator().next();
				String range;
				if (util.getRoleCount(roleSequence) > 1) {
					combination = " combination ";
					range = verbalizeRange(((FrequencyConstraint) ormConstraint).get_frequencyRange());

					prefix = "Each "
							+ printInvolvedObjectTypes(util.getConstrainedRoles((FrequencyConstraint) ormConstraint),
									true, ", ")
							+ combination + " in the population of \"";

					facts = writeFacts((FrequencyConstraint) ormConstraint, "", "", "");

					suffix = "\" occurs there " + range + " times. [ORM compound internal Frequency Constraint]";

				}
				// TODO remove - should never reach this else!
				// else {
				// // don't need to add a constraint for internal
				// single-role
				// // frequency constraints. This is reflected in the
				// // multiplicity of the association!
				// // range = verbalizeMultiplicity(
				// //
				// util.getMultiplicity(util.getNeighboringRole(util.getRole(roleSequence,
				// // 0))));
				// }
			} else {
				// TODO interpredicate FC
			}

		} else if (ormConstraint instanceof ValueConstraint) {
			ArrayList<ValueRange> listOfValueRanges = new ArrayList<>();
			((ValueConstraint) ormConstraint).get_valueRange().forEach(range -> listOfValueRanges.add(range));

			String actor;
			String constraintType;
			if (((ValueConstraint) ormConstraint).is_isRoleConstraint()) {
				actor = ((ValueConstraint) ormConstraint).get_valueConstrainedRole().get_player().get_name();
				constraintType = "on a role";
			} else {
				actor = ((ValueConstraint) ormConstraint).get_valueConstrainedValueType().get_name();
				constraintType = "on a value type";
			}
			prefix = "The possible values of " + actor + " in \"";
			if (((ValueConstraint) ormConstraint).is_isRoleConstraint()) {
				facts = verbalizeFactWithOneConstrainedRole(
						((ValueConstraint) ormConstraint).get_valueConstrainedRole(), "", "");
			} else {
				for (Role role : ((ValueConstraint) ormConstraint).get_valueConstrainedValueType().get_playedRole()) {
					facts = verbalizeFactWithOneConstrainedRole(role, "", "");
				}
			}
			if (!facts.equals("")) {
				prefix = "The possible values of " + actor + " in \"";
				suffix = "\" are " + verbalizeValueRanges(listOfValueRanges) + ". [ORM Value Constraint "
						+ constraintType + "]";
			} else {
				prefix = "The possible values of " + actor;
				suffix = " are " + verbalizeValueRanges(listOfValueRanges) + ". [ORM Value Constraint " + constraintType
						+ "]";
			}
		} else if (ormConstraint instanceof RingConstraint) {
			assert util.getRoleSequenceCount(((RingConstraint) ormConstraint)) == 1;
			RingConstraintKind type = ((RingConstraint) ormConstraint).get_type();
			ArrayList<ArrayList<Role>> involvedRoles = util.getConstrainedRoles((RingConstraint) ormConstraint);
			ArrayList<ObjectType> definedObjectTypes = getDefinedObjectTypes((RingConstraint) ormConstraint, false);
			String player1 = definedObjectTypes.get(0).get_name();
			String player2 = definedObjectTypes.get(1).get_name();

			switch (type) {
			case ACYCLIC:
				prefix = "No " + definedObjectTypes.get(0).get_name()
						+ " may cycle back to itself via one or more traversals through ";
				facts = verbalizeFactWithMultipleConstrainedRoles(involvedRoles.get(0), definedObjectTypes, false, 0,
						"", "");
				suffix = ". [ORM acyclic Ring Constraint]";
				break;
			case ANTISYMMETRIC:
				prefix = "If ";
				facts = verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "1", "", "2", false, false)
						+ " and " + player1 + "1" + " is not " + player2 + "2 then it is impossible that "
						+ verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "2", "", "1", false, false);

				suffix = ". [ORM antisymmetric Ring Constraint]";
				break;
			case ASYMMETRIC:
				prefix = "If ";
				facts = verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "1", "", "2", false, false)
						+ " then it is impossible that "
						+ verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "2", "", "1", false, false);

				suffix = ". [ORM asymmetric Ring Constraint]";
				break;
			case INTRANSITIVE:
				prefix = "If ";
				facts = verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "1", "", "2", false, false)
						+ " and "
						+ verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "2", "", "3", false, false)
						+ " then it is impossible that "
						+ verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "1", "", "3", false, false);

				suffix = ". [ORM intransitive Ring Constraint]";
				break;
			case IRREFLEXIVE:
				prefix = "No ";
				facts = verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "", "the same", "", false,
						false);
				suffix = ". [ORM irreflexive Ring Constraint]";
				break;
			case PURELYREFLEXIVE:
				prefix = "If ";
				facts = verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "1", "some", "2", false,
						false) + " then " + player1 + "1 = " + player2 + "2";
				suffix = ". [ORM purely reflexive Ring Constraint]";
				break;
			case REFLEXIVE:
				prefix = "If ";
				facts = verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "1", "some", "2", false,
						false) + " then "
						+ verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "1", " itself", "", false,
								true);
				suffix = ". [ORM irreflexive Ring Constraint]";
				break;
			case SYMMETRIC:
				prefix = "If ";
				facts = verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "1", "some", "2", false,
						false) + " then "
						+ verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "1", "", "2", false, false);
				suffix = ". [ORM symmetric Ring Constraint]";
				break;
			case TRANSITIVE:
				prefix = "If ";
				facts = verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "1", "", "2", false, false)
						+ " and "
						+ verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "2", "", "3", false, false)
						+ " then "
						+ verbalizeBinaryFact(involvedRoles.get(0), definedObjectTypes, "", "1", "", "3", false, false);

				suffix = ". [ORM transitive Ring Constraint]";
				break;
			case STRONGLYINTRANSITIVE:
				break;

			}
		} else if (ormConstraint instanceof MandatoryConstraint) {
			// TODO remove - this isn't needed since the constraint is implied
			// by the multiplicity values!
			assert util.getRoleSequenceCount((MandatoryConstraint) ormConstraint) == 1;

			ArrayList<ArrayList<Role>> involvedRoles = util.getConstrainedRoles((MandatoryConstraint) ormConstraint);
			assert involvedRoles.get(0).size() == 1;
			prefix = "Each ";
			if (util.isLastRoleInReadingOrder(involvedRoles.get(0).get(0), involvedRoles.get(0).get(0).get_fact())) {
				prefix = "For each " + involvedRoles.get(0).get(0).get_player().get_name() + ", ";
				facts = verbalizeFactWithOneConstrainedRole(involvedRoles.get(0).get(0), "some ", "that ");
			} else {
				prefix = "Each ";
				facts = verbalizeFactWithOneConstrainedRole(involvedRoles.get(0).get(0), "", "at least one ");
			}

			suffix = ". [ORM internal Mandatory Constraint]";
		} else if (ormConstraint instanceof UniquenessConstraint) {
			RoleSequence roleSequence = util.getRoleSequence((UniquenessConstraint) ormConstraint, 0);
			ArrayList<Role> listOfRoles = new ArrayList<>();
			roleSequence.get_role().forEach(role -> listOfRoles.add(role));

			if (((UniquenessConstraint) ormConstraint).is_isInterpredicateConstraint()) {
				// TODO funktioniert nicht ohen join path!!
				facts = writeFacts((UniquenessConstraint) ormConstraint, "and ", "that", "at most one");
			} else {
				if (util.getRoleCount(roleSequence) == 1) {
					// a 1:n or n:1 uniqueness constraint
					// verbalization depends on the reading order and the
					// position of the constrained role within it

					if (util.isLastRoleInReadingOrder(listOfRoles.get(0), listOfRoles.get(0).get_fact())) {
						// the constrained role is last in the reading order, so
						// role player is emphasized using "For each <role
						// player>, "
						prefix = "For each " + listOfRoles.get(0).get_player().get_name() + ", ";
						facts = verbalizeFactWithOneConstrainedRole(listOfRoles.get(0), "at most one ", "that ");
						// TODO
						// if (listOfRoles.get(0).get_identifiedEntityObject()
						// != null) {
						// suffix = ". This association with " +
						// listOfRoles.get(0).get_player().get_name()
						// + " provides the preferred identification scheme for
						// "
						// +
						// listOfRoles.get(0).get_identifiedEntityObject().get_name()
						// + ". [ORM Uniqueness Constraint and preferred
						// identifier]";
						// }
					} else {
						// the constrained role is the first in the reading
						// order, so the fact is only preceded by "Each "
						prefix = "Each ";
						facts = verbalizeFactWithOneConstrainedRole(listOfRoles.get(0), "", "at most one ");
						// TODO
						// if (listOfRoles.get(0).get_identifiedEntityObject()
						// != null) {
						// suffix = ". This association with " +
						// listOfRoles.get(0).get_player().get_name()
						// + " provides the preferred identification scheme for
						// "
						// +
						// listOfRoles.get(0).get_identifiedEntityObject().get_name()
						// + ". [ORM Uniqueness Constraint and preferred
						// identifier]";
						// }
					}
				} else {
					ArrayList<ObjectType> definedObjectTypes = getDefinedObjectTypes(
							(UniquenessConstraint) ormConstraint, true);
					String player1 = listOfRoles.get(0).get_player().get_name();
					String player2 = listOfRoles.get(1).get_player().get_name();

					// a n:m uniqueness constraint
					prefix = "It is possible that ";
					facts = verbalizeBinaryFact(listOfRoles, definedObjectTypes, "some", "", "more than one", "", false,
							false) + " and that for some " + player2 + ", "
							+ verbalizeBinaryFact(listOfRoles, definedObjectTypes, "more than one", "", "that", "",
									false, false);

					// TODO preferred ID
				}
				suffix = ". [ORM Uniqueness Constraint]";
			}
		}

		sb.append(prefix + facts + suffix);
		return sb.toString();
	}

	private String verbalizeFactWithMultipleConstrainedRoles(ArrayList<Role> involvedRoles,
			ArrayList<ObjectType> definedRoles, boolean isSubsetConstraint, int roleSequenceIndex,
			String constrainedRolePlayerIndicator, String otherRolePlayerIndicator) {
		assert involvedRoles.size() > 0;

		StringBuffer sb = new StringBuffer();

		Fact fact = involvedRoles.get(0).get_fact();
		ArrayList<RoleKind> factRoles = new ArrayList<>();
		fact.get_factRole().forEach(role -> factRoles.add(role));

		String actor = "";
		String adjective = otherRolePlayerIndicator;

		String readingOrder = fact.get_readingOrder();

		// iterate over the roles contained in the fact and insert them into the
		// "holes" of the predicate reading
		if (fact.get_factKind() == FactKind.SUBTYPEFACT) {
			for (int i = 0; i < factRoles.size(); i++) {
				// this must be a subtype constraint
				Role currentRole = (Role) factRoles.get(i);
				if (isConstrainedRolePlayer(involvedRoles, currentRole)) {
					adjective = constrainedRolePlayerIndicator;
				} else {
					adjective = otherRolePlayerIndicator;
				}
				if (currentRole instanceof Role) {
					actor = currentRole.get_player().get_name();
				} else if (currentRole instanceof RoleProxy) {
					actor = ((RoleProxy) currentRole).get_linkedRole().get_player().get_name();
				}
				sb.append(adjective + actor);
				if (i == 0) {
					sb.append(" is a subtype of ");
				}
			}
		} else {
			Pattern pattern = Pattern.compile("\\{\\d+\\}");
			Matcher matcher = pattern.matcher(readingOrder);

			int i = 0;
			int endOfPreviousMatch = 0;
			while (matcher.find()) {

				String predicateReadingSection = readingOrder.substring(endOfPreviousMatch, matcher.end());
				boolean hasHyphenatedAdjective = hasHyphenatedAdjective(predicateReadingSection);
				RoleKind currentRole = factRoles.get(i);
				actor = util.getPlayer(currentRole).get_name();

				if (hasHyphenatedAdjective) {
					int modifiedEndIndex = getHyphenatedAdjectiveStartIndex(predicateReadingSection);
					sb.append(readingOrder.substring(endOfPreviousMatch, endOfPreviousMatch + modifiedEndIndex));
				} else {
					sb.append(readingOrder.substring(endOfPreviousMatch, matcher.start()));
				}

				if (isConstrainedRolePlayer(involvedRoles, factRoles.get(i))) {
					int pos = involvedRoles.indexOf(currentRole);

					// check whether the role player is one of the initially
					// defined ones
					if (util.getPlayer(currentRole) != definedRoles.get(pos)) {
						// if he isn't, include the hierarchy information
						sb.append("some " + util.getPlayer(currentRole).get_name() + " that is ");
						actor = definedRoles.get(pos).get_name();
					}
					if (isSubsetConstraint && roleSequenceIndex == 0 && involvedRoles.size() > 1) {
						adjective = "some ";

					} else {
						adjective = "that ";
					}
					adjective = constrainedRolePlayerIndicator;
				}
				sb.append(adjective);
				if (hasHyphenatedAdjective) {
					sb.append(getHyphenatedAdjective(predicateReadingSection));
				}
				sb.append(actor);

				++i;
				endOfPreviousMatch = matcher.end();

			}
			sb.append(readingOrder.substring(endOfPreviousMatch, readingOrder.length()));
		}
		return sb.toString();
	}

	/**
	 * This method is called during generation of the GReQL constraint message
	 * string. This method generates a string containing all names of
	 * {@link ObjectType}s involved in a {@link RoleSequenceConstraint}. It uses
	 * the first of possibly multiple {@link RoleSequence}s to retrieve the
	 * players.
	 * 
	 * @param involvedRoles
	 *            - An array list which, for each {@link RoleSequence} contained
	 *            in the {@link RoleSequenceConstraint}, contains an array list
	 *            of {@link Role}s
	 * @return a string listing all {@link ObjectType}s involved in a
	 *         constraint.
	 */
	private String printInvolvedObjectTypes(ArrayList<ArrayList<Role>> involvedRoles, boolean unique,
			String separator) {

		StringBuffer sb = new StringBuffer();
		Iterator<ObjectType> iterator;
		ArrayList<ObjectType> involvedObjectTypes = new ArrayList<>();

		involvedRoles.get(0).forEach(role -> involvedObjectTypes.add(role.get_player()));
		if (unique) {
			Set<ObjectType> uniqueInvolvedObjectTypes = new HashSet<>(involvedObjectTypes);

			iterator = uniqueInvolvedObjectTypes.iterator();

		} else {

			iterator = involvedObjectTypes.iterator();
		}

		while (iterator.hasNext()) {
			sb.append(iterator.next().get_name());
			if (iterator.hasNext()) {
				sb.append(separator);
			}
		}
		return sb.toString();
	}

	/**
	 * A method that takes a list of related {@link ObjectType}s as input and
	 * returns the {@link ObjectType} which is highest in their hierarchy
	 * 
	 * @param istConstrainedRolePlayer
	 *            - an array list of {@link ObjectType} that are in
	 *            sub/supertype relations to each other (not necessarily direct
	 *            relations)
	 * @return the {@link ObjectType} which is highest within the hierarchy of
	 *         the input {@link ObjectType}s (<b>not</b> the {@link ObjectType}
	 *         that is globally highest in the hierarchy)
	 */
	public ObjectType findHighestInHierarchy(ArrayList<ObjectType> istConstrainedRolePlayer) {
		assert istConstrainedRolePlayer.size() > 0;
		ObjectType highestInHierarchy = istConstrainedRolePlayer.get(0);
		int i = 0;
		for (ObjectType ot : istConstrainedRolePlayer) {
			boolean isHighest = false;
			// check whether this object type is the supertype of the others
			for (int j = i + 1; j < istConstrainedRolePlayer.size(); j++) {

				ObjectType potentialSubtype = istConstrainedRolePlayer.get(j);
				isHighest = util.isSupertypeOfObjectType(ot, potentialSubtype);
				if (!isHighest) {
					// must be the subtype of potentialSubtype, so no need to
					// continue search with ot
					break;
				}
			}
			if (isHighest) {
				highestInHierarchy = ot;
				return highestInHierarchy;
			}

			++i;
		}
		return highestInHierarchy;
	}

	private String writeFacts(RoleSequenceConstraint ormConstraint, String separator,
			String constrainedRolePlayerIndicator, String otherRolePlayerIndicator) {

		StringBuffer sb = new StringBuffer();
		ArrayList<ArrayList<Role>> involvedRoles = util.getConstrainedRoles(ormConstraint);

		assert involvedRoles.size() > 0;
		assert involvedRoles.get(0).size() > 0;
		int i = 0;

		for (ArrayList<Role> listOfRoles : involvedRoles) {
			// the RoleSequence contains Roles from exactly one fact
			if (util.allRolesFromSingleFact(listOfRoles)) {

				ArrayList<ObjectType> definedObjectTypes = getDefinedObjectTypes(ormConstraint, false);

				if (ormConstraint instanceof SetComparisonConstraint
						&& ((SetComparisonConstraint) ormConstraint).get_type() == SetComparisonConstraintKind.SUBSET) {
					sb.append(verbalizeFactWithMultipleConstrainedRoles(listOfRoles, definedObjectTypes, true, i,
							constrainedRolePlayerIndicator, otherRolePlayerIndicator));
				} else {
					sb.append(verbalizeFactWithMultipleConstrainedRoles(listOfRoles, definedObjectTypes, false, i,
							constrainedRolePlayerIndicator, otherRolePlayerIndicator));
				}

				if (i < involvedRoles.size() - 1) {
					sb.append(separator);
				}
			} else {
				// the RoleSequence contains Roles from different facts
				int j = 0;
				for (Role role : listOfRoles) {
					sb.append(verbalizeFactWithOneConstrainedRole(role, constrainedRolePlayerIndicator,
							otherRolePlayerIndicator));
					if (j < involvedRoles.get(0).size() - 1) {
						sb.append(separator);
					}
					j++;
				}
			}
			i++;
		}

		return sb.toString();
	}

	private String verbalizeFactWithOneConstrainedRole(Role role, String constrainedRolePlayerIndicator,
			String otherRolePlayerIndicator) {

		StringBuffer sb = new StringBuffer();
		Fact fact = role.get_fact();
		ArrayList<RoleKind> factRoles = new ArrayList<>();
		fact.get_factRole().forEach(factRole -> factRoles.add(factRole));
		String readingOrder = fact.get_readingOrder();
		String actor = "";
		String adjective = "";

		if (fact.get_factKind() == FactKind.SUBTYPEFACT) {
			for (int i = 0; i < factRoles.size(); i++) {
				// this must be a subtype constraint
				RoleKind currentRole = factRoles.get(i);
				if (role == currentRole) {
					adjective = constrainedRolePlayerIndicator;
				} else {
					adjective = otherRolePlayerIndicator;
				}
				actor = util.getPlayer(currentRole).get_name();
				sb.append(adjective + actor);
				if (i == 0) {
					sb.append(" is a subtype of ");
				}
			}
		} else {
			Pattern pattern = Pattern.compile("\\{\\d+\\}");
			Matcher matcher = pattern.matcher(readingOrder);

			int i = 0;
			int endOfPreviousMatch = 0;
			while (matcher.find()) {

				String predicateReadingSection = readingOrder.substring(endOfPreviousMatch, matcher.end());
				boolean hasHyphenatedAdjective = hasHyphenatedAdjective(predicateReadingSection);
				RoleKind currentRole = factRoles.get(i);
				actor = util.getPlayer(currentRole).get_name();
				String hyphenatedAdjective = "";

				if (hasHyphenatedAdjective) {
					int modifiedEndIndex = getHyphenatedAdjectiveStartIndex(predicateReadingSection);
					sb.append(readingOrder.substring(endOfPreviousMatch, endOfPreviousMatch + modifiedEndIndex));
					hyphenatedAdjective = getHyphenatedAdjective(predicateReadingSection);
				} else {
					sb.append(readingOrder.substring(endOfPreviousMatch, matcher.start()));
				}

				if (role == factRoles.get(i)) {
					adjective = constrainedRolePlayerIndicator;
				} else {
					adjective = otherRolePlayerIndicator;
				}
				sb.append(adjective + hyphenatedAdjective);
				sb.append(actor);

				++i;
				endOfPreviousMatch = matcher.end();

			}
			sb.append(readingOrder.substring(endOfPreviousMatch, readingOrder.length()));
		}

		return sb.toString();

	}

	private String verbalizeBinaryFact(ArrayList<Role> involvedRoles, ArrayList<ObjectType> definedRoles,
			String prefixPlayer1, String suffixPlayer1, String prefixPlayer2, String suffixPlayer2, boolean mutePlayer1,
			boolean mutePlayer2) {
		StringBuffer sb = new StringBuffer();
		Fact fact = involvedRoles.get(0).get_fact();
		ArrayList<RoleKind> factRoles = new ArrayList<>();
		fact.get_factRole().forEach(role -> factRoles.add(role));
		String adjective = "some ";
		String suffix = "";
		String actor;

		String readingOrder = fact.get_readingOrder();

		Pattern pattern = Pattern.compile("\\{\\d+\\}");
		Matcher matcher = pattern.matcher(readingOrder);

		int i = 0;
		int endOfPreviousMatch = 0;
		while (matcher.find()) {

			String predicateReadingSection = readingOrder.substring(endOfPreviousMatch, matcher.end());
			boolean hasHyphenatedAdjective = hasHyphenatedAdjective(predicateReadingSection);
			actor = ((Role) factRoles.get(i)).get_player().get_name();

			if (hasHyphenatedAdjective) {
				int modifiedEndIndex = getHyphenatedAdjectiveStartIndex(predicateReadingSection);
				sb.append(readingOrder.substring(endOfPreviousMatch, endOfPreviousMatch + modifiedEndIndex));
			} else {
				sb.append(readingOrder.substring(endOfPreviousMatch, matcher.start()));
			}

			if (factRoles.get(i) == involvedRoles.get(0)) {
				if (!prefixPlayer1.equals("")) {
					adjective = prefixPlayer1 + " ";
				} else {
					adjective = prefixPlayer1;
				}
				if (mutePlayer1) {
					actor = "";
				}

				suffix = suffixPlayer1;
			} else if (factRoles.get(i) == involvedRoles.get(1)) {
				if (!prefixPlayer2.equals("")) {
					adjective = prefixPlayer2 + " ";
				} else {
					adjective = prefixPlayer2;
				}
				if (mutePlayer2) {
					actor = "";
				}
				suffix = suffixPlayer2;
			} else {
				sb.append(adjective);
			}
			sb.append(adjective);
			if (hasHyphenatedAdjective) {
				sb.append(getHyphenatedAdjective(predicateReadingSection));
			}
			sb.append(actor);
			sb.append(suffix);

			++i;
			endOfPreviousMatch = matcher.end();

		}
		sb.append(readingOrder.substring(endOfPreviousMatch, readingOrder.length()));

		return sb.toString();
	}

	/**
	 * This method receives lists of {@link Role}s contained in
	 * {@link RoleSequenceConstraint}s' RoleSequences. It returns, for each
	 * {@link Role}, the {@link ObjectType} player which is highest in
	 * hierarchy. This method can only be used for {@link RoleSequence}
	 * constraints that demand that the i-th {@link Role} of a
	 * {@link RoleSequence} is always played by compatible {@link ObjectType}s.
	 * 
	 * @param involvedRoles
	 *            - An array list which, for each {@link RoleSequence} contained
	 *            in the {@link RoleSequenceConstraint}, contains an array list
	 *            of {@link Role}s
	 * @return an array list containing the {@link ObjectType}s which were
	 *         highest in hierarchy
	 */
	private ArrayList<ObjectType> getHighestInHierarchyForEachRole(RoleSequenceConstraint roleSequenceConstraint,
			boolean unique) {
		ArrayList<ArrayList<Role>> involvedRoles = util.getConstrainedRoles(roleSequenceConstraint);
		int nrRoleSequences = involvedRoles.size();
		int nrInvolvedRoles = 0;

		if (nrRoleSequences > 0) {
			nrInvolvedRoles = involvedRoles.get(0).size();
		}

		ArrayList<ObjectType> highestInHierarchyForEachRole = new ArrayList<>();
		for (int i = 0; i < nrInvolvedRoles; i++) {
			// get the players of the i-st role in each of the RoleSequences
			// (they are either identical or in a subtype relation)
			ArrayList<ObjectType> istConstrainedRolePlayer = new ArrayList<>();
			for (int j = 0; j < nrRoleSequences; j++) {
				istConstrainedRolePlayer.add(involvedRoles.get(j).get(i).get_player());
			}
			ObjectType highest = findHighestInHierarchy(istConstrainedRolePlayer);
			highestInHierarchyForEachRole.add(highest);
		}
		if (unique) {
			HashSet<ObjectType> uniqueHighestInHierarchyForEachRole = new HashSet<>(highestInHierarchyForEachRole);
			highestInHierarchyForEachRole.clear();
			highestInHierarchyForEachRole.addAll(uniqueHighestInHierarchyForEachRole);
		}
		return highestInHierarchyForEachRole;
	}

	/**
	 * This method is called during generation of the GReQL constraint message
	 * string. This method generates a string containing all names of
	 * {@link ObjectType}s involved in a {@link RoleSequenceConstraint}. The
	 * method inserts the {@link ObjectType}s highest in hierarchy into the
	 * string.
	 * 
	 * Example: A constraint with two {@link RoleSequence}s. RoleSequence 1
	 * contains roles played by: "A", "B" RoleSequence 2 contains roles played
	 * by: "AsSubtype" (a subtype of "A") and "B" The generated string will
	 * read: "For each A and B, " - it won't mention "AsSubtype" since this is
	 * not the {@link ObjectType} highest in the subtype/supertype hierarchy.
	 * 
	 * @param involvedRoles
	 *            - An array list which, for each {@link RoleSequence} contained
	 *            in the {@link RoleSequenceConstraint}, contains an array list
	 *            of {@link Role}s
	 * @return a string listing all {@link ObjectType}s involved in a
	 *         constraint.
	 */
	private String printHighestInvolvedObjectTypes(RoleSequenceConstraint roleSequenceConstraint, boolean unique) {
		ArrayList<ObjectType> highestInHierarchyForEachRole = new ArrayList<>();
		StringBuffer sb = new StringBuffer();

		highestInHierarchyForEachRole = getHighestInHierarchyForEachRole(roleSequenceConstraint, unique);
		Set<ObjectType> uniqueHighestInHierarchyForEachRole = new HashSet<>(highestInHierarchyForEachRole);

		Iterator<ObjectType> iterator = uniqueHighestInHierarchyForEachRole.iterator();
		while (iterator.hasNext()) {
			sb.append(iterator.next().get_name());
			if (iterator.hasNext()) {
				sb.append(" and ");
			}
		}
		sb.append(", ");
		return sb.toString();
	}

	private String verbalizeRanges(ArrayList<Range> listOfRanges) {
		StringBuffer sb = new StringBuffer();

		for (Range range : listOfRanges) {
			sb.append(verbalizeRange(range));
		}
		return sb.toString();
	}

	private String verbalizeRange(Range range) {
		StringBuffer sb = new StringBuffer();

		String min = "";
		String max = "";
		String sep = "";
		if (!range.isUnsetAttribute("minValue") && !range.isUnsetAttribute("maxValue")) {
			if (range.get_minValue() == range.get_maxValue()) {
				min = " exactly " + range.get_maxValue();
			} else {
				sep = " and ";
			}

		}
		if (min.equals("") && !range.isUnsetAttribute("minValue")) {
			min = "at least " + range.get_minValue();
		}

		if ((min.equals("") || sep.equals(" and ")) && !range.isUnsetAttribute("maxValue")) {
			if (range.get_maxValue() != -1) {
				max = "at most " + range.get_maxValue();
			} else {
				// remove and, since the upper bound is infinity
				sep = "";
			}
		}
		sb.append(min + sep + max);

		return sb.toString();
	}

	private boolean isConstrainedRolePlayer(ArrayList<Role> involvedRoles, RoleKind currentRole) {
		if (involvedRoles.contains(currentRole)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean hasHyphenatedAdjective(String substring) {
		Pattern hyphenatedAdjective = Pattern.compile("\\w+-{1}\\s*\\{\\d+\\}");
		Matcher hyphenatedAdjectiveMatcher = hyphenatedAdjective.matcher(substring);

		return hyphenatedAdjectiveMatcher.find();

	}

	private int getHyphenatedAdjectiveStartIndex(String substring) {
		Pattern hyphenatedAdjective = Pattern.compile("\\w+-{1}\\s*\\{\\d+\\}");
		Matcher hyphenatedAdjectiveMatcher = hyphenatedAdjective.matcher(substring);

		if (hyphenatedAdjectiveMatcher.find()) {
			return hyphenatedAdjectiveMatcher.start();
		}
		return -1;
	}

	private String getHyphenatedAdjective(String substring) {
		String adjective = "";
		Pattern hyphenatedAdjective = Pattern.compile("\\w+-{1}");
		Matcher hyphenatedAdjectiveMatcher = hyphenatedAdjective.matcher(substring);

		if (hyphenatedAdjectiveMatcher.find()) {
			adjective = hyphenatedAdjectiveMatcher.group(hyphenatedAdjectiveMatcher.groupCount());
		}

		return adjective;
	}

	private ArrayList<ObjectType> getDefinedObjectTypes(RoleSequenceConstraint roleSequenceConstraint, boolean unique) {
		ArrayList<ArrayList<Role>> involvedRoles = util.getConstrainedRoles(roleSequenceConstraint);
		ArrayList<ObjectType> involvedObjectTypes = new ArrayList<>();
		if (unique) {
			Set<Role> uniqueInvolvedRoles = new HashSet<>(involvedRoles.get(0));
			uniqueInvolvedRoles.forEach(uniqueRole -> involvedObjectTypes.add(uniqueRole.get_player()));
		} else {
			involvedRoles.get(0).forEach(role -> involvedObjectTypes.add(role.get_player()));
		}

		return involvedObjectTypes;
	}

	private String verbalizeValueRanges(ArrayList<ValueRange> listOfValueRanges) {
		StringBuffer sb = new StringBuffer();
		Iterator<ValueRange> iterator = listOfValueRanges.iterator();
		while (iterator.hasNext()) {
			sb.append(verbalizeValueRange(iterator.next()));
			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	private String verbalizeMultiplicity(ArrayList<Integer> minMaxMultiplicity) {
		StringBuffer sb = new StringBuffer();
		if (minMaxMultiplicity.get(0) == minMaxMultiplicity.get(1)) {
			sb.append("exactly " + minMaxMultiplicity.get(0));
		} else {
			sb.append("at least " + minMaxMultiplicity.get(0) + " and at most " + minMaxMultiplicity.get(1));
		}
		return sb.toString();
	}

	private String verbalizeValueRange(ValueRange valueRange) {
		StringBuffer sb = new StringBuffer();

		String min = "";
		String max = "";
		String sep = "";
		if (!valueRange.get_minValue().equals("") && !valueRange.get_maxValue().equals("")) {
			if (valueRange.get_minValue().equals(valueRange.get_maxValue())) {
				min = " exactly " + valueRange.get_maxValue();
			} else {
				sep = " and ";
			}

		}
		if (min.equals("") && !valueRange.get_minValue().equals("")) {
			min = "at least " + valueRange.get_minValue();
		}

		if ((min.equals("") || sep.equals(" and ")) && !valueRange.get_maxValue().equals("")) {
			max = "at most " + valueRange.get_maxValue();
		}
		sb.append(min + sep + max);

		return sb.toString();

	}

}
