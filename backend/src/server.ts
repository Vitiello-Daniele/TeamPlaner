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

app.get("/users", async (_request, response) => {
  const users = await prisma.user.findMany({
    orderBy: {
      createdAt: "desc"
    },
    select: {
      id: true,
      name: true,
      email: true
    }
  });

  response.json({ users });
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

  response.json({
    teams: teams.map(mapTeam),
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
  const isTrainer = await canManageTeam(teamRequest.teamId, userId);

  if (!isOwnInvite && !isTrainer) {
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

app.listen(port, () => {
  console.log(`TeamPlaner Backend läuft auf Port ${port}`);
});
