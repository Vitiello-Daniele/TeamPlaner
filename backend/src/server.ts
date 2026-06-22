import cors from "cors";
import bcrypt from "bcryptjs";
import dotenv from "dotenv";
import express, { type NextFunction, type Request, type Response } from "express";
import jwt from "jsonwebtoken";
import { PrismaClient } from "@prisma/client";

dotenv.config();

const app = express();
const prisma = new PrismaClient();
const port = Number(process.env.PORT ?? 3000);
const jwtSecret = process.env.JWT_SECRET ?? "private-token";

app.use(cors());
app.use(express.json());

type AuthTokenPayload = {
  userId: string;
};

type AuthRequest = Request & {
  userId?: string;
};

function createToken(userId: string): string {
  return jwt.sign({ userId } satisfies AuthTokenPayload, jwtSecret, {
    expiresIn: "7d"
  });
}

function requireAuth(request: AuthRequest, response: Response, next: NextFunction): void {
  const authHeader = request.header("Authorization");
  const token = authHeader?.startsWith("Bearer ") ? authHeader.slice(7) : null;

  if (!token) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  try {
    const payload = jwt.verify(token, jwtSecret) as AuthTokenPayload;
    request.userId = payload.userId;
    next();
  } catch {
    response.status(401).json({ error: "Token ist ungültig" });
  }
}

function publicUser(user: { id: string; name: string; email: string }) {
  return {
    id: user.id,
    name: user.name,
    email: user.email
  };
}

function routeParam(value: string | string[] | undefined): string {
  if (Array.isArray(value)) {
    return value[0] ?? "";
  }

  return value ?? "";
}

function queryParam(value: unknown): string {
  if (Array.isArray(value)) {
    return String(value[0] ?? "");
  }

  return String(value ?? "");
}

function isValidEventDate(date: string): boolean {
  const match = /^(\d{2})\.(\d{2})\.(\d{4})$/.exec(date);

  if (!match) {
    return false;
  }

  const day = Number(match[1]);
  const month = Number(match[2]);
  const year = Number(match[3]);
  const parsedDate = new Date(Date.UTC(year, month - 1, day));

  return parsedDate.getUTCFullYear() === year &&
    parsedDate.getUTCMonth() === month - 1 &&
    parsedDate.getUTCDate() === day;
}

function isValidEventTime(time: string): boolean {
  const match = /^(\d{2}):(\d{2})$/.exec(time);

  if (!match) {
    return false;
  }

  const hour = Number(match[1]);
  const minute = Number(match[2]);

  return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
}

function createInviteCode(teamName: string): string {
  const base = teamName
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, "")
    .slice(0, 4)
    .padEnd(4, "X");
  const randomPart = Math.floor(1000 + Math.random() * 9000);

  return `${base}${randomPart}`;
}

async function createUniqueInviteCode(teamName: string): Promise<string> {
  for (let attempt = 0; attempt < 10; attempt += 1) {
    const inviteCode = createInviteCode(teamName);
    const existingTeam = await prisma.team.findUnique({
      where: { inviteCode }
    });

    if (!existingTeam) {
      return inviteCode;
    }
  }

  return `${Date.now()}`.slice(-8);
}

function mapTeam(team: {
  id: string;
  name: string;
  inviteCode: string;
  inviteCodeActive: boolean;
  members: Array<{
    id: string;
    role: string;
    user: {
      name: string;
    };
  }>;
}) {
  return {
    id: team.id,
    name: team.name,
    inviteCode: team.inviteCode,
    inviteCodeActive: team.inviteCodeActive,
    members: team.members.map((member) => ({
      id: member.id,
      name: member.user.name,
      role: member.role
    }))
  };
}

function mapRequest(request: {
  id: string;
  teamId: string;
  type: string;
  status: string;
  user: {
    name: string;
  };
}) {
  return {
    id: request.id,
    teamId: request.teamId,
    userName: request.user.name,
    type: request.type,
    status: request.status
  };
}

function mapDuty(duty: {
  id: string;
  teamId: string;
  type: string;
  title: string;
  description: string;
}) {
  return {
    id: duty.id,
    teamId: duty.teamId,
    type: duty.type,
    title: duty.title,
    description: duty.description
  };
}

function mapEvent(event: {
  id: string;
  teamId: string;
  type: string;
  title: string;
  date: string;
  time: string;
  location: string;
  dutyLinks: Array<{
    dutyId: string;
  }>;
  participations: Array<{
    memberId: string;
    status: string;
  }>;
}) {
  return {
    id: event.id,
    teamId: event.teamId,
    type: event.type,
    title: event.title,
    date: event.date,
    time: event.time,
    location: event.location,
    dutyIds: event.dutyLinks.map((link) => link.dutyId),
    teilnahmen: event.participations.map((participation) => ({
      memberId: participation.memberId,
      status: participation.status
    }))
  };
}

function mapAssignment(assignment: {
  id: string;
  eventId: string;
  dutyId: string;
  memberId: string;
}) {
  return {
    id: assignment.id,
    eventId: assignment.eventId,
    dutyId: assignment.dutyId,
    memberId: assignment.memberId
  };
}

async function canManageTeam(teamId: string, userId: string): Promise<boolean> {
  const membership = await prisma.teamMember.findUnique({
    where: {
      teamId_userId: {
        teamId,
        userId
      }
    }
  });

  return membership?.role === "Trainer";
}

async function isTeamMember(teamId: string, userId: string): Promise<boolean> {
  const membership = await prisma.teamMember.findUnique({
    where: {
      teamId_userId: {
        teamId,
        userId
      }
    }
  });

  return membership != null;
}

async function currentMembership(teamId: string, userId: string) {
  return prisma.teamMember.findUnique({
    where: {
      teamId_userId: {
        teamId,
        userId
      }
    }
  });
}

async function teamWithMembers(teamId: string) {
  return prisma.team.findUnique({
    where: { id: teamId },
    include: {
      members: {
        orderBy: {
          createdAt: "asc"
        },
        include: {
          user: {
            select: {
              name: true
            }
          }
        }
      }
    }
  });
}

function eventInclude() {
  return {
    dutyLinks: true,
    participations: true
  };
}

function createFairAssignments(
  events: Array<{
    id: string;
    dutyLinks: Array<{ dutyId: string }>;
  }>,
  members: Array<{
    id: string;
    user: {
      name: string;
    };
  }>,
  currentAssignments: Array<{
    id: string;
    eventId: string;
    dutyId: string;
    memberId: string;
  }>,
  replaceExisting: boolean
) {
  const result = replaceExisting ? [] : [...currentAssignments];
  const assignmentCounts = new Map<string, number>();

  result.forEach((assignment) => {
    assignmentCounts.set(assignment.memberId, (assignmentCounts.get(assignment.memberId) ?? 0) + 1);
  });

  events.forEach((event) => {
    event.dutyLinks.forEach((dutyLink) => {
      const alreadyAssigned = result.some((assignment) =>
        assignment.eventId === event.id && assignment.dutyId === dutyLink.dutyId
      );

      if (!alreadyAssigned) {
        const usedForEvent = new Set(
          result
            .filter((assignment) => assignment.eventId === event.id)
            .map((assignment) => assignment.memberId)
        );
        const preferredMembers = members.filter((member) => !usedForEvent.has(member.id));
        const candidates = preferredMembers.length > 0 ? preferredMembers : members;
        const nextMember = [...candidates]
          .sort((left, right) => {
            const countDiff = (assignmentCounts.get(left.id) ?? 0) - (assignmentCounts.get(right.id) ?? 0);

            if (countDiff !== 0) {
              return countDiff;
            }

            return left.user.name.localeCompare(right.user.name, "de");
          })
          [0];

        if (nextMember) {
          result.push({
            id: "",
            eventId: event.id,
            dutyId: dutyLink.dutyId,
            memberId: nextMember.id
          });
          assignmentCounts.set(nextMember.id, (assignmentCounts.get(nextMember.id) ?? 0) + 1);
        }
      }
    });
  });

  return result;
}

app.get("/health", (_request, response) => {
  response.json({
    status: "ok",
    service: "teamplaner-backend"
  });
});

app.post("/auth/register", async (request: Request, response: Response) => {
  const firstName = String(request.body.firstName ?? "").trim();
  const lastName = String(request.body.lastName ?? "").trim();
  const email = String(request.body.email ?? "").trim().toLowerCase();
  const password = String(request.body.password ?? "");
  const name = `${firstName} ${lastName}`.trim();
  const namePattern = /^[A-Za-zÄÖÜäöüß'-]+$/;
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  if (!firstName || !lastName || !email || !password) {
    response.status(400).json({
      error: "Vorname, Nachname, E-Mail und Passwort sind erforderlich"
    });
    return;
  }

  if (password.length < 6) {
    response.status(400).json({
      error: "Das Passwort braucht mindestens 6 Zeichen"
    });
    return;
  }

  if (!namePattern.test(firstName) || !namePattern.test(lastName)) {
    response.status(400).json({
      error: "Vorname und Nachname dürfen nur Buchstaben, Bindestrich und Apostroph enthalten"
    });
    return;
  }

  if (!emailPattern.test(email)) {
    response.status(400).json({
      error: "Bitte eine gültige E-Mail eingeben"
    });
    return;
  }

  const existingUser = await prisma.user.findUnique({
    where: { email }
  });

  if (existingUser) {
    response.status(409).json({ error: "E-Mail ist bereits registriert" });
    return;
  }

  const passwordHash = await bcrypt.hash(password, 10);
  const user = await prisma.user.create({
    data: {
      name,
      email,
      passwordHash
    }
  });
  const token = createToken(user.id);

  response.status(201).json({
    token,
    user: publicUser(user)
  });
});

app.post("/auth/login", async (request: Request, response: Response) => {
  const email = String(request.body.email ?? "").trim().toLowerCase();
  const password = String(request.body.password ?? "");

  if (!email || !password) {
    response.status(400).json({ error: "E-Mail und Passwort sind erforderlich" });
    return;
  }

  const user = await prisma.user.findUnique({
    where: { email }
  });

  if (!user) {
    response.status(401).json({ error: "Login fehlgeschlagen" });
    return;
  }

  const passwordMatches = await bcrypt.compare(password, user.passwordHash);

  if (!passwordMatches) {
    response.status(401).json({ error: "Login fehlgeschlagen" });
    return;
  }

  response.json({
    token: createToken(user.id),
    user: publicUser(user)
  });
});

app.get("/auth/me", requireAuth, async (request: AuthRequest, response: Response) => {
  const user = await prisma.user.findUnique({
    where: {
      id: request.userId
    }
  });

  if (!user) {
    response.status(404).json({ error: "Nutzer nicht gefunden" });
    return;
  }

  response.json({
    user: publicUser(user)
  });
});

app.get("/users", requireAuth, async (request: AuthRequest, response: Response) => {
  const search = queryParam(request.query.search).trim().toLowerCase();

  if (search.length < 2) {
    response.json({ users: [] });
    return;
  }

  const users = await prisma.user.findMany({
    orderBy: {
      name: "asc"
    },
    select: {
      id: true,
      name: true,
      email: true
    }
  });
  const filteredUsers = users
    .filter((user) =>
      user.name.toLowerCase().includes(search) ||
        user.email.toLowerCase().includes(search)
    )
    .slice(0, 8);

  response.json({ users: filteredUsers });
});

app.get("/teams", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  const memberships = await prisma.teamMember.findMany({
    where: { userId },
    include: {
      team: {
        include: {
          members: {
            orderBy: {
              createdAt: "asc"
            },
            include: {
              user: {
                select: {
                  name: true
                }
              }
            }
          }
        }
      }
    },
    orderBy: {
      createdAt: "asc"
    }
  });
  const teams = memberships.map((membership) => membership.team);
  const teamIds = teams.map((team) => team.id);
  const trainerTeamIds = memberships
    .filter((membership) => membership.role === "Trainer")
    .map((membership) => membership.teamId);

  const requests = await prisma.teamRequest.findMany({
    where: {
      status: "Open",
      OR: [
        { userId },
        {
          teamId: {
            in: trainerTeamIds
          }
        }
      ]
    },
    include: {
      user: {
        select: {
          name: true
        }
      }
    },
    orderBy: {
      createdAt: "asc"
    }
  });
  const duties = await prisma.duty.findMany({
    where: {
      teamId: {
        in: teamIds
      }
    },
    orderBy: {
      createdAt: "asc"
    }
  });
  const events = await prisma.teamEvent.findMany({
    where: {
      teamId: {
        in: teamIds
      }
    },
    include: eventInclude(),
    orderBy: {
      createdAt: "asc"
    }
  });
  const eventIds = events.map((event) => event.id);
  const assignments = await prisma.dutyAssignment.findMany({
    where: {
      eventId: {
        in: eventIds
      }
    }
  });

  response.json({
    teams: teams.map(mapTeam),
    events: events.map(mapEvent),
    duties: duties.map(mapDuty),
    assignments: assignments.map(mapAssignment),
    requests: requests.map(mapRequest)
  });
});

app.post("/teams", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;
  const name = String(request.body.name ?? "").trim();

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  if (!name) {
    response.status(400).json({ error: "Teamname ist erforderlich" });
    return;
  }

  const inviteCode = await createUniqueInviteCode(name);
  const team = await prisma.team.create({
    data: {
      name,
      inviteCode,
      inviteCodeActive: true,
      members: {
        create: {
          userId,
          role: "Trainer"
        }
      }
    },
    include: {
      members: {
        include: {
          user: {
            select: {
              name: true
            }
          }
        }
      }
    }
  });

  response.status(201).json({ team: mapTeam(team) });
});

app.post("/teams/join", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;
  const inviteCode = String(request.body.inviteCode ?? "").trim().toUpperCase();

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  if (!inviteCode) {
    response.status(400).json({ error: "Invite-Code ist erforderlich" });
    return;
  }

  const team = await prisma.team.findFirst({
    where: {
      inviteCode,
      inviteCodeActive: true
    }
  });

  if (!team) {
    response.status(404).json({ error: "Team wurde nicht gefunden" });
    return;
  }

  const membership = await prisma.teamMember.findUnique({
    where: {
      teamId_userId: {
        teamId: team.id,
        userId
      }
    }
  });

  if (membership) {
    response.status(409).json({ error: "Du bist bereits in diesem Team" });
    return;
  }

  const existingRequest = await prisma.teamRequest.findFirst({
    where: {
      teamId: team.id,
      userId,
      type: "JoinRequest",
      status: "Open"
    },
    include: {
      user: {
        select: {
          name: true
        }
      }
    }
  });

  if (existingRequest) {
    response.json({ request: mapRequest(existingRequest) });
    return;
  }

  const teamRequest = await prisma.teamRequest.create({
    data: {
      teamId: team.id,
      userId,
      type: "JoinRequest",
      status: "Open"
    },
    include: {
      user: {
        select: {
          name: true
        }
      }
    }
  });

  response.status(201).json({ request: mapRequest(teamRequest) });
});

app.post("/teams/:teamId/invites", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;
  const teamId = routeParam(request.params.teamId);
  const userQuery = String(request.body.user ?? "").trim().toLowerCase();

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  if (!(await canManageTeam(teamId, userId))) {
    response.status(403).json({ error: "Keine Berechtigung für dieses Team" });
    return;
  }

  if (!userQuery) {
    response.status(400).json({ error: "Nutzer ist erforderlich" });
    return;
  }

  const users = await prisma.user.findMany({
    select: {
      id: true,
      name: true,
      email: true
    }
  });
  const invitedUser = users.find((user) =>
    user.email.toLowerCase() === userQuery ||
      user.name.toLowerCase() === userQuery
  );

  if (!invitedUser) {
    response.status(404).json({ error: "Nutzer wurde nicht gefunden" });
    return;
  }

  const membership = await prisma.teamMember.findUnique({
    where: {
      teamId_userId: {
        teamId,
        userId: invitedUser.id
      }
    }
  });

  if (membership) {
    response.status(409).json({ error: "Nutzer ist bereits im Team" });
    return;
  }

  const existingInvite = await prisma.teamRequest.findFirst({
    where: {
      teamId,
      userId: invitedUser.id,
      type: "Invite",
      status: "Open"
    }
  });

  if (existingInvite) {
    response.status(409).json({ error: "Einladung ist bereits offen" });
    return;
  }

  const invite = await prisma.teamRequest.create({
    data: {
      teamId,
      userId: invitedUser.id,
      type: "Invite",
      status: "Open"
    },
    include: {
      user: {
        select: {
          name: true
        }
      }
    }
  });

  response.status(201).json({ request: mapRequest(invite) });
});

app.patch("/team-requests/:requestId", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;
  const requestId = routeParam(request.params.requestId);
  const status = String(request.body.status ?? "").trim();

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  if (status !== "Accepted" && status !== "Rejected") {
    response.status(400).json({ error: "Status ist ungültig" });
    return;
  }

  const teamRequest = await prisma.teamRequest.findUnique({
    where: { id: requestId },
    include: {
      user: {
        select: {
          name: true
        }
      }
    }
  });

  if (!teamRequest || teamRequest.status !== "Open") {
    response.status(404).json({ error: "Anfrage wurde nicht gefunden" });
    return;
  }

  const isOwnInvite = teamRequest.type === "Invite" && teamRequest.userId === userId;
  const isOwnJoinCancel = teamRequest.type === "JoinRequest" &&
    teamRequest.userId === userId &&
    status === "Rejected";
  const isTrainer = await canManageTeam(teamRequest.teamId, userId);

  if (!isOwnInvite && !isOwnJoinCancel && !isTrainer) {
    response.status(403).json({ error: "Keine Berechtigung für diese Anfrage" });
    return;
  }

  const updatedRequest = await prisma.teamRequest.update({
    where: { id: requestId },
    data: { status },
    include: {
      user: {
        select: {
          name: true
        }
      }
    }
  });

  if (status === "Accepted") {
    await prisma.teamMember.upsert({
      where: {
        teamId_userId: {
          teamId: teamRequest.teamId,
          userId: teamRequest.userId
        }
      },
      update: {},
      create: {
        teamId: teamRequest.teamId,
        userId: teamRequest.userId,
        role: "Member"
      }
    });
  }

  response.json({ request: mapRequest(updatedRequest) });
});

app.delete("/teams/:teamId/members/:memberId", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;
  const teamId = routeParam(request.params.teamId);
  const memberId = routeParam(request.params.memberId);

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  if (!(await canManageTeam(teamId, userId))) {
    response.status(403).json({ error: "Keine Berechtigung für dieses Team" });
    return;
  }

  const member = await prisma.teamMember.findUnique({
    where: { id: memberId }
  });

  if (!member || member.teamId !== teamId) {
    response.status(404).json({ error: "Mitglied wurde nicht gefunden" });
    return;
  }

  if (member.role === "Trainer") {
    response.status(400).json({ error: "Trainer kann nicht entfernt werden" });
    return;
  }

  await prisma.teamMember.delete({
    where: { id: memberId }
  });

  response.status(204).send();
});

app.patch("/teams/:teamId/invite-code", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;
  const teamId = routeParam(request.params.teamId);
  const action = String(request.body.action ?? "").trim();

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  if (!(await canManageTeam(teamId, userId))) {
    response.status(403).json({ error: "Keine Berechtigung für dieses Team" });
    return;
  }

  const team = await teamWithMembers(teamId);

  if (!team) {
    response.status(404).json({ error: "Team wurde nicht gefunden" });
    return;
  }

  const data = action === "deactivate"
    ? { inviteCodeActive: false }
    : {
        inviteCode: await createUniqueInviteCode(team.name),
        inviteCodeActive: true
      };

  const updatedTeam = await prisma.team.update({
    where: { id: teamId },
    data,
    include: {
      members: {
        include: {
          user: {
            select: {
              name: true
            }
          }
        }
      }
    }
  });

  response.json({ team: mapTeam(updatedTeam) });
});

app.post("/teams/:teamId/duties", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;
  const teamId = routeParam(request.params.teamId);
  const type = String(request.body.type ?? "").trim();
  const title = String(request.body.title ?? "").trim();
  const description = String(request.body.description ?? "").trim();

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  if (!(await canManageTeam(teamId, userId))) {
    response.status(403).json({ error: "Keine Berechtigung für dieses Team" });
    return;
  }

  if (!type || !title) {
    response.status(400).json({ error: "Diensttyp und Titel sind erforderlich" });
    return;
  }

  const duty = await prisma.duty.create({
    data: {
      teamId,
      type,
      title,
      description
    }
  });

  response.status(201).json({ duty: mapDuty(duty) });
});

app.delete("/duties/:dutyId", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;
  const dutyId = routeParam(request.params.dutyId);

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  const duty = await prisma.duty.findUnique({
    where: { id: dutyId }
  });

  if (!duty) {
    response.status(404).json({ error: "Dienst wurde nicht gefunden" });
    return;
  }

  if (!(await canManageTeam(duty.teamId, userId))) {
    response.status(403).json({ error: "Keine Berechtigung für dieses Team" });
    return;
  }

  await prisma.duty.delete({
    where: { id: dutyId }
  });

  response.status(204).send();
});

app.post("/teams/:teamId/events", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;
  const teamId = routeParam(request.params.teamId);
  const type = String(request.body.type ?? "").trim();
  const title = String(request.body.title ?? "").trim();
  const date = String(request.body.date ?? "").trim();
  const time = String(request.body.time ?? "").trim();
  const location = String(request.body.location ?? "").trim();
  const dutyIds: string[] = Array.isArray(request.body.dutyIds) ? request.body.dutyIds.map(String) : [];
  const participantIds: string[] = Array.isArray(request.body.participantIds) ? request.body.participantIds.map(String) : [];

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  if (!(await canManageTeam(teamId, userId))) {
    response.status(403).json({ error: "Keine Berechtigung für dieses Team" });
    return;
  }

  if (!type || !title || !date || !time || participantIds.length === 0) {
    response.status(400).json({ error: "Terminart, Titel, Datum, Uhrzeit und Teilnehmer sind erforderlich" });
    return;
  }

  if (!isValidEventDate(date) || !isValidEventTime(time)) {
    response.status(400).json({ error: "Datum oder Uhrzeit ist ungültig" });
    return;
  }

  const event = await prisma.teamEvent.create({
    data: {
      teamId,
      type,
      title,
      date,
      time,
      location,
      dutyLinks: {
        create: dutyIds.map((dutyId) => ({
          dutyId
        }))
      },
      participations: {
        create: participantIds.map((memberId) => ({
          memberId,
          status: "Offen"
        }))
      }
    },
    include: eventInclude()
  });

  response.status(201).json({ event: mapEvent(event) });
});

app.patch("/events/:eventId", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;
  const eventId = routeParam(request.params.eventId);

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  const existingEvent = await prisma.teamEvent.findUnique({
    where: { id: eventId },
    include: eventInclude()
  });

  if (!existingEvent) {
    response.status(404).json({ error: "Termin wurde nicht gefunden" });
    return;
  }

  const membership = await currentMembership(existingEvent.teamId, userId);

  if (!membership) {
    response.status(403).json({ error: "Keine Berechtigung für dieses Team" });
    return;
  }

  if (membership.role !== "Trainer") {
    const ownStatus = Array.isArray(request.body.teilnahmen)
      ? request.body.teilnahmen.find((item: { memberId?: unknown }) => String(item.memberId ?? "") === membership.id)
      : null;

    if (!ownStatus) {
      response.status(403).json({ error: "Nur der eigene Teilnahmestatus darf geändert werden" });
      return;
    }

    await prisma.eventParticipation.update({
      where: {
        eventId_memberId: {
          eventId,
          memberId: membership.id
        }
      },
      data: {
        status: String(ownStatus.status ?? "Offen")
      }
    });
  } else {
    const type = String(request.body.type ?? "").trim();
    const title = String(request.body.title ?? "").trim();
    const date = String(request.body.date ?? "").trim();
    const time = String(request.body.time ?? "").trim();
    const location = String(request.body.location ?? "").trim();
    const dutyIds: string[] = Array.isArray(request.body.dutyIds) ? request.body.dutyIds.map(String) : [];
    const teilnahmen: Array<{ memberId?: unknown; status?: unknown }> = Array.isArray(request.body.teilnahmen)
      ? request.body.teilnahmen
      : [];

    if (!type || !title || !date || !time || teilnahmen.length === 0) {
      response.status(400).json({ error: "Terminart, Titel, Datum, Uhrzeit und Teilnehmer sind erforderlich" });
      return;
    }

    if (!isValidEventDate(date) || !isValidEventTime(time)) {
      response.status(400).json({ error: "Datum oder Uhrzeit ist ungültig" });
      return;
    }

    await prisma.teamEvent.update({
      where: { id: eventId },
      data: {
        type,
        title,
        date,
        time,
        location
      }
    });
    await prisma.eventDuty.deleteMany({
      where: { eventId }
    });
    await prisma.eventDuty.createMany({
      data: dutyIds.map((dutyId) => ({
        eventId,
        dutyId
      }))
    });
    await prisma.eventParticipation.deleteMany({
      where: { eventId }
    });
    await prisma.eventParticipation.createMany({
      data: teilnahmen.map((teilnahme) => ({
        eventId,
        memberId: String(teilnahme.memberId ?? ""),
        status: String(teilnahme.status ?? "Offen")
      }))
    });
  }

  const updatedEvent = await prisma.teamEvent.findUnique({
    where: { id: eventId },
    include: eventInclude()
  });

  response.json({ event: updatedEvent ? mapEvent(updatedEvent) : null });
});

app.delete("/events/:eventId", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;
  const eventId = routeParam(request.params.eventId);

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  const event = await prisma.teamEvent.findUnique({
    where: { id: eventId }
  });

  if (!event) {
    response.status(404).json({ error: "Termin wurde nicht gefunden" });
    return;
  }

  if (!(await canManageTeam(event.teamId, userId))) {
    response.status(403).json({ error: "Keine Berechtigung für dieses Team" });
    return;
  }

  await prisma.teamEvent.delete({
    where: { id: eventId }
  });

  response.status(204).send();
});

app.post("/assignments", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;
  const eventId = String(request.body.eventId ?? "").trim();
  const dutyId = String(request.body.dutyId ?? "").trim();
  const memberId = String(request.body.memberId ?? "").trim();

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  if (!eventId || !dutyId || !memberId) {
    response.status(400).json({ error: "Termin, Dienst und Mitglied sind erforderlich" });
    return;
  }

  const event = await prisma.teamEvent.findUnique({
    where: { id: eventId },
    include: {
      dutyLinks: true
    }
  });

  if (!event) {
    response.status(404).json({ error: "Termin wurde nicht gefunden" });
    return;
  }

  if (!(await canManageTeam(event.teamId, userId))) {
    response.status(403).json({ error: "Keine Berechtigung für dieses Team" });
    return;
  }

  const hasDuty = event.dutyLinks.some((link) => link.dutyId === dutyId);
  const member = await prisma.teamMember.findUnique({
    where: { id: memberId }
  });

  if (!hasDuty || !member || member.teamId !== event.teamId) {
    response.status(400).json({ error: "Dienst oder Mitglied passt nicht zu diesem Termin" });
    return;
  }

  const assignment = await prisma.dutyAssignment.upsert({
    where: {
      eventId_dutyId: {
        eventId,
        dutyId
      }
    },
    update: {
      memberId
    },
    create: {
      eventId,
      dutyId,
      memberId
    }
  });

  response.status(201).json({ assignment: mapAssignment(assignment) });
});

app.delete("/assignments/:assignmentId", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;
  const assignmentId = routeParam(request.params.assignmentId);

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  const assignment = await prisma.dutyAssignment.findUnique({
    where: { id: assignmentId },
    include: {
      event: true
    }
  });

  if (!assignment) {
    response.status(404).json({ error: "Zuweisung wurde nicht gefunden" });
    return;
  }

  if (!(await canManageTeam(assignment.event.teamId, userId))) {
    response.status(403).json({ error: "Keine Berechtigung für dieses Team" });
    return;
  }

  await prisma.dutyAssignment.delete({
    where: { id: assignmentId }
  });

  response.status(204).send();
});

app.post("/teams/:teamId/assignments/fair", requireAuth, async (request: AuthRequest, response: Response) => {
  const userId = request.userId;
  const teamId = routeParam(request.params.teamId);
  const replaceExisting = Boolean(request.body.replaceExisting);

  if (!userId) {
    response.status(401).json({ error: "Nicht angemeldet" });
    return;
  }

  if (!(await canManageTeam(teamId, userId))) {
    response.status(403).json({ error: "Keine Berechtigung für dieses Team" });
    return;
  }

  const members = await prisma.teamMember.findMany({
    where: { teamId },
    include: {
      user: {
        select: {
          name: true
        }
      }
    }
  });
  const events = await prisma.teamEvent.findMany({
    where: { teamId },
    include: {
      dutyLinks: true
    },
    orderBy: {
      createdAt: "asc"
    }
  });
  const eventIds = events.map((event) => event.id);
  const currentAssignments = await prisma.dutyAssignment.findMany({
    where: {
      eventId: {
        in: eventIds
      }
    }
  });
  const nextAssignments = createFairAssignments(
    events,
    members,
    currentAssignments,
    replaceExisting
  );

  if (replaceExisting) {
    await prisma.dutyAssignment.deleteMany({
      where: {
        eventId: {
          in: eventIds
        }
      }
    });
  }

  for (const assignment of nextAssignments) {
    await prisma.dutyAssignment.upsert({
      where: {
        eventId_dutyId: {
          eventId: assignment.eventId,
          dutyId: assignment.dutyId
        }
      },
      update: {
        memberId: assignment.memberId
      },
      create: {
        eventId: assignment.eventId,
        dutyId: assignment.dutyId,
        memberId: assignment.memberId
      }
    });
  }

  const assignments = await prisma.dutyAssignment.findMany({
    where: {
      eventId: {
        in: eventIds
      }
    }
  });

  response.json({ assignments: assignments.map(mapAssignment) });
});

app.listen(port, () => {
  console.log(`TeamPlaner Backend läuft auf Port ${port}`);
});
