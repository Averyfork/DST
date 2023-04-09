package edu.isi.karma.research.dynamic;

import edu.isi.karma.modeling.alignment.LinkIdFactory;
import edu.isi.karma.modeling.alignment.SemanticModel;
import edu.isi.karma.rep.alignment.*;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.python.modules.struct;

import java.util.*;

/**
 * @author chl
 */
public class DynamicUpdateUtils {

    public static Node findEdgeWithoutLabel(Node inode, Node v, LabeledLink l,
                                            SemanticModel currentModel) {
        DirectedWeightedMultigraph currentGraph = currentModel.getGraph();
        for (Object n : currentGraph.vertexSet()) {
            Node nn = (Node) n;
            if (!Objects.equals(nn.getId(), inode.getId()) && nn.getUri().equals(inode.getUri())
                    && !Objects.equals(nn.getId(), v.getId())) {
                int flag = 0;
                for (Object l0 : currentGraph.edgesOf(nn)) {
                    if (Objects.equals(((LabeledLink) l0).getUri(), l.getUri())) {
                        flag = 1;
                        break;
                    }
                }
                if (flag == 0) {
                    return nn;
                }
            }
        }
        return null;
    }

    public static HashSet<LabeledLink> findAllEdges(Node n1, Node n2, DirectedWeightedMultigraph G) {
        HashSet<LabeledLink> allEdges = new HashSet<>();

        Map<Node, Integer> vis1 = new HashMap<>();
        Map<Node, Integer> vis2 = new HashMap<>();
        Map<LabeledLink, Integer> mark1 = new HashMap<>();
        Map<LabeledLink, Integer> mark2 = new HashMap<>();

        Stack<Node> mainStack = new Stack<>();
        mainStack.push(n1);
        vis1.put(n1, 1);
        while (!mainStack.empty()) {
            Node tempNode = mainStack.pop();
            vis1.put(tempNode, 1);
            Set<LabeledLink> nodeSet = G.edgesOf(tempNode);
            for (LabeledLink l : nodeSet) {
                if (!vis1.containsKey(l.getTarget())) {
                    vis1.put(l.getTarget(), 1);
                    mainStack.push(l.getTarget());
                    if (!mark1.containsKey(l)) {
                        mark1.put(l, 1);
                    }
                }
                if (!vis1.containsKey(l.getSource())) {
                    vis1.put(l.getSource(), 1);
                    mainStack.push(l.getSource());
                    if (!mark1.containsKey(l)) {
                        mark1.put(l, 0);
                    }
                }
            }
        }
        mainStack.push(n2);
        vis2.put(n2, 1);
        while (!mainStack.empty()) {
            Node tempNode = mainStack.pop();
            vis2.put(tempNode, 1);
            Set<LabeledLink> nodeSet = G.edgesOf(tempNode);
            for (LabeledLink l : nodeSet) {
                if (!vis2.containsKey(l.getSource())) {
                    vis2.put(l.getSource(), 1);
                    mainStack.push(l.getSource());
                    if (mark1.containsKey(l) && !mark2.containsKey(l) && mark1.get(l) == 1) {
                        allEdges.add(l);
                        mark2.put(l, 0);
                    }
                }
                if (!vis2.containsKey(l.getTarget())) {
                    vis2.put(l.getTarget(), 1);
                    mainStack.push(l.getTarget());
                    if (mark1.containsKey(l) && !mark2.containsKey(l) && mark1.get(l) == 0) {
                        allEdges.add(l);
                        mark2.put(l, 1);
                    }
                }
            }
        }


        return allEdges;

    }


    public static boolean judgeRepeatable(String sourceUri, String linkUri, final List<SemanticModel> trainingData) {
        boolean isAllowRepeat = false;
        int times;
        int repeatTimes = 0;
        int haveMultiLinksSM = 0;
        for (SemanticModel sm : trainingData) {
            times = 0;
            List<InternalNode> sourceNodes = new ArrayList<>();
            DirectedWeightedMultigraph trainingModel = sm.getGraph();
            for (InternalNode o : sm.getInternalNodes()) {
                if (o.getUri().equals(sourceUri)) {
                    sourceNodes.add(o);
                }
            }
            if (sourceNodes.size() >= 2) {
                haveMultiLinksSM++;
                for (InternalNode n : sourceNodes) {
                    times = 0;
                    for (Object oo : trainingModel.outgoingEdgesOf(n)) {
                        LabeledLink ll = (LabeledLink) oo;
                        if (ll.getUri().equals(linkUri)) {
                            times++;
                        }
                    }
                    if (times > 1) {
                        repeatTimes++;
                    }
                }
            }
        }
        if (2 * repeatTimes > haveMultiLinksSM) {
            isAllowRepeat = true;
        }
        return isAllowRepeat;
    }
    public static void main(String[] args){
        System.out.println("1 "+ FuzzySearch.partialRatio("Boots1331", "place"));
    }
}
