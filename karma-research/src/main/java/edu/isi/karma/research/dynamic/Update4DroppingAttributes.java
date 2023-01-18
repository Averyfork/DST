package edu.isi.karma.research.dynamic;

import edu.isi.karma.modeling.alignment.SemanticModel;
import edu.isi.karma.rep.alignment.*;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import java.util.ArrayList;
import java.util.List;

import static edu.isi.karma.research.dynamic.Update4AddingAttributes.findAllEdges;

public class Update4DroppingAttributes {
    public static void recursionSemanticRemove(Node v, SemanticModel currentModel) {
        List<LabeledLink> temp = new ArrayList<>(currentModel.getGraph().incomingEdgesOf(v));
        Node linkedInternalNode;
        if (!temp.isEmpty()) {
            linkedInternalNode = temp.get(0).getSource();
            currentModel.getGraph().removeEdge(temp.get(0));
        } else {
            currentModel.getGraph().removeVertex(v);
            return;
        }

        currentModel.getGraph().removeVertex(v);
        List<LabeledLink> eOut = new ArrayList<>(currentModel.getGraph().outgoingEdgesOf(linkedInternalNode));

        int trueDegree = eOut.size();

        if (trueDegree == 0) {
            recursionSemanticRemove(linkedInternalNode, currentModel);
        } else {
            return;
        }
    }



    public static void update_remove(Node n, SemanticModel currentModel, DirectedWeightedMultigraph G) throws Exception {

        List<LabeledLink> temp = new ArrayList<LabeledLink>(currentModel.getGraph().incomingEdgesOf(n));
        Node linkedInternalNode = null;
        if (!temp.isEmpty()) {
            linkedInternalNode = temp.get(0).getSource();
            currentModel.getGraph().removeEdge(temp.get(0));
        }

        currentModel.getGraph().removeVertex(n);
        currentModel.getColumnNodes().remove((ColumnNode) n);
        currentModel.getMappingToSourceColumns().remove((ColumnNode) n);


        DirectedWeightedMultigraph currentGraph = currentModel.getGraph();
        List<LabeledLink> eIn = new ArrayList<>(currentGraph.incomingEdgesOf(linkedInternalNode));
        List<LabeledLink> eOut = new ArrayList<>(currentGraph.outgoingEdgesOf(linkedInternalNode));
        int trueDegree = 0;

        for (LabeledLink l : eOut) {
            trueDegree++;
        }
        if (trueDegree == 0) {
            currentModel.getGraph().removeVertex(linkedInternalNode);
        }
        else if (trueDegree == 1) {
            Node inNode = null;
            Node outNode = eOut.get(0).getTarget();
            if (outNode.getType() == NodeType.ColumnNode) {
                return;
            } else if (outNode.getType() == NodeType.InternalNode) {
                if (!eIn.isEmpty()) {
                    inNode = eIn.get(0).getSource();
                } else {
                    return;
                }
                double currentWeight = eOut.get(0).getWeight() + eIn.get(0).getWeight();
                DirectedWeightedMultigraph tempG = (DirectedWeightedMultigraph) currentGraph.clone();
                tempG.removeVertex(linkedInternalNode);
                if(!G.vertexSet().contains(outNode)){
                    return;
                }
                for (Object o : G.incomingEdgesOf(outNode)) {
                    DefaultLink dl = (DefaultLink) o;
                    LabeledLink l = null;
                    if(dl.getType() != LinkType.CompactObjectPropertyLink && dl.getType() != LinkType.CompactSubClassLink){
                        l = (LabeledLink) o;
                    }
                    else{
                        continue;
                    }
                    if (l.getSource() != linkedInternalNode) {
                        tempG.addVertex(l.getSource());
                        tempG.addEdge(l.getSource(), outNode, l);
                    }
                }
                ArrayList allEdges = new ArrayList<LabeledLink>(findAllEdges(inNode, outNode, tempG));
                if (!allEdges.isEmpty()) {
                    double minWeight = ((LabeledLink) allEdges.get(0)).getWeight();
                    LabeledLink minEdge = null;
                    for (Object o : allEdges) {
                        LabeledLink l = (LabeledLink) o;
                        if (l.getTarget() == outNode) {
                            int sumFrequency = 0;
                            double newWeight = l.getWeight() + (-1000) * sumFrequency;
                            if (newWeight < minWeight) {
                                minEdge = l;
                                minWeight = newWeight;
                            }
                        }
                    }
                    if (minWeight < currentWeight && minEdge != null) {
                        currentGraph.removeVertex(linkedInternalNode);
                        currentGraph.removeEdge(inNode, linkedInternalNode);
                        currentGraph.removeEdge(linkedInternalNode, outNode);
                        currentModel.getGraph().addEdge(minEdge.getSource(), minEdge.getTarget(), minEdge);
                    }
                }
            }

        } else if (trueDegree >= 2) {
            return;
        }
    }
}
