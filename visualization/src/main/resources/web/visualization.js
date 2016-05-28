(function () {
    class NodeDataSet extends vis.DataSet {
        constructor() {
            super();
        }

        upsert(node) {
            if (this.get(node.id) == null) {
                this.add(node);
            } else {
                this.update({
                    'id': node.id,
                    'value': node.value,
                    'label': node.label
                });
            }
            return this;
        }
    }

    class EdgesDataSet extends vis.DataSet {
        constructor() {
            super();
        }

        upsert(edge) {
            var pEdge = this.get(edge.id);
            if (pEdge == null) {
                this.add(edge);
            } else {
                this.update({
                    'id': edge.id,
                    'value': pEdge.value + 1,
                    'label': pEdge.value + 1
                });
            }
            return this;
        }
    }

    function nodeId(name) {
        var pos = name.indexOf('#')
        return pos == -1 ? name : name.substr(0, pos);
    }

    function createFromNode(data) {
        return {
            'id': nodeId(data.sender),
            'title': nodeId(data.sender)
        }
    }

    function createToNode(data) {
        return {
            'id': nodeId(data.receiver),
            'title': nodeId(data.receiver),
            'label': data.receiverMailBoxSize,
            'value': data.receiverMailBoxSize
        }
    }

    function createEdge(nodeFrom, nodeTo) {
        return {
            'id': nodeFrom.id + '->' + nodeTo.id,
            'from': nodeFrom.id,
            'to': nodeTo.id,
            'value': 1,
            'label': 1
        }
    }

    // create an array with nodes
    var nodes = new NodeDataSet();

    // create an array with edges
    var edges = new EdgesDataSet();

    // initialize your network!
    var network = new vis.Network(
        document.getElementById('graph'),
        {
            'nodes': nodes,
            'edges': edges
        },
        {
            'edges': {
                'arrows': {
                    'to': {
                        'enabled': true,
                        'scaleFactor': 0.2
                    }
                },
                'arrowStrikethrough': false
            },
            'physics': {
                'barnesHut': {
                    'gravitationalConstant': -2000,
                    'centralGravity': 1,
                    'springLength': 45,
                },
                'minVelocity': 0.75
            }
        }
    );

    // create SSE listener
    var source = new EventSource('/api/events');
    source.addEventListener('vmm', function (event) {
        var data = JSON.parse(event.data);

        var nodeFrom = createFromNode(data);
        var nodeTo = createToNode(data);

        nodes.upsert(nodeFrom)
        nodes.upsert(nodeTo)

        edges.upsert(createEdge(nodeFrom, nodeTo));
    }, false);
})()