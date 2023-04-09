package edu.isi.karma.research.dynamic;

import edu.isi.karma.modeling.alignment.SemanticModel;
import edu.isi.karma.rep.alignment.InternalNode;
import edu.isi.karma.rep.alignment.LabeledLink;
import edu.isi.karma.rep.alignment.LinkType;
import edu.isi.karma.rep.alignment.Node;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * @author chl
 */
public class ModelRefinement {

    public static void modelCleaning(SemanticModel currentModel, DirectedWeightedMultigraph G) throws Exception {
        List<InternalNode> ins = new ArrayList(currentModel.getInternalNodes());
        DirectedWeightedMultigraph currentGraph = currentModel.getGraph();
        List<Node> cns = new ArrayList<Node>(currentGraph.vertexSet());
        List<Node> nsFromG = new ArrayList<Node>(G.vertexSet());
        Queue<InternalNode> clean1Node = new PriorityQueue<>((n1, n2) -> {
            int s1 = currentModel.getGraph().incomingEdgesOf(n1).size();
            int s2 = currentModel.getGraph().incomingEdgesOf(n2).size();
            return s1-s2;
        });
        Queue<InternalNode> clean2Node = new PriorityQueue<>((n1,n2) -> {
            int s1 = currentModel.getGraph().outgoingEdgesOf(n1).size();
            int s2 = currentModel.getGraph().outgoingEdgesOf(n2).size();
            return s1-s2;
        });
        for(InternalNode in : ins){
            clean1Node.add(in);
            clean2Node.add(in);
        }
        for (InternalNode cur : clean1Node) {
            List<LabeledLink> curLink = new ArrayList(currentModel.getGraph().outgoingEdgesOf(cur));
            if(currentModel.getGraph().incomingEdgesOf(cur).size() == 0 && curLink.size() == 1) {
                if (curLink.get(0).getType() == LinkType.ObjectPropertyLink) {
                    currentModel.getGraph().removeVertex(cur);
                    clean2Node.remove(cur);
                }
            }
        }
        for (InternalNode cur : clean2Node) {
            List<LabeledLink> curLink = new ArrayList(currentModel.getGraph().outgoingEdgesOf(cur));
            if(curLink.size() == 0) {
                currentModel.getGraph().removeVertex(cur);
            }
        }
    }
}
