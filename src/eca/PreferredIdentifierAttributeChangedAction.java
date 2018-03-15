package eca;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import de.uni_koblenz.jgralab.Edge;
import de.uni_koblenz.jgralab.Graph;
import de.uni_koblenz.jgralab.Record;
import de.uni_koblenz.jgralab.Vertex;
import de.uni_koblenz.jgralab.eca.Action;
import de.uni_koblenz.jgralab.eca.events.ChangeAttributeEvent;
import de.uni_koblenz.jgralab.eca.events.Event;
import de.uni_koblenz.jgralab.schema.EdgeClass;
import de.uni_koblenz.jgralab.schema.VertexClass;

/**
 * This class is used to adjust the preferred identifier attributes of instances
 * of a user-defined adjacent {@link VertexClass}. The attribute values are only
 * adjusted if the {@link Vertex} that originally triggered the
 * {@link ChangeAttributeEvent} causing this {@link Action} has an {@link Edge}
 * connecting it to an instance of the predefined neighboring
 * {@link VertexClass}. For preferred identifiers that don't propagate to other
 * {@link VertexClass}es (via ORM external UniquenessConstraint and the marker
 * as preferred identifier), this Action will do nothing.
 * 
 * @author Alicia Owen
 *
 * @param <AEC>
 */
public class PreferredIdentifierAttributeChangedAction implements Action<VertexClass> {

	/**
	 * The {@link VertexClass} which has a preferred identifier attribute that
	 * should update automatically when this attribute is set in an associated
	 * {@link VertexClass} instance. TODO add an example!
	 */
	VertexClass neighboringVC;

	/**
	 * The {@link EdgeClass} that connects the {@link VertexClass}es of interest
	 * (i.e. the VertexClass that holds an attribute which is part of the
	 * preferred identifier of another)
	 */
	EdgeClass connectingEC;

	public PreferredIdentifierAttributeChangedAction(VertexClass nextVC, EdgeClass connectingEC) {

		this.connectingEC = connectingEC;
		this.neighboringVC = nextVC;
	}

	@Override
	public void doAction(Event<VertexClass> event) {
		System.out.println("\n>>>>>>>>>>>>>>>>>>> doAction <<<<<<<<<<<<<<<<<<<<<<<");
		if (event instanceof ChangeAttributeEvent<?>) {

			PreferredIdentifierAttributeChangedCondition<VertexClass> uniquenessCondition = new PreferredIdentifierAttributeChangedCondition<>();
			boolean uniquenessConditionEvaluation = uniquenessCondition.evaluate(event);
			GraphValidityCondition<VertexClass> graphValidCondition = new GraphValidityCondition<>();
			boolean graphValidEvaluation = graphValidCondition.evaluate(event);

			Vertex sourceVertex = (Vertex) ((ChangeAttributeEvent<VertexClass>) event).getElement();
			String changedAttributeName = ((ChangeAttributeEvent<VertexClass>) event).getAttributeName();
			printRecord(sourceVertex.toString(), sourceVertex.getAttribute(changedAttributeName), false);

			if (uniquenessConditionEvaluation && graphValidEvaluation) {
				System.out.println("Case 1: Attribute change is valid & Graph is valid");
				// if the value within the Record that was changed is unique to
				// the VertexClass containing this Record as attribute, keep the
				// change and possibly propagate it to other Vertices
				if (((Record) sourceVertex.getAttribute(changedAttributeName)) == null) {
					// the Record was reset to null after an attempt to add a
					// non-unique value. Don't need to propagate this change!
					return;
				}
				Graph graph = event.getGraph();

				if (neighboringVC != null && connectingEC != null) {

					ArrayList<Vertex> neighboringVerticesOfType = getConnectedVerticesOfType(sourceVertex, graph);

					for (Vertex nextVertex : neighboringVerticesOfType) {
						System.out.println("~~~~~~~~~~~~> PROPAGATING TO " + nextVertex.toString());
						printRecord(nextVertex.toString(), nextVertex.getAttribute(changedAttributeName), true);

						// retrieve the entries of the Records of the VC
						// instance
						// whose
						// attribute changed and of the VC instance which is
						// affected by this
						// change (the "neighboringVC" instance)

						Map<String, Object> updatedKeyValuePairs = ((Record) sourceVertex
								.getAttribute(changedAttributeName)).toPMap();
						Map<String, Object> keyValuePairsNeighbor = ((Record) nextVertex
								.getAttribute(changedAttributeName)).toPMap();

						// create a new Record for the "neighboringVC" instance
						// containing the updated values from the sourceVertex
						ArrayList<Object> dataForRecord = new ArrayList<>();
						for (String componentName : keyValuePairsNeighbor.keySet()) {
							if (updatedKeyValuePairs.keySet().contains(componentName)) {
								// grab the value from the sourceVertex' Record
								System.out.println(String.format(" -- Found common key between %s and %s: %s",
										componentName, sourceVertex.toString(), nextVertex.toString()));
								System.out.println(String.format(
										" --- Adding new Record component (from SourceVertex): \"%s\" <-> \"%s\"",
										componentName, updatedKeyValuePairs.get(componentName).toString()));
								dataForRecord.add(updatedKeyValuePairs.get(componentName));
							} else {
								// grab the data from the neighboringVC's Record
								System.out.println(" -- Copying already existing Record component.");
								System.out.println(String.format(
										" --- Adding new Record component (from original Record): \"%s\" <-> \"%s\"",
										componentName, keyValuePairsNeighbor.get(componentName).toString()));
								dataForRecord.add(keyValuePairsNeighbor.get(componentName));
							}
						}

						Object data = createNewRecord(changedAttributeName, nextVertex, keyValuePairsNeighbor,
								dataForRecord);
						nextVertex.setAttribute(changedAttributeName, data);
						printRecord(nextVertex.toString(), nextVertex.getAttribute(changedAttributeName), false);

					}

				}
			} else if (!(uniquenessConditionEvaluation)) {
				System.out.println("Case 2: Duplicate preferred identifier. Restore original data ...");
				// must ignore the GraphValidCondition or this will be an
				// endless do-undo-do-undo-cycle!
				// restore the original Record, do not propagate the change to
				// other Vertices!
				sourceVertex.setAttribute(changedAttributeName,
						((ChangeAttributeEvent<VertexClass>) event).getOldValue());
				printRecord(sourceVertex.toString(), sourceVertex.getAttribute(changedAttributeName), false);

			}

		}
		System.out.println("################## endAction ####################\n\n");

	}

	/**
	 * @param changedAttributeName
	 * @param nextVertex
	 * @param keyValuePairsNeighbor
	 * @param dataForRecord
	 */
	private Object createNewRecord(String changedAttributeName, Vertex nextVertex,
			Map<String, Object> keyValuePairsNeighbor, ArrayList<Object> dataForRecord) {

		// get the name of the Class implementing the Record for the
		// nextVertex Vertex
		String className = nextVertex.getAttribute(changedAttributeName).getClass().getName();
		// define the classes of parameters the Class with className will need
		// in its constructor
		int parameterCount = keyValuePairsNeighbor.size();
		Class<String>[] parameters = new Class[parameterCount];

		for (int i = 0; i < parameterCount; ++i) {
			parameters[i] = String.class;
		}

		try {
			// try getting the Class associated with this name
			Class cl = Class.forName(className);
			Constructor constructor = cl.getConstructor(parameters);

			Object data = constructor.newInstance(dataForRecord.toArray());
			return data;

		} catch (ClassNotFoundException e) {
			System.err.println("ClassNotFoundException: " + e.getMessage());
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			System.err.println("NoSuchMethodException: " + e.getMessage());
			e.printStackTrace();
		} catch (SecurityException e) {
			System.err.println("SecurityException: " + e.getMessage());
			e.printStackTrace();
		} catch (InstantiationException e) {
			System.err.println("InstantiationException: " + e.getMessage());
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.err.println("IllegalAccessException: " + e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			System.err.println("IllegalArgumentException: " + e.getMessage());
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			System.err.println("InvocationTargetException: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param event
	 * @param graph
	 * @param neighboringVerticesOfType
	 */
	private ArrayList<Vertex> getConnectedVerticesOfType(Vertex sourceVertex, Graph graph) {

		ArrayList<Vertex> neighboringVerticesOfType = new ArrayList<>();

		for (Vertex vertexOfType : graph.vertices(neighboringVC)) {

			// if there is an edge connecting the element that triggered
			// the
			// ChangeAttributeEvent and the current vertexOfType, then
			// add
			// this vertexOfType to a list for an update in the next
			// step
			Iterator<Edge> edgesOfType = sourceVertex.incidences(connectingEC).iterator();
			while (edgesOfType.hasNext()) {
				Edge currentEdge = edgesOfType.next();
				if ((currentEdge.getAlpha() == sourceVertex && currentEdge.getOmega() == vertexOfType)
						|| (currentEdge.getAlpha() == vertexOfType && currentEdge.getOmega() == sourceVertex)) {
					System.out.println(String.format("\n -> Found neighboring vertex: %s - %s ",
							sourceVertex.toString(), vertexOfType.toString()));
					neighboringVerticesOfType.add(vertexOfType);
				}
			}

		}
		return neighboringVerticesOfType;
	}

	private void printRecord(String name, Record record, boolean beforeChange) {

		String firstLine = "\t-----------------------------------------\n  \t| %s's record (%s)\t|\n\t-----------------------------------------\n";
		System.out.println(String.format(firstLine, name, beforeChange ? "before" : "after"));
		if (record == null) {
			System.out.println("\t\t<empty record>");
		} else {
			for (String key : record.toPMap().keySet()) {
				String entry = "\t\t{%s <-> %s}";
				System.out.println(String.format(entry, key, record.toPMap().get(key)));

			}
		}
		String lastLine = " \t........................................\n";
		System.out.println(lastLine);

	}

}
