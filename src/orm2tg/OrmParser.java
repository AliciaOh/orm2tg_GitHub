package orm2tg;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;

import de.uni_koblenz.jgralab.AttributedElement;
import de.uni_koblenz.jgralab.ImplementationType;
import de.uni_koblenz.jgralab.exception.GraphIOException;
import de.uni_koblenz.jgralab.graphvalidator.ConstraintViolation;
import de.uni_koblenz.jgralab.graphvalidator.GraphValidator;
import de.uni_koblenz.jgralab.impl.ConsoleProgressFunction;
import de.uni_koblenz.jgralab.utilities.xml2tg.Xml2Tg;
import de.uni_koblenz.jgralab.utilities.xml2tg.XmlGraphUtilities;
import de.uni_koblenz.jgralab.utilities.xml2tg.schema.Element;
import ormschema.ORMGraph;
import ormschema.ORMSchema;
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
import ormschema.constraints.ValueComparisonConstraint;
import ormschema.constraints.ValueComparisonKind;
import ormschema.constraints.ValueConstraint;
import ormschema.derivations.CombinationKind;
import ormschema.derivations.ConstraintRoleProjection;
import ormschema.derivations.JoinPath;
import ormschema.derivations.JoinPathProjection;
import ormschema.derivations.JoinRule;
import ormschema.derivations.PathedRole;
import ormschema.derivations.PathedRoleKind;
import ormschema.derivations.RolePath;
import ormschema.derivations.SubPath;
import ormschema.structure.ConceptualDataType;
import ormschema.structure.ConceptualDataTypeKinds;
import ormschema.structure.EntityObject;
import ormschema.structure.EntityType;
import ormschema.structure.Fact;
import ormschema.structure.FactKind;
import ormschema.structure.ObjectType;
import ormschema.structure.ObjectifiedType;
import ormschema.structure.Range;
import ormschema.structure.Role;
import ormschema.structure.RoleKind;
import ormschema.structure.RoleProxy;
import ormschema.structure.RoleSequence;
import ormschema.structure.ValueRange;
import ormschema.structure.ValueType;
import utilities.ORMSchemaGraphUtilities;

/****
 * 
 * class which creates a DOM tree from an ORM file, parses this DOM tree for
 * relevant information and stores it in a TGraph (ORMGraph)
 * 
 * Written for NORMA (Neumont Object-Role Modeling Architect), CPL release
 * September 2015. Homepage: http://orm.sourceforge.net
 */

public class OrmParser extends Xml2Tg {

	private static XmlGraphUtilities xu;
	private static ORMSchemaGraphUtilities util;
	private ORMGraph ormGraph;

	/**
	 * A map used to retrieve the "orm:ObjectifiedType" {@link Element} from the
	 * ORM schema file given the XML ID of this {@link Element}
	 */
	private HashMap<String, Element> objectifiedTypeIDToSchemaElement;

	public OrmParser() {
		setIgnoreCharacters(false);
		// orm:ModelErrors contains references to entity type elements
		// containing errors etc.
		// orm:ModelErrorDisplayFilter produces undefined references if
		// orm:ModelErrors isn't included
		addIgnoredElements("ormDiagram:ORMDiagram", "orm:Instances", "orm:ModelErrors", "orm:ModelErrorDisplayFilter",
				"orm:RoleInstances");
		addIdAttributes("*/id");
		addIdRefAttributes("*/ref");
	}

	// public static void main(String[] args) throws FileNotFoundException,
	// XMLStreamException, GraphIOException {
	// String inputFile;
	// File folder = new File("src/ormtestschemas");
	// File[] listOfFiles = folder.listFiles();
	// OrmParser orm2tg = new OrmParser();
	// for (int i = 0; i < listOfFiles.length; i++) {
	// inputFile = listOfFiles[i].getAbsolutePath();
	//
	// // inputFile = "src/ormtestschemas/objectifiedType_ternary.orm";
	// // i = listOfFiles.length;
	// try {
	// orm2tg.process(inputFile);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
	// }

	@Override
	public void process(String fileName) throws XMLStreamException, FileNotFoundException {
		System.out.println("\n\n--------------------------------------------------------------");
		System.out.println("Processing " + fileName + "...");

		super.process(fileName);
		xu = new XmlGraphUtilities(getXmlGraph());
		util = new ORMSchemaGraphUtilities();
		checkForInvalidValueTypes();
		checkForDerivations();
		checkForRawDataTypes();
		ormGraph = ORMSchema.instance().createORMGraph(ImplementationType.STANDARD);
		DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY);
		Date date = new Date();
		ormGraph.set_importDate(dateFormat.format(date));
		ormGraph.set_importedFileName(fileName);

		File file = new File(fileName);

		String name = file.getName();
		ormGraph.set_schemaName(file.getName().substring(0, name.lastIndexOf(".orm")));
		Element ormModelElement = xu.firstChildWithName(xu.getRootElement(), "orm:ORMModel");
		ormGraph.set_xmlId(xu.getAttributeValue(ormModelElement, "id"));

		processValueTypes();
		processEntityTypes();
		processObjectifiedTypes();
		postprocessObjects();
		processFacts();
		postprocessObjectifiedTypes();
		postprocessFacts();
		processRoleSequenceConstraints();
		removeUnwantedObjectifications();
		postprocessRoles();

		String outputFilename = ormGraph.get_schemaName();

		GraphValidator gv = new GraphValidator(ormGraph);
		for (ConstraintViolation cv : gv.validate()) {
			for (AttributedElement<?, ?> ae : cv.getOffendingElements()) {
				System.out.println(ae.toString());
			}
			System.out.println(cv.getMessage());
		}

		saveGraph("output/" + outputFilename + ".tg");
	}

	/**
	 * This method is used to detect ORM value types that don't participate in
	 * any relations.
	 * 
	 * @throws XMLStreamException
	 */
	private void checkForInvalidValueTypes() throws XMLStreamException {
		for (Element valueTypeElement : xu.elementsWithName("orm:ValueType")) {
			if (xu.firstChildWithName(valueTypeElement, "orm:PlayedRoles") == null) {
				throw new XMLStreamException(getFilename()
						+ ": ORM schemas containing value types that don't participate in relations can't be mapped to grUML (each ValueType needs to have at least one relation with an EntityType or an ObjectifiedType).");
			}
		}

	}

	/**
	 * * A method that removes the {@link ObjectifiedType}s (from
	 * "orm:ObjectifiedType" {@link Element}s in the ORM schema) and the
	 * {@link Fact}s (from "orm:ImpliedFact" {@link Element}s) including their
	 * {@link RoleKind}s from the schema leaving only a binary {@link Fact} with
	 * a spanning {@link UniquenessConstraint}.
	 * 
	 * Reasoning: If a binary fact type has a spanning uniqueness constraint
	 * (covering both fact roles at once), NORMA generates an
	 * {@link ObjectifiedType} from this {@link Fact}. The {@link ObjectType}s
	 * participating in this {@linkplain Fact} now are related to this
	 * {@link ObjectifiedType} representing the {@link Fact} through implied
	 * {@link Fact}s ("orm:ImpliedFact"). Since there is no obvious reason to
	 * obscure the binary relationship in this way, this method regenerates the
	 * binary relation by removing the {@link ObjectifiedType} and the implied
	 * {@link Fact}s, as well as the internal {@link Constraint}s within these
	 * implied Facts.
	 * 
	 */

	private void removeUnwantedObjectifications() {
		ArrayList<ObjectifiedType> objectifiedTypesToBeDeleted = new ArrayList<>();
		ArrayList<Fact> impliedFactsToBeDeleted = new ArrayList<>();
		ArrayList<Constraint> impliedFactConstraintsToBeDeleted = new ArrayList<>();

		for (ObjectifiedType objectifiedType : ormGraph.getObjectifiedTypeVertices()) {

			String objectifiedTypeId = objectifiedType.get_xmlId();
			Element objectifiedTypeElement = objectifiedTypeIDToSchemaElement.get(objectifiedTypeId);

			Element nestedPredicateElement = xu.firstChildWithName(objectifiedTypeElement, "orm:NestedPredicate");
			if (xu.hasAttribute(nestedPredicateElement, "IsImplied")
					&& xu.getAttributeValue(nestedPredicateElement, "IsImplied").equals("true")) {
				Fact objectifiedFact = getNestedPredicate(objectifiedTypeElement);

				if (util.isBinaryFact(objectifiedFact) && util.hasSpanningUC(objectifiedFact)) {
					// retrieve the implied Fact by getting the Roles
					// played by the ObjectifiedType

					for (Role role : objectifiedType.get_playedRole()) {
						impliedFactsToBeDeleted.add(role.get_fact());
						for (Constraint constraint : role.get_fact().get_constraint()) {
							assert !constraint.is_isInterpredicateConstraint();
							impliedFactConstraintsToBeDeleted.add(constraint);
						}
					}
					objectifiedTypesToBeDeleted.add(objectifiedType);

				}
			}
		}

		for (Constraint constraint : impliedFactConstraintsToBeDeleted) {
			ormGraph.deleteVertex(constraint);
		}

		for (Fact impliedFact : impliedFactsToBeDeleted) {
			ormGraph.deleteVertex(impliedFact);
		}
		// finally delete the objectifiedType itself
		for (ObjectifiedType objectifiedType : objectifiedTypesToBeDeleted) {
			ormGraph.deleteVertex(objectifiedType);
		}
	}

	/**
	 * A method to check whether the provided ORM schema contains (semi-)
	 * derivations. If it does, it cannot be mapped to a grUML schema.
	 * 
	 * @throws XMLStreamException
	 *             - if the ORM schema contains a (semi-) derivation.
	 */
	private void checkForDerivations() throws XMLStreamException {

		for (Element element : xu.elementsWithName("orm:DerivationRule")) {
			if (element != null) {
				throw new XMLStreamException(
						getFilename() + ": ORM schemas containing (semi-) derivations cannot be mapped to grUML.");
			}
		}

	}

	/**
	 * A method to check whether the provided ORM schema contains value types
	 * with the conceptual data type "raw". If it does, it cannot be mapped to a
	 * grUML schema since grUML has no corresponding domain.
	 * 
	 * @throws XMLStreamException
	 *             - if the ORM schema contains a value type linked to a raw
	 *             data type.
	 */
	private void checkForRawDataTypes() throws XMLStreamException {

		for (Element element : xu.elementsWithName("orm:DataTypes")) {
			if (xu.firstChildWithName(element, "orm:FixedLengthRawDataDataType") != null
					|| xu.firstChildWithName(element, "orm:VariableLengthRawDataDataType") != null
					|| xu.firstChildWithName(element, "orm:LargeLengthRawDataDataType") != null
					|| xu.firstChildWithName(element, "orm:PictureRawDataDataType") != null
					|| xu.firstChildWithName(element, "orm:OleObjectRawDataDataType") != null) {
				throw new XMLStreamException(getFilename()
						+ ": ORM schemas containing value types with conceptual data types of the type \"raw\" cannot be mapped to grUML.");
			}
		}

	}

	/**
	 * A method to process the {@link Element}s with the name "orm:EntityType"
	 * within the ORM schema
	 *
	 * @throws XMLStreamException
	 *             if the processed "orm:EntityType" has a constraint of unknown
	 *             type
	 *
	 */
	private void processEntityTypes() throws XMLStreamException {
		for (Element entityTypeElement : xu.elementsWithName("orm:EntityType")) {

			EntityType entityType = ormGraph.createEntityType();
			entityType.set_xmlId(xu.getAttributeValue(entityTypeElement, "id"));
			entityType.set_name(getName(entityTypeElement));
			entityType.set_referenceMode(xu.getAttributeValue(entityTypeElement, "_ReferenceMode"));
			assert !entityType.get_name().equals("");

			if (xu.hasAttribute(entityTypeElement, "IsIndependent")
					&& xu.getAttributeValue(entityTypeElement, "IsIndependent").equals("true")) {
				entityType.set_isIndependent(true);
			} else {
				entityType.set_isIndependent(false);
			}

			if (hasRoles(entityTypeElement)) {
				createRoles(entityTypeElement);
			}

			if (hasCardinalityConstraint(entityTypeElement)) {
				Constraint constraint = createConstraint(getCardinalityConstraintElement(entityTypeElement));
				entityType.add_objectTypeCardinalityConstraint((CardinalityConstraint) constraint);
			}
		}
	}

	/**
	 * A method to process the {@link Element}s with the name "orm:ValueType"
	 * within the ORM schema
	 *
	 * @throws XMLStreamException
	 *
	 */
	private void processValueTypes() throws XMLStreamException {
		for (Element valueTypeElement : xu.elementsWithName("orm:ValueType")) {

			ValueType valueType = ormGraph.createValueType();
			valueType.set_xmlId(xu.getAttributeValue(valueTypeElement, "id"));
			valueType.set_name(getName(valueTypeElement));
			assert !valueType.get_name().equals("");

			if (xu.hasAttribute(valueTypeElement, "IsImplicitBooleanValue")) {
				if (xu.getAttributeValue(valueTypeElement, "IsImplicitBooleanValue").equals("true")) {
					valueType.set_isImplicitBooleanValue(true);
				}
			}

			valueType.add_conceptualDataType(getConceptualDataType(valueTypeElement));

			if (hasValueConstraint(valueTypeElement)) {
				Constraint constraint = createConstraint(getValueConstraintElement(valueTypeElement));
				valueType.add_valueTypeValueConstraint((ValueConstraint) constraint);
			}

			if (hasCardinalityConstraint(valueTypeElement)) {
				Constraint constraint = createConstraint(getCardinalityConstraintElement(valueTypeElement));
				valueType.add_objectTypeCardinalityConstraint((CardinalityConstraint) constraint);
			}

			valueType.set_xmlId(xu.getAttributeValue(valueTypeElement, "id"));
			if (hasRoles(valueTypeElement)) {
				createRoles(valueTypeElement);
			}
		}
	}

	/**
	 * A method to process the {@link Element}s with the name
	 * "orm:ObjectifiedType" within the ORM schema
	 *
	 * @throws XMLStreamException
	 *             if the processed "orm:ObjectifiedType" has a constraint of
	 *             unknown type
	 *
	 */
	private void processObjectifiedTypes() throws XMLStreamException {
		for (Element objectifiedTypeElement : xu.elementsWithName("orm:ObjectifiedType")) {

			ObjectifiedType objectifiedType = ormGraph.createObjectifiedType();
			objectifiedType.set_xmlId(xu.getAttributeValue(objectifiedTypeElement, "id"));
			if (objectifiedTypeIDToSchemaElement == null) {
				objectifiedTypeIDToSchemaElement = new HashMap<>();
			}
			objectifiedTypeIDToSchemaElement.put(objectifiedType.get_xmlId(), objectifiedTypeElement);

			objectifiedType.set_name(getName(objectifiedTypeElement));
			objectifiedType.set_referenceMode(xu.getAttributeValue(objectifiedTypeElement, "_ReferenceMode"));
			assert !objectifiedType.get_name().equals("");

			if (xu.hasAttribute(objectifiedTypeElement, "IsIndependent")
					&& xu.getAttributeValue(objectifiedTypeElement, "IsIndependent").equals("true")) {
				objectifiedType.set_isIndependent(true);
			} else {
				objectifiedType.set_isIndependent(false);
			}

			if (hasRoles(objectifiedTypeElement)) {
				createRoles(objectifiedTypeElement);
			}
			if (hasCardinalityConstraint(objectifiedTypeElement)) {
				Constraint constraint = createConstraint(getCardinalityConstraintElement(objectifiedTypeElement));
				objectifiedType.add_objectTypeCardinalityConstraint((CardinalityConstraint) constraint);
			}
		}
	}

	/**
	 * A method to process the {@link Element}s with the name "orm:Fact" within
	 * the ORM schema
	 *
	 * @throws XMLStreamException
	 *             if the elements within the processed "orm:Facts" have
	 *             constraints of unknown type
	 *
	 */
	private void processFacts() throws XMLStreamException {
		String[] factKinds = { "orm:Fact", "orm:ImpliedFact", "orm:SubtypeFact" };
		for (Element factContainer : xu.elementsWithName("orm:Facts")) {
			for (int i = 0; i < factKinds.length; ++i) {

				for (Element factElement : xu.childrenWithName(factContainer, factKinds[i])) {

					createFactFromElement(factElement);
				}
			}
		}
	}

	/**
	 * A method to process the {@link Element}s with the name "orm:Constraints"
	 * within the ORM schema.This {@linkplain Element} contains almost all
	 * constraints that appear in the schema (explicitly and implicitly). The
	 * constraints not contained in this element are {@link ValueConstraint}s
	 * and {@link CardinalityConstraint}s.
	 *
	 * @throws XMLStreamException
	 *             - if the orm file contains elements that cannot be mapped
	 *
	 */
	private void processRoleSequenceConstraints() throws XMLStreamException {
		for (Element constraintContainerElement : xu.elementsWithName("orm:Constraints")) {
			for (Element constraintElement : constraintContainerElement.get_children()) {
				createConstraint(constraintElement);

			}
		}
	}

	private void postprocessObjects() throws XMLStreamException {
		postprocessEntityTypes();
		postprocessValueTypes();

	}

	/**
	 * a method to fill in the information about "orm:ValueType" {@link Element}
	 * s that couldn't be added previously because not all referenced objects
	 * had been generated
	 * 
	 * @throws Exception
	 *             if ValueTypes in the ORM schema havbe subtypes or supertypes,
	 *             they cannot be mapped.
	 */
	private void postprocessValueTypes() throws XMLStreamException {
		for (Element valueType : xu.elementsWithName("orm:ValueType")) {
			if (hasSubtype(valueType) || hasSupertype(valueType)) {
				throw new XMLStreamException("Value types with subtypes/supertypes are not supported by this mapping.");
			}
		}
	}

	/**
	 * a method to fill in the information about "orm:EntityType"
	 * {@link Element}s that couldn't be added previously because not all
	 * referenced objects had been generated
	 */
	private void postprocessEntityTypes() {
		for (Element entityTypeElement : xu.elementsWithName("orm:EntityType")) {

			EntityType entityType = null;

			entityType = (EntityType) getObjectTypeFromXmlId(xu.getAttributeValue(entityTypeElement, "id"));
			if (hasSupertype(entityTypeElement)) {
				for (ObjectType objectType : getSupertypes(entityTypeElement)) {
					entityType.add_supertype(objectType);
				}
			}
			if (hasSubtype(entityTypeElement)) {
				for (ObjectType objectType : getSubtypes(entityTypeElement)) {
					entityType.add_subtype(objectType);
				}
			}
			for (ObjectType preferredIdentifier : getPreferredIdentifierValueTypes(entityTypeElement)) {

				entityType.add_preferredIdentifierValueType((ValueType) preferredIdentifier);

			}

			for (Role preferredIdentifierRole : getPreferredIdentifierRoles(entityTypeElement)) {
				entityType.add_preferredIdentifierRole(preferredIdentifierRole);
			}
		}
	}

	/**
	 * a method to fill in the information about "orm:ObjectifiedType"
	 * {@link Element}s that couldn't be added previously because not all
	 * referenced objects had been generated
	 */
	private void postprocessObjectifiedTypes() {

		for (Element objectifiedTypeElement : xu.elementsWithName("orm:ObjectifiedType")) {

			ObjectifiedType objectifiedType = (ObjectifiedType) getObjectTypeFromXmlId(
					xu.getAttributeValue(objectifiedTypeElement, "id"));

			// add an association with the fact that caused this objectification
			objectifiedType.add_fact(getNestedPredicate(objectifiedTypeElement));

			for (ObjectType preferredIdentifier : getPreferredIdentifierValueTypes(objectifiedTypeElement)) {

				objectifiedType.add_preferredIdentifierValueType((ValueType) preferredIdentifier);

			}
			for (Role preferredIdentifierRole : getPreferredIdentifierRoles(objectifiedTypeElement)) {
				objectifiedType.add_preferredIdentifierRole(preferredIdentifierRole);
			}
		}
	}

	private void postprocessFacts() {
		for (Fact fact : ormGraph.getFactVertices()) {

			if (fact.get_factKind() == FactKind.FACT) {
				if (fact.get_objectifiedType() != null) {
					// the fact has a spanning UC or is of arity >= 3
					ObjectifiedType objectifiedType = fact.get_objectifiedType();

					// find the implied facts the implied objectified type is
					// involved in
					for (Role role : objectifiedType.get_playedRole()) {
						if (role.get_fact().get_factKind() == FactKind.IMPLIEDFACT) {
							fact.add_impliedFact(role.get_fact());
						}
					}
				}
			} else {
				continue;
			}

		}
		// find the impliedFacts and add them to the nested predicate fact!
	}

	/**
	 * A method to extract the {@link Role}s involved in the definition of the
	 * input {@link Element}'s preferred identifiers. These are required for the
	 * mapping.
	 * 
	 * @param entityObjectElement
	 *            - the "orm:EntityType" or "orm:ObjectifiedType"
	 *            {@link Element} that must have at least one preferred
	 *            identifier (any "orm:Object" Element from the schema).
	 * @return a list of {@link Role}'s whose players are the preferred
	 *         identifiers of the input {@link Element}.
	 */
	private ArrayList<Role> getPreferredIdentifierRoles(Element entityObjectElement) {
		ArrayList<Role> roles = new ArrayList<>();

		// check for direct definition of preferred identifiers
		if (xu.firstChildWithName(entityObjectElement, "orm:PreferredIdentifier") != null) {
			Element prefID = xu.firstChildWithName(entityObjectElement, "orm:PreferredIdentifier");
			Element uc = xu.getReferencedElement(prefID, "ref");

			for (Element roleElement : xu.childrenWithName(xu.firstChildWithName(uc, "orm:RoleSequence"), "orm:Role")) {
				for (Role role : ormGraph.getRoleVertices()) {
					if (role.get_xmlId().equals(xu.getAttributeValue(roleElement, "ref"))) {
						roles.add(role);
					}
				}
			}
		}

		return roles;
	}

	/**
	 * A method that returns the attribute "Name" of ORM schema {@link Element}
	 * s.
	 *
	 * @param element
	 *            - the {@link Element} for which the name is requested
	 * @return the name of the input {@link Element} as {@code String} or
	 *         {@code null} if the {@link Element} has no such attribute.
	 */
	private String getName(Element element) {
		if (xu.hasAttribute(element, "Name")) {
			return xu.getAttributeValue(element, "Name");
		} else if (xu.hasAttribute(element, "_Name")) {
			return xu.getAttributeValue(element, "_Name");
		} else {
			return null;
		}
	}

	/**
	 * A method to extract the preferred identifiers' value types for an
	 * "orm:EntityType" or "orm:ObjectifiedType" {@link Element} from an ORM
	 * schema. Any {@link ObjectType} can be a preferred identifier.
	 *
	 * @param entityObjectElement
	 *            - the "orm:EntityType" or "orm:ObjectifiedType"
	 *            {@link Element} which must have at least one preferred
	 *            identifier
	 * @return the list of the input {@link Element}'s preferred identifiers'
	 *         {@link ValueType}s
	 */
	private ArrayList<ValueType> getPreferredIdentifierValueTypes(Element entityObjectElement) {
		ArrayList<ValueType> ids = new ArrayList<>();

		if (entityObjectElement.get_name().equals("orm:ValueType")) {
			ids.add((ValueType) getObjectTypeFromXmlId(xu.getAttributeValue(entityObjectElement, "id")));
		}

		// check for direct definition of preferred identifiers
		if (xu.firstChildWithName(entityObjectElement, "orm:PreferredIdentifier") != null) {
			Element prefID = xu.firstChildWithName(entityObjectElement, "orm:PreferredIdentifier");
			Element uc = xu.getReferencedElement(prefID, "ref");

			for (Element role : xu.childrenWithName(xu.firstChildWithName(uc, "orm:RoleSequence"), "orm:Role")) {

				Element factRole = xu.getReferencedElement(role, "ref");
				Element preferredIdentifierElement = xu
						.getReferencedElement(xu.firstChildWithName(factRole, "orm:RolePlayer"), "ref");
				ArrayList<ValueType> preferredIdentifiers = getPreferredIdentifierValueTypes(
						preferredIdentifierElement);
				ids.addAll(preferredIdentifiers);
			}
			return ids;
		}
		return ids;
	}

	/**
	 * A method to retrieve the ORM schema {@link Element}
	 * "orm:CardinalityConstraint" or "orm:UnaryRoleCardinalityConstraint" from
	 * the schema
	 *
	 * @param roleOrEntityObjectElement
	 *            - the {@link Element} which contains an
	 *            "orm:CardinalityConstraint" or
	 *            "orm:UnaryRoleCardinalityConstraint" {@link Element}
	 * @return the "orm:CardinalityConstraint" or
	 *         "orm:UnaryRoleCardinalityConstraint" {@link Element} of the input
	 *         {@link Element}
	 */
	private Element getCardinalityConstraintElement(Element roleOrEntityObjectElement) {
		Element cardinalityConstraintElement;
		if (roleOrEntityObjectElement.get_name().equals("orm:EntityType")
				|| roleOrEntityObjectElement.get_name().equals("orm:ObjectifiedType")
				|| roleOrEntityObjectElement.get_name().equals("orm:ValueType")) {
			cardinalityConstraintElement = xu.firstChildWithName(
					xu.firstChildWithName(roleOrEntityObjectElement, "orm:CardinalityRestriction"),
					"orm:CardinalityConstraint");
		} else {
			assert roleOrEntityObjectElement.get_name().equals("orm:Role");
			cardinalityConstraintElement = xu.firstChildWithName(
					xu.firstChildWithName(roleOrEntityObjectElement, "orm:CardinalityRestriction"),
					"orm:UnaryRoleCardinalityConstraint");

		}
		return cardinalityConstraintElement;
	}

	/**
	 * A method to return the supertypes of a given {@link Element}
	 *
	 * @param element
	 *            - the {@link Element} in the ORM schema that has at least one
	 *            supertype
	 * @return a list of the input {@link Element}'s supertypes (as
	 *         {@link ObjectType}
	 */
	private ArrayList<ObjectType> getSupertypes(Element element) {
		ArrayList<ObjectType> supertypes = new ArrayList<>();
		Element playedRoles = xu.firstChildWithName(element, "orm:PlayedRoles");
		for (Element subtypeRole : xu.childrenWithName(playedRoles, "orm:SubtypeMetaRole")) {

			Element subtype = xu.getReferencedElement(subtypeRole, "ref");
			for (Element supertypeRole : xu.childrenWithName(subtype.get_parent(), "orm:SupertypeMetaRole")) {
				Element supertypeElement = xu
						.getReferencedElement(xu.firstChildWithName(supertypeRole, "orm:RolePlayer"), "ref");

				ObjectType supertype = getObjectTypeFromXmlId(xu.getAttributeValue(supertypeElement, "id"));

				supertypes.add(supertype);
			}
		}
		return supertypes;
	}

	/**
	 * A method to return the subtypes of a given {@link Element}
	 *
	 * @param element
	 *            - the {@link Element} in the ORM schema that has at least one
	 *            subtype
	 * @return a list of the input {@link Element}'s subtypes (as
	 *         {@link ObjectType}
	 */
	private ArrayList<ObjectType> getSubtypes(Element element) {

		ArrayList<ObjectType> subtypes = new ArrayList<>();
		Element playedRoles = xu.firstChildWithName(element, "orm:PlayedRoles");

		for (Element supertypeRole : xu.childrenWithName(playedRoles, "orm:SupertypeMetaRole")) {
			Element supertype = xu.getReferencedElement(supertypeRole, "ref");

			for (Element subtypeRole : xu.childrenWithName(supertype.get_parent(), "orm:SubtypeMetaRole")) {
				Element subtypeElement = xu.getReferencedElement(xu.firstChildWithName(subtypeRole, "orm:RolePlayer"),
						"ref");
				ObjectType subtype = getObjectTypeFromXmlId(xu.getAttributeValue(subtypeElement, "id"));
				subtypes.add(subtype);
			}
		}
		return subtypes;

	}

	/**
	 * a method to check whether an {@link CardinalityConstraint} is defined
	 * within the ORM schema for the input {@link Element}
	 *
	 * @param entityType
	 *            - the {@link Element} that may or may not have a
	 *            {@link CardinalityConstraint}
	 * @return - true, if the {@link Element} entityType has a
	 *         {@link CardinalityConstraint}, false if not.
	 */
	private boolean hasCardinalityConstraint(Element entityType) {
		if (xu.firstChildWithName(entityType, "orm:CardinalityRestriction") != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * A method to extract the {@link ConceptualDataType} for a given valueType
	 * {@link Element}
	 *
	 * @param valueType
	 *            - the value type {@link Element} from the ORM schema for which
	 *            the conceptual data type is requested
	 * @return
	 * @throws XMLStreamException
	 *             - if the conceptual data type is unknown and cannot be mapped
	 */
	private ConceptualDataType getConceptualDataType(Element valueType) throws XMLStreamException {

		ConceptualDataType conceptualDatatype = ormGraph.createConceptualDataType();

		Element conceptualDataType = xu.firstChildWithName(valueType, "orm:ConceptualDataType");
		Element dataType = xu.getReferencedElement(conceptualDataType, "ref");
		String dataTypeString = dataType.get_name();

		switch (dataTypeString) {
		case "orm:AutoCounterNumericDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.AUTOCOUNTERNUMERICDATATYPE);
			break;
		case "orm:AutoTimestampTemporalDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.AUTOTIMESTAMPTEMPORALDATATYPE);
			break;
		case "orm:DateAndTimeTemporalDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.DATEANDTIMETEMPORALDATATYPE);
			break;
		case "orm:DateTemporalDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.DATETEMPORALDATATYPE);
			break;
		case "orm:DecimalNumericDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.DECIMALNUMERICDATATYPE);
			break;
		case "orm:DoublePrecisionFloatingPointNumericDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.DOUBLEPRECISIONFLOATINGPOINTNUMERICDATATYPE);
			break;
		case "orm:FixedLengthRawDataDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.FIXEDLENGTHRAWDATADATATYPE);
			break;
		case "orm:FixedLengthTextDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.FIXEDLENGTHTEXTDATATYPE);
			break;
		case "orm:LargeLengthRawDataDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.LARGELENGTHRAWDATADATATYPE);
			break;
		case "orm:LargeLengthTextDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.LARGELENGTHTEXTDATATYPE);
			break;
		case "orm:MoneyNumericDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.MONEYNUMERICDATATYPE);
			break;
		case "orm:ObjectIdOtherDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.OBJECTIDOTHERDATATYPE);
			break;
		case "orm:OleObjectRawDataDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.OLEOBJECTRAWDATADATATYPE);
			break;
		case "orm:PictureRawDataDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.PICTURERAWDATADATATYPE);
			break;
		case "orm:RowIdOtherDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.ROWIDOTHERDATATYPE);
			break;
		case "orm:SignedIntegerNumericDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.SIGNEDINTEGERNUMERICDATATYPE);
			break;
		case "orm:SignedLargeIntegerNumericDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.SIGNEDLARGEINTEGERNUMERICDATATYPE);
			break;
		case "orm:SignedSmallIntegerNumericDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.SIGNEDSMALLINTEGERNUMERICDATATYPE);
			break;
		case "orm:SinglePrecisionFloatingPointNumericDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.SINGLEPRECISIONFLOATINGPOINTNUMERICDATATYPE);
			break;
		case "orm:TimeTemporalDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.TIMETEMPORALDATATYPE);
			break;
		case "orm:TrueOrFalseLogicalDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.TRUEORFALSELOGICALDATATYPE);
			break;
		case "orm:UnsignedIntegerNumericDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.UNSIGNEDINTEGERNUMERICDATATYPE);
			break;
		case "orm:UnsignedLargeIntegerNumericDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.UNSIGNEDLARGEINTEGERNUMERICDATATYPE);
			break;
		case "orm:UnsignedSmallIntegerNumericDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.UNSIGNEDSMALLINTEGERNUMERICDATATYPE);
			break;
		case "orm:UnsignedTinyIntegerNumericDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.UNSIGNEDTINYINTEGERNUMERICDATATYPE);
			break;
		case "orm:VariableLengthRawDataDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.VARIABLELENGTHRAWDATADATATYPE);
			break;
		case "orm:VariableLengthTextDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.VARIABLELENGTHTEXTDATATYPE);
			break;
		case "orm:YesOrNoLogicalDataType":
			conceptualDatatype.set_dataType(ConceptualDataTypeKinds.YESORNOLOGICALDATATYPE);
			break;
		default:
			throw new XMLStreamException(
					"The data type " + dataTypeString + " is unknown and cannot be mapped to a grUML schema.");
		}
		return conceptualDatatype;
	}

	/**
	 * A method to generate a {@link ValueRange} from an "orm:ValueRange"
	 * {@link Element} within the ORM schema
	 *
	 * @param element
	 *            - an "orm:ValueRange" {@link Element} (contained in an
	 *            "orm:ValueType" or "orm:Role" {@link Element} in an ORM
	 *            schema)
	 * @return the {@link ValueRange} object
	 */
	private ValueRange createValueRange(Element valueRangeElement) {

		ValueRange valueRange = ormGraph.createValueRange();

		// MinValue, MinInclusion, InvariantMinValue - MaxValue,
		// MaxInclusion,
		// InvariantMaxValue
		valueRange.set_minValue(xu.getAttributeValue(valueRangeElement, "MinValue"));
		if (xu.getAttributeValue(valueRangeElement, "MinInclusion").equals("NotSet")) {
			valueRange.set_minInclusion(false);
		} else {
			valueRange.set_minInclusion(true);
		}

		if (xu.hasAttribute(valueRangeElement, "InvariantMinValue")) {
			valueRange.set_invariantMinValue(xu.getAttributeValue(valueRangeElement, "InvariantMinValue"));
		}
		valueRange.set_maxValue(xu.getAttributeValue(valueRangeElement, "MaxValue"));

		if (xu.getAttributeValue(valueRangeElement, "MaxInclusion").equals("NotSet")) {
			valueRange.set_maxInclusion(false);
		} else {
			valueRange.set_maxInclusion(true);
		}
		if (xu.hasAttribute(valueRangeElement, "InvariantMaxValue")) {
			valueRange.set_invariantMaxValue(xu.getAttributeValue(valueRangeElement, "InvariantMaxValue"));
		}

		return valueRange;
	}

	private Fact getNestedPredicate(Element objectifiedElement) {

		String nestedPredicateID = xu.getAttributeValue(
				xu.getReferencedElement(xu.firstChildWithName(objectifiedElement, "orm:NestedPredicate"), "ref"), "id");
		return getFactFromXmlId(nestedPredicateID);
	}

	private boolean hasRoles(Element element) {
		if (xu.firstChildWithName(element, "orm:PlayedRoles") != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * A method to check whether the input {@link Element} has a value
	 * constraint in the provided ORM schema.
	 *
	 * @param element
	 *            - "orm:Role" or "orm:ValueType" {@link Element}
	 * @return true, if the {@link Element} has a value constraint (indicated by
	 *         "orm:ValueRestriction") and false if not.
	 */
	private boolean hasValueConstraint(Element element) {

		if (xu.firstChildWithName(element, "orm:ValueRestriction") != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * A method to generate the {@link Role}s played by the input
	 * "orm:EntityType", "orm:ObjectifiedType" or "orm:ValueType"
	 * {@link Element}
	 *
	 * @param objectTypeElement
	 *            - the {@link Element} whose roles are generated
	 * @throws XMLStreamException
	 *             if the {@link Role} played by the input {@link Element} has
	 *             an unknown constraint
	 *
	 */
	private void createRoles(Element objectTypeElement) throws XMLStreamException {
		String[] roleKinds = { "orm:SupertypeMetaRole", "orm:SubtypeMetaRole", "orm:Role" };

		Element playedRolesElement = xu.firstChildWithName(objectTypeElement, "orm:PlayedRoles");

		for (int i = 0; i < roleKinds.length; i++) {
			for (Element referencingRoleElement : xu.childrenWithName(playedRolesElement, roleKinds[i])) {
				Element roleElement = xu.getReferencedElement(referencingRoleElement, "ref");
				createRole(roleElement, objectTypeElement);
			}
		}
	}

	/**
	 * A method to create a {@link Role} based on the {@link Element}
	 * "orm:Role", "orm:SubtypeMetaRole" or "orm:SupertypeMetaRole" of the
	 * encompassing {@link Element} "orm:ValueType", "orm:EntityType" or
	 * "orm:ObjectifiedType"
	 *
	 * @param roleElement
	 *            - the "orm:Role", "orm:SubtypeMetaRole" or
	 *            "orm:SupertypeMetaRole" {@link Element}
	 * @param rolePlayerElement
	 *            - the "orm:ValueType", "orm:EntityType" or
	 *            "orm:ObjectifiedType" {@link Element} that plays the role in
	 *            question
	 * @throws XMLStreamException
	 *             - if the roleElement has an unknown constraint
	 */
	private void createRole(Element roleElement, Element rolePlayerElement) throws XMLStreamException {

		String id = xu.getAttributeValue(roleElement, "id");
		Role role;

		ObjectType objectType = getObjectTypeFromXmlId(xu.getAttributeValue(rolePlayerElement, "id"));
		role = ormGraph.createRole();
		role.add_player(objectType);
		role.set_xmlId(id);
		role.set_name(getName(roleElement));

		if (hasValueConstraint(roleElement)) {
			ValueConstraint valueConstraint = (ValueConstraint) createConstraint(
					getValueConstraintElement(roleElement));
			valueConstraint.set_isRoleConstraint(true);
			role.add_roleValueConstraint(valueConstraint);

		}
		if (hasCardinalityConstraint(roleElement)) {
			CardinalityConstraint cardinalityConstraint = (CardinalityConstraint) createConstraint(
					getCardinalityConstraintElement(roleElement));
			cardinalityConstraint.set_isRoleConstraint(true);
			role.add_roleCardinalityConstraint(cardinalityConstraint);
		}

	}

	/**
	 * A method to retrieve the {@link Element} representing a value constraint
	 * for the input {@link Element} in an ORM schema
	 *
	 * @param element
	 *            - the {@link Element} that has a value constraint
	 *            ("orm:ValueType" or "orm:Role")
	 *
	 * @return the {@link Element} representing the value constraint of the
	 *         input {@link Element} in the ORM schema
	 */
	private Element getValueConstraintElement(Element element) {

		Element valueRestrictionElement = xu.firstChildWithName(element, "orm:ValueRestriction");
		Element valueConstraintElement;
		if (xu.firstChildWithName(valueRestrictionElement, "orm:RoleValueConstraint") != null) {
			valueConstraintElement = xu.firstChildWithName(valueRestrictionElement, "orm:RoleValueConstraint");
		} else {
			assert xu.firstChildWithName(valueRestrictionElement, "orm:ValueConstraint") != null;
			valueConstraintElement = xu.firstChildWithName(valueRestrictionElement, "orm:ValueConstraint");
		}
		return valueConstraintElement;
	}

	/**
	 * A method that creates {@link Fact}s based on the input "orm:Fact",
	 * "orm:ImpliedFact" or "orm:SubtypeFact" {@link Element} from the ORM
	 * schema
	 *
	 * @param factElement
	 *            - the input "orm:Fact", "orm:ImpliedFact" or "orm:SubtypeFact"
	 *            {@link Element} for which a {@link Fact} is created
	 * @throws XMLStreamException
	 *             if the processed
	 *             "orm:Fact"/"orm:ImpliedFact"/"orm:SubtypeFact" contains a
	 *             constraint of unknown type
	 */
	private void createFactFromElement(Element factElement) throws XMLStreamException {

		Fact fact = ormGraph.createFact();
		if (xu.firstChildWithName(factElement, "orm:Notes") != null) {
			for (Element noteElement : xu.childrenWithName(xu.firstChildWithName(factElement, "orm:Notes"),
					"orm:Note")) {
				String text = xu.getText(xu.firstChildWithName(noteElement, "orm:Text"));
				if (text.equals("Composition")) {
					fact.set_representsComposition(true);
				}
			}
		}

		fact.set_xmlId(xu.getAttributeValue(factElement, "id"));
		fact.set_name(xu.getAttributeValue(factElement, "_Name"));
		if (!factElement.get_name().equals("orm:SubtypeFact")) {
			fact.set_readingOrder(getReadingOrder(factElement));
		}
		setFactKind(factElement, fact);
		addRolesToFact(factElement, fact);
		checkIfOnlyValueTypePlayers(fact);

	}

	/**
	 * This method is used to check whether a fact contains at least one entity
	 * type. If it doesn't, this ORM schema cannot be mapped to grUML.
	 * 
	 * @param fact
	 *            - the {@link Fact} that may or may not contain only
	 *            {@link ValueType}s as players
	 * @throws XMLStreamException
	 *             - if only {@link ValueType}s participate in the input
	 *             {@link Fact}
	 */
	private void checkIfOnlyValueTypePlayers(Fact fact) throws XMLStreamException {
		boolean onlyValueTypePlayers = true;
		for (RoleKind roleKind : fact.get_factRole()) {
			if (util.getPlayer(roleKind) instanceof EntityObject) {
				onlyValueTypePlayers = false;
				break;
			}
		}
		if (onlyValueTypePlayers) {
			throw new XMLStreamException(getFilename()
					+ ": ORM schemas containing relations in which only ValueTypes participate can't be mapped to grUML (each ValueType needs to have at least one relation with an EntityType or an ObjectifiedType).");

		}

	}

	/**
	 * A method that sets the attribute "factKind" of the input {@link Fact}
	 * depending on the type of the input {@link Element} "factElement".
	 * 
	 * @param factElement
	 *            - the orm schema {@link Element} of the type
	 *            "orm:SubtypeFact", "orm:Fact" or "orm:ImpliedFact"
	 * @param fact
	 *            - the {@link Fact} that will receive a FactKind depending on
	 *            the type of the {@link Element} "factElement" it was generated
	 *            from
	 */
	private void setFactKind(Element factElement, Fact fact) {
		if (factElement.get_name().equals("orm:SubtypeFact")) {
			fact.set_factKind(FactKind.SUBTYPEFACT);
		} else if (factElement.get_name().equals("orm:Fact")) {
			fact.set_factKind(FactKind.FACT);
		} else {
			assert factElement.get_name().equals("orm:ImpliedFact");
			fact.set_factKind(FactKind.IMPLIEDFACT);
		}

	}

	/**
	 * A method to retrieve the information in the "orm:ReadingOrder"
	 * {@link Element} of a fact in an ORM schema. It includes the semantics of
	 * the relation and indicates which parts of the fact name are the names of
	 * the involved object types and which are the predicate. An n-ary fact can
	 * have n! different readings but this method only retrieves the reading
	 * order corresponding to the fact name.
	 * 
	 * Example: Fact name = "Person likes Language" reading = "{0} likes {1}
	 * where {0} and {1} stand for elements 0 and 1 in the {@link RoleSequence}
	 * (retrieved in getOrderedRoleSequenceElement(Element)).
	 * 
	 * @param factElement
	 *            - an "orm:Fact" or "orm:ImpliedFact" {@link Element} for which
	 *            the reading order should be retrieved
	 * @return the reading order as String. The predicate "holes" are
	 *         represented as curly brackets containing a number indicating that
	 *         the object type at this position of the role sequence plays this
	 *         role.
	 */
	private String getReadingOrder(Element factElement) {
		Element readingOrderElement = xu.firstChildWithName(xu.firstChildWithName(factElement, "orm:ReadingOrders"),
				"orm:ReadingOrder");
		// the same RoleSequence can have multiple readings - select the first
		// (corresponds to the fact name)
		Element readingElement = xu.firstChildWithName(xu.firstChildWithName(readingOrderElement, "orm:Readings"),
				"orm:Reading");
		String readingOrder = xu.getText(xu.firstChildWithName(readingElement, "orm:Data"));
		return readingOrder;
	}

	/**
	 * This method adds {@link Role}s to {@link Fact}s representing "orm:Fact",
	 * "orm:SubtypeFact" and "orm:ImpliedFact" elements
	 * 
	 * @param factElement
	 *            - the "orm:Fact", "orm:SubtypeFact" or "orm:ImpliedFact"
	 *            {@link Element} from the ORM schema file
	 * @param fact
	 *            - the {@link Fact} object representing the input
	 *            {@link Element}
	 * @throws XMLStreamException
	 * 
	 */
	private void addRolesToFact(Element factElement, Fact fact) throws XMLStreamException {

		Iterator<Element> roleIterator;

		boolean isUnaryOrSubtypeFact = false;

		if (representsUnaryFact(factElement) || factElement.get_name().equals("orm:SubtypeFact")) {
			isUnaryOrSubtypeFact = true;
			roleIterator = xu.firstChildWithName(factElement, "orm:FactRoles").get_children().iterator();
		} else {
			// get roles participating in the fact in the order
			// corresponding to
			// the
			// fact name and add them to the fact
			Element roleSequenceElement = getOrderedRoleSequenceElement(factElement);
			roleIterator = xu.childrenWithName(roleSequenceElement, "orm:Role").iterator();
		}

		RoleKind role;
		while (roleIterator.hasNext()) {

			Element roleElement = roleIterator.next();

			if (!isUnaryOrSubtypeFact) {
				roleElement = xu.getReferencedElement(roleElement, "ref");
			}

			if (roleElement.get_name().equals("orm:RoleProxy")) {
				role = ormGraph.createRoleProxy();
				((RoleProxy) role).add_linkedRole((Role) getRoleKindFromXmlId(
						xu.getAttributeValue(xu.firstChildWithName(roleElement, "orm:Role"), "ref")));

			} else {
				role = getRoleKindFromXmlId(xu.getAttributeValue(roleElement, "id"));
			}

			fact.add_factRole((role));

			if (role instanceof Role && ((Role) role).get_roleValueConstraint() != null) {
				fact.add_constraint(((Role) role).get_roleValueConstraint());
			}
			if (role instanceof Role && ((Role) role).get_roleCardinalityConstraint() != null) {
				fact.add_constraint(((Role) role).get_roleCardinalityConstraint());
			}
		}
	}

	/**
	 * A method to check whether the input "orm:Fact" or "orm:ImpliedFact"
	 * {@link Element} represents a unary relation. In ORM, a unary relation is
	 * modeled as a binary relation, where the object type participating in the
	 * unary relation has a relation to a value type of type boolean (indicated
	 * by the attribute "IsImplicitBoolean" of the "orm:ValueType"
	 * {@link Element}).
	 * 
	 * @param factElement
	 *            - the factElement which may represent a unary relation
	 * @return true of the input {@link Element} represents a unary relation.
	 *         False if not.
	 */
	private boolean representsUnaryFact(Element factElement) {

		boolean factIncludesImplicitBooleanValue = false;
		int counter = 0;
		Element factRolesElement = xu.firstChildWithName(factElement, "orm:FactRoles");
		for (Element roleElement : xu.childrenWithName(factRolesElement, "orm:Role")) {
			Element rolePlayerElement = xu.getReferencedElement(xu.firstChildWithName(roleElement, "orm:RolePlayer"),
					"ref");
			if (isImplicitBooleanValue(rolePlayerElement)) {
				factIncludesImplicitBooleanValue = true;
			}
			++counter;
		}
		for (Element roleElement : xu.childrenWithName(factRolesElement, "orm:RoleProxy")) {
			Element linkedRoleElement = xu.getReferencedElement(xu.firstChildWithName(roleElement, "orm:Role"), "ref");
			Element rolePlayerElement = xu.firstChildWithName(linkedRoleElement, "orm:RolePlayer");
			if (isImplicitBooleanValue(rolePlayerElement)) {
				factIncludesImplicitBooleanValue = true;
			}
			++counter;
		}

		if (counter == 2 && factIncludesImplicitBooleanValue) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * A method to check whether the input {@link Element} is an "orm:ValueType"
	 * with the attribute "IsImplicitBooleanValue".
	 * 
	 * @param objectTypeElement
	 *            - an {@link Element} which may be an implicit boolean value
	 * @return true if the input {@link Element} is an implicit boolean value.
	 *         False if not.
	 */
	private boolean isImplicitBooleanValue(Element objectTypeElement) {
		if (objectTypeElement.get_name().equals("orm:ValueType")
				&& xu.hasAttribute(objectTypeElement, "IsImplicitBooleanValue")
				&& xu.getAttributeValue(objectTypeElement, "IsImplicitBooleanValue").equals("true")) {
			return true;
		}
		return false;
	}

	/**
	 * A method to find the "orm:ReadingOrder" {@link Element} corresponding to
	 * the input "orm:Fact" or "orm:ImpliedFact". Within the "orm:ReadingOrder"
	 * {@link Element}, the {@link RoleSequence} corresponding to this reading
	 * can be found. This method always selects the first "orm:ReadingOrder"
	 * {@link Element} which corresponds to the reading of the fact.
	 * 
	 * @param factElement
	 *            - the "orm:Fact" or "orm:ImpliedFact" {@link Element} whose
	 *            name implies the reading direction, i.e. the
	 *            {@link RoleSequence}.
	 * @return the "orm:RoleSequence" {@link Element} corresponding to the
	 *         reading of the input fact
	 */
	private Element getOrderedRoleSequenceElement(Element factElement) {
		assert factElement.get_name().equals("orm:Fact") || factElement.get_name().equals("orm:ImpliedFact");
		Element readingOrdersElement = xu.firstChildWithName(factElement, "orm:ReadingOrders");
		Element readingOrderElement = xu.firstChildWithName(readingOrdersElement, "orm:ReadingOrder");

		Element roleSequenceElement = xu.firstChildWithName(readingOrderElement, "orm:RoleSequence");

		return roleSequenceElement;
	}

	/**
	 * A method to create a {@link Constraint} from an ORM schema
	 * {@link Element}
	 *
	 * @param constraintElement
	 *            - an {@link Element} representing a constraint in an ORM
	 *            schema
	 * @throws XMLStreamException
	 *             if the kind of constraint is unknown
	 * @return the {@link Constraint} corresponding to the input
	 *         constraintElement {@link Element} from the ORM schema
	 */
	private Constraint createConstraint(Element constraintElement) throws XMLStreamException {

		String constraintKind = constraintElement.get_name();
		Constraint constraint = null;

		switch (constraintKind) {

		case "orm:MandatoryConstraint":
			if (isExclusiveOrConstraint(constraintElement)) {
				constraint = ormGraph.createSetComparisonConstraint();
				((SetComparisonConstraint) constraint).set_type(SetComparisonConstraintKind.EXCLUSIVEOR);
			} else if (isInclusiveOrConstraint(constraintElement)) {
				constraint = ormGraph.createSetComparisonConstraint();
				((SetComparisonConstraint) constraint).set_type(SetComparisonConstraintKind.INCLUSIVEOR);
			} else {
				constraint = ormGraph.createMandatoryConstraint();
				if (xu.hasAttribute(constraintElement, "IsImplied")
						&& xu.getAttributeValue(constraintElement, "IsImplied").equals("true")) {
					((MandatoryConstraint) constraint).set_isImplied(true);
				}
			}
			break;

		case "orm:UniquenessConstraint":
			constraint = ormGraph.createUniquenessConstraint();
			break;

		case "orm:EqualityConstraint":
			constraint = ormGraph.createSetComparisonConstraint();
			((SetComparisonConstraint) constraint).set_type(SetComparisonConstraintKind.EQUALITY);
			break;

		case "orm:RingConstraint":
			constraint = ormGraph.createRingConstraint();
			((RingConstraint) constraint).set_type(getRingConstraintKind(constraintElement));
			break;

		case "orm:SubsetConstraint":
			constraint = ormGraph.createSetComparisonConstraint();
			((SetComparisonConstraint) constraint).set_type(SetComparisonConstraintKind.SUBSET);
			break;

		case "orm:ValueComparisonConstraint":
			constraint = ormGraph.createValueComparisonConstraint();
			((ValueComparisonConstraint) constraint).set_operator(getValueComparisonOperator(constraintElement));
			break;

		case "orm:ExclusionConstraint":
			if (!isExclusiveOrConstraint(constraintElement)) {

				constraint = ormGraph.createSetComparisonConstraint();
				((SetComparisonConstraint) constraint).set_type(SetComparisonConstraintKind.EXCLUSION);
			}
			break;

		case "orm:FrequencyConstraint":
			constraint = ormGraph.createFrequencyConstraint();
			((FrequencyConstraint) constraint).add_frequencyRange(createRange(constraintElement));
			break;

		case "orm:UnaryRoleCardinalityConstraint":
		case "orm:CardinalityConstraint":
			constraint = ormGraph.createCardinalityConstraint();

			for (Element range : xu.childrenWithName(xu.firstChildWithName(constraintElement, "orm:Ranges"),
					"orm:CardinalityRange")) {
				((CardinalityConstraint) constraint).add_cardinalityRange(createRange(range));
			}
			break;

		case "orm:ValueConstraint":
		case "orm:RoleValueConstraint":
			constraint = ormGraph.createValueConstraint();

			for (Element valueRangeElement : xu
					.childrenWithName(xu.firstChildWithName(constraintElement, "orm:ValueRanges"), "orm:ValueRange")) {
				ValueRange valueRange = createValueRange(valueRangeElement);
				((ValueConstraint) constraint).add_valueRange(valueRange);
			}
			break;

		default:
			throw new XMLStreamException(
					"The constraint " + constraintKind + " is unknown and cannot be mapped to a grUML schema.");
		}

		if (constraint != null) {
			if (constraint instanceof RoleSequenceConstraint) {
				for (RoleSequence roleSequence : createRoleSequences(constraintElement)) {
					// link the RoleSequenceConstraints to their Roles via
					// RoleSequences
					((RoleSequenceConstraint) constraint).add_roleSequence(roleSequence);
					addRoleSequenceConstraintToFact((RoleSequenceConstraint) constraint);
					if (hasJoinRule(constraintElement)) {
						for (Element joinRuleElement : getJoinRuleElements(constraintElement)) {
							roleSequence.add_joinRule(createJoinRule(joinRuleElement));
						}

					}
				}

			}
			constraint.set_xmlId(xu.getAttributeValue(constraintElement, "id"));
			constraint.set_isInterpredicateConstraint(util.isInterpredicateConstraint(constraint));
		}

		return constraint;
	}

	private JoinRule createJoinRule(Element joinRuleElement) {
		JoinRule joinRule = ormGraph.createJoinRule();

		for (Element joinPathElement : xu.childrenWithName(joinRuleElement, "orm:JoinPath")) {
			joinRule.add_joinPath(createJoinPath(joinPathElement));
		}
		return joinRule;
	}

	/**
	 * A method that creates a {@link JoinPath} from an "orm:JoinPath"
	 * {@link Element} in the ORM schema.
	 * 
	 * @param joinPathElement
	 *            - the "orm:JoinPath" {@link Element}
	 * @return the {@link JoinPath} generated from the information contained in
	 *         the input "orm:JoinPath" {@link Element}
	 */
	private JoinPath createJoinPath(Element joinPathElement) {
		JoinPath joinPath = ormGraph.createJoinPath();
		joinPath.set_xmlId(xu.getAttributeValue(joinPathElement, "id"));

		ArrayList<Element> listOfRolePathElements = new ArrayList<>();
		xu.childrenWithName(xu.firstChildWithName(joinPathElement, "orm:PathComponents"), "orm:RolePath")
				.forEach(element -> listOfRolePathElements.add(element));

		for (Element rolePathElement : listOfRolePathElements) {
			joinPath.add_containedRolePath(createRolePath(rolePathElement));
		}
		ArrayList<Element> listOfJoinPathProjectionElements = new ArrayList<>();
		xu.childrenWithName(xu.firstChildWithName(joinPathElement, "orm:JoinPathProjections"), "orm:JoinPathProjection")
				.forEach(element -> listOfJoinPathProjectionElements.add(element));
		for (Element joinPathProjectionElement : listOfJoinPathProjectionElements) {
			joinPath.add_joinPathProjection(createJoinPathProjection(joinPathProjectionElement));
		}

		return joinPath;
	}

	/**
	 * A method that creates a {@link RolePath} from an "orm:RolePath"
	 * {@link Element} in the ORM schema.
	 * 
	 * @param joinPathElement
	 *            - the "orm:RolePath" {@link Element}
	 * @return the {@link RolePath} generated from the information contained in
	 *         the input "orm:RolePath" {@link Element}
	 */
	private RolePath createRolePath(Element rolePathElement) {
		RolePath rolePath = ormGraph.createRolePath();
		// set xmlId attribute
		rolePath.set_xmlId(xu.getAttributeValue(rolePathElement, "id"));
		// set operator attribute
		if (xu.hasAttribute(rolePathElement, "SplitCombinationOperator")) {
			if (xu.getAttributeValue(rolePathElement, "SplitCombinationOperator").equals("And")) {
				rolePath.set_operator(CombinationKind.AND);
			} else if (xu.getAttributeValue(rolePathElement, "SplitCombinationOperator").equals("Or")) {
				rolePath.set_operator(CombinationKind.INCLUSIVEOR);
			} else if (xu.getAttributeValue(rolePathElement, "SplitCombinationOperator").equals("Xor")) {
				rolePath.set_operator(CombinationKind.EXCLUSIVEOR);
			}
		} else {
			rolePath.set_operator(CombinationKind.NONE);
		}
		// create the association with the role path's root object type
		Element rootObjectType = xu.firstChildWithName(rolePathElement, "orm:RootObjectType");
		rolePath.add_rootObjectType(getObjectTypeFromXmlId(xu.getAttributeValue(rootObjectType, "ref")));

		if (xu.firstChildWithName(rolePathElement, "orm:PathedRoles") != null) {
			for (Element pathedRoleElement : xu
					.childrenWithName(xu.firstChildWithName(rolePathElement, "orm:PathedRoles"), "orm:PathedRole")) {
				rolePath.add_pathedRole(createPathedRole(pathedRoleElement));
			}
		} else if (xu.firstChildWithName(rolePathElement, "orm:SubPaths") != null) {
			for (Element subPathElement : xu.childrenWithName(xu.firstChildWithName(rolePathElement, "orm:SubPaths"),
					"orm:SubPath")) {
				rolePath.add_subPath(createSubPath(subPathElement));
			}

		}

		return rolePath;
	}

	/**
	 * A method that creates a {@link SubPath} from an "orm:SubPath"
	 * {@link Element} in the ORM schema.
	 * 
	 * @param joinPathElement
	 *            - the "orm:SubPath" {@link Element}
	 * @return the {@link SubPath} generated from the information contained in
	 *         the input "orm:SubPath" {@link Element}
	 */
	private SubPath createSubPath(Element subPathElement) {
		SubPath subPath = ormGraph.createSubPath();
		// set attribute xmlId
		subPath.set_xmlId(xu.getAttributeValue(subPathElement, "id"));
		// create the PathedRoles SubPath is associated with
		ArrayList<Element> listOfPathedRoleElements = new ArrayList<>();
		xu.childrenWithName(xu.firstChildWithName(subPathElement, "orm:PathedRoles"), "orm:PathedRole")
				.forEach(element -> listOfPathedRoleElements.add(element));
		;

		for (Element pathedRoleElement : listOfPathedRoleElements) {
			subPath.add_subPathPathedRole(createPathedRole(pathedRoleElement));
		}
		return subPath;
	}

	/**
	 * A method that creates a {@link PathedRole} from an "orm:PathedRole"
	 * {@link Element} in the ORM schema.
	 * 
	 * @param joinPathElement
	 *            - the "orm:PathedRole" {@link Element}
	 * @return the {@link PathedRole} generated from the information contained
	 *         in the input "orm:PathedRole" {@link Element}
	 */
	private PathedRole createPathedRole(Element pathedRoleElement) {

		PathedRole pathedRole = ormGraph.createPathedRole();
		// set attribute xmlId
		pathedRole.set_xmlId(xu.getAttributeValue(pathedRoleElement, "id"));
		// add association to the referenced role
		Role referencedRole = (Role) getRoleKindFromXmlId(xu.getAttributeValue(pathedRoleElement, "ref"));
		pathedRole.add_referencedRole(referencedRole);
		// set the type
		if (xu.getAttributeValue(pathedRoleElement, "Purpose").equals("PostInnerJoin")) {
			pathedRole.set_type(PathedRoleKind.POSTINNERJOIN);
		} else if (xu.getAttributeValue(pathedRoleElement, "Purpose").equals("SameFactType")) {
			pathedRole.set_type(PathedRoleKind.SAMEFACTTYPE);
		}

		return pathedRole;
	}

	/**
	 * A method that creates a {@link JoinPathProjection} from an
	 * "orm:JoinPathProjection" {@link Element} in the ORM schema.
	 * 
	 * @param joinPathElement
	 *            - the "orm:JoinPathProjection" {@link Element}
	 * @return the {@link JoinPathProjection} generated from the information
	 *         contained in the input "orm:JoinPathProjection" {@link Element}
	 */
	private JoinPathProjection createJoinPathProjection(Element joinPathProjectionElement) {
		JoinPathProjection joinPathProjection = ormGraph.createJoinPathProjection();
		// set attribute xmlId
		joinPathProjection.set_xmlId(xu.getAttributeValue(joinPathProjectionElement, "id"));
		// add association with the referenced RolePath
		joinPathProjection
				.add_referencedRolePath(getRolePathFromXmlId(xu.getAttributeValue(joinPathProjectionElement, "ref")));
		// add ConstraintRoleProjections
		for (Element constraintRoleProjectionElement : xu.childrenWithName(joinPathProjectionElement,
				"orm:ConstraintRoleProjection")) {
			joinPathProjection
					.add_constraintRoleProjection(createConstraintRoleProjection(constraintRoleProjectionElement));
		}

		return joinPathProjection;
	}

	/**
	 * A method to generate a {@link ConstraintRoleProjection} from the
	 * "orm:ConstraintRoleProjection" {@link Element} in the ORM schema
	 * 
	 * @param constraintRoleProjectionElement
	 *            - the "orm:ConstraintRoleProjection" {@link Element} from the
	 *            ORM schema
	 * @return the {@link ConstraintRoleProjection} created from the information
	 *         in the ORM schema
	 */
	private ConstraintRoleProjection createConstraintRoleProjection(Element constraintRoleProjectionElement) {
		ConstraintRoleProjection constraintRoleProjection = ormGraph.createConstraintRoleProjection();
		// add attribute xmlId
		constraintRoleProjection.set_xmlId(xu.getAttributeValue(constraintRoleProjectionElement, "id"));
		// check what the projection source is: a role or an object type
		if (xu.firstChildWithName(xu.firstChildWithName(constraintRoleProjectionElement, "orm:ProjectedFrom"),
				"orm:PathedRole") != null) {
			// add association with the PathedRole which is the projection
			// source
			Element pathedRoleElement = xu.firstChildWithName(
					xu.firstChildWithName(constraintRoleProjectionElement, "orm:ProjectedFrom"), "orm:PathedRole");
			assert getNumberOfChildrenWithName(
					xu.firstChildWithName(constraintRoleProjectionElement, "orm:ProjectedFrom"), "orm:PathedRole") == 1;

			constraintRoleProjection.add_projectionSourcePathedRole(
					getPathedRoleFromXmlId(xu.getAttributeValue(pathedRoleElement, "ref")));
		} else if (xu.firstChildWithName(xu.firstChildWithName(constraintRoleProjectionElement, "orm:ProjectedFrom"),
				"orm:PathRoot") != null) {
			// add association with the ObjectType which is the projection
			// source
			Element pathedRoleElement = xu.firstChildWithName(
					xu.firstChildWithName(constraintRoleProjectionElement, "orm:ProjectedFrom"), "orm:PathRoot");
			assert getNumberOfChildrenWithName(
					xu.firstChildWithName(constraintRoleProjectionElement, "orm:ProjectedFrom"), "orm:PathRoot") == 1;

			constraintRoleProjection.add_projectionSourceObjectType(getObjectTypeFromXmlId(
					xu.getAttributeValue(xu.getReferencedElement(pathedRoleElement, "ref"), "ref")));
		}

		// add association with Roles that are the target of the projection
		constraintRoleProjection.add_projectionTarget((Role) getRoleKindFromXmlId(
				xu.getAttributeValue(xu.getReferencedElement(constraintRoleProjectionElement, "ref"), "ref")));
		return constraintRoleProjection;
	}

	/**
	 * A method to get a {@link PathedRole} given its XML-ID in the ORM schema
	 * file.
	 * 
	 * @param xmlId
	 *            - the ID of the {@link PathedRole} which should be returned
	 * @return the {@link PathedRole} if it can be found. Null if not.
	 */
	private PathedRole getPathedRoleFromXmlId(String xmlId) {
		for (PathedRole pathedRole : ormGraph.getPathedRoleVertices()) {
			if (pathedRole.get_xmlId().equals(xmlId)) {
				return pathedRole;
			}
		}
		return null;
	}

	/**
	 * A method that counts the number of children of a specific name an
	 * {@link Element} has
	 * 
	 * @param parentElement
	 *            - the {@link Element} whose children with a specific name are
	 *            counted
	 * @param childElementName
	 *            - the name of the {@link Element} that should be counted
	 * @return the number of children of the input {@link Element} with the
	 *         input name
	 */
	private int getNumberOfChildrenWithName(Element parentElement, String childElementName) {
		int count = 0;
		Iterator<Element> iterator = xu.childrenWithName(parentElement, childElementName).iterator();
		while (iterator.hasNext()) {
			iterator.next();
			++count;
		}
		return count;
	}

	/**
	 * A method to get a {@link RolePath} given its XML-ID in the ORM schema
	 * file.
	 * 
	 * @param xmlId
	 *            - the ID of the {@link RolePath} which should be returned
	 * @return the {@link RolePath} if it can be found. Null if not.
	 */
	private RolePath getRolePathFromXmlId(String xmlId) {
		for (RolePath rolePath : ormGraph.getRolePathVertices()) {
			if (rolePath.get_xmlId().equals(xmlId)) {
				return rolePath;
			}
		}
		return null;
	}

	/**
	 * A method that checks whether an ORM constraint {@link Element} contains
	 * an "orm:JoinRule" {@link Element}.
	 * 
	 * @param constraintElement
	 *            - the {@link Element} that may or may not contain an
	 *            "orm:JoinRule" {@link Element}
	 * @return true, if the input Element contains an "orm:JoinRule" Element and
	 *         false if not.
	 */
	private boolean hasJoinRule(Element constraintElement) {
		if (xu.firstChildWithName(constraintElement, "orm:RoleSequences") == null) {
			for (Element roleSequenceElement : xu.childrenWithName(constraintElement, "orm:RoleSequence")) {
				Iterator<Element> iterator = xu.childrenWithName(roleSequenceElement, "orm:JoinRule").iterator();
				if (iterator.hasNext()) {
					return true;
				}
			}

		} else {
			for (Element roleSequenceElement : xu.childrenWithName(
					xu.firstChildWithName(constraintElement, "orm:RoleSequences"), "orm:RoleSequence")) {
				Iterator<Element> iterator = xu.childrenWithName(roleSequenceElement, "orm:JoinRule").iterator();
				if (iterator.hasNext()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * A method that returns the "orm:JoinRule" {@link Element} contained in an
	 * ORM constraint {@link Element}.
	 * 
	 * @param constraintElement
	 *            - the {@link Element} that contains an "orm:JoinRule"
	 *            {@link Element}
	 * @return the "orm:JoinRule" {@link Element} contained in the input Element
	 */
	private ArrayList<Element> getJoinRuleElements(Element constraintElement) {
		ArrayList<Element> listOfElements = new ArrayList<>();
		if (hasMultipleRoleSequences(constraintElement)) {
			for (Element childElement : xu.childrenWithName(
					xu.firstChildWithName(constraintElement, "orm:RoleSequences"), "orm:RoleSequence")) {
				xu.childrenWithName(childElement, "orm:JoinRule").forEach(element -> listOfElements.add(element));
			}
		} else {
			for (Element childElement : xu.childrenWithName(constraintElement, "orm:RoleSequence")) {
				xu.childrenWithName(childElement, "orm:JoinRule").forEach(element -> listOfElements.add(element));
			}
		}
		return listOfElements;
	}

	/**
	 * A method that checks whether the input {@link Element} represents an
	 * inclusive-or constraint. The ORM schema doesn't contain a specific
	 * inclusive-or constraint element. Inclusive-or constraints are represented
	 * as {@link MandatoryConstraint} with multiple {@link Role} in a single
	 * {@link RoleSequence}. If the inclusive-or constraint is set explicitly,
	 * the {@link MandatoryConstraint} is named "InclusiveOrConstraint".
	 * 
	 * @param constraintElement
	 *            - the {@link Element} which may or may not represent an
	 *            inclusive-or constraint
	 * @return true if the input {@link Element} represents an inclusive-or
	 *         constraint. False if not.
	 */
	private boolean isInclusiveOrConstraint(Element constraintElement) {
		// explicit inclusive or constraint
		if (xu.getAttributeValue(constraintElement, "Name").startsWith("InclusiveOrConstraint")) {
			return true;
		}
		// implicit exclusive or constraint
		int counter = 0;
		Iterator<Element> iterator = xu
				.childrenWithName(xu.firstChildWithName(constraintElement, "orm:RoleSequence"), "orm:Role").iterator();
		while (iterator.hasNext()) {
			iterator.next();
			++counter;
		}

		return counter > 1 ? true : false;
	}

	/**
	 * This method creates an edge between the input
	 * {@link RoleSequenceConstraint} and the {@link Fact}(s) it belongs to
	 * 
	 * @param constraint
	 *            - the constraint that should be linked to the fact(s) it
	 *            belongs to.
	 */
	private void addRoleSequenceConstraintToFact(RoleSequenceConstraint constraint) {

		Fact fact = null;
		boolean constraintAlreadyAdded = false;

		for (RoleSequence rs : constraint.get_roleSequence()) {
			for (Role role : rs.get_role()) {
				fact = role.get_fact();

				for (Constraint fetchedConstraint : fact.get_constraint()) {
					if (fetchedConstraint == constraint) {
						constraintAlreadyAdded = true;
						break;
					}
				}
			}
			if (!constraintAlreadyAdded) {
				fact.add_constraint(constraint);
			}
		}

	}

	/**
	 * A method to create a {@link Range} object.
	 *
	 * @param element
	 *            - an "orm:CardinalityRange" or "orm:FrequencyConstraint"
	 *            {@link Element} from an ORM schema file
	 * @return the generated {@link Range} object. If the upper or lower bound
	 *         are not defined in the ORM schema, the value is set to -1.
	 */
	private Range createRange(Element element) {
		Range fromTo = ormGraph.createRange();
		String lowerBoundName;
		String upperBoundName;
		if (element.get_name().equals("orm:CardinalityRange")) {

			lowerBoundName = "From";
			upperBoundName = "To";
		} else {
			assert element.get_name().equals("orm:FrequencyConstraint");
			lowerBoundName = "MinFrequency";
			upperBoundName = "MaxFrequency";
		}
		if (xu.hasAttribute(element, lowerBoundName)) {
			fromTo.set_minValue(Integer.parseInt(xu.getAttributeValue(element, lowerBoundName)));
		} else {
			fromTo.set_minValue(-1);
		}

		if (xu.hasAttribute(element, upperBoundName)) {
			fromTo.set_maxValue(Integer.parseInt(xu.getAttributeValue(element, upperBoundName)));
		} else {
			fromTo.set_maxValue(-1);
		}
		return fromTo;
	}

	/**
	 * A method to detect the operator of the value comparison constraint
	 * represented by the input {@link Element}
	 *
	 * @param valueComparisonConstraintElement
	 *            - the {@link Element} of an ORM schema representing a value
	 *            comparison constraint
	 * @return the {@link ValueComparisonKind} of the input {@link Element}
	 * @throws XMLStreamException
	 *             if the {@link Element} has value comparison operator not
	 *             listed in {@link ValueComparisonKind}
	 */
	private ValueComparisonKind getValueComparisonOperator(Element valueComparisonConstraintElement)
			throws XMLStreamException {
		String valueComaprisonOperator = xu.getAttributeValue(valueComparisonConstraintElement, "Operator");

		switch (valueComaprisonOperator) {
		case ("Equal"):
			return ValueComparisonKind.EQUAL;
		case ("GreaterThan"):
			return ValueComparisonKind.GREATER;
		case ("GreaterThanOrEqual"):
			return ValueComparisonKind.GREATEREQUAL;
		case ("LessThan"):
			return ValueComparisonKind.LESS;
		case ("LessThanOrEqual"):
			return ValueComparisonKind.LESSEQUAL;
		case ("NotEqual"):
			return ValueComparisonKind.NOTEQUAL;

		default:
			throw new XMLStreamException("The value comparison operator " + valueComaprisonOperator
					+ " is unknown and cannot be mapped to a grUML schema.");

		}
	}

	/**
	 * A method to detect the type of the ring constraint represented by the
	 * input {@link Element}
	 *
	 * @param ringConstraintElement
	 *            - the {@link Element} of an ORM schema representing a ring
	 *            constraint
	 * @return the {@link RingConstraintKind} of the input {@link Element}
	 * @throws XMLStreamException
	 *             if the {@link Element} has a kind of ring constraint not
	 *             listed in {@link RingConstraintKind}
	 */
	private RingConstraintKind getRingConstraintKind(Element ringConstraintElement) throws XMLStreamException {
		String ringConstraintType = xu.getAttributeValue(ringConstraintElement, "Type");

		switch (ringConstraintType) {
		case ("Acyclic"):
			return RingConstraintKind.ACYCLIC;
		case ("Antisymmetric"):
			return RingConstraintKind.ANTISYMMETRIC;
		case ("Asymmetric"):
			return RingConstraintKind.ASYMMETRIC;
		case ("Intransitive"):
			return RingConstraintKind.INTRANSITIVE;
		case ("Irreflexive"):
			return RingConstraintKind.IRREFLEXIVE;
		case ("PurelyReflexive"):
			return RingConstraintKind.PURELYREFLEXIVE;
		case ("Reflexive"):
			return RingConstraintKind.REFLEXIVE;
		case ("StronglyIntransitive"):
			return RingConstraintKind.STRONGLYINTRANSITIVE;
		case ("Symmetric"):
			return RingConstraintKind.SYMMETRIC;
		case ("Transitive"):
			return RingConstraintKind.TRANSITIVE;
		default:
			throw new XMLStreamException("The ring constraint type " + ringConstraintType
					+ " is unknown and cannot be mapped to a grUML schema.");

		}
	}

	/**
	 * A method that generates a list of {@link RoleSequence}s from an
	 * {@link Element} representing a constraint in an ORM schema. Most
	 * constraints in an ORM schema apply to at least one RoleSequence. The
	 * exceptions are ValueConstraints and CardinalityConstraints - they apply
	 * to ValueTypes/EntityTypes/ObjetifiedTypes directly or to a single Role.
	 *
	 * @param constraintElement
	 *            - the {@link Element} representing a constraint
	 * @return the {@code ArrayList} of {@link RoleSequence}s for the input
	 * 
	 *         {@link Element}
	 */
	private ArrayList<? extends RoleSequence> createRoleSequences(Element constraintElement) {

		ArrayList<RoleSequence> roleSequences = new ArrayList<>();

		if (xu.firstChildWithName(constraintElement, "orm:RoleSequences") != null) {
			for (Element roleSequenceElement : xu.childrenWithName(
					xu.firstChildWithName(constraintElement, "orm:RoleSequences"), "orm:RoleSequence")) {
				roleSequences.add(createRoleSequence(roleSequenceElement));
			}
		} else {
			// handle constraints that don't contain "orm:RoleSequences"
			roleSequences.add(createRoleSequence(xu.firstChildWithName(constraintElement, "orm:RoleSequence")));
		}
		return roleSequences;
	}

	/**
	 * A method to create {@link RoleSequence} objects from "orm:RoleSequence"
	 * {@link Element}s contained in an ORM constraint definition in an ORM
	 * schema.
	 *
	 * @param roleSequenceElement
	 *            - the "orm:RoleSequence" {@link Element} from within a
	 *            constraint definition
	 * @return the {@link RoleSequence} generated from the information within
	 *         the input {@link Element} from the ORM schema
	 */
	private RoleSequence createRoleSequence(Element roleSequenceElement) {

		RoleSequence roleSequence = ormGraph.createRoleSequence();
		for (Element roleElement : xu.childrenWithName(roleSequenceElement, "orm:Role")) {
			roleSequence.add_role((Role) getRoleKindFromXmlId(xu.getAttributeValue(roleElement, "ref")));
		}
		return roleSequence;
	}

	/**
	 * method to check whether a supertype is defined for the input
	 * {@link Element} in the ORM schema
	 *
	 * @param element
	 *            - the schema {@link Element} that may or may not have a
	 *            supertype
	 * @return - true, if the {@link Element} hat a supertype, false if not.
	 */
	private boolean hasSupertype(Element element) {
		if (xu.firstChildWithName(element, "orm:PlayedRoles") == null) {
			return false;
		}
		Element playedRoles = xu.firstChildWithName(element, "orm:PlayedRoles");
		if (xu.firstChildWithName(playedRoles, "orm:SubtypeMetaRole") != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * method to check whether a subtype is defined for the input
	 * {@link Element} in the ORM schema
	 *
	 * @param element
	 *            - the schema {@link Element} that may or may not have a
	 *            subtype
	 * @return - true, if the {@link Element} hat a subtype, false if not.
	 */
	private boolean hasSubtype(Element element) {
		if (xu.firstChildWithName(element, "orm:PlayedRoles") == null) {
			return false;
		}
		Element playedRoles = xu.firstChildWithName(element, "orm:PlayedRoles");
		if (xu.firstChildWithName(playedRoles, "orm:SupertypeMetaRole") != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * a method to check whether a "orm:MandatoryConstraint" or a
	 * "orm:ExclusionConstraint" {@link Element} is an exclusive-or constraint
	 * or not
	 *
	 * @param constraintElement
	 *            - the {@link Element} from the ORM schema that may or may
	 *            represent an exclusive-or constraint
	 * @return true, if the input {@link Element} represents an exclusive-or
	 *         constraint and false if not
	 */
	private boolean isExclusiveOrConstraint(Element constraintElement) {
		if (xu.firstChildWithName(constraintElement, "orm:ExclusiveOrExclusionConstraint") != null
				|| xu.firstChildWithName(constraintElement, "orm:ExclusiveOrMandatoryConstraint") != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * A method to store the generated TGraph.
	 *
	 * @param filename
	 *            the path to the storage location.
	 */
	private void saveGraph(String filename) {
		System.out.println("Saving graph to " + filename);
		try {
			ormGraph.save(filename, new ConsoleProgressFunction());
		} catch (GraphIOException e) {
			System.err.println("An Exception occured while saving graph to " + filename + ".");
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private void printObjectifiedType() {
		for (ObjectifiedType ot : ormGraph.getObjectifiedTypeVertices()) {
			String name = ot.get_name();
			System.out.println("-------Objectified Type------");
			System.out.println("name = " + name + "\ndataType = ");
			if (ot.get_objectTypeCardinalityConstraint() != null) {
				System.out.println("CardinalityConstraint");
				for (Range range : ot.get_objectTypeCardinalityConstraint().get_cardinalityRange()) {
					System.out.println("min cardinality: " + range.get_minValue());
					System.out.println("max cardinality: " + range.get_maxValue());
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private void printEntityTypes() {
		for (EntityType et : ormGraph.getEntityTypeVertices()) {
			String name = et.get_name();
			System.out.println("-------Entity Type------");
			System.out.println("name = " + name);
			for (ValueType vt : et.get_preferredIdentifierValueType()) {
				System.out.println("Preferred Identifier: " + vt.get_name());
			}
			if (et.get_objectTypeCardinalityConstraint() != null) {
				System.out.println("CardinalityConstraint");
				for (Range range : et.get_objectTypeCardinalityConstraint().get_cardinalityRange()) {
					System.out.println("min cardinality: " + range.get_minValue());
					System.out.println("max cardinality: " + range.get_maxValue());
				}
			}

		}
	}

	@SuppressWarnings("unused")
	private void printValueTypes() {
		for (ValueType vt : ormGraph.getValueTypeVertices()) {

			String name = vt.get_name();
			String dataType = vt.get_conceptualDataType().get_dataType().name();
			System.out.println("-------Value Type------");
			System.out.println("name = " + name + "\ndataType = " + dataType);

			if (vt.get_valueTypeValueConstraint() != null) {
				int i = 1;
				for (ValueRange vr : vt.get_valueTypeValueConstraint().get_valueRange()) {
					String minValue = vr.get_minValue();
					String invariantMinValue = vr.get_invariantMinValue();
					String maxValue = vr.get_maxValue();
					String invariantMaxValue = vr.get_invariantMaxValue();
					boolean minInclusion = vr.is_maxInclusion();
					boolean maxInclusion = vr.is_minInclusion();
					System.out.println("ValueRange " + i + "\nminValue = " + minValue + "\ninvariantMinValue = "
							+ invariantMinValue + "\nminInclusion = " + minInclusion + "\nmaxValue = " + maxValue
							+ "\ninvariantMaxValue = " + invariantMaxValue + "\nmaxInclusion = " + maxInclusion);
					++i;
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private void printFacts() {
		for (Fact fact : ormGraph.getFactVertices()) {
			String name = fact.get_name();
			System.out.println("-------Fact------");
			System.out.println("name = " + name);
			System.out.println("\n - ROLES -");

			for (RoleKind role : fact.get_factRole()) {
				if (role instanceof Role) {
					String playerType;
					if (((Role) role).get_player() instanceof ValueType) {
						playerType = "ValueType";
					} else if (((Role) role).get_player() instanceof EntityType) {
						playerType = "EntityType";
					} else {
						playerType = "ObjectifiedType";
					}
					System.out.println("\nRole " + ((Role) role).get_name() + " played by: "
							+ ((Role) role).get_player().get_name() + " (" + playerType + ")");

					if (playerType.equals("EntityType") || playerType.equals("ObjectifiedType")) {
						if (((EntityObject) ((Role) role).get_player()).get_objectTypeCardinalityConstraint() != null) {
							System.out.println("- " + playerType + " constraints - ");
							StringBuilder ranges = new StringBuilder();
							for (Range range : ((EntityObject) ((Role) role).get_player())
									.get_objectTypeCardinalityConstraint().get_cardinalityRange()) {
								if (ranges.length() == 0) {
									ranges.append("[" + range.get_minValue() + ", " + range.get_maxValue() + "]");
								} else {
									ranges.append(", [" + range.get_minValue() + ", " + range.get_maxValue() + "]");
								}

								System.out.println("+ CardinalityConstraint (" + ranges.toString() + ")");
							}
						}

					}
					if (playerType.equals("ValueType")) {

						if (((ValueType) ((Role) role).get_player()).get_valueTypeValueConstraint() != null) {
							System.out.println("- " + playerType + "constraints - ");
							StringBuilder ranges = new StringBuilder();
							for (ValueRange range : ((ValueType) ((Role) role).get_player())
									.get_valueTypeValueConstraint().get_valueRange()) {
								if (ranges.length() == 0) {
									ranges.append("{" + range.get_minValue() + ", " + range.get_maxValue() + "}");
								} else {
									ranges.append(", {" + range.get_minValue() + ", " + range.get_maxValue() + "}");
								}
							}
							System.out.println("+ ValueConstraint (" + ranges.toString() + ")");
						}
					}
					System.out.println("- role constraints - ");
					for (RoleSequence rs : ((Role) role).get_roleSequenceContainer()) {

						Constraint constraint = rs.get_associatedConstraint();

						if (constraint instanceof SetComparisonConstraint) {
							System.out.println("+ SetComparisonConstraint ( "
									+ ((SetComparisonConstraint) constraint).get_type().name() + ")");
						} else if (constraint instanceof ValueComparisonConstraint) {
							System.out.println("+ ValueComparisonConstraint ( "
									+ ((ValueComparisonConstraint) constraint).get_operator().name() + ")");
						} else if (constraint instanceof MandatoryConstraint) {
							String type = ((MandatoryConstraint) constraint).is_isInterpredicateConstraint()
									? "interpredicate" : "internal";
							System.out.println("+ MandatoryConstraint (" + type + ")");
						} else if (constraint instanceof UniquenessConstraint) {
							String type = ((UniquenessConstraint) constraint).is_isInterpredicateConstraint()
									? "interpredicate" : "internal";
							System.out.println("+ UniquenessConstraint (" + type + ")");
						} else if (constraint instanceof RingConstraint) {
							String type = ((RingConstraint) constraint).get_type().name();
							System.out.println("+ RingConstraint (" + type + ")");
						} else if (constraint instanceof FrequencyConstraint) {
							String type = ((FrequencyConstraint) constraint).is_isInterpredicateConstraint()
									? "interpredicate" : "internal";
							int lowerBound = ((FrequencyConstraint) constraint).get_frequencyRange().get_minValue();
							int upperBound = ((FrequencyConstraint) constraint).get_frequencyRange().get_maxValue();

							System.out.println("+ FrequencyConstraint (from " + lowerBound + " to " + upperBound + "; "
									+ type + ")");
						}

					}
					System.out.println("- role multiplicity - ");

					if (((Role) role).get_roleCardinalityConstraint() != null) {
						StringBuilder ranges = new StringBuilder();
						for (Range range : ((Role) role).get_roleCardinalityConstraint().get_cardinalityRange()) {
							if (ranges.length() == 0) {
								ranges.append("[" + range.get_minValue() + ", " + range.get_maxValue() + "]");
							} else {
								ranges.append(", [" + range.get_minValue() + ", " + range.get_maxValue() + "]");
							}
						}
						System.out.println("+ CardinalityConstraint (" + ranges.toString() + ")");
					}
					if (((Role) role).get_roleValueConstraint() != null) {

						if (((Role) role).get_roleValueConstraint() != null) {
							StringBuilder ranges = new StringBuilder();
							for (ValueRange range : ((Role) role).get_roleValueConstraint().get_valueRange()) {
								if (ranges.length() == 0) {
									ranges.append("{" + range.get_minValue() + ", " + range.get_maxValue() + "}");
								} else {
									ranges.append(", {" + range.get_minValue() + ", " + range.get_maxValue() + "}");
								}
							}
							System.out.println("+ ValueConstraint (" + ranges.toString() + ")");
						}
					}
				} else {
					System.out.println("Role Proxy of role played by "
							+ ((RoleProxy) role).get_linkedRole().get_player().get_name() + " in fact type "
							+ ((RoleProxy) role).get_linkedRole().get_fact().get_name());
				}
			}
			System.out.println();
		}
	}

	/**
	 * @return the ormGraph
	 */
	public ORMGraph getOrmGraph() {
		return ormGraph;
	}

	/**
	 * A method to retrieve an {@link ObjectType} based on its XML ID.
	 * 
	 * @param xmlId
	 *            - the XML ID identifying the {@link ObjectType}
	 * @return the {@link ObjectType} if it exists, else null.
	 */
	private ObjectType getObjectTypeFromXmlId(String xmlId) {
		for (ObjectType ot : ormGraph.getObjectTypeVertices()) {
			if (ot.get_xmlId().equals(xmlId)) {
				return ot;
			}
		}
		return null;
	}

	/**
	 * A method to retrieve an {@link Fact} based on its XML ID.
	 * 
	 * @param xmlId
	 *            - the XML ID identifying the {@link Fact}
	 * @return the {@link Fact} if it exists, else null.
	 */
	private Fact getFactFromXmlId(String xmlId) {
		for (Fact fact : ormGraph.getFactVertices()) {
			if (fact.get_xmlId().equals(xmlId)) {
				return fact;
			}
		}
		return null;
	}

	/**
	 * A method to retrieve an {@link RoleKind} based on its XML ID.
	 * 
	 * @param xmlId
	 *            - the XML ID identifying the {@link RoleKind}
	 * @return the {@link RoleKind} if it exists, else null.
	 */
	private RoleKind getRoleKindFromXmlId(String xmlId) {
		for (RoleKind role : ormGraph.getRoleKindVertices()) {
			if (role.get_xmlId().equals(xmlId)) {
				return role;
			}
		}
		return null;
	}

	/**
	 * This is a method that processes the {@link Role}s played by
	 * {@link EntityType}s, {@link ObjectifiedType}s and {@link ValueType}s. For
	 * each {@link Role} in the ORM graph it checks for
	 * {@link MandatoryConstraint}s, sets the mutliplicity values and adjusts
	 * them if a {@link Role} has an internal {@link FrequencyConstraint}.
	 */
	private void postprocessRoles() {

		for (Role role : ormGraph.getRoleVertices()) {
			// don't try computing multiplicities for ternary and higher arity
			// facts since they are also represented as binary facts between the
			// fact players and an ObjectifiedType representing the (ternary or
			// higher arity) fact
			if (util.getFactArity(role.get_fact()) > 2) {
				continue;
			}
			setMandatoryConstraints(role);
			setMultiplicities(role);
			applyInternalFrequencyConstraints(role);
		}
		for (RoleProxy roleProxy : ormGraph.getRoleProxyVertices()) {
			setMandatoryConstraints(roleProxy);
			setMultiplicities(roleProxy);
		}
	}

	/**
	 * This method checks whether the input {@link Role} has an internal
	 * {@link FrequencyConstraint} and if it does, adjusts the multiplicity
	 * values of its neighboring {@link Role} accordingly.
	 * 
	 * @param role
	 *            - the role which might have a {@link FrequencyConstraint} and
	 *            if so, has its multiplicities adjusted accordingly (stored in
	 *            the neighboring role!).
	 */
	private void applyInternalFrequencyConstraints(Role role) {
		Constraint constraint;
		RoleKind neighboringRole = util.getNeighboringRole(role);
		// if the neighboring role is a RoleProxy, it won't have a frequency
		// constraint
		if (neighboringRole instanceof RoleProxy) {
			return;
		}
		for (RoleSequence rs : ((Role) neighboringRole).get_roleSequenceContainer()) {
			constraint = rs.get_associatedConstraint();
			if (!constraint.is_isInterpredicateConstraint()) {
				if (constraint instanceof FrequencyConstraint) {
					Range range = ((FrequencyConstraint) constraint).get_frequencyRange();
					// Frequency Ranges always have a lower bound of 1 even if
					// other constraints allow a lower bound of 0. Keep the
					// lower bound of 0.
					if (range.get_minValue() > 1) {
						((Role) neighboringRole).set_multiplicityLowerBound(range.get_minValue());
					}
					if (range.get_maxValue() == 0) {
						// 0 represents the integer value for infinity in an ORM
						// schema file
						((Role) neighboringRole).set_multiplicityUpperBound(Integer.MAX_VALUE);
					} else {
						((Role) neighboringRole).set_multiplicityUpperBound(range.get_maxValue());
					}
				}
			}
		}

	}

	/**
	 * A method to set the multiplicities for the input {@link Role}. Note that
	 * the multiplicities applying to the input {@link Role} are stored in the
	 * neighboring {@link Role} (in analogy to UML multiplicities). The
	 * multiplicity values are determined based on {@link MandatoryConstraint}s
	 * and {@link UniquenessConstraint}s applying to the input {@link Role}.
	 * 
	 * @param role
	 *            - the {@link Role} whose multiplicities will be set
	 */
	private void setMultiplicities(Role role) {

		// is this role part of an objectified fact? Then don't compute
		// multiplicities! These multiplicities will be reflected in a GReQL
		// constraint.
		if (role.get_fact().get_objectifiedType() != null) {
			return;
		}
		Constraint constraint;
		boolean roleHasUC = false;
		RoleKind neighboringRole = util.getNeighboringRole(role);

		for (RoleSequence rs : role.get_roleSequenceContainer()) {
			constraint = rs.get_associatedConstraint();
			if (!constraint.is_isInterpredicateConstraint()) {
				if (constraint instanceof UniquenessConstraint) {
					roleHasUC = true;
					int size = util.getRoleSequenceSize(rs);

					if (size == 1) {
						// single UniquenessConstraint on "role"
						if (neighboringRole != null) {
							if (role.is_isMandatory()) {
								neighboringRole.set_multiplicityLowerBound(1);
								neighboringRole.set_multiplicityUpperBound(1);
							} else {
								neighboringRole.set_multiplicityLowerBound(0);
								neighboringRole.set_multiplicityUpperBound(1);
							}

						}
					} else if (size == 2) {
						// spanning UniquenessConstraint over both roles
						// ("role", "neighboringRole")
						if (neighboringRole != null) {
							if (role.is_isMandatory()) {
								neighboringRole.set_multiplicityLowerBound(1);
								neighboringRole.set_multiplicityUpperBound(Integer.MAX_VALUE);
							} else {
								neighboringRole.set_multiplicityLowerBound(0);
								neighboringRole.set_multiplicityUpperBound(Integer.MAX_VALUE);
							}

						}
					}
				}
			}
		}
		// this role doesn't have a uniqueness constraint (but
		// "neighboringRole" must by ORM definition)
		if (!roleHasUC) {
			if (role.is_isMandatory()) {
				neighboringRole.set_multiplicityLowerBound(1);
				neighboringRole.set_multiplicityUpperBound(Integer.MAX_VALUE);
			} else {
				neighboringRole.set_multiplicityLowerBound(0);
				neighboringRole.set_multiplicityUpperBound(Integer.MAX_VALUE);
			}
		}

	}

	/**
	 * A method to set the multiplicities for the input {@link RoleProxy}. Note
	 * that the multiplicities applying to the input {@link RoleProxy} are
	 * stored in the neighboring {@link Role} (in analogy to UML
	 * multiplicities). By default, {@link RoleProxy}s don't have an internal
	 * {@link UniquenessConstraint} but may or may not have a
	 * {@link MandatoryConstraint}, i.e. they either have multiplicity * or
	 * 1..*.
	 * 
	 * @param roleProxy
	 *            - the {@link RoleProxy} whose multiplicities will be set
	 */
	private void setMultiplicities(RoleProxy roleProxy) {
		RoleKind neighboringRole = util.getNeighboringRole(roleProxy);
		assert neighboringRole instanceof Role;

		if (roleProxy.is_isMandatory()) {
			neighboringRole.set_multiplicityLowerBound(1);
			neighboringRole.set_multiplicityUpperBound(Integer.MAX_VALUE);
		} else {
			neighboringRole.set_multiplicityLowerBound(0);
			neighboringRole.set_multiplicityUpperBound(Integer.MAX_VALUE);
		}

	}

	/**
	 * A method to set the 'isMandatory' attribute of {@link Role}s. It sets the
	 * attribute to true, if the {@link Role} has an internal
	 * {@link MandatoryConstraint} and to false if not.
	 * 
	 * @param role
	 *            - the {@link Role} that will have its 'isMandatory' attribute
	 *            set
	 */
	private void setMandatoryConstraints(Role role) {
		Constraint constraint;

		for (RoleSequence rs : role.get_roleSequenceContainer()) {
			constraint = rs.get_associatedConstraint();
			if (!constraint.is_isInterpredicateConstraint()) {
				if (constraint instanceof MandatoryConstraint) {
					role.set_isMandatory(true);
				}
			}
		}
	}

	/**
	 * A method to set the 'isMandatory' attribute of {@link RoleProxy}s. It
	 * sets the attribute to true, if the {@link Role} linked to the
	 * {@link RoleProxy} has an internal {@link MandatoryConstraint} and to
	 * false if not.
	 * 
	 * @param roleProxy
	 *            - the {@link RoleProxy} that will have its 'isMandatory'
	 *            attribute set
	 */
	private void setMandatoryConstraints(RoleProxy roleProxy) {
		Role linkedRole = roleProxy.get_linkedRole();
		Constraint constraint;

		for (RoleSequence rs : linkedRole.get_roleSequenceContainer()) {
			constraint = rs.get_associatedConstraint();
			if (!constraint.is_isInterpredicateConstraint()) {
				if (constraint instanceof MandatoryConstraint) {
					roleProxy.set_isMandatory(true);
				}
			}
		}

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
	private int getNumberOfRoleSequences(Element constraintElement) {
		int count = 0;
		hasMultipleRoleSequences(constraintElement);
		Iterator<Element> iterator = xu.childrenWithName(constraintElement, "orm:RoleSequence").iterator();
		while (iterator.hasNext()) {
			iterator.next();
			++count;
		}

		return count;
	}

	/**
	 * A method to check whether a given constraint {@link Element} contains
	 * multiple "orm:RoleSequence" {@link Element}s. If so, they will be
	 * children of an "orm:RoleSequences" {@link Element}.
	 * 
	 * @param constraintElement
	 *            - the constraint {@link Element} that may have multiple
	 *            "orm:RoleSequence" {@link Element}
	 * @return true, if the input {@link Element} has multiple
	 *         "orm:RoleSequence" Elements and false if not.
	 */
	private boolean hasMultipleRoleSequences(Element constraintElement) {
		if (xu.firstChildWithName(constraintElement, "orm:RoleSequences") != null) {
			return true;
		} else {
			assert xu.firstChildWithName(constraintElement, "orm:RoleSequence") != null;
			return false;
		}
	}

}