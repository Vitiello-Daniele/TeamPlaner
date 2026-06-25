import test from "node:test";
import assert from "node:assert/strict";
import { isPastEventDate, isValidEventDate, isValidEventTime } from "../src/validation.js";

test("validiert echte Kalenderdaten", () => {
  assert.equal(isValidEventDate("25.06.2026"), true);
  assert.equal(isValidEventDate("31.02.2026"), false);
  assert.equal(isValidEventDate("2026-06-25"), false);
});

test("validiert Uhrzeiten im 24-Stunden-Format", () => {
  assert.equal(isValidEventTime("18:30"), true);
  assert.equal(isValidEventTime("24:00"), false);
  assert.equal(isValidEventTime("18:75"), false);
});

test("erkennt Termine in der Vergangenheit", () => {
  const today = new Date(Date.UTC(2026, 5, 24));

  assert.equal(isPastEventDate("23.06.2026", today), true);
  assert.equal(isPastEventDate("24.06.2026", today), false);
  assert.equal(isPastEventDate("25.06.2026", today), false);
});
