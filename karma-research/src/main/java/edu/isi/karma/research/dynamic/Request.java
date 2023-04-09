package edu.isi.karma.research.dynamic;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import edu.isi.karma.modeling.alignment.SemanticModel;
import edu.isi.karma.rep.alignment.ColumnNode;
import edu.isi.karma.rep.alignment.LabeledLink;
import edu.isi.karma.rep.alignment.Node;
import edu.isi.karma.rep.alignment.NodeType;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chl
 */
public class Request {


    private final Multimap<String, ColumnNode> request_list;

    Request(Multimap<String, ColumnNode> R) {
        request_list = R;
    }

    public Multimap<String, ColumnNode> getRequest_list() {
        return this.request_list;
    }


    public static Request getRequestList(SemanticModel currentModel, SemanticModel correctModel,
                                         DirectedWeightedMultigraph G) {
        Multimap<String, ColumnNode> example = ArrayListMultimap.create();
        List<String> currentNodeName = new ArrayList<>();
        List<String> correctNodeName = new ArrayList<>();
        for (ColumnNode cn : currentModel.getColumnNodes()) {
            for (ColumnNode cng : correctModel.getColumnNodes()) {
                String s1 = cn.getColumnName().toLowerCase().replaceAll("\\s*", "").
                        replaceAll("_", "");
                String s2 = cng.getColumnName().toLowerCase().replaceAll("\\s*", "").
                        replaceAll("_", "");
                if (s1.contains(s2) || s2.contains(s1)) {
                    List<LabeledLink> cnLabeledLink = new ArrayList<>
                            (currentModel.getGraph().incomingEdgesOf(cn));
                    List<LabeledLink> cnGLabeledLink = new ArrayList<>
                            (correctModel.getGraph().incomingEdgesOf(cng));
                    Node cnLinkedInternalNode = cnLabeledLink.get(0).getSource();
                    if (cnLabeledLink.get(0).getUri().equals(cnGLabeledLink.get(0).getUri()) && cnLinkedInternalNode.getUri().
                            equals(cnGLabeledLink.get(0).getSource().getUri())) {
                        currentModel.getGraph().removeVertex(cn);
                        currentModel.getColumnNodes().remove(cn);
                        currentModel.getMappingToSourceColumns().remove(cn);
                        currentModel.getGraph().addVertex(cng);
                        currentModel.getColumnNodes().add(cng);
                        currentModel.getMappingToSourceColumns().put(cng, cng);
                        if (currentModel.getGraph().edgesOf(cng).isEmpty()) {
                            currentModel.getGraph().addEdge(cnLinkedInternalNode, cng, cnLabeledLink.get(0));
                        }
                        break;
                    }
                }
            }
        }
        for (Node n : currentModel.getGraph().vertexSet()) {
            if (n.getType() == NodeType.ColumnNode) {
                ColumnNode cn = (ColumnNode) n;
                currentNodeName.add(cn.getUserSemanticTypes().get(0).getModelLabelString() + cn.getColumnName());
            }

        }
        for (Node n : correctModel.getGraph().vertexSet()) {
            if (n.getType() == NodeType.ColumnNode) {
                ColumnNode cn = (ColumnNode) n;
                correctNodeName.add(cn.getUserSemanticTypes().get(0).getModelLabelString() + cn.getColumnName());
            }
        }
        for (Node n : correctModel.getGraph().vertexSet()) {
            if (n.getType() == NodeType.ColumnNode) {
                ColumnNode cn = (ColumnNode) n;
                if (!currentNodeName.contains(cn.getUserSemanticTypes().get(0).
                        getModelLabelString() + cn.getColumnName())) {
                    for (Object go : G.vertexSet()) {
                        if(((Node)go).getType() == NodeType.ColumnNode){
                            ColumnNode gn = (ColumnNode) go;
                            if (gn.getId().equals(n.getId())) {
                                example.put("add", gn);
                            }
                        }

                    }
                }
            }
        }
        for (Node n : currentModel.getGraph().vertexSet()) {
            if (n.getType() == NodeType.ColumnNode) {
                ColumnNode cn = (ColumnNode) n;
                if (!correctNodeName.contains(cn.getUserSemanticTypes().get(0).
                        getModelLabelString() + cn.getColumnName())) {
                    example.put("remove", cn);
                }
            }
        }
        return new Request(example);
    }
}
