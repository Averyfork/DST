package edu.isi.karma.research.dynamic;


import edu.isi.karma.modeling.alignment.LinkIdFactory;
import edu.isi.karma.modeling.alignment.SemanticModel;
import edu.isi.karma.rep.alignment.*;
import org.jgrapht.graph.DirectedWeightedMultigraph;

import java.util.*;

import static edu.isi.karma.research.dynamic.DynamicUpdate_SemanticModels.cloneNode;
import static edu.isi.karma.research.dynamic.SelectSemanticTypes.*;


/**
 * @author chl, wwm
 */
public class Update4AddingAttributes {

    public static Node findEdgeWithoutLabel(Node inode, Node v, LabeledLink l,
                                            SemanticModel currentModel,
                                            DirectedWeightedMultigraph G) {
        DirectedWeightedMultigraph currentGraph = currentModel.getGraph();
        for (Object n : currentGraph.vertexSet()) {
            Node nn = (Node) n;
            if (!Objects.equals(nn.getId(), inode.getId()) &&
                    nn.getUri().equals(inode.getUri()) &&
                    !Objects.equals(nn.getId(), v.getId())) {
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

    public static void operation4AddingAtt(Node u, SemanticModel currentModel, DirectedWeightedMultigraph G,
                                           final List<SemanticModel> trainingData,
                                           DynamicUpdate_SemanticModels modelLearner,
                                           Map<String, String> extractSemanticTypes,
                                           int numberOfCandidates) throws Exception {
        HashSet<LabeledLink> temp_U = new HashSet<>();
        List<InternalNode> currentInternalNodes = new ArrayList<>(currentModel.getInternalNodes());
        DirectedWeightedMultigraph currentGraph = currentModel.getGraph();
        String targetAttributeId = u.getId();
        Node v = null;
        LabeledLink ll = null;
        List<LabeledLink> eu = new ArrayList<>();

        if (numberOfCandidates == 1) {
            eu = new ArrayList<>(G.incomingEdgesOf(u));
            if (eu.size() == 0) {
                return;
            }
        } else if (numberOfCandidates == 4) {
            List<LabeledLink> teu = new ArrayList<>(G.incomingEdgesOf(u));
            eu = new ArrayList<>();
            if (teu.isEmpty()) {
                return;
            }
            String chosenClassNodeUri = extractSemanticTypes.get(targetAttributeId);
            for (LabeledLink l : teu) {
                if ((l.getSource().getUri() + l.getUri()).equals(chosenClassNodeUri)) {
                    eu.add(l);
                }
            }
        }

        String linkedInternalNodeUri = eu.get(0).getSource().getUri();
        String linkUri = eu.get(0).getUri();

        boolean isAllowRepeat = false;
        if (eu.size() == 1) {

            ll = eu.get(0);
            v = ll.getSource();
            if (!isAllowRepeat) {

                if (currentGraph.containsVertex(v)) {
                    for (Object l : currentGraph.edgesOf(v)) {
                        LabeledLink tl = (LabeledLink) l;
                        if (Objects.equals(tl.getUri(), ll.getUri())) {
                            int mark = 0;
                            for (Object n : G.vertexSet()) {
                                Node nn = (Node) n;
                                if (!Objects.equals(nn.getId(), v.getId()) &&
                                        nn.getUri().equals(v.getUri())) {

                                    int flag = 0;
                                    if (currentGraph.containsVertex(nn)) {
                                        for (Object l0 : currentGraph.edgesOf(nn)) {
                                            if (Objects.equals(((LabeledLink) l0).getUri(), ll.getUri())) {
                                                flag = 1;
                                                break;
                                            }
                                        }
                                    }
                                    if (flag == 0) {
                                        mark = 1;
                                        v = nn;
                                        String linkId = LinkIdFactory.getLinkId(ll.getUri(), v.getId(), u.getId());
                                        LabeledLink link = new DataPropertyLink(linkId, new Label(ll.getUri()));
                                        G.addEdge(v, u, link);
                                        ll = link;
                                        break;
                                    }
                                }
                                if (mark == 1) {
                                    break;
                                }
                            }
                            if (mark == 0) {
                                v = cloneNode(v, currentModel, modelLearner, G);
                                String linkId = LinkIdFactory.getLinkId(ll.getUri(), v.getId(), u.getId());
                                LabeledLink link = new DataPropertyLink(linkId, new Label(ll.getUri()));
                                G.addEdge(v, u, link);
                                ll = link;
                                break;
                            }
                        }
                    }
                }
            }
        } else if (eu.size() > 1) {

            final String targetNodeName = ((ColumnNode) u).getColumnName().toLowerCase();
            eu.sort(new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {

                    String s1 = ((LabeledLink) o1).getSource().getId();
                    String s2 = ((LabeledLink) o2).getSource().getId();
                    int coherence1 = 0;
                    int coherence2 = 0;
                    coherence1 = ((LabeledLink) o1).getSource().getModelIds().size();
                    coherence2 = ((LabeledLink) o2).getSource().getModelIds().size();
                    if (coherence1 == 0 && coherence2 == 0) {
                        //前面modelId从大到小，但是判断不出来的时候，直接看标号则是从小到大，因此我们取负数
                        coherence1 = (int) (((LabeledLink) o1).getSource().getId().charAt(((LabeledLink) o1).
                                getSource().getId().length() - 1)) - (int) ('0');
                        coherence2 = (int) (((LabeledLink) o2).getSource().getId().charAt(((LabeledLink) o2).
                                getSource().getId().length() - 1)) - (int) ('0');
                        coherence1 *= (-1);
                        coherence2 *= (-1);
                    }
                    if (coherence1 != coherence2) {
                        return coherence1 < coherence2 ? 1 : -1;
                    } else {
                        return 0;
                    }
                }
            });
            if (!eu.isEmpty()) {
                boolean isInternalNodeEnough = false;
                for (LabeledLink l : eu) {
                    boolean isHaveLink = false;
                    String s = l.getUri();
                    String NodeId = l.getSource().getId();
                    Node sourceNode = null;
                    for (Node n : currentInternalNodes) {
                        if (n.getId().equals(NodeId)) {
                            sourceNode = n;
                            break;
                        }
                    }
                    if (sourceNode != null) {
                        for (LabeledLink currentNodeLink : currentModel.getGraph().outgoingEdgesOf(sourceNode)) {
                            if (currentNodeLink instanceof DataPropertyLink) {
                                String ss = currentNodeLink.getUri();
                                if (ss.equals(s)) {
                                    isHaveLink = true;
                                    break;
                                }
                            }
                        }
                        if (!isHaveLink) {
                            isInternalNodeEnough = true;
                            ll = l;
                            v = l.getSource();
                            break;
                        }
                    } else {
                        isInternalNodeEnough = true;
                        ll = l;
                        v = l.getSource();
                        break;
                    }
                }
                if (!isInternalNodeEnough && isAllowRepeat) {
                    for (LabeledLink l : eu) {
                        boolean isHaveLink = false;
                        String s = l.getUri() + ((ColumnNode) u).getColumnName();
                        String NodeId = l.getSource().getId();
                        Node sourceNode = null;
                        for (Node n : currentInternalNodes) {
                            if (n.getId().equals(NodeId)) {
                                sourceNode = n;
                                break;
                            }
                        }
                        if (sourceNode != null) {
                            for (LabeledLink currentNodeLink : currentModel.getGraph().outgoingEdgesOf(sourceNode)) {
                                if (currentNodeLink instanceof DataPropertyLink) {
                                    String ss = currentNodeLink.getUri() + ((ColumnNode) currentNodeLink.getTarget()).
                                            getColumnName();
                                    if (ss.equals(s)) {
                                        isHaveLink = true;
                                        break;
                                    }
                                }
                            }
                            if (!isHaveLink) {
                                isInternalNodeEnough = true;
                                ll = l;
                                v = l.getSource();
                                break;
                            }
                        } else {
                            isInternalNodeEnough = true;
                            ll = l;
                            v = l.getSource();
                            break;
                        }
                    }
                }
                if (!isInternalNodeEnough) {
                    List<String> sameIdFromG = new ArrayList<>();
                    for (LabeledLink euLabeledLink : eu) {
                        sameIdFromG.add(euLabeledLink.getSource().getId());
                    }
                    for (InternalNode in : currentInternalNodes) {
                        if (in.getUri().equals(linkedInternalNodeUri)) {
                            if (!sameIdFromG.contains(in.getId())) {
                                boolean isAvailable = true;
                                for (Object ino : currentGraph.outgoingEdgesOf(in)) {
                                    LabeledLink inLabeledLink = (LabeledLink) ino;
                                    if (inLabeledLink.getUri().equals(linkUri)) {
                                        isAvailable = false;
                                        break;
                                    }
                                }
                                if (isAvailable) {
                                    isInternalNodeEnough = true;
                                    v = in;
                                    String linkId = LinkIdFactory.getLinkId(linkUri, v.getId(), u.getId());
                                    ll = new DataPropertyLink(linkId, new Label(linkUri));
                                }
                            }
                        }
                    }
                    if (!isInternalNodeEnough) {
                        v = cloneNode(eu.get(0).getSource(), currentModel, modelLearner, G);
                        String linkId = LinkIdFactory.getLinkId(linkUri, v.getId(), u.getId());
                        LabeledLink link = new DataPropertyLink(linkId, new Label(linkUri));
                        G.addEdge(v, u, link);
                        ll = link;
                    }
                }
            }
        }
        if (!currentGraph.containsVertex(v)) {
            currentGraph.addVertex(v);
            Set<DefaultLink> e = G.edgesOf(v);
            for (DefaultLink l : e) {
                if ((currentGraph.containsVertex(l.getSource()) &&
                        currentGraph.containsVertex(l.getTarget())) &&
                        l.getType() == LinkType.ObjectPropertyLink) {
                    temp_U.add((LabeledLink) l);
                }
            }
            if (temp_U.isEmpty()) {
                currentGraph.removeVertex(v);
                HashMap<Node, Integer> vis = new HashMap<Node, Integer>();
                HashMap<Node, Double> dis = new HashMap<Node, Double>();
                HashMap<Node, LabeledLink> preLink = new HashMap<Node, LabeledLink>();
                HashMap<Node, Node> preNode = new HashMap<Node, Node>();
                class Edge implements Comparable<Edge> {
                    int w;
                    Node to;

                    Edge() {
                        to = null;
                        w = 0;
                    }

                    Edge(Node m_to, int m_w) {
                        to = m_to;
                        w = m_w;
                    }

                    @Override
                    public int compareTo(Edge obj) {
                        return this.w - obj.w;
                    }
                }

                Queue<Edge> pq = new PriorityQueue<>();
                pq.add(new Edge(v, 0));
                dis.put(v, (double) 0);
                while (!pq.isEmpty()) {
                    Node to = pq.peek().to;
                    int d = pq.peek().w;
                    pq.poll();
                    if (vis.containsKey(to)) {
                        continue;
                    }
                    vis.put(to, 1);
                    if (currentModel.getGraph().vertexSet().contains(to)) {
                        Node inode = to;
                        LabeledLink minEdge = preLink.get(to);
                        boolean has_same_label = false;
                        for (Object l : currentGraph.edgesOf(inode)) {
                            if (((LabeledLink) l).getUri().equals(minEdge.getUri())) {
                                has_same_label = true;
                                break;
                            }
                        }
                        if (has_same_label) {
                            Node tempNode = findEdgeWithoutLabel(inode, preNode.get(to), minEdge, currentModel, G);
                            if (tempNode != null) {
                                inode = tempNode;
                                String linkId = LinkIdFactory.getLinkId(minEdge.getUri(), inode.getId(),
                                        preNode.get(to).getId());
                                LabeledLink link = new DataPropertyLink(linkId, new Label(minEdge.getUri()));
                                G.addEdge(inode, preNode.get(to), link);
                                preLink.put(inode, link);
                                preNode.put(inode, preNode.get(to));
                            }
                        }
                        to = inode;
                        while (to != v) {
                            currentGraph.addVertex(preNode.get(to));
                            currentGraph.addEdge(preLink.get(to).getSource(), preLink.get(to).getTarget(), preLink.get(to));
                            to = preNode.get(to);
                        }
                        break;
                    }
                    for (Object l : G.edgesOf(to)) {
                        DefaultLink dl = (DefaultLink) l;
                        LabeledLink tl;
                        if (dl.getType() != LinkType.CompactObjectPropertyLink &&
                                dl.getType() != LinkType.CompactSubClassLink) {
                            tl = (LabeledLink) dl;
                        } else {
                            continue;
                        }
                        if (!vis.containsKey(tl.getTarget()) && (!dis.containsKey(tl.getTarget()) ||
                                dis.get(tl.getTarget()) > d + tl.getWeight() + 10000) &&
                                !G.outgoingEdgesOf(tl.getTarget()).isEmpty()) {
                            dis.put(tl.getTarget(), d + tl.getWeight() + 10000);
                            pq.add(new Edge(tl.getTarget(), (int) (d + tl.getWeight() + 10000)));
                            preNode.put(tl.getTarget(), to);
                            preLink.put(tl.getTarget(), tl);
                        }
                        if (!vis.containsKey(tl.getSource()) && (!dis.containsKey(tl.getSource()) ||
                                dis.get(tl.getSource()) > d + tl.getWeight() + 10000) &&
                                !G.outgoingEdgesOf(tl.getSource()).isEmpty()) {
                            dis.put(tl.getSource(), d + tl.getWeight() + 10000);
                            pq.add(new Edge(tl.getSource(), (int) (d + tl.getWeight() + 10000)));
                            preNode.put(tl.getSource(), to);
                            preLink.put(tl.getSource(), tl);
                        }
                    }
                }
                pq.clear();
                if (!currentGraph.containsVertex(v)) {
                    currentGraph.addVertex(v);
                }
            }
            if (!temp_U.isEmpty()) {
                List<LabeledLink> U = new ArrayList<LabeledLink>(temp_U);
                U.sort(new Comparator() {

                    @Override
                    public int compare(Object o1, Object o2) {
                        int coherence1;
                        int coherence2;
                        coherence1 = ((LabeledLink) o1).getSource().getModelIds().size();
                        coherence2 = ((LabeledLink) o2).getSource().getModelIds().size();
                        if (coherence1 == coherence2) {
                            coherence1 = (int) (((LabeledLink) o1).getSource().getId().charAt(((LabeledLink) o1).
                                    getSource().getId().length() - 1)) - (int) ('0');
                            coherence2 = (int) (((LabeledLink) o2).getSource().getId().charAt(((LabeledLink) o2).
                                    getSource().getId().length() - 1)) - (int) ('0');
                            coherence1 *= (-1);
                            coherence2 *= (-1);
                        }
                        if (coherence1 != coherence2) {
                            return coherence1 < coherence2 ? 1 : -1;
                        } else {
                            return 0;
                        }
                    }
                });

                int chosenIn = 0;
                Node inode;
                LabeledLink minEdge = U.get(chosenIn);
                boolean isFind = false;
                for (; chosenIn < U.size(); chosenIn++) {
                    minEdge = U.get(chosenIn);
                    boolean has_same_label = false;
                    if (v == minEdge.getSource()) {
                        inode = minEdge.getTarget();
                    } else {
                        inode = minEdge.getSource();
                    }
                    for (Object l : currentGraph.edgesOf(inode)) {
                        if (((LabeledLink) l).getUri().equals(U.get(chosenIn).getUri())) {
                            has_same_label = true;
                            break;
                        }
                    }
                    if (!has_same_label) {
                        currentGraph.addEdge(minEdge.getSource(), minEdge.getTarget(), minEdge);
                        isFind = true;
                        break;
                    }
                }
                if (!isFind) {
                    currentGraph.addEdge(U.get(0).getSource(), U.get(0).getTarget(), U.get(0));
                }
                for (int i = chosenIn; i < U.size(); i++) {
                    minEdge = U.get(i);
                    Node sourceNode = minEdge.getSource();
                    Node targetNode = minEdge.getTarget();
                    HashSet<LabeledLink> allEdges = findAllEdges(sourceNode, targetNode, currentGraph);
                    if (!allEdges.isEmpty()) {
                        List<LabeledLink> E = new ArrayList<LabeledLink>(allEdges);
                        LabeledLink maxWeightEdge = E.get(0);
                        for (LabeledLink l : E) {
                            if (l.getWeight() > maxWeightEdge.getWeight()) {
                                maxWeightEdge = l;
                            }
                        }
                        if (maxWeightEdge.getWeight() > minEdge.getWeight()) {
                            currentGraph.removeEdge(maxWeightEdge);
                            currentGraph.addEdge(minEdge.getSource(), minEdge.getTarget(), minEdge);
                        }
                    }
                }
            }
        }
        currentGraph.addVertex(u);
        currentModel.getColumnNodes().add((ColumnNode) u);
        currentModel.getMappingToSourceColumns().put((ColumnNode) u, (ColumnNode) u);
        currentGraph.addEdge(v, u, ll);
    }


    public static List<HashMap<String, String>> addingAmbiguousNodes(SemanticModel currentModel,
                                                                     HashSet<ColumnNode> ambiguousNodes) {
        //根据启发式信息对不确定type的点进行判断，挑选出某个组合
        List<HashMap<String, String>> resultMapList = new ArrayList<>();
        for (ColumnNode cn : ambiguousNodes) {
            List<SemanticType> semanticTypes4CN = cn.getTopKLearnedSemanticTypes(4);
        }
        return resultMapList;
    }


    public static void update4AddingAttributes(List<ColumnNode> addList, SemanticModel currentModel,
                                               DirectedWeightedMultigraph G,
                                               final List<SemanticModel> trainingData,
                                               DynamicUpdate_SemanticModels modelLearner,
                                               int numberOfCandidates,
                                               boolean useCorrectType) {
        HashMap<String, String> certainedSemanticTypes = new HashMap<>();
        HashMap<String, String> uncertainedSemanticTypes = new HashMap<>();
        HashSet<ColumnNode> ambiguousNodes = new HashSet<>();
        if (!useCorrectType) {
            if (numberOfCandidates == 1) {

            } else if (numberOfCandidates > 1) {
//                SelectSemanticTypes sst = getSemanticTypes1(addList, currentModel, G, trainingData, numberOfCandidates,
//                        certainedSemanticTypes);
                certainedSemanticTypes = getSemanticTypes(addList, currentModel, G, trainingData, numberOfCandidates);
            }
        }
        for (Object o : addList) {
            ColumnNode n = (ColumnNode) o;
            try {
                if (!ambiguousNodes.contains(n)) {
                    operation4AddingAtt(n, currentModel, G, trainingData, modelLearner, certainedSemanticTypes, numberOfCandidates);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!ambiguousNodes.isEmpty()) {
            List<HashMap<String, String>> semanticTypesCombinations = new ArrayList<>();

        }
    }
}
