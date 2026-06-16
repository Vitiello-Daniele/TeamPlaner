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

app.get("/health", (_request, response) => {
  response.json({
    status: "ok",
    service: "teamplaner-backend"
  });
});

app.post("/auth/register", async (request: Request, response: Response) => {
  const name = String(request.body.name ?? "").trim();
  const email = String(request.body.email ?? "").trim().toLowerCase();
  const password = String(request.body.password ?? "");

  if (!name || !email || password.length < 6) {
    response.status(400).json({
      error: "Name, E-Mail und Passwort mit mindestens 6 Zeichen sind erforderlich"
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

app.listen(port, () => {
  console.log(`TeamPlaner Backend läuft auf Port ${port}`);
});
