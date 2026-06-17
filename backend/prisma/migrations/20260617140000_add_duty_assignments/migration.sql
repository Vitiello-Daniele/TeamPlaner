CREATE TABLE "DutyAssignment" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "eventId" TEXT NOT NULL,
    "dutyId" TEXT NOT NULL,
    "memberId" TEXT NOT NULL,
    CONSTRAINT "DutyAssignment_eventId_fkey" FOREIGN KEY ("eventId") REFERENCES "TeamEvent" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "DutyAssignment_eventId_dutyId_fkey" FOREIGN KEY ("eventId", "dutyId") REFERENCES "EventDuty" ("eventId", "dutyId") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "DutyAssignment_memberId_fkey" FOREIGN KEY ("memberId") REFERENCES "TeamMember" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE UNIQUE INDEX "DutyAssignment_eventId_dutyId_key" ON "DutyAssignment"("eventId", "dutyId");
