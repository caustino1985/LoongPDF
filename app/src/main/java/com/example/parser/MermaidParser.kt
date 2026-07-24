package com.example.parser

object MermaidParser {

    fun parse(code: String): MermaidDiagramData {
        val trimmed = code.trim()
        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return MermaidDiagramData.Generic(code)

        val firstLine = lines.first().lowercase()

        return when {
            firstLine.contains("flowchart") || firstLine.contains("graph") -> parseFlowchart(lines)
            firstLine.contains("sequencediagram") || firstLine.contains("sequence") -> parseSequence(lines)
            else -> parseFlowchart(lines) // Fallback attempt
        }
    }

    private fun parseFlowchart(lines: List<String>): MermaidDiagramData {
        val nodeMap = mutableMapOf<String, MermaidNode>()
        val edges = mutableListOf<MermaidEdge>()

        // Regex patterns for Mermaid flowchart nodes and edges
        // A[Text] or A{Text} or A((Text)) or A(Text)
        val nodeDefRegex = Regex("""^(\w+)\s*([\[\{\(]+)\s*(.*?)\s*([\]\}\)]+)$""")
        // Edge with label: A -->|Label| B or A -- Label --> B
        val edgeLabelRegex1 = Regex("""^(\w+)\s*-->\|(.*?)\|\s*(\w+)$""")
        val edgeLabelRegex2 = Regex("""^(\w+)\s*--\s*(.*?)\s*-->\s*(\w+)$""")
        // Simple edge: A --> B
        val simpleEdgeRegex = Regex("""^(\w+)\s*-->\s*(\w+)$""")

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.startsWith("%")) continue // Comment

            val edgeMatch1 = edgeLabelRegex1.matchEntire(line)
            if (edgeMatch1 != null) {
                val from = edgeMatch1.groupValues[1]
                val label = edgeMatch1.groupValues[2].trim()
                val to = edgeMatch1.groupValues[3]
                ensureNode(from, nodeMap)
                ensureNode(to, nodeMap)
                edges.add(MermaidEdge(from, to, label))
                continue
            }

            val edgeMatch2 = edgeLabelRegex2.matchEntire(line)
            if (edgeMatch2 != null) {
                val from = edgeMatch2.groupValues[1]
                val label = edgeMatch2.groupValues[2].trim()
                val to = edgeMatch2.groupValues[3]
                ensureNode(from, nodeMap)
                ensureNode(to, nodeMap)
                edges.add(MermaidEdge(from, to, label))
                continue
            }

            val simpleEdgeMatch = simpleEdgeRegex.matchEntire(line)
            if (simpleEdgeMatch != null) {
                val from = simpleEdgeMatch.groupValues[1]
                val to = simpleEdgeMatch.groupValues[2]
                ensureNode(from, nodeMap)
                ensureNode(to, nodeMap)
                edges.add(MermaidEdge(from, to, ""))
                continue
            }

            val nodeMatch = nodeDefRegex.matchEntire(line)
            if (nodeMatch != null) {
                val id = nodeMatch.groupValues[1]
                val openBrack = nodeMatch.groupValues[2]
                val label = nodeMatch.groupValues[3].trim()
                val shape = when {
                    openBrack.contains("{") -> NodeShape.DIAMOND
                    openBrack.contains("((") -> NodeShape.CIRCLE
                    openBrack.contains("(") -> NodeShape.ROUNDED
                    else -> NodeShape.RECTANGLE
                }
                nodeMap[id] = MermaidNode(id, label, shape)
            }
        }

        if (nodeMap.isEmpty() && edges.isEmpty()) {
            return MermaidDiagramData.Generic(lines.joinToString("\n"))
        }

        return MermaidDiagramData.Flowchart(
            nodes = nodeMap.values.toList(),
            edges = edges
        )
    }

    private fun parseSequence(lines: List<String>): MermaidDiagramData {
        val participants = mutableListOf<MermaidParticipant>()
        val participantIds = mutableSetOf<String>()
        val messages = mutableListOf<MermaidMessage>()

        val participantRegex = Regex("""^participant\s+(\w+)(?:\s+as\s+(.+))?$""", RegexOption.IGNORE_CASE)
        val msgRegex = Regex("""^(\w+)\s*(?:->>|->)\s*(\w+)\s*:\s*(.+)$""")

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.startsWith("%")) continue

            val pMatch = participantRegex.matchEntire(line)
            if (pMatch != null) {
                val id = pMatch.groupValues[1]
                val name = pMatch.groupValues[2].ifEmpty { id }
                if (!participantIds.contains(id)) {
                    participantIds.add(id)
                    participants.add(MermaidParticipant(id, name))
                }
                continue
            }

            val mMatch = msgRegex.matchEntire(line)
            if (mMatch != null) {
                val from = mMatch.groupValues[1]
                val to = mMatch.groupValues[2]
                val text = mMatch.groupValues[3].trim()

                if (!participantIds.contains(from)) {
                    participantIds.add(from)
                    participants.add(MermaidParticipant(from, from))
                }
                if (!participantIds.contains(to)) {
                    participantIds.add(to)
                    participants.add(MermaidParticipant(to, to))
                }
                messages.add(MermaidMessage(from, to, text))
            }
        }

        return MermaidDiagramData.Sequence(participants, messages)
    }

    private fun ensureNode(id: String, nodeMap: MutableMap<String, MermaidNode>) {
        if (!nodeMap.containsKey(id)) {
            nodeMap[id] = MermaidNode(id = id, label = id, shape = NodeShape.RECTANGLE)
        }
    }
}
