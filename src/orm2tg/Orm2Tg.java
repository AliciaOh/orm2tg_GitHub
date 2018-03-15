package orm2tg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import de.uni_koblenz.jgralab.AttributedElement;
import de.uni_koblenz.jgralab.exception.GraphIOException;
import de.uni_koblenz.jgralab.schema.AggregationKind;
import de.uni_koblenz.jgralab.schema.Attribute;
import de.uni_koblenz.jgralab.schema.Domain;
import de.uni_koblenz.jgralab.schema.EdgeClass;
import de.uni_koblenz.jgralab.schema.RecordDomain;
import de.uni_koblenz.jgralab.schema.Schema;
import de.uni_koblenz.jgralab.schema.VertexClass;
import de.uni_koblenz.jgralab.schema.impl.ConstraintImpl;
import de.uni_koblenz.jgralab.schema.impl.SchemaImpl;
import grumlconstraintgenerator.AcyclicRingConstraintGenerator;
import grumlconstraintgenerator.AntisymmetricRingConstraintGenerator;
import grumlconstraintgenerator.AsymmetricRingConstraintGenerator;
import grumlconstraintgenerator.InclusiveOrConstraintGenerator;
import grumlconstraintgenerator.IntransitiveRingConstraintGenerator;
import grumlconstraintgenerator.IrreflexiveRingConstraintGenerator;
import grumlconstraintgenerator.MandatoryConstraintGenerator;
import grumlconstraintgenerator.PurelyReflexiveRingConstraintGenerator;
import grumlconstraintgenerator.ReflexiveRingConstraintGenerator;
import grumlconstraintgenerator.SymmetricRingConstraintGenerator;
import grumlconstraintgenerator.TransitiveRingConstraintGenerator;
import grumlconstraintgenerator.UniquenessConstraintGenerator;
import grumlconstraintgenerator.ValueConstraintGenerator;
import ormschema.ORMGraph;
import ormschema.constraints.Constraint;
import ormschema.constraints.FrequencyConstraint;
import ormschema.constraints.MandatoryConstraint;
import ormschema.constraints.RingConstraint;
import ormschema.constraints.RingConstraintKind;
import ormschema.constraints.SetComparisonConstraint;
import ormschema.constraints.SetComparisonConstraintKind;
import ormschema.constraints.UniquenessConstraint;
import ormschema.constraints.ValueConstraint;
import ormschema.structure.ConceptualDataType;
import ormschema.structure.ConceptualDataTypeKinds;
import ormschema.structure.EntityObject;
import ormschema.structure.Fact;
import ormschema.structure.FactKind;
import ormschema.structure.ObjectType;
import ormschema.structure.Role;
import ormschema.structure.RoleKind;
import ormschema.structure.RoleProxy;
import ormschema.structure.ValueType;
import utilities.ORMSchemaGraphUtilities;
import utilities.ReportGenerator;
import utilities.StringManipulations;

/**
 * 
 * This class represents the mapping tool that maps from ORM to grUML, as
 * described in chapter 2 and 3 of my thesis.
 * 
 * @author Alicia Owen
 *
 */

public class Orm2Tg {

	/**
	 * the ORM schema, which will be mapped to a grUML schema, in a TGraph
	 * representation
	 */
	private ORMGraph ormSchemaGraph;
	/**
	 * the input file is used to generate a grUML schema stored in
	 * {@code tgSchema}
	 */
	private Schema tgSchema;

	/**
	 * A map that holds {@link EntityObject}s as keys and {@link VertexClass}es
	 * as values. Required to retrieve {@link VertexClass}es when processing
	 * {@link Fact}s from the ORM schema graph.
	 */
	private HashMap<EntityObject, VertexClass> entityObjectToVertexClassMap;
	/**
	 * A map that creates the link between the XML IDs of ORM schema elements
	 * (Object Types and Facts) and the names they receive during the mapping
	 * step. This map is needed to insert the correct names into the GReQL
	 * constraints.
	 */
	private HashMap<String, String> xmlIdTogrUMLNameMap;

	/**
	 * This map creates a link between the grUML names of the objects mapped
	 * from the ORM schema and their XML IDs. The only purpose of this map is to
	 * improve the naming of attributes.
	 */
	private HashMap<String, String> grUMLNameToXmlIdMap;

	private ArrayList<String> qualifiedNamesList;

	/**
	 * attributes with invalid names receive the name "attribute" followed by
	 * the value of this counter in order to make the name unique
	 */
	private int counter = 0;
	private ReportGenerator reportGenerator;
	ORMSchemaGraphUtilities util;

	public static void main(String[] args) throws FileNotFoundException, IOException, XMLStreamException {
		String inputFile;
		File folder = new File("src/ormtestschemas/fully_mappable");
		File[] listOfFiles = folder.listFiles();
		Orm2Tg orm2tg = new Orm2Tg();

		for (int i = 0; i < listOfFiles.length; i++) {
			inputFile = listOfFiles[i].getAbsolutePath();
			System.out.println("Processing " + inputFile);

			try {
				orm2tg.transformOrm2Tg(inputFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public void transformOrm2Tg(String inputOrmSchema) throws XMLStreamException, SecurityException, IOException {
		util = new ORMSchemaGraphUtilities();
		qualifiedNamesList = new ArrayList<>();
		xmlIdTogrUMLNameMap = new HashMap<>();
		grUMLNameToXmlIdMap = new HashMap<>();

		parseInputOrmSchema(inputOrmSchema);
		initializeReportGenerator();
		initializeTgSchema();

		createVertexClasses();
		createAssociations();
		createConstraints();

		reportGenerator.finish();
		saveGraph("outputschemas/" + tgSchema.getName() + ".tg");
	}

	/**
	 * 
	 */
	private void initializeTgSchema() {
		String schemaName = ormSchemaGraph.get_schemaName();
		schemaName = StringManipulations.firstCharToUpperCase(schemaName);
		tgSchema = new SchemaImpl(schemaName, "orm2tg.tg.schema");
		tgSchema.createGraphClass(schemaName + "Graph");
	}

	/**
	 * @throws IOException
	 */
	private void initializeReportGenerator() throws IOException {
		String filepath = "mappingreports/" + ormSchemaGraph.get_schemaName() + ".txt";
		reportGenerator = new ReportGenerator(filepath, true, ormSchemaGraph.get_schemaName());
	}

	/**
	 * @param inputOrmSchema
	 * @throws XMLStreamException
	 * @throws FileNotFoundException
	 */
	private void parseInputOrmSchema(String inputOrmSchema) throws XMLStreamException, FileNotFoundException {
		OrmParser ormParser = new OrmParser();
		ormParser.process(inputOrmSchema);
		ormSchemaGraph = ormParser.getOrmGraph();
	}

	private void createVertexClasses() {
		for (EntityObject entityObject : ormSchemaGraph.getEntityObjectVertices()) {
			if (entityObjectToVertexClassMap == null) {
				entityObjectToVertexClassMap = new HashMap<>();
			}
			createVertexClass(entityObject);
		}
	}

	/**
	 * A method to create a {@link VertexClass} from an {@link EntityObject}
	 * from the ORM schema graph.
	 * 
	 * @param entityObject
	 *            - the {@link EntityObject} that will be transformed into a new
	 *            {@link VertexClass} in the grUML schema
	 */
	private VertexClass createVertexClass(EntityObject entityObject) {

		if (entityObjectToVertexClassMap.get(entityObject) != null) {
			return entityObjectToVertexClassMap.get(entityObject);
		}

		String name = entityObject.get_name().replaceAll("\\s+", "");
		if (!isValidAttributedElementName(name)) {
			name = adjustAttributedElementName(name);
		}
		VertexClass vertexClass = tgSchema.getGraphClass().createVertexClass(name);
		vertexClass.setQualifiedName(name);

		// if the entity object is independent, this is reflected in the
		// multiplicity values but also explicitly through a comment added to
		// this element
		if (entityObject.is_isIndependent()) {
			vertexClass.addComment("independent");
		}
		qualifiedNamesList.add(name);
		// add the EntityObject to the xmlID to grUML name map
		xmlIdTogrUMLNameMap.put(entityObject.get_xmlId(), name);
		grUMLNameToXmlIdMap.put(name, entityObject.get_xmlId());

		entityObjectToVertexClassMap.put(entityObject, vertexClass);

		createVertexClassHierarchy(entityObject);

		createVertexClassAttributes(vertexClass, entityObject);
		return vertexClass;
	}

	/**
	 * A method that checks whether the input {@link EntityObject} from the ORM
	 * schema graph has supertypes and if it does, it creates
	 * {@link VertexClass}es to represent these in the grUML schema and adds
	 * them as superclasses to the grUML schema representation of the
	 * {@link EntityObject}.
	 * 
	 * @param entityObject
	 *            - the {@link EntityObject} that may have supertypes.
	 */
	private void createVertexClassHierarchy(EntityObject entityObject) {
		for (ObjectType supertype : entityObject.get_supertype()) {
			assert supertype instanceof EntityObject;

			VertexClass vc = entityObjectToVertexClassMap.get(entityObject);
			VertexClass vcSupertype = createVertexClass((EntityObject) supertype);
			vc.addSuperClass(vcSupertype);
		}
	}

	/**
	 * A method to create the attributes of a {@link VertexClass}. A
	 * {@link VertexClass} can receive attributes from two sources:
	 * <ol>
	 * <li>from {@link ValueType}s that the {@link EntityObject} (now
	 * transformed to this {@link VertexClass}) is in relation with</li>
	 * <li>the {@link EntityObject}'s reference mode or other preferred
	 * reference scheme</li>
	 * </ol>
	 * 
	 * @param vertexClass
	 *            - the {@link VertexClass} to represent the
	 *            {@link EntityObject} (from the ORM schema graph) in a grUML
	 *            schema
	 * @param entityObject
	 *            - the {@link EntityObject} that is represented by the input
	 *            {@link VertexClass}
	 */
	private void createVertexClassAttributes(VertexClass vertexClass, EntityObject entityObject) {

		createAttributeFromPreferredIdentifiers(vertexClass, entityObject);
		createAttributeFromRelations(vertexClass, entityObject);
	}

	/**
	 * A method that creates {@link Attribute}s for the input
	 * {@link VertexClass} by analyzing the {@link Fact}s the input
	 * {@link EntityObject} participates in. If the {@link EntityObject}
	 * participates in a {@link Fact} that also contains a {@link ValueType},
	 * this {@link ValueType} becomes an {@link Attribute} of the
	 * {@link VertexClass}.
	 * 
	 * @param vertexClass
	 *            - the {@link VertexClass} that represents the input
	 *            {@link EntityObject} in the grUML schema.
	 * @param entityObject
	 *            - the {@link EntityObject} from the ORM schema graph which is
	 *            now represented as a {@link VertexClass} in the grUML schema.
	 */
	private void createAttributeFromRelations(VertexClass vertexClass, EntityObject entityObject) {

		// don't need to consider RoleProxys here since they can't be
		// ValueTypes, thus don't map to attributes
		for (Role entityObjectRole : entityObject.get_playedRole()) {
			for (RoleKind otherFactRole : entityObjectRole.get_fact().get_factRole()) {
				boolean createAttributeFromOtherRole = true;
				if (otherFactRole == entityObjectRole) {
					continue;
				}
				for (Role preferredIdentifierRole : entityObject.get_preferredIdentifierRole()) {
					// make sure not to transform a preferredIdentifierRole to
					// an attribute of the entityObject (would be a duplicate)
					if (otherFactRole == preferredIdentifierRole) {
						createAttributeFromOtherRole = false;
					}
				}

				if (createAttributeFromOtherRole && otherFactRole instanceof Role
						&& ((Role) otherFactRole).get_player() instanceof ValueType) {

					Domain domain = mapOrm2GrumlDomain(
							((ValueType) ((Role) otherFactRole).get_player()).get_conceptualDataType().get_dataType());
					// check the multiplicity values on the
					// entityObjectRole: if its upper bound is > 1, then the
					// attribute will be a set
					if (((Role) otherFactRole).get_multiplicityUpperBound() > 1) {
						domain = tgSchema.createSetDomain(domain);
					}

					String name = StringManipulations
							.firstCharToLowerCase(((Role) otherFactRole).get_player().get_name());
					reportGenerator.write("Input name: " + name);

					if (!isValidAttributeName(name)) {
						name = adjustAttributeName(name);
					}

					if (hasDuplicateAttribute(vertexClass, name)) {

						name = qualifyDuplicateAttribute(vertexClass, name);
					}
					// add the role player (i.e. the ValueType) to the map
					// of
					// xmlIds and grUML names
					xmlIdTogrUMLNameMap.put(((Role) otherFactRole).get_player().get_xmlId(), name);
					grUMLNameToXmlIdMap.put(name, ((Role) otherFactRole).get_player().get_xmlId());
					reportGenerator.write("\tOutput name: " + name + "\t[AttributeFromRelation]\n");
					vertexClass.createAttribute(name, domain);

				}
			}
		}
	}

	/**
	 * A method that creates an {@link Attribute} for the input
	 * {@link VertexClass} for each preferredIdentifier the {@link EntityObject}
	 * (from the ORM schema graph) has. The preferredIdentifier is a
	 * {@link ValueType} whose values are used to identify the input
	 * {@link EntityObject}.
	 * 
	 * @param vertexClass
	 *            - the {@link VertexClass} that represents the input
	 *            {@link EntityObject} in the grUML schema.
	 * @param entityObject
	 *            - the {@link EntityObject} from the ORM schema graph which is
	 *            now represented as a {@link VertexClass} in the grUML schema.
	 */
	private void createAttributeFromPreferredIdentifiers(VertexClass vertexClass, EntityObject entityObject) {

		String name = "";
		for (Role preferredIdentifierRole : entityObject.get_preferredIdentifierRole()) {
			ObjectType objectType = preferredIdentifierRole.get_player();
			if (objectType instanceof ValueType) {

				Domain domain = mapOrm2GrumlDomain(((ValueType) objectType).get_conceptualDataType().get_dataType());

				String referenceMode = entityObject.get_referenceMode();

				if (!referenceMode.equals("")) {
					referenceMode = StringManipulations.firstCharToUpperCase(referenceMode);

					if (objectType.get_name().endsWith("Value")) {
						// the ValueType represents a unit-based reference mode
						name = entityObject.get_name() + "In" + referenceMode;
					} else {
						name = entityObject.get_name() + referenceMode;
					}
				} else {
					name = objectType.get_name();
				}

				reportGenerator.write("Input name: " + name);

				// check whether the attribute name is valid
				if (!isValidAttributeName(name)) {
					name = adjustAttributeName(name);
				}
				// check whether the vertexClass already has an attribute with
				// this name
				if (hasDuplicateAttribute(vertexClass, name)) {

					name = qualifyDuplicateAttribute(vertexClass, name);
				}
				reportGenerator.write("\tOutput name: " + name + "\t[AttributeFromPreferredIdentifier]\n");
				// add the ValueType to the xmlID to grUML name map
				xmlIdTogrUMLNameMap.put(objectType.get_xmlId(), name);
				grUMLNameToXmlIdMap.put(name, objectType.get_xmlId());

				// store the preferred identifiers in a RecordDomain
				RecordDomain preferredIdentifier;
				if (!vertexClass.containsAttribute("preferredIdentifiers")) {
					preferredIdentifier = tgSchema
							.createRecordDomain("preferredIdentifiers." + vertexClass.getQualifiedName());
					vertexClass.createAttribute("preferredIdentifiers", preferredIdentifier);
				}
				preferredIdentifier = (RecordDomain) vertexClass.getAttribute("preferredIdentifiers").getDomain();
				// don't add an attribute twice (could happen with objectified
				// types in a ring relation)
				if (!preferredIdentifier.hasComponent(name)) {
					preferredIdentifier.addComponent(name, domain);
				}

				qualifiedNamesList.add(name);

			} else {
				createAttributeFromPreferredIdentifiers(vertexClass,
						(EntityObject) preferredIdentifierRole.get_player());
			}

		}

	}

	/**
	 * This method qualifies the input name with "_x" where x is the counter for
	 * variables of this name in this VertexClass.
	 * 
	 * @param vertexClass
	 *            - the VertexClass containing which should receive an attribute
	 *            with name stem "name"
	 * @param name
	 *            - the name stem for the attribute that should be set
	 * @return the qualified attribute name for the input VertexClass
	 */
	private String qualifyDuplicateAttribute(VertexClass vertexClass, String name) {
		String qualifiedAttributeName = "";
		ArrayList<String> listOfAttributeNames = new ArrayList<>();
		vertexClass.getAttributeList().forEach(attribute -> listOfAttributeNames.add(attribute.getName()));

		if (containsNameStem(listOfAttributeNames, name)) {
			if (listOfAttributeNames.contains(name)) {
				// this attribute is the first duplicate -> update the original
				// attribute
				Attribute existingAttribute = vertexClass.getAttribute(name);
				existingAttribute.setName(name + "_1");
			}
			qualifiedAttributeName = name + "_" + getNameStemCount(listOfAttributeNames, name);

		}
		return qualifiedAttributeName;
	}

	private boolean hasDuplicateAttribute(VertexClass vertexClass, String name) {
		ArrayList<String> listOfAttributeNames = new ArrayList<>();
		vertexClass.getAttributeList().forEach(attribute -> listOfAttributeNames.add(attribute.getName()));

		return containsNameStem(listOfAttributeNames, name);
	}

	/**
	 * A method to check whether the input String is a valid grUML attribute
	 * name or not.
	 * 
	 * @param name
	 *            - the name that is checked for its adherence of grUML naming
	 *            conventions.
	 * @return true, if the input name is according to grUML conventions, false
	 *         if not.
	 */
	private boolean isValidAttributeName(String name) {
		// The name must begin with an lower case letter, followed by any number
		// of words containing [a-zA-Z_0-9]
		final Pattern ATTRIBUTE_NAME_PATTERN = Pattern.compile("\\p{Lower}\\w*");
		return ATTRIBUTE_NAME_PATTERN.matcher(name).matches();
	}

	/**
	 * This method checks whether the input String is a valid grUML name for an
	 * {@link AttributedElement} or not.
	 * 
	 * @param name
	 *            - the name that is checked for its adherence of grUML naming
	 *            conventions.
	 * @return true, if the input name is according to grUML conventions, false
	 *         if not.
	 */
	private boolean isValidAttributedElementName(String name) {

		if (qualifiedNamesList.contains(name)) {
			return false;
		}
		// The name must begin with an upper case letter, followed by any number
		// of words containing [a-zA-Z_0-9]
		final Pattern ATTRIBUTED_ELEMENT_NAME_PATTERN = Pattern.compile("\\p{Upper}\\w*?");
		return ATTRIBUTED_ELEMENT_NAME_PATTERN.matcher(name).matches();

	}

	/**
	 * A method used to check future {@link VertexClass} names for compatibility
	 * with grUML naming conventions. If the input string is incompatible, it is
	 * adjusted so it becomes compatible.
	 * 
	 * @param name
	 *            - the input String that is transformed into a valid grUML
	 *            {@link VertexClass} name
	 * @return a valid grUML {@link VertexClass} or {@link EdgeClass} name
	 */
	private String adjustAttributedElementName(String name) {

		final Pattern ATTRIBUTED_ELEMENT_NAME_PATTERN = Pattern.compile("\\w*");
		Matcher matcher = ATTRIBUTED_ELEMENT_NAME_PATTERN.matcher(name);

		StringBuffer stringBuffer = new StringBuffer();

		while (matcher.find()) {
			stringBuffer.append(name.substring(matcher.start(), matcher.end()));
		}

		if (stringBuffer.length() == 0) {

			while (qualifiedNamesList.contains("VertexClass_" + counter)) {
				++counter;
			}
			stringBuffer.append("VertexClass_" + counter);

			++counter;
		} else {
			if (containsNameStem(qualifiedNamesList, stringBuffer.toString())) {
				if (qualifiedNamesList.contains(stringBuffer.toString())) {

					// the name occurs more than once in the grUML schema:
					// append a
					// counter to the element already in the list
					qualifiedNamesList.remove(stringBuffer.toString());
					String nameReplacement = stringBuffer.toString() + "_"
							+ getNameStemCount(qualifiedNamesList, stringBuffer.toString());
					tgSchema.getAttributedElementClass(stringBuffer.toString()).setQualifiedName(nameReplacement);
					qualifiedNamesList.add(nameReplacement);
					xmlIdTogrUMLNameMap.put(grUMLNameToXmlIdMap.get(stringBuffer.toString()), nameReplacement);
				}
				// add a counter to the new element
				stringBuffer.append("_" + getNameStemCount(qualifiedNamesList, stringBuffer.toString()));
				qualifiedNamesList.add(stringBuffer.toString());

			}
		}

		char firstChar = Character.toUpperCase(stringBuffer.charAt(0));
		stringBuffer.setCharAt(0, firstChar);

		return stringBuffer.toString();
	}

	private boolean containsNameStem(ArrayList<String> qualifiedNamesList, String nameStem) {
		for (String name : qualifiedNamesList) {
			if (name.length() >= nameStem.length() && nameStem.equals(name.substring(0, nameStem.length()))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A method used to check future attribute names for compatibility with
	 * grUML naming conventions. If the input string is incompatible, it is
	 * adjusted so it becomes compatible.
	 * 
	 * @param name
	 *            - the input String that is transformed into a valid grUML
	 *            attribute name
	 * @return a valid grUML attribute name
	 */
	private String adjustAttributeName(String name) {

		String preprocessedName = name;

		if (name.contains("#")) {
			preprocessedName = preprocessedName.replace("#", "No");
		}
		if (name.contains("%")) {
			preprocessedName = preprocessedName.replace("%", "Percent");
		}

		final Pattern ATTRIBUTE_NAME_PATTERN = Pattern.compile("\\w+");
		Matcher matcher = ATTRIBUTE_NAME_PATTERN.matcher(preprocessedName);

		StringBuffer stringBuffer = new StringBuffer();

		while (matcher.find()) {
			stringBuffer.append(StringManipulations
					.firstCharToUpperCase(preprocessedName.substring(matcher.start(), matcher.end())));
		}
		char firstChar = Character.toLowerCase(stringBuffer.charAt(0));
		stringBuffer.setCharAt(0, firstChar);

		return stringBuffer.toString();
	}

	/**
	 * A method to create grUML {@link VertexClass}es and the
	 * {@link EdgeClass}es connecting them. To achieve this, it iterates over
	 * all {@link Fact}s in the ORM schema graph, extracts the {@link Role}s and
	 * their players ({@link ObjectType}s) which become grUML
	 * {@link VertexClass}es. The {@link EdgeClass}es are generated from the
	 * {@link Fact} name and the {@link UniquenessConstraint}s,
	 * {@link MandatoryConstraint}s and {@link FrequencyConstraint}s which
	 * define the multiplicities of the association.
	 * 
	 */
	private void createAssociations() {

		for (Fact fact : ormSchemaGraph.getFactVertices()) {
			if (fact.get_objectifiedType() != null) {
				// this fact is either objectified or has arity > 3. Don't
				// include the "original" fact in the grUML schema. Use
				// the facts implied by this fact instead (which will happen
				// eventually during the for loop)!
				continue;
			}

			// don't create associations representing subtype relations!
			if (fact.get_factKind() == FactKind.SUBTYPEFACT) {
				continue;
			}

			// need to collect all fact roles
			boolean interEntityObjectFact = true;
			ArrayList<RoleKind> listOfRoles = new ArrayList<>();

			// check whether this fact contains ValueTypes - if it does, it
			// won't become an association
			for (RoleKind role : fact.get_factRole()) {
				if (role instanceof RoleProxy) {
					if (!(((RoleProxy) role).get_linkedRole().get_player() instanceof EntityObject)) {
						interEntityObjectFact = false;
					}
				} else {
					if (!(((Role) role).get_player() instanceof EntityObject)) {
						interEntityObjectFact = false;
					}
				}

				listOfRoles.add(role);
			}
			// if this is the case, then generate an Association
			if (interEntityObjectFact) {

				String qualifiedName = fact.get_name();

				if (!isValidAttributedElementName(qualifiedName)) {
					qualifiedName = adjustAttributedElementName(qualifiedName);
				}
				// add the Fact to the xmlID to grUML name map
				xmlIdTogrUMLNameMap.put(fact.get_xmlId(), qualifiedName);
				grUMLNameToXmlIdMap.put(qualifiedName, fact.get_xmlId());
				qualifiedNamesList.add(qualifiedName);

				assert listOfRoles.size() == 2;

				ObjectType sourceObjectType;
				String fromRoleName = "";
				if (listOfRoles.get(0) instanceof Role) {
					sourceObjectType = ((Role) listOfRoles.get(0)).get_player();
					fromRoleName = (listOfRoles.get(0)).get_name();
				} else {
					sourceObjectType = ((RoleProxy) listOfRoles.get(0)).get_linkedRole().get_player();
					fromRoleName = ((RoleProxy) listOfRoles.get(0)).get_linkedRole().get_name();
				}
				VertexClass from = entityObjectToVertexClassMap.get(sourceObjectType);
				int fromMin = (listOfRoles.get(0)).get_multiplicityLowerBound();
				int fromMax = (listOfRoles.get(0)).get_multiplicityUpperBound();
				AggregationKind aggrFrom = AggregationKind.NONE;

				ObjectType targetObjectType;
				String toRoleName = "";
				if (listOfRoles.get(1) instanceof Role) {
					targetObjectType = ((Role) listOfRoles.get(1)).get_player();
					toRoleName = (listOfRoles.get(1)).get_name();
				} else {
					targetObjectType = ((RoleProxy) listOfRoles.get(1)).get_linkedRole().get_player();
					toRoleName = ((RoleProxy) listOfRoles.get(1)).get_linkedRole().get_name();
				}

				VertexClass to = entityObjectToVertexClassMap.get(targetObjectType);
				int toMin = (listOfRoles.get(1)).get_multiplicityLowerBound();
				int toMax = (listOfRoles.get(1)).get_multiplicityUpperBound();
				de.uni_koblenz.jgralab.schema.AggregationKind aggrTo = AggregationKind.NONE;

				tgSchema.getGraphClass().createEdgeClass(qualifiedName, from, fromMin, fromMax, fromRoleName, aggrFrom,
						to, toMin, toMax, toRoleName, aggrTo);

			}

		}
	}

	/**
	 * A method to map from ORM conceptual data types to the {@link Domain}s
	 * provided in grUML.
	 * 
	 * @param conceptualDataTypeKind
	 *            - the kind of {@link ConceptualDataType}.
	 * @return the corresponding grUML value {@link Domain}
	 */
	private Domain mapOrm2GrumlDomain(ConceptualDataTypeKinds conceptualDataTypeKind) {
		String conceptualDataTypeName = conceptualDataTypeKind.name();
		switch (conceptualDataTypeName) {

		case ("AUTOCOUNTERNUMERICDATATYPE"):
			return (tgSchema.getIntegerDomain());
		case ("AUTOTIMESTAMPTEMPORALDATATYPE"):
			return (tgSchema.getStringDomain());
		case ("DATEANDTIMETEMPORALDATATYPE"):
			return (tgSchema.getStringDomain());
		case ("DATETEMPORALDATATYPE"):
			return (tgSchema.getStringDomain());
		case ("DECIMALNUMERICDATATYPE"):
			return (tgSchema.getDoubleDomain());
		case ("DOUBLEPRECISIONFLOATINGPOINTNUMERICDATATYPE"):
			return (tgSchema.getDoubleDomain());
		case ("FIXEDLENGTHRAWDATADATATYPE"):
			// this case is handled in OrmParser.java by returning an Exception
			return (null);
		case ("FIXEDLENGTHTEXTDATATYPE"):
			return (tgSchema.getStringDomain());
		case ("LARGELENGTHRAWDATADATATYPE"):
			// this case is handled in OrmParser.java by returning an Exception
			return (null);
		case ("LARGELENGTHTEXTDATATYPE"):
			return (tgSchema.getStringDomain());
		case ("MONEYNUMERICDATATYPE"):
			return (tgSchema.getDoubleDomain());
		case ("OBJECTIDOTHERDATATYPE"):
			return (tgSchema.getIntegerDomain());
		case ("OLEOBJECTRAWDATADATATYPE"):
			// this case is handled in OrmParser.java by returning an Exception
			return (null);
		case ("PICTURERAWDATADATATYPE"):
			// this case is handled in OrmParser.java by returning an Exception
			return (null);
		case ("ROWIDOTHERDATATYPE"):
			return (tgSchema.getIntegerDomain());
		case ("SIGNEDINTEGERNUMERICDATATYPE"):
			return (tgSchema.getIntegerDomain());
		case ("SIGNEDLARGEINTEGERNUMERICDATATYPE"):
			return (tgSchema.getLongDomain());
		case ("SIGNEDSMALLINTEGERNUMERICDATATYPE"):
			return (tgSchema.getIntegerDomain());
		case ("SINGLEPRECISIONFLOATINGPOINTNUMERICDATATYPE"):
			return (tgSchema.getDoubleDomain());
		case ("TIMETEMPORALDATATYPE"):
			return (tgSchema.getStringDomain());
		case ("TRUEORFALSELOGICALDATATYPE"):
			return (tgSchema.getBooleanDomain());
		case ("UNSIGNEDINTEGERNUMERICDATATYPE"):
			return (tgSchema.getLongDomain());
		case ("UNSIGNEDLARGEINTEGERNUMERICDATATYPE"):
			return (tgSchema.getLongDomain());
		case ("UNSIGNEDSMALLINTEGERNUMERICDATATYPE"):
			return (tgSchema.getIntegerDomain());
		case ("UNSIGNEDTINYINTEGERNUMERICDATATYPE"):
			return (tgSchema.getIntegerDomain());
		case ("VARIABLELENGTHRAWDATADATATYPE"):
			// this case is handled in OrmParser.java by returning an Exception
			return (null);
		case ("VARIABLELENGTHTEXTDATATYPE"):
			return (tgSchema.getStringDomain());
		case ("YESORNOLOGICALDATATYPE"):
			return (tgSchema.getBooleanDomain());
		}
		return null;
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
			tgSchema.save(filename);
		} catch (GraphIOException e) {
			System.err.println("An Exception occured while saving graph to " + filename + ".");
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * This method iterates over all the constraints that were parsed from the
	 * ORM schema file and creates grUML constraints when necessary.
	 * 
	 * @throws IOException
	 */
	private void createConstraints() throws IOException {

		for (Constraint constraint : ormSchemaGraph.getConstraintVertices()) {
			ConstraintImpl greqlConstraint = null;

			if (requiresGrUMLConstraint(constraint)) {

				if (constraint instanceof ValueConstraint) {
					greqlConstraint = new ValueConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
							.generateGReQLConstraint(constraint);

				} else if (constraint instanceof RingConstraint) {
					switch (((RingConstraint) constraint).get_type()) {
					case ACYCLIC:
						greqlConstraint = new AcyclicRingConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
								.generateGReQLConstraint(constraint);
						break;
					case ANTISYMMETRIC:
						greqlConstraint = new AntisymmetricRingConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
								.generateGReQLConstraint(constraint);
						break;
					case ASYMMETRIC:
						greqlConstraint = new AsymmetricRingConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
								.generateGReQLConstraint(constraint);
						break;
					case INTRANSITIVE:
						greqlConstraint = new IntransitiveRingConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
								.generateGReQLConstraint(constraint);
						break;
					case IRREFLEXIVE:
						greqlConstraint = new IrreflexiveRingConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
								.generateGReQLConstraint(constraint);
						break;
					case PURELYREFLEXIVE:
						greqlConstraint = new PurelyReflexiveRingConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
								.generateGReQLConstraint(constraint);
						break;
					case REFLEXIVE:
						greqlConstraint = new ReflexiveRingConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
								.generateGReQLConstraint(constraint);
						break;
					case STRONGLYINTRANSITIVE:
						RingConstraint tempConstraint = (RingConstraint) constraint;
						tempConstraint.set_type(RingConstraintKind.INTRANSITIVE);
						greqlConstraint = new IntransitiveRingConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
								.generateGReQLConstraint(constraint);
						tempConstraint.set_type(RingConstraintKind.IRREFLEXIVE);
						greqlConstraint = new IrreflexiveRingConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
								.generateGReQLConstraint(constraint);

						break;

					case SYMMETRIC:
						greqlConstraint = new SymmetricRingConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
								.generateGReQLConstraint(constraint);
						break;
					case TRANSITIVE:
						greqlConstraint = new TransitiveRingConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
								.generateGReQLConstraint(constraint);
						break;

					default:
						break;

					}
				} else if (constraint instanceof UniquenessConstraint) {
					if (!constraint.is_isInterpredicateConstraint()) {
						greqlConstraint = new UniquenessConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
								.generateGReQLConstraint(constraint);
					}

				} else if (constraint instanceof MandatoryConstraint) {
					greqlConstraint = new MandatoryConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
							.generateGReQLConstraint(constraint);

				} else if (constraint instanceof SetComparisonConstraint) {
					if (((SetComparisonConstraint) constraint).get_type() == SetComparisonConstraintKind.INCLUSIVEOR) {
						if (util.isSubtypeConstraint((SetComparisonConstraint) constraint)
								&& isExhaustiveSubtypeConstraint((SetComparisonConstraint) constraint)) {

							ObjectType supertype = util.retrieveSupertype((SetComparisonConstraint) constraint);
							VertexClass vc = entityObjectToVertexClassMap.get(supertype);
							// make the superclass abstract
							vc.setAbstract(true);

							String subclassEnumeration = printgrUMLNames(
									util.getSubtypes((SetComparisonConstraint) constraint));
							// add a warning to the schema and don't continue
							// processing
							String subtypeConstraintMessage = "The vertex classes %s are exhaustive for their superclass %s. ORM allows subtype instances with this constraint to represent multiple of the constrained subtypes. GrUML can't express that a vertex class instance is a member of multiple subclasses at once. ";

							vc.addComment(
									String.format(subtypeConstraintMessage, subclassEnumeration, vc.getUniqueName()));

							continue;
						}
						greqlConstraint = new InclusiveOrConstraintGenerator(xmlIdTogrUMLNameMap, tgSchema)
								.generateGReQLConstraint(constraint);

					} else if (((SetComparisonConstraint) constraint)
							.get_type() == SetComparisonConstraintKind.EXCLUSIVEOR) {
						if (util.isSubtypeConstraint((SetComparisonConstraint) constraint)) {

							ObjectType supertype = util.retrieveSupertype((SetComparisonConstraint) constraint);
							VertexClass vc = entityObjectToVertexClassMap.get(supertype);
							// make the superclass abstract
							vc.setAbstract(true);
							continue;
						}
					}
				}
				if (greqlConstraint != null) {
					tgSchema.getGraphClass().addConstraint(greqlConstraint);
				}
			}

		}

	}

	/**
	 * This method returns the enumeration of the input {@link ObjectType}s'
	 * grUML equivalents' names in form of a string with comma separated values
	 * 
	 * @param listOfObjectTypes
	 *            - the list of {@link ObjectType} whose grUML names should be
	 *            enumerated in a string
	 * @return a string enumerating the grUML names of the input
	 *         {@link ObjectType}s after their mapping
	 */
	private String printgrUMLNames(ArrayList<ObjectType> listOfObjectTypes) {

		StringBuilder sb = new StringBuilder();
		String separator = "";
		int i = 0;

		for (ObjectType ot : listOfObjectTypes) {

			if (i == listOfObjectTypes.size() - 1) {
				separator = " and ";
			}

			String grUMLName = xmlIdTogrUMLNameMap.get(ot.get_xmlId());
			sb.append(separator).append(grUMLName);
			separator = ", ";
			i++;

		}
		return sb.toString();
	}

	/**
	 * This method checks whether the input constraint requires a GReQL
	 * constraint. Some constraints are represented through multiplicity values
	 * but need to be expressed explicitly as GReQL constraints if they refer to
	 * {@link Fact}s that don't map to associations in grUML.
	 * 
	 * @param constraint
	 *            - the constraint that may or may not need to be represented as
	 *            a GReQL constraint
	 * @return true if a GReQL constraint needs to be constructed. False if not.
	 */
	private boolean requiresGrUMLConstraint(Constraint constraint) {
		if (constraint instanceof FrequencyConstraint
				&& !((FrequencyConstraint) constraint).is_isInterpredicateConstraint()
				&& util.isSingleRoleConstrained((FrequencyConstraint) constraint)) {
			return false;
		}

		if (constraint instanceof UniquenessConstraint
				&& !((UniquenessConstraint) constraint).is_isInterpredicateConstraint()) {
			if (util.isSingleRoleConstrained((UniquenessConstraint) constraint)) {
				return false;
			} else {
				if (util.containsValueType(util.getFact((UniquenessConstraint) constraint))) {
					return false;
				}
			}
		}

		if (constraint instanceof MandatoryConstraint) {
			// MandatoryConstraints always apply to only one role
			boolean condition1 = util.containsValueType(util.getFact((MandatoryConstraint) constraint));
			boolean condition2 = util.getRole(util.getRoleSequence((MandatoryConstraint) constraint, 0), 0)
					.get_player() instanceof EntityObject;
			boolean condition3 = util.getFactArity(util.getFact((MandatoryConstraint) constraint)) == 2;

			if (!(condition1 && condition2 && condition3)) {

				// if the mandatory constraint applies to a role which is
				// played
				// by an EntityObject but a fact which contains a ValueType,
				// then a GreQL constraint is required!
				return false;

			}
		}
		return true;
	}

	/**
	 * This method takes a list of strings as input and counts how many strings
	 * the list contains which begin with the method's second argument
	 * "nameStem".
	 * 
	 * @param listOfStrings
	 *            - a list of strings
	 * @param nameStem
	 *            - the name stem whose occurrences as prefix in the strings
	 *            from "listOfStrings" is counted
	 * @return the number of times "nameStem" is a prefix of a string within
	 *         "listOfStrings"
	 */
	private int getNameStemCount(ArrayList<String> listOfStrings, String nameStem) {
		int counter = 1;
		for (String name : listOfStrings) {
			if (name.length() >= nameStem.length() && nameStem.equals(name.substring(0, nameStem.length()))) {
				++counter;
			}
		}
		return counter;
	}

	public ORMGraph getORMGraph() {
		return ormSchemaGraph;
	}

	/**
	 * @return the xmlIdTogrUMLNameMap
	 */
	public HashMap<String, String> getXmlIdTogrUMLNameMap() {
		return xmlIdTogrUMLNameMap;
	}

	/**
	 * This method checks whether an input inclusive-or subtype constraint
	 * composes an exhaustive subtype constraint or not. The exclusive subtype
	 * constraint consists of an inclusive-or constraint and an exclusion
	 * constraint that applies to a subset of the roles constrained by the
	 * inclusive or constraint.
	 * 
	 * @param constraint
	 * @return
	 */
	public boolean isExhaustiveSubtypeConstraint(SetComparisonConstraint constraint) {

		Set<ObjectType> setOfSubtypes = new HashSet<>(util.getSubtypes(constraint));

		for (SetComparisonConstraint setComparisonConstraint : ormSchemaGraph.getSetComparisonConstraintVertices()) {
			if (setComparisonConstraint.get_type() == SetComparisonConstraintKind.EXCLUSION
					&& util.isSubtypeConstraint(setComparisonConstraint)) {
				Set<ObjectType> setOfExclusiveSubtypes = new HashSet<>(util.getSubtypes(setComparisonConstraint));
				if (setOfSubtypes.containsAll(setOfExclusiveSubtypes)) {
					return false;
				}
			}
		}
		return true;
	}

}
