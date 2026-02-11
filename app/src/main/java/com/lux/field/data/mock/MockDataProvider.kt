package com.lux.field.data.mock

import com.lux.field.domain.model.CablePullDetail
import com.lux.field.domain.model.CrewMember
import com.lux.field.domain.model.SpliceDetail
import com.lux.field.domain.model.Task
import com.lux.field.domain.model.TaskStatus
import com.lux.field.domain.model.TaskStep
import com.lux.field.domain.model.WorkOrder
import com.lux.field.domain.model.WorkOrderAssignment
import com.lux.field.domain.model.WorkOrderLocation
import com.lux.field.domain.model.WorkOrderStatus
import com.lux.field.domain.model.WorkOrderType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockDataProvider @Inject constructor() {

    private val mockUser = CrewMember(
        id = "crew-001",
        name = "David Cohen",
        phone = "+972501234567",
        role = "lead_tech",
        crewId = "alpha-crew-1",
    )

    fun mockLogin(phone: String, code: String): CrewMember = mockUser

    fun getWorkOrders(): List<WorkOrder> = listOf(
        createDropInstallWO(),
        createSpliceWO(),
        createDuctInstallWO(),
    )

    fun getWorkOrderDetail(id: String): WorkOrder? =
        getWorkOrders().find { it.id == id }

    fun updateTaskStatus(taskId: String, status: TaskStatus) {
        // no-op for mock — status is updated locally via Room
    }

    fun getMockChatResponse(userMessage: String): String {
        val lower = userMessage.lowercase()
        return when {
            "splice" in lower || "fusion" in lower ->
                "For fusion splicing, ensure the fiber ends are clean-cleaved at 90°. " +
                    "Target splice loss should be under 0.1 dB. Use the heat shrink protector after each splice."
            "otdr" in lower || "test" in lower ->
                "Run the OTDR from the ONT end. Look for splice events — each should show less than 0.1 dB loss. " +
                    "Check for any unexpected reflections that could indicate a bad connector."
            "cable" in lower || "pull" in lower ->
                "When pulling cable, apply steady tension and don't exceed the cable's rated bend radius. " +
                    "Use lubricant if the conduit run is longer than 30m."
            "ont" in lower || "install" in lower ->
                "Mount the ONT at eye level near a power outlet. Connect the fiber patch cord carefully — " +
                    "the optical connector is fragile. Check the PON and LOS LEDs after powering on."
            "safety" in lower || "danger" in lower ->
                "Always wear safety glasses when working with fiber — glass shards are nearly invisible. " +
                    "Never look into the end of a lit fiber. Use the fiber waste container."
            "help" in lower ->
                "I can help with fiber splicing techniques, cable pulling tips, ONT installation, " +
                    "OTDR testing procedures, and safety guidelines. What do you need?"
            else ->
                "I understand your question. Based on the current task, make sure to follow the steps in order " +
                    "and document any issues you encounter. Let me know if you need specific guidance."
        }
    }

    fun getMockPhotoAnalysis(cameraFacing: String): String =
        if (cameraFacing == "back") {
            """
            |**Overall: Pass**
            |
            |• Fiber bend radius — GOOD: cable routed with gentle curves, no kinks visible
            |• Cable routing — GOOD: neat and organized along mounting surface
            |• Connection security — GOOD: connectors fully seated
            |• Labeling — CONCERN: cable label partially obscured, re-position for readability
            |• Work area — GOOD: clean, no loose fiber shards visible
            |
            |No safety hazards detected.
            """.trimMargin().trim()
        } else {
            """
            |• **Technician visible**: Yes
            |• **Environment**: indoor utility room, cable trays visible in background
            |• **PPE check**: safety glasses worn; no hard hat (indoor site — acceptable)
            |• **On-site confidence**: HIGH
            """.trimMargin().trim()
        }

    private fun createDropInstallWO(): WorkOrder = WorkOrder(
        id = "wo-drop-101",
        type = WorkOrderType.DROP_INSTALL,
        baselineId = "BL-2024-045",
        projectId = "proj-tlv-north",
        tier = 3,
        status = WorkOrderStatus.IN_PROGRESS,
        priority = 2,
        title = "Fiber drop install — 14 Rothschild Blvd",
        description = "Install fiber drop cable from distribution point DP-R14 to customer premises unit 5B. Single residential unit, existing conduit available.",
        location = WorkOrderLocation(
            address = "14 Rothschild Blvd, Tel Aviv",
            latitude = 32.0636,
            longitude = 34.7708,
            zoneId = "zone-tlv-central",
        ),
        requirements = listOf("Drop cable 50m", "ONT unit", "Fiber patch cord 3m", "Wall mount bracket"),
        assignment = WorkOrderAssignment(
            crewId = "alpha-crew-1",
            crewName = "Alpha Crew",
            assignedAt = "2026-01-28T08:00:00Z",
            scheduledDate = "2026-02-01",
        ),
        tasks = listOf(
            Task(
                id = "task-drop-1",
                workOrderId = "wo-drop-101",
                sequence = 1,
                type = "site_survey",
                label = "Site survey",
                description = "Verify conduit path from DP to customer premises",
                estimatedMinutes = 15,
                status = TaskStatus.COMPLETED,
                steps = listOf(
                    TaskStep("step-d1-1", 1, "Locate distribution point", "Find DP-R14 on building exterior", true),
                    TaskStep("step-d1-2", 2, "Check conduit condition", "Inspect conduit for blockages or damage", true),
                    TaskStep("step-d1-3", 3, "Confirm entry point", "Verify cable entry to unit 5B", true),
                ),
                checkpointRequired = true,
                voiceGuidance = "Start by locating the distribution point DP-R14 on the north side of the building.",
                spliceDetail = null,
                cablePullDetail = null,
            ),
            Task(
                id = "task-drop-2",
                workOrderId = "wo-drop-101",
                sequence = 2,
                type = "cable_pull",
                label = "Pull drop cable",
                description = "Pull fiber drop cable from DP-R14 through conduit to unit 5B",
                estimatedMinutes = 30,
                status = TaskStatus.IN_PROGRESS,
                steps = listOf(
                    TaskStep("step-d2-1", 1, "Prep cable end", "Strip and clean fiber cable end", false),
                    TaskStep("step-d2-2", 2, "Feed through conduit", "Pull cable from DP to premises", false),
                    TaskStep("step-d2-3", 3, "Secure cable", "Fix cable with clips every 30cm at entry point", false),
                    TaskStep("step-d2-4", 4, "Leave service loop", "Leave 1m service loop at both ends", false),
                ),
                checkpointRequired = false,
                voiceGuidance = "Carefully feed the cable through the conduit. Make sure to leave slack at both ends.",
                spliceDetail = null,
                cablePullDetail = CablePullDetail(
                    cableType = "G.657A2 single fiber",
                    lengthMeters = 42.0,
                    startPoint = "DP-R14",
                    endPoint = "Unit 5B wall plate",
                ),
            ),
            Task(
                id = "task-drop-3",
                workOrderId = "wo-drop-101",
                sequence = 3,
                type = "ont_install",
                label = "Install ONT",
                description = "Mount and connect ONT in customer premises",
                estimatedMinutes = 20,
                status = TaskStatus.PENDING,
                steps = listOf(
                    TaskStep("step-d3-1", 1, "Mount wall bracket", "Install ONT wall bracket near power outlet", false),
                    TaskStep("step-d3-2", 2, "Connect fiber", "Attach fiber patch cord from wall plate to ONT", false),
                    TaskStep("step-d3-3", 3, "Power on ONT", "Connect power adapter and verify LED status", false),
                    TaskStep("step-d3-4", 4, "Run light test", "Verify optical signal level within range", false),
                ),
                checkpointRequired = true,
                voiceGuidance = "Mount the ONT bracket at eye level near the closest power outlet.",
                spliceDetail = null,
                cablePullDetail = null,
            ),
            Task(
                id = "task-drop-4",
                workOrderId = "wo-drop-101",
                sequence = 4,
                type = "testing",
                label = "End-to-end test",
                description = "Test fiber connectivity from OLT to ONT",
                estimatedMinutes = 15,
                status = TaskStatus.PENDING,
                steps = listOf(
                    TaskStep("step-d4-1", 1, "OTDR test", "Run OTDR from ONT back to OLT", false),
                    TaskStep("step-d4-2", 2, "Power meter reading", "Record optical power level at ONT", false),
                    TaskStep("step-d4-3", 3, "Speed test", "Run bandwidth test to confirm service tier", false),
                ),
                checkpointRequired = true,
                voiceGuidance = null,
                spliceDetail = null,
                cablePullDetail = null,
            ),
            Task(
                id = "task-drop-5",
                workOrderId = "wo-drop-101",
                sequence = 5,
                type = "documentation",
                label = "Photo documentation",
                description = "Take photos of installation for records",
                estimatedMinutes = 10,
                status = TaskStatus.PENDING,
                steps = listOf(
                    TaskStep("step-d5-1", 1, "Photo: DP connection", "Photograph connection at distribution point", false),
                    TaskStep("step-d5-2", 2, "Photo: Cable route", "Photograph cable routing path", false),
                    TaskStep("step-d5-3", 3, "Photo: ONT install", "Photograph mounted ONT with LEDs visible", false),
                ),
                checkpointRequired = false,
                voiceGuidance = null,
                spliceDetail = null,
                cablePullDetail = null,
            ),
        ),
        createdAt = "2026-01-25T10:00:00Z",
        updatedAt = "2026-01-28T08:00:00Z",
    )

    private fun createSpliceWO(): WorkOrder = WorkOrder(
        id = "wo-splice-202",
        type = WorkOrderType.SPLICE,
        baselineId = "BL-2024-052",
        projectId = "proj-tlv-north",
        tier = 2,
        status = WorkOrderStatus.SCHEDULED,
        priority = 3,
        title = "Fiber splice — Junction box JB-7 Dizengoff",
        description = "Splice 12-fiber ribbon cable at junction box JB-7. Connect feeder F-012 to distribution D-034.",
        location = WorkOrderLocation(
            address = "142 Dizengoff St, Tel Aviv",
            latitude = 32.0804,
            longitude = 34.7745,
            zoneId = "zone-tlv-central",
        ),
        requirements = listOf("Fusion splicer", "Fiber cleaver", "Splice tray 12-port", "Heat shrink protectors x12"),
        assignment = WorkOrderAssignment(
            crewId = "alpha-crew-1",
            crewName = "Alpha Crew",
            assignedAt = "2026-01-29T08:00:00Z",
            scheduledDate = "2026-02-03",
        ),
        tasks = listOf(
            Task(
                id = "task-spl-1",
                workOrderId = "wo-splice-202",
                sequence = 1,
                type = "prep",
                label = "Open junction box",
                description = "Access junction box JB-7 and prepare work area",
                estimatedMinutes = 10,
                status = TaskStatus.PENDING,
                steps = listOf(
                    TaskStep("step-s1-1", 1, "Unlock enclosure", "Use key C-7 to open JB-7", false),
                    TaskStep("step-s1-2", 2, "Identify cables", "Locate feeder F-012 and distribution D-034", false),
                    TaskStep("step-s1-3", 3, "Prep workspace", "Clean splice tray area and set up splicer", false),
                ),
                checkpointRequired = false,
                voiceGuidance = "Junction box JB-7 is mounted on the utility pole at street level.",
                spliceDetail = null,
                cablePullDetail = null,
            ),
            Task(
                id = "task-spl-2",
                workOrderId = "wo-splice-202",
                sequence = 2,
                type = "splice",
                label = "Fusion splice — 12 fibers",
                description = "Splice all 12 fibers from F-012 to D-034 using fusion splicer",
                estimatedMinutes = 60,
                status = TaskStatus.PENDING,
                steps = listOf(
                    TaskStep("step-s2-1", 1, "Strip fiber 1-6", "Strip and clean first 6 fibers from both cables", false),
                    TaskStep("step-s2-2", 2, "Splice fiber 1-6", "Fusion splice fibers 1 through 6", false),
                    TaskStep("step-s2-3", 3, "Strip fiber 7-12", "Strip and clean remaining 6 fibers", false),
                    TaskStep("step-s2-4", 4, "Splice fiber 7-12", "Fusion splice fibers 7 through 12", false),
                    TaskStep("step-s2-5", 5, "Apply heat shrink", "Protect each splice with heat shrink tubing", false),
                    TaskStep("step-s2-6", 6, "Arrange in tray", "Place spliced fibers into splice tray", false),
                ),
                checkpointRequired = true,
                voiceGuidance = "Follow the color code sequence: blue, orange, green, brown, slate, white for fibers 1 through 6.",
                spliceDetail = SpliceDetail(
                    fiberCount = 12,
                    spliceType = "fusion",
                    enclosureId = "JB-7",
                ),
                cablePullDetail = null,
            ),
            Task(
                id = "task-spl-3",
                workOrderId = "wo-splice-202",
                sequence = 3,
                type = "testing",
                label = "OTDR verification",
                description = "Test each spliced fiber with OTDR for loss measurement",
                estimatedMinutes = 30,
                status = TaskStatus.PENDING,
                steps = listOf(
                    TaskStep("step-s3-1", 1, "Test fibers 1-6", "OTDR test each fiber, record splice loss", false),
                    TaskStep("step-s3-2", 2, "Test fibers 7-12", "OTDR test remaining fibers", false),
                    TaskStep("step-s3-3", 3, "Verify all under spec", "Confirm all splices under 0.1 dB loss", false),
                ),
                checkpointRequired = true,
                voiceGuidance = null,
                spliceDetail = null,
                cablePullDetail = null,
            ),
            Task(
                id = "task-spl-4",
                workOrderId = "wo-splice-202",
                sequence = 4,
                type = "documentation",
                label = "Close and document",
                description = "Secure junction box and photograph completed work",
                estimatedMinutes = 10,
                status = TaskStatus.PENDING,
                steps = listOf(
                    TaskStep("step-s4-1", 1, "Photo: Splice tray", "Photograph organized splice tray", false),
                    TaskStep("step-s4-2", 2, "Close enclosure", "Seal and lock junction box JB-7", false),
                    TaskStep("step-s4-3", 3, "Update label", "Attach updated cable label to enclosure", false),
                ),
                checkpointRequired = false,
                voiceGuidance = null,
                spliceDetail = null,
                cablePullDetail = null,
            ),
        ),
        createdAt = "2026-01-27T14:00:00Z",
        updatedAt = "2026-01-29T08:00:00Z",
    )

    private fun createDuctInstallWO(): WorkOrder = WorkOrder(
        id = "wo-duct-303",
        type = WorkOrderType.DUCT_INSTALL,
        baselineId = "BL-2024-060",
        projectId = "proj-tlv-north",
        tier = 1,
        status = WorkOrderStatus.PENDING,
        priority = 1,
        title = "Duct installation — Ben Yehuda to Frishman",
        description = "Install 2-way micro-duct from manhole MH-22 on Ben Yehuda St to manhole MH-25 on Frishman St. 120m route along existing trench.",
        location = WorkOrderLocation(
            address = "Ben Yehuda / Frishman, Tel Aviv",
            latitude = 32.0842,
            longitude = 34.7700,
            zoneId = "zone-tlv-central",
        ),
        requirements = listOf("Micro-duct 2-way 120m", "Duct couplers x8", "Pull rope 150m", "Manhole access key MH-22/25"),
        assignment = WorkOrderAssignment(
            crewId = "alpha-crew-1",
            crewName = "Alpha Crew",
            assignedAt = "2026-01-30T08:00:00Z",
            scheduledDate = "2026-02-05",
        ),
        tasks = listOf(
            Task(
                id = "task-duct-1",
                workOrderId = "wo-duct-303",
                sequence = 1,
                type = "site_survey",
                label = "Survey route",
                description = "Walk the route from MH-22 to MH-25, verify trench and access points",
                estimatedMinutes = 20,
                status = TaskStatus.PENDING,
                steps = listOf(
                    TaskStep("step-dc1-1", 1, "Access MH-22", "Open manhole MH-22 on Ben Yehuda", false),
                    TaskStep("step-dc1-2", 2, "Verify trench path", "Walk route, confirm trench is clear", false),
                    TaskStep("step-dc1-3", 3, "Access MH-25", "Open manhole MH-25 on Frishman", false),
                    TaskStep("step-dc1-4", 4, "Check clearance", "Confirm duct space available in both manholes", false),
                ),
                checkpointRequired = true,
                voiceGuidance = "Start at manhole MH-22, it's on the northeast corner of Ben Yehuda and Gordon.",
                spliceDetail = null,
                cablePullDetail = null,
            ),
            Task(
                id = "task-duct-2",
                workOrderId = "wo-duct-303",
                sequence = 2,
                type = "cable_pull",
                label = "Pull rope installation",
                description = "Feed pull rope through existing trench from MH-22 to MH-25",
                estimatedMinutes = 25,
                status = TaskStatus.PENDING,
                steps = listOf(
                    TaskStep("step-dc2-1", 1, "Attach rope to rod", "Fix pull rope to guide rod", false),
                    TaskStep("step-dc2-2", 2, "Feed through trench", "Push rod through trench, pull rope follows", false),
                    TaskStep("step-dc2-3", 3, "Secure at both ends", "Tie off rope at MH-22 and MH-25", false),
                ),
                checkpointRequired = false,
                voiceGuidance = null,
                spliceDetail = null,
                cablePullDetail = null,
            ),
            Task(
                id = "task-duct-3",
                workOrderId = "wo-duct-303",
                sequence = 3,
                type = "duct_install",
                label = "Install micro-duct",
                description = "Pull micro-duct through trench using pull rope",
                estimatedMinutes = 45,
                status = TaskStatus.PENDING,
                steps = listOf(
                    TaskStep("step-dc3-1", 1, "Attach duct to rope", "Secure micro-duct to pull rope with tape", false),
                    TaskStep("step-dc3-2", 2, "Pull duct through", "Slowly pull duct from MH-25 end", false),
                    TaskStep("step-dc3-3", 3, "Install couplers", "Connect duct sections with couplers at joints", false),
                    TaskStep("step-dc3-4", 4, "Seal ends", "Cap duct ends to prevent debris entry", false),
                    TaskStep("step-dc3-5", 5, "Test with mandrel", "Push mandrel through to verify clear path", false),
                ),
                checkpointRequired = true,
                voiceGuidance = "Pull slowly and steadily. Stop if you feel resistance — do not force the duct.",
                spliceDetail = null,
                cablePullDetail = CablePullDetail(
                    cableType = "2-way micro-duct 10/8mm",
                    lengthMeters = 120.0,
                    startPoint = "MH-22 Ben Yehuda",
                    endPoint = "MH-25 Frishman",
                ),
            ),
            Task(
                id = "task-duct-4",
                workOrderId = "wo-duct-303",
                sequence = 4,
                type = "testing",
                label = "Pressure test",
                description = "Air pressure test to verify duct integrity",
                estimatedMinutes = 15,
                status = TaskStatus.PENDING,
                steps = listOf(
                    TaskStep("step-dc4-1", 1, "Seal test end", "Cap one end with pressure fitting", false),
                    TaskStep("step-dc4-2", 2, "Pressurize", "Apply 100 kPa air pressure", false),
                    TaskStep("step-dc4-3", 3, "Wait and verify", "Hold for 10 min, verify no pressure drop", false),
                ),
                checkpointRequired = true,
                voiceGuidance = null,
                spliceDetail = null,
                cablePullDetail = null,
            ),
            Task(
                id = "task-duct-5",
                workOrderId = "wo-duct-303",
                sequence = 5,
                type = "documentation",
                label = "Documentation and close",
                description = "Photo documentation and close manholes",
                estimatedMinutes = 15,
                status = TaskStatus.PENDING,
                steps = listOf(
                    TaskStep("step-dc5-1", 1, "Photo: MH-22 duct entry", "Photograph duct entry in manhole MH-22", false),
                    TaskStep("step-dc5-2", 2, "Photo: MH-25 duct entry", "Photograph duct entry in manhole MH-25", false),
                    TaskStep("step-dc5-3", 3, "Close manholes", "Secure both manhole covers", false),
                    TaskStep("step-dc5-4", 4, "Update GIS", "Record duct route in field notes for GIS update", false),
                ),
                checkpointRequired = false,
                voiceGuidance = null,
                spliceDetail = null,
                cablePullDetail = null,
            ),
        ),
        createdAt = "2026-01-29T10:00:00Z",
        updatedAt = "2026-01-30T08:00:00Z",
    )
}
