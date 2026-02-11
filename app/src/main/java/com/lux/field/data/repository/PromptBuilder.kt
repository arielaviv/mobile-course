package com.lux.field.data.repository

import com.lux.field.domain.model.Task

/**
 * Builds all prompts for the Claude FTTH AI agent.
 */
object PromptBuilder {

    fun buildSystemPrompt(task: Task?): String {
        val base = """
            |You are an expert FTTH (Fiber to the Home) field technician assistant for Lux Field.
            |You help technicians complete fiber optic installation, maintenance, and testing tasks on-site.
            |
            |Response rules:
            |• Keep answers short — the technician reads on a phone screen
            |• Use bullet points for steps or lists
            |• Lead with the most critical info first
            |• If a safety hazard is mentioned or implied, address it IMMEDIATELY before anything else
            |
            |Core expertise:
            |• Fusion splicing — fiber preparation, cleave angle, arc settings, splice protection
            |• OTDR testing — trace interpretation, event identification, loss measurement
            |• Cable pulling — tension limits, bend radius, lubricant, service loops
            |• ONT installation — mounting, patching, LED verification, optical power levels
            |• Duct and conduit work — installation, couplers, sealing, pressure testing
            |• Connectors — cleaning, inspection, polishing, return loss
            |• Safety — laser safety, fiber shards, confined spaces, working at height, PPE
            |
            |Quality reference values:
            |• Fusion splice loss: < 0.1 dB (re-splice if > 0.15 dB)
            |• Mechanical splice loss: < 0.3 dB
            |• Connector loss: < 0.5 dB
            |• ONT optical power: acceptable range -8 dBm to -28 dBm
            |• Bend radius minimum: 15 mm for drop cable, 20× outer diameter for feeder cable
            |• OTDR dead zone: account for launch fiber length
        """.trimMargin()

        val taskContext = task?.let { buildTaskContext(it) } ?: ""

        return if (taskContext.isNotEmpty()) "$base\n\n$taskContext" else base
    }

    fun buildWorkPhotoPrompt(task: Task?): String {
        val typeBlock = task?.let { buildPhotoInspectionCriteria(it.type) } ?: ""

        return buildString {
            appendLine("Analyze this FTTH work photo. Provide a structured quality assessment.")
            appendLine()
            appendLine("Format your response as:")
            appendLine("**Overall: [Pass | Needs Attention | Fail]**")
            appendLine()
            appendLine("Then list observations as bullet points. Rate each item: GOOD / CONCERN / ISSUE")
            appendLine()
            appendLine("Always check:")
            appendLine("• Fiber bend radius (min 15 mm for drop cable)")
            appendLine("• Cable routing and organization")
            appendLine("• Connection security")
            appendLine("• Labeling and documentation visible")
            appendLine("• Work area cleanliness")
            if (typeBlock.isNotEmpty()) {
                appendLine()
                appendLine("Task-specific criteria:")
                appendLine(typeBlock)
            }
            appendLine()
            appendLine("Safety check: scan for any visible hazards (exposed fiber shards, tripping hazards, missing PPE, laser sources).")
            appendLine()
            appendLine("Keep response under 150 words.")
        }.trim()
    }

    fun buildPresencePhotoPrompt(): String = buildString {
        appendLine("This is a proof-of-presence selfie from a field technician at an FTTH job site.")
        appendLine()
        appendLine("Provide a structured verification:")
        appendLine("• **Technician visible**: Yes / No")
        appendLine("• **Environment**: brief description of surroundings")
        appendLine("• **PPE check**: note any visible safety gear (hard hat, safety vest, glasses) or absence")
        appendLine("• **On-site confidence**: HIGH / MEDIUM / LOW")
        appendLine()
        appendLine("Keep response under 80 words.")
    }.trim()

    private fun buildTaskContext(task: Task): String {
        val completedSteps = task.steps.count { it.isCompleted }
        val totalSteps = task.steps.size

        val header = """
            |Current task context:
            |• Task: ${task.label}
            |• Type: ${task.type}
            |• Description: ${task.description}
            |• Status: ${task.status.name.lowercase().replace('_', ' ')}
            |• Progress: $completedSteps / $totalSteps steps completed
        """.trimMargin()

        val typeGuidance = buildTypeGuidance(task)

        return if (typeGuidance.isNotEmpty()) "$header\n\n$typeGuidance" else header
    }

    private fun buildTypeGuidance(task: Task): String = when (task.type) {
        "splice" -> buildString {
            appendLine("Splice task guidance:")
            task.spliceDetail?.let { detail ->
                appendLine("• Fiber count: ${detail.fiberCount}")
                appendLine("• Splice type: ${detail.spliceType}")
                detail.enclosureId?.let { appendLine("• Enclosure: $it") }
            }
            appendLine("• Follow color code sequence: blue, orange, green, brown, slate, white (repeat for fibers 7-12)")
            appendLine("• Target splice loss: < 0.1 dB per splice")
            appendLine("• Organize fibers neatly in splice tray — no crossovers")
            appendLine("• Apply heat shrink protector to every splice before placing in tray")
        }.trim()

        "cable_pull" -> buildString {
            appendLine("Cable pull guidance:")
            task.cablePullDetail?.let { detail ->
                appendLine("• Cable type: ${detail.cableType}")
                appendLine("• Length: ${detail.lengthMeters} m")
                appendLine("• Route: ${detail.startPoint} → ${detail.endPoint}")
            }
            appendLine("• Maintain minimum bend radius at all times")
            appendLine("• Use cable lubricant for conduit runs over 30 m")
            appendLine("• Do not exceed rated tensile load")
            appendLine("• Leave 1 m service loop at each termination point")
        }.trim()

        "ont_install" -> buildString {
            appendLine("ONT installation guidance:")
            appendLine("• Mount at eye level near a power outlet")
            appendLine("• Handle fiber patch cord with care — optical connectors are fragile")
            appendLine("• After powering on, verify LED status:")
            appendLine("  - PON: solid green = registered")
            appendLine("  - LOS: off = signal OK; red = no signal (check fiber)")
            appendLine("  - POWER: solid green")
            appendLine("• Measure optical power — acceptable range: -8 dBm to -28 dBm")
            appendLine("• If power < -28 dBm, check connectors and upstream splices")
        }.trim()

        "testing" -> buildString {
            appendLine("Testing guidance:")
            appendLine("• OTDR settings: use 1310 nm and 1550 nm wavelengths")
            appendLine("• Set range to 2× expected fiber length")
            appendLine("• Use launch fiber to clear near-end dead zone")
            appendLine("• Check loss budget: total link loss should be within design spec")
            appendLine("• Any splice showing > 0.15 dB — flag for re-splice")
            appendLine("• Record all measurements for documentation")
        }.trim()

        "duct_install" -> buildString {
            appendLine("Duct installation guidance:")
            task.cablePullDetail?.let { detail ->
                appendLine("• Duct type: ${detail.cableType}")
                appendLine("• Length: ${detail.lengthMeters} m")
                appendLine("• Route: ${detail.startPoint} → ${detail.endPoint}")
            }
            appendLine("• Inspect duct for kinks or damage before installation")
            appendLine("• Use approved couplers at all joints — ensure click/lock engagement")
            appendLine("• Seal both ends immediately after installation to prevent debris")
            appendLine("• Run mandrel test to verify duct integrity")
            appendLine("• Perform air pressure test: 100 kPa, hold 10 min, no drop")
        }.trim()

        "site_survey" -> buildString {
            appendLine("Site survey guidance:")
            appendLine("• Check and photograph: access points, conduit routes, existing infrastructure")
            appendLine("• Note any obstructions, damage, or deviations from plan")
            appendLine("• Verify space availability in enclosures / manholes")
            appendLine("• Record GPS coordinates at key points")
            appendLine("• Look for potential hazards: traffic, confined spaces, overhead lines")
        }.trim()

        "documentation" -> buildString {
            appendLine("Documentation guidance:")
            appendLine("• Photograph: every connection point, cable route, completed installation")
            appendLine("• Include labels and identifiers in frame")
            appendLine("• Capture LED status on active equipment")
            appendLine("• Take wide shot + close-up for each area")
            appendLine("• Ensure photos are well-lit and in focus")
        }.trim()

        else -> ""
    }

    private fun buildPhotoInspectionCriteria(taskType: String): String = when (taskType) {
        "splice" -> buildString {
            appendLine("• Splice tray organization (fibers routed neatly, no crossovers)")
            appendLine("• Heat shrink protectors applied to all splices")
            appendLine("• Color code sequence maintained")
            appendLine("• Tray seated properly in enclosure")
        }.trim()

        "cable_pull" -> buildString {
            appendLine("• Bend radius compliance (no kinks or sharp bends)")
            appendLine("• Cable secured with clips at correct intervals")
            appendLine("• Service loops present at termination points")
            appendLine("• Cable jacket undamaged")
        }.trim()

        "ont_install" -> buildString {
            appendLine("• ONT mounted level and secure")
            appendLine("• Fiber patch cord routed neatly (no strain on connector)")
            appendLine("• Power cable managed properly")
            appendLine("• LED status visible and correct")
        }.trim()

        "testing" -> buildString {
            appendLine("• Test equipment properly connected")
            appendLine("• Launch fiber in use")
            appendLine("• Screen readings legible in photo")
        }.trim()

        "duct_install" -> buildString {
            appendLine("• Duct entry sealed properly")
            appendLine("• Couplers fully engaged at joints")
            appendLine("• No visible kinks or crush points")
            appendLine("• End caps in place")
        }.trim()

        "site_survey", "documentation" -> buildString {
            appendLine("• Subject clearly visible and in focus")
            appendLine("• Labels and identifiers readable")
            appendLine("• Adequate lighting")
        }.trim()

        else -> ""
    }
}
