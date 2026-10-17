package com.quickdeliver.dto;

import java.util.List;

public class ZoneGraphDto {

    public static class ZoneNode {
        private Long id;
        private String zoneCode;
        private String name;
        private int availableRidersCount;
        private List<String> riderNames;

        public ZoneNode() {}

        public ZoneNode(Long id, String zoneCode, String name, int availableRidersCount, List<String> riderNames) {
            this.id = id;
            this.zoneCode = zoneCode;
            this.name = name;
            this.availableRidersCount = availableRidersCount;
            this.riderNames = riderNames;
        }

        public Long getId() { return id; }
        public String getZoneCode() { return zoneCode; }
        public String getName() { return name; }
        public int getAvailableRidersCount() { return availableRidersCount; }
        public List<String> getRiderNames() { return riderNames; }
    }

    public static class ZoneEdge {
        private String source;
        private String target;

        public ZoneEdge() {}
        public ZoneEdge(String source, String target) {
            this.source = source;
            this.target = target;
        }
        public String getSource() { return source; }
        public String getTarget() { return target; }
    }

    private List<ZoneNode> nodes;
    private List<ZoneEdge> edges;

    public ZoneGraphDto() {}

    public ZoneGraphDto(List<ZoneNode> nodes, List<ZoneEdge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<ZoneNode> getNodes() { return nodes; }
    public List<ZoneEdge> getEdges() { return edges; }
}
